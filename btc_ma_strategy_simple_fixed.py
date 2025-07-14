"""
BTC期货交易策略 - 简化修复版MA20/MA50均线系统
彻底解决Series布尔运算问题
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

def calculate_indicators(data):
    """计算技术指标"""
    df = data.copy()
    
    # 计算移动平均线
    df['MA20'] = df['Close'].rolling(window=20).mean()
    df['MA50'] = df['Close'].rolling(window=50).mean()
    
    # 计算RSI
    delta = df['Close'].diff()
    gain = delta.where(delta > 0, 0).rolling(window=14).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=14).mean()
    rs = gain / loss
    df['RSI'] = 100 - (100 / (1 + rs))
    
    # 删除NaN值
    df = df.dropna()
    
    # 计算交易信号 - 使用数值方法避免Series问题
    df['Signal'] = 0
    
    for i in range(1, len(df)):
        try:
            # 获取当前和前一天的MA值，确保是标量
            ma20_current = float(df['MA20'].iloc[i])
            ma50_current = float(df['MA50'].iloc[i])
            ma20_prev = float(df['MA20'].iloc[i-1])
            ma50_prev = float(df['MA50'].iloc[i-1])
            rsi_current = float(df['RSI'].iloc[i])
            
            # 金叉检测 - MA20从下方穿越MA50向上
            if (ma20_current > ma50_current and ma20_prev <= ma50_prev and 
                rsi_current < 80):  # RSI过滤
                df.iloc[i, df.columns.get_loc('Signal')] = 1
            
            # 死叉检测 - MA20从上方穿越MA50向下  
            elif (ma20_current < ma50_current and ma20_prev >= ma50_prev and 
                  rsi_current > 20):  # RSI过滤
                df.iloc[i, df.columns.get_loc('Signal')] = -1
                
        except (ValueError, TypeError, IndexError):
            continue
    
    return df

def backtest_strategy(data, initial_capital=100000, stop_loss=0.05, take_profit=0.15):
    """回测策略"""
    df = data.copy()
    
    # 初始化
    capital = float(initial_capital)
    position = 0  # 0: 无持仓, 1: 多头, -1: 空头
    entry_price = 0.0
    trades = []
    equity_curve = []
    
    for i in range(len(df)):
        # 获取标量值
        current_price = float(df['Close'].iloc[i])
        signal = int(df['Signal'].iloc[i])
        date = df.index[i].strftime('%Y-%m-%d')
        
        # 风险管理
        if position != 0:
            if position == 1:  # 多头持仓
                pnl_ratio = (current_price - entry_price) / entry_price
                if pnl_ratio <= -stop_loss or pnl_ratio >= take_profit:
                    # 平多头
                    pnl = pnl_ratio * capital
                    capital += pnl
                    trades.append({
                        'date': date,
                        'action': 'CLOSE_LONG',
                        'price': current_price,
                        'entry_price': entry_price,
                        'pnl': pnl,
                        'pnl_ratio': pnl_ratio,
                        'capital': capital,
                        'reason': 'Stop Loss' if pnl_ratio <= -stop_loss else 'Take Profit'
                    })
                    position = 0
                    entry_price = 0.0
                    
            elif position == -1:  # 空头持仓
                pnl_ratio = (entry_price - current_price) / entry_price
                if pnl_ratio <= -stop_loss or pnl_ratio >= take_profit:
                    # 平空头
                    pnl = pnl_ratio * capital
                    capital += pnl
                    trades.append({
                        'date': date,
                        'action': 'CLOSE_SHORT',
                        'price': current_price,
                        'entry_price': entry_price,
                        'pnl': pnl,
                        'pnl_ratio': pnl_ratio,
                        'capital': capital,
                        'reason': 'Stop Loss' if pnl_ratio <= -stop_loss else 'Take Profit'
                    })
                    position = 0
                    entry_price = 0.0
        
        # 处理交易信号
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
                    'capital': capital,
                    'reason': 'Signal Reversal'
                })
            # 开多头
            position = 1
            entry_price = current_price
            trades.append({
                'date': date,
                'action': 'OPEN_LONG',
                'price': current_price,
                'capital': capital,
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
                    'capital': capital,
                    'reason': 'Signal Reversal'
                })
            # 开空头
            position = -1
            entry_price = current_price
            trades.append({
                'date': date,
                'action': 'OPEN_SHORT',
                'price': current_price,
                'capital': capital,
                'reason': 'MA20/50 Death Cross'
            })
        
        # 计算当前权益
        if position != 0:
            if position == 1:  # 多头
                unrealized_pnl = (current_price - entry_price) / entry_price * capital
            else:  # 空头
                unrealized_pnl = (entry_price - current_price) / entry_price * capital
            current_equity = capital + unrealized_pnl
        else:
            current_equity = capital
            
        equity_curve.append(current_equity)
    
    # 最后平仓
    if position != 0:
        final_price = float(df['Close'].iloc[-1])
        if position == 1:
            pnl_ratio = (final_price - entry_price) / entry_price
        else:
            pnl_ratio = (entry_price - final_price) / entry_price
        pnl = pnl_ratio * capital
        capital += pnl
        trades.append({
            'date': df.index[-1].strftime('%Y-%m-%d'),
            'action': f'CLOSE_{"LONG" if position == 1 else "SHORT"}',
            'price': final_price,
            'entry_price': entry_price,
            'pnl': pnl,
            'pnl_ratio': pnl_ratio,
            'capital': capital,
            'reason': 'Final Close'
        })
    
    return {
        'final_capital': capital,
        'trades': trades,
        'equity_curve': equity_curve,
        'data': df
    }

def analyze_results(results, initial_capital):
    """分析回测结果"""
    trades = results['trades']
    final_capital = results['final_capital']
    equity_curve = results['equity_curve']
    data = results['data']
    
    # 基本统计
    total_return = (final_capital - initial_capital) / initial_capital
    total_days = len(data)
    
    print("=" * 70)
    print("BTC期货MA20/MA50简化修复版策略回测结果")
    print("=" * 70)
    print(f"回测期间: {data.index[0].strftime('%Y-%m-%d')} 至 {data.index[-1].strftime('%Y-%m-%d')}")
    print(f"总天数: {total_days}")
    print(f"初始资金: ${initial_capital:,.2f}")
    print(f"最终资金: ${final_capital:,.2f}")
    print(f"总收益: ${final_capital - initial_capital:,.2f}")
    print(f"总收益率: {total_return:.2%}")
    
    if total_days > 0:
        annualized_return = (1 + total_return) ** (365 / total_days) - 1
        print(f"年化收益率: {annualized_return:.2%}")
    
    # 信号统计
    signals = data['Signal'].value_counts()
    print(f"\n信号统计:")
    print(f"总信号数: {len(data[data['Signal'] != 0])}")
    if 1 in signals:
        print(f"买入信号: {signals[1]}")
    if -1 in signals:
        print(f"卖出信号: {signals[-1]}")
    
    # 交易统计
    if trades:
        open_trades = [t for t in trades if 'OPEN' in t['action']]
        close_trades = [t for t in trades if 'CLOSE' in t['action'] and 'pnl' in t]
        
        print(f"\n交易统计:")
        print(f"开仓次数: {len(open_trades)}")
        print(f"平仓次数: {len(close_trades)}")
        
        if close_trades:
            win_trades = [t for t in close_trades if t['pnl'] > 0]
            lose_trades = [t for t in close_trades if t['pnl'] <= 0]
            
            win_rate = len(win_trades) / len(close_trades) if close_trades else 0
            avg_win = np.mean([t['pnl'] for t in win_trades]) if win_trades else 0
            avg_loss = np.mean([t['pnl'] for t in lose_trades]) if lose_trades else 0
            
            total_pnl = sum([t['pnl'] for t in close_trades])
            
            print(f"盈利交易: {len(win_trades)}")
            print(f"亏损交易: {len(lose_trades)}")
            print(f"胜率: {win_rate:.2%}")
            print(f"平均盈利: ${avg_win:,.2f}")
            print(f"平均亏损: ${avg_loss:,.2f}")
            print(f"总盈亏: ${total_pnl:,.2f}")
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

def plot_results(results):
    """绘制结果图表"""
    data = results['data']
    equity_curve = results['equity_curve']
    
    # 创建图表
    fig, axes = plt.subplots(3, 1, figsize=(15, 12))
    
    # 价格和均线图
    axes[0].plot(data.index, data['Close'], label='BTC Price', alpha=0.7)
    axes[0].plot(data.index, data['MA20'], label='MA20', alpha=0.8)
    axes[0].plot(data.index, data['MA50'], label='MA50', alpha=0.8)
    
    # 标记买卖点
    buy_signals = data[data['Signal'] == 1]
    sell_signals = data[data['Signal'] == -1]
    
    if len(buy_signals) > 0:
        axes[0].scatter(buy_signals.index, buy_signals['Close'], 
                       color='green', marker='^', s=100, label='Buy Signal', zorder=5)
    if len(sell_signals) > 0:
        axes[0].scatter(sell_signals.index, sell_signals['Close'], 
                       color='red', marker='v', s=100, label='Sell Signal', zorder=5)
    
    axes[0].set_title('BTC价格与MA20/MA50交易信号')
    axes[0].legend()
    axes[0].grid(True, alpha=0.3)
    
    # RSI图
    axes[1].plot(data.index, data['RSI'], label='RSI', color='purple')
    axes[1].axhline(y=70, color='r', linestyle='--', alpha=0.7, label='Overbought(70)')
    axes[1].axhline(y=30, color='g', linestyle='--', alpha=0.7, label='Oversold(30)')
    axes[1].axhline(y=80, color='r', linestyle=':', alpha=0.5, label='Filter(80)')
    axes[1].axhline(y=20, color='g', linestyle=':', alpha=0.5, label='Filter(20)')
    axes[1].set_title('RSI指标')
    axes[1].legend()
    axes[1].grid(True, alpha=0.3)
    axes[1].set_ylim(0, 100)
    
    # 权益曲线
    if len(equity_curve) > 0:
        axes[2].plot(data.index, equity_curve, label='Strategy Equity', color='blue')
        buy_hold_value = [100000 * (data['Close'].iloc[i] / data['Close'].iloc[0]) 
                         for i in range(len(data))]
        axes[2].plot(data.index, buy_hold_value, label='Buy & Hold', color='orange', alpha=0.7)
        axes[2].set_title('权益曲线对比')
        axes[2].legend()
        axes[2].grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig('btc_ma_strategy_simple_fixed_results.png', dpi=300, bbox_inches='tight')
    plt.show()
    print("图表已保存为: btc_ma_strategy_simple_fixed_results.png")

def main():
    """主函数"""
    print("正在初始化BTC MA20/MA50简化修复版策略...")
    
    # 参数设置
    initial_capital = 100000  # 10万美元
    stop_loss = 0.05         # 5%止损
    take_profit = 0.15       # 15%止盈
    
    print("正在获取BTC历史数据...")
    try:
        # 获取数据
        btc_data = get_btc_data(start_date='2020-01-01')
        print(f"成功获取数据: {len(btc_data)} 个交易日")
        
        print("计算技术指标和信号...")
        data_with_indicators = calculate_indicators(btc_data)
        
        # 显示信号统计
        signal_stats = data_with_indicators['Signal'].value_counts()
        print(f"信号生成统计: {dict(signal_stats)}")
        
        print("开始回测...")
        results = backtest_strategy(data_with_indicators, initial_capital, stop_loss, take_profit)
        
        print("分析结果...")
        analyze_results(results, initial_capital)
        
        # 保存交易记录
        if results['trades']:
            trades_df = pd.DataFrame(results['trades'])
            trades_df.to_csv('btc_ma_trades_simple_fixed.csv', index=False)
            print(f"\n交易记录已保存至: btc_ma_trades_simple_fixed.csv")
            
            # 显示交易记录
            print(f"\n交易记录:")
            print(trades_df.to_string(index=False))
        else:
            print("\n警告: 没有产生任何交易!")
        
        # 保存详细数据
        data_with_indicators['Equity'] = results['equity_curve']
        data_with_indicators.to_csv('btc_ma_strategy_data_simple_fixed.csv')
        print("\n详细数据已保存至: btc_ma_strategy_data_simple_fixed.csv")
        
        # 绘制图表
        print("正在生成图表...")
        plot_results(results)
        
    except Exception as e:
        print(f"运行出错: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()