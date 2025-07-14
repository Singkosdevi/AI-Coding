"""
BTC期货交易策略 - 改进版MA20/MA50均线系统
"""

import pandas as pd
import numpy as np
import yfinance as yf
import matplotlib.pyplot as plt
import warnings
warnings.filterwarnings('ignore')

def get_btc_data(start_date='2020-01-01', end_date=None):
    """获取BTC数据"""
    if end_date is None:
        end_date = pd.Timestamp.now().strftime('%Y-%m-%d')
    btc = yf.download('BTC-USD', start=start_date, end=end_date)
    return btc

def calculate_ma_signals_improved(data):
    """计算改进的MA信号 - 使用MA20和MA50"""
    df = data.copy()
    
    # 计算移动平均线
    df['MA20'] = df['Close'].rolling(window=20).mean()
    df['MA50'] = df['Close'].rolling(window=50).mean()
    df['MA120'] = df['Close'].rolling(window=120).mean()  # 作为趋势过滤器
    
    # 计算RSI
    delta = df['Close'].diff()
    gain = (delta.where(delta > 0, 0)).rolling(window=14).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=14).mean()
    rs = gain / loss
    df['RSI'] = 100 - (100 / (1 + rs))
    
    # 计算MACD
    exp1 = df['Close'].ewm(span=12).mean()
    exp2 = df['Close'].ewm(span=26).mean()
    df['MACD'] = exp1 - exp2
    df['MACD_Signal'] = df['MACD'].ewm(span=9).mean()
    df['MACD_Histogram'] = df['MACD'] - df['MACD_Signal']
    
    # 计算信号
    df['MA_Signal'] = 0
    
    # 短期均线信号 (MA20 vs MA50)
    df['MA20_prev'] = df['MA20'].shift(1)
    df['MA50_prev'] = df['MA50'].shift(1)
    
    # 使用标量值进行信号计算，避免Series问题
    for i in range(1, len(df)):
        try:
            # 获取标量值
            ma20_current = df.iloc[i]['MA20']
            ma50_current = df.iloc[i]['MA50']
            ma20_prev = df.iloc[i-1]['MA20']
            ma50_prev = df.iloc[i-1]['MA50']
            ma120_current = df.iloc[i]['MA120']
            close_current = df.iloc[i]['Close']
            
            # 检查是否为NaN
            if (np.isnan(ma20_current) or np.isnan(ma50_current) or 
                np.isnan(ma20_prev) or np.isnan(ma50_prev) or 
                np.isnan(ma120_current)):
                continue
                
            # 金叉检测
            if (ma20_current > ma50_current and 
                ma20_prev <= ma50_prev and
                close_current > ma120_current):
                df.iloc[i, df.columns.get_loc('MA_Signal')] = 1
                
            # 死叉检测    
            elif (ma20_current < ma50_current and 
                  ma20_prev >= ma50_prev and
                  close_current < ma120_current):
                df.iloc[i, df.columns.get_loc('MA_Signal')] = -1
        except:
            continue
    
    # 多重过滤条件
    df['Filtered_Signal'] = df['MA_Signal'].copy()
    
    # RSI过滤
    df.loc[(df['MA_Signal'] == 1) & (df['RSI'] > 75), 'Filtered_Signal'] = 0
    df.loc[(df['MA_Signal'] == -1) & (df['RSI'] < 25), 'Filtered_Signal'] = 0
    
    # MACD确认
    df.loc[(df['MA_Signal'] == 1) & (df['MACD'] < df['MACD_Signal']), 'Filtered_Signal'] = 0
    df.loc[(df['MA_Signal'] == -1) & (df['MACD'] > df['MACD_Signal']), 'Filtered_Signal'] = 0
    
    return df.dropna()

def backtest_strategy_improved(data, initial_capital=100000, stop_loss=0.03, take_profit=0.08):
    """改进的回测策略"""
    df = data.copy()
    
    # 初始化
    capital = initial_capital
    position = 0  # 0: 无持仓, 1: 多头, -1: 空头
    entry_price = 0
    trades = []
    equity_curve = [initial_capital]
    
    for i in range(len(df)):
        current_price = float(df.iloc[i]['Close'])
        signal_value = df.iloc[i]['Filtered_Signal']
        try:
            signal = float(signal_value)
        except (TypeError, ValueError):
            signal = 0
        if np.isnan(signal):
            signal = 0
        date = df.index[i]
        
        # 风险管理
        if position != 0:
            if position == 1:  # 多头持仓
                pnl_ratio = (current_price - entry_price) / entry_price
                if pnl_ratio <= -stop_loss or pnl_ratio >= take_profit:
                    # 平仓
                    pnl = pnl_ratio * capital
                    capital += pnl
                    trades.append({
                        'date': date,
                        'action': 'CLOSE_LONG',
                        'price': current_price,
                        'entry_price': entry_price,
                        'pnl': pnl,
                        'pnl_ratio': pnl_ratio,
                        'reason': 'Risk Management'
                    })
                    position = 0
                    entry_price = 0
            elif position == -1:  # 空头持仓
                pnl_ratio = (entry_price - current_price) / entry_price
                if pnl_ratio <= -stop_loss or pnl_ratio >= take_profit:
                    # 平仓
                    pnl = pnl_ratio * capital
                    capital += pnl
                    trades.append({
                        'date': date,
                        'action': 'CLOSE_SHORT',
                        'price': current_price,
                        'entry_price': entry_price,
                        'pnl': pnl,
                        'pnl_ratio': pnl_ratio,
                        'reason': 'Risk Management'
                    })
                    position = 0
                    entry_price = 0
        
        # 处理信号
        if signal == 1 and position <= 0:  # 买入信号
            if position == -1:  # 先平空头
                pnl_ratio = (entry_price - current_price) / entry_price
                pnl = pnl_ratio * capital
                capital += pnl
                trades.append({
                    'date': date,
                    'action': 'CLOSE_SHORT',
                    'price': current_price,
                    'entry_price': entry_price,
                    'pnl': pnl,
                    'pnl_ratio': pnl_ratio,
                    'reason': 'Signal Close'
                })
            # 开多头
            position = 1
            entry_price = current_price
            trades.append({
                'date': date,
                'action': 'OPEN_LONG',
                'price': current_price,
                'reason': 'MA20/50 Golden Cross'
            })
            
        elif signal == -1 and position >= 0:  # 卖出信号
            if position == 1:  # 先平多头
                pnl_ratio = (current_price - entry_price) / entry_price
                pnl = pnl_ratio * capital
                capital += pnl
                trades.append({
                    'date': date,
                    'action': 'CLOSE_LONG',
                    'price': current_price,
                    'entry_price': entry_price,
                    'pnl': pnl,
                    'pnl_ratio': pnl_ratio,
                    'reason': 'Signal Close'
                })
            # 开空头
            position = -1
            entry_price = current_price
            trades.append({
                'date': date,
                'action': 'OPEN_SHORT',
                'price': current_price,
                'reason': 'MA20/50 Death Cross'
            })
        
        # 更新权益曲线
        if position != 0:
            if position == 1:
                unrealized_pnl = (current_price - entry_price) / entry_price * capital
            else:
                unrealized_pnl = (entry_price - current_price) / entry_price * capital
            current_equity = capital + unrealized_pnl
        else:
            current_equity = capital
            
        equity_curve.append(current_equity)
    
    # 最后平仓
    if position != 0:
        final_price = df.iloc[-1]['Close']
        if position == 1:
            pnl_ratio = (final_price - entry_price) / entry_price
        else:
            pnl_ratio = (entry_price - final_price) / entry_price
        pnl = pnl_ratio * capital
        capital += pnl
        trades.append({
            'date': df.index[-1],
            'action': f'CLOSE_{"LONG" if position == 1 else "SHORT"}',
            'price': final_price,
            'entry_price': entry_price,
            'pnl': pnl,
            'pnl_ratio': pnl_ratio,
            'reason': 'Final Close'
        })
    
    return {
        'final_capital': capital,
        'trades': trades,
        'equity_curve': equity_curve[1:],  # 去掉初始值
        'data': df
    }

def analyze_results_improved(results, initial_capital):
    """分析改进版回测结果"""
    trades = results['trades']
    final_capital = results['final_capital']
    equity_curve = results['equity_curve']
    data = results['data']
    
    # 基本统计
    total_return = (final_capital - initial_capital) / initial_capital
    total_days = len(data)
    
    print("=" * 60)
    print("BTC期货MA20/MA50改进策略回测结果")
    print("=" * 60)
    print(f"回测期间: {data.index[0].strftime('%Y-%m-%d')} 至 {data.index[-1].strftime('%Y-%m-%d')}")
    print(f"总天数: {total_days}")
    print(f"初始资金: ${initial_capital:,.2f}")
    print(f"最终资金: ${final_capital:,.2f}")
    print(f"总收益率: {total_return:.2%}")
    
    if total_days > 0:
        annualized_return = (1 + total_return) ** (365 / total_days) - 1
        print(f"年化收益率: {annualized_return:.2%}")
    
    # 交易统计
    if trades:
        close_trades = [t for t in trades if 'CLOSE' in t['action'] and 'pnl' in t]
        if close_trades:
            win_trades = [t for t in close_trades if t['pnl'] > 0]
            lose_trades = [t for t in close_trades if t['pnl'] < 0]
            
            win_rate = len(win_trades) / len(close_trades) if close_trades else 0
            avg_win = np.mean([t['pnl'] for t in win_trades]) if win_trades else 0
            avg_loss = np.mean([t['pnl'] for t in lose_trades]) if lose_trades else 0
            
            total_pnl = sum([t['pnl'] for t in close_trades])
            
            print(f"\n交易统计:")
            print(f"总交易次数: {len(close_trades)}")
            print(f"盈利交易: {len(win_trades)}")
            print(f"亏损交易: {len(lose_trades)}")
            print(f"胜率: {win_rate:.2%}")
            print(f"平均盈利: ${avg_win:.2f}")
            print(f"平均亏损: ${avg_loss:.2f}")
            print(f"总盈亏: ${total_pnl:.2f}")
            if avg_loss != 0:
                print(f"盈亏比: {abs(avg_win/avg_loss):.2f}")
    
    # 风险指标
    if len(equity_curve) > 1:
        equity_series = pd.Series(equity_curve)
        returns = equity_series.pct_change().dropna()
        if len(returns) > 0:
            volatility = returns.std() * np.sqrt(365)
            max_drawdown = calculate_max_drawdown(equity_series)
            
            print(f"\n风险指标:")
            print(f"波动率: {volatility:.2%}")
            if volatility > 0 and 'annualized_return' in locals():
                sharpe_ratio = annualized_return / volatility
                print(f"夏普比率: {sharpe_ratio:.2f}")
            print(f"最大回撤: {max_drawdown:.2%}")
    
    # 基准比较
    btc_return = float((data['Close'].iloc[-1] - data['Close'].iloc[0]) / data['Close'].iloc[0])
    print(f"\n基准比较:")
    print(f"BTC买入持有收益率: {btc_return:.2%}")
    print(f"策略超额收益: {total_return - btc_return:.2%}")
    
    return results

def calculate_max_drawdown(equity_curve):
    """计算最大回撤"""
    peak = equity_curve.expanding().max()
    drawdown = (equity_curve - peak) / peak
    return drawdown.min()

def main():
    """主函数"""
    print("正在初始化BTC MA20/MA50改进策略...")
    
    # 参数设置
    initial_capital = 100000  # 10万美元
    stop_loss = 0.03         # 3%止损
    take_profit = 0.08       # 8%止盈
    
    print("正在获取BTC历史数据...")
    try:
        # 获取数据
        btc_data = get_btc_data(start_date='2020-01-01')
        print(f"成功获取数据: {len(btc_data)} 个交易日")
        
        print("计算技术指标...")
        data_with_signals = calculate_ma_signals_improved(btc_data)
        
        print("开始回测...")
        results = backtest_strategy_improved(data_with_signals, initial_capital, stop_loss, take_profit)
        
        print("分析结果...")
        analyze_results_improved(results, initial_capital)
        
        # 保存交易记录
        if results['trades']:
            trades_df = pd.DataFrame(results['trades'])
            trades_df.to_csv('btc_ma_trades_improved.csv', index=False)
            print(f"\n交易记录已保存至: btc_ma_trades_improved.csv")
            
            # 显示前10笔交易
            print(f"\n前10笔交易预览:")
            print(trades_df.head(10).to_string())
        
        # 保存详细数据
        data_with_signals['Equity'] = results['equity_curve']
        data_with_signals.to_csv('btc_ma_strategy_data_improved.csv')
        print("详细数据已保存至: btc_ma_strategy_data_improved.csv")
        
    except Exception as e:
        print(f"运行出错: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()