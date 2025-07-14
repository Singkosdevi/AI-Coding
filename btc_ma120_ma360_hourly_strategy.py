"""
BTC期货MA120/MA360均线策略 - 1小时级别回测
基于120小时和360小时移动平均线的交叉信号
"""

import pandas as pd
import numpy as np
import yfinance as yf
import matplotlib.pyplot as plt
import warnings
warnings.filterwarnings('ignore')

def get_btc_hourly_data(start_date='2020-01-01', end_date=None):
    """获取BTC 1小时级别数据"""
    if end_date is None:
        end_date = pd.Timestamp.now().strftime('%Y-%m-%d')
    
    # 获取1小时数据，yfinance最多支持730天的1小时数据
    # 所以我们分段获取数据
    btc_data = []
    
    start = pd.Timestamp(start_date)
    end = pd.Timestamp(end_date)
    
    # 分段获取数据，每次获取700天
    current_start = start
    
    while current_start < end:
        current_end = min(current_start + pd.Timedelta(days=700), end)
        
        print(f"获取数据: {current_start.strftime('%Y-%m-%d')} 到 {current_end.strftime('%Y-%m-%d')}")
        
        try:
            data = yf.download('BTC-USD', 
                             start=current_start.strftime('%Y-%m-%d'), 
                             end=current_end.strftime('%Y-%m-%d'),
                             interval='1h')
            if not data.empty:
                btc_data.append(data)
        except Exception as e:
            print(f"获取数据失败: {e}")
        
        current_start = current_end
    
    # 合并所有数据
    if btc_data:
        full_data = pd.concat(btc_data)
        full_data = full_data.sort_index()
        # 去除重复数据
        full_data = full_data[~full_data.index.duplicated(keep='first')]
        return full_data
    else:
        # 如果分段获取失败，尝试获取最近的数据
        print("分段获取失败，尝试获取最近730天数据...")
        return yf.download('BTC-USD', period='730d', interval='1h')

def calculate_ma_signals_hourly(data):
    """计算1小时级别的MA120/MA360信号"""
    df = data.copy()
    
    # 计算移动平均线 (120小时 ≈ 5天, 360小时 ≈ 15天)
    df['MA120'] = df['Close'].rolling(window=120).mean()
    df['MA360'] = df['Close'].rolling(window=360).mean()
    
    # 计算RSI (1小时级别，使用14小时RSI)
    delta = df['Close'].diff()
    gain = delta.where(delta > 0, 0).rolling(window=14).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=14).mean()
    rs = gain / loss
    df['RSI'] = 100 - (100 / (1 + rs))
    
    # 删除NaN值
    df = df.dropna()
    
    # 计算交易信号
    df['Signal'] = 0
    
    for i in range(1, len(df)):
        try:
            # 获取当前和前一小时的MA值
            ma120_current = float(df['MA120'].iloc[i])
            ma360_current = float(df['MA360'].iloc[i])
            ma120_prev = float(df['MA120'].iloc[i-1])
            ma360_prev = float(df['MA360'].iloc[i-1])
            rsi_current = float(df['RSI'].iloc[i])
            
            # 金叉检测 - MA120从下方穿越MA360向上
            if (ma120_current > ma360_current and ma120_prev <= ma360_prev and 
                rsi_current < 75):  # RSI过滤，1小时级别放宽到75
                df.iloc[i, df.columns.get_loc('Signal')] = 1
            
            # 死叉检测 - MA120从上方穿越MA360向下  
            elif (ma120_current < ma360_current and ma120_prev >= ma360_prev and 
                  rsi_current > 25):  # RSI过滤，1小时级别放宽到25
                df.iloc[i, df.columns.get_loc('Signal')] = -1
                
        except (ValueError, TypeError, IndexError):
            continue
    
    return df

def backtest_hourly_strategy(data, initial_capital=100000, stop_loss=0.03, take_profit=0.10):
    """1小时级别回测策略"""
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
        timestamp = df.index[i].strftime('%Y-%m-%d %H:%M:%S')
        
        # 风险管理 - 检查止损止盈（1小时级别降低止损止盈）
        if position != 0:
            if position == 1:  # 多头持仓
                pnl_ratio = (current_price - entry_price) / entry_price
                if pnl_ratio <= -stop_loss or pnl_ratio >= take_profit:
                    # 平多头
                    pnl = pnl_ratio * capital
                    capital += pnl
                    trades.append({
                        'timestamp': timestamp,
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
                        'timestamp': timestamp,
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
                    'timestamp': timestamp,
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
                'timestamp': timestamp,
                'action': 'OPEN_LONG',
                'price': current_price,
                'capital': capital,
                'reason': 'MA120/360 Golden Cross'
            })
            
        elif signal == -1 and position >= 0:  # 卖出信号
            if position == 1:  # 先平多头
                pnl_ratio = (current_price - entry_price) / entry_price
                pnl = pnl_ratio * capital
                capital += pnl
                trades.append({
                    'timestamp': timestamp,
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
                'timestamp': timestamp,
                'action': 'OPEN_SHORT',
                'price': current_price,
                'capital': capital,
                'reason': 'MA120/360 Death Cross'
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
            'timestamp': df.index[-1].strftime('%Y-%m-%d %H:%M:%S'),
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

def analyze_hourly_results(results, initial_capital):
    """分析1小时级别回测结果"""
    trades = results['trades']
    final_capital = results['final_capital']
    equity_curve = results['equity_curve']
    data = results['data']
    
    # 基本统计
    total_return = (final_capital - initial_capital) / initial_capital
    total_hours = len(data)
    total_days = total_hours / 24
    
    print("=" * 80)
    print("BTC期货MA120/MA360策略 - 1小时级别回测结果")
    print("=" * 80)
    print(f"回测期间: {data.index[0].strftime('%Y-%m-%d %H:%M')} 至 {data.index[-1].strftime('%Y-%m-%d %H:%M')}")
    print(f"总小时数: {total_hours:,.0f} 小时")
    print(f"总天数: {total_days:.1f} 天")
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
        print(f"买入信号: {signals[1]} 次")
    if -1 in signals:
        print(f"卖出信号: {signals[-1]} 次")
    
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
                
            # 计算持仓时间统计
            holding_times = []
            for i in range(len(open_trades)):
                if i < len(close_trades):
                    open_time = pd.Timestamp(open_trades[i]['timestamp'])
                    close_time = pd.Timestamp(close_trades[i]['timestamp'])
                    holding_hours = (close_time - open_time).total_seconds() / 3600
                    holding_times.append(holding_hours)
            
            if holding_times:
                avg_holding = np.mean(holding_times)
                print(f"平均持仓时间: {avg_holding:.1f} 小时 ({avg_holding/24:.1f} 天)")
    
    # 风险指标
    if len(equity_curve) > 1:
        equity_series = pd.Series(equity_curve)
        returns = equity_series.pct_change().dropna()
        if len(returns) > 0:
            # 年化波动率 (1小时数据)
            volatility = returns.std() * np.sqrt(24 * 365)
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

def plot_hourly_results(results):
    """绘制1小时级别结果图表"""
    data = results['data']
    equity_curve = results['equity_curve']
    
    # 创建图表
    fig, axes = plt.subplots(4, 1, figsize=(16, 16))
    
    # 价格和均线图 (显示最近30天数据以避免过于密集)
    recent_data = data.tail(30*24)  # 最近30天的小时数据
    
    axes[0].plot(recent_data.index, recent_data['Close'], label='BTC Price', alpha=0.7, linewidth=1)
    axes[0].plot(recent_data.index, recent_data['MA120'], label='MA120 (5d)', alpha=0.8)
    axes[0].plot(recent_data.index, recent_data['MA360'], label='MA360 (15d)', alpha=0.8)
    
    # 标记最近的买卖点
    recent_buy_signals = recent_data[recent_data['Signal'] == 1]
    recent_sell_signals = recent_data[recent_data['Signal'] == -1]
    
    if len(recent_buy_signals) > 0:
        axes[0].scatter(recent_buy_signals.index, recent_buy_signals['Close'], 
                       color='green', marker='^', s=50, label='Buy Signal', zorder=5)
    if len(recent_sell_signals) > 0:
        axes[0].scatter(recent_sell_signals.index, recent_sell_signals['Close'], 
                       color='red', marker='v', s=50, label='Sell Signal', zorder=5)
    
    axes[0].set_title('BTC价格与MA120/MA360交易信号 (最近30天)')
    axes[0].legend()
    axes[0].grid(True, alpha=0.3)
    
    # RSI图 (最近30天)
    axes[1].plot(recent_data.index, recent_data['RSI'], label='RSI', color='purple', linewidth=1)
    axes[1].axhline(y=70, color='r', linestyle='--', alpha=0.7, label='Overbought(70)')
    axes[1].axhline(y=30, color='g', linestyle='--', alpha=0.7, label='Oversold(30)')
    axes[1].axhline(y=75, color='r', linestyle=':', alpha=0.5, label='Filter(75)')
    axes[1].axhline(y=25, color='g', linestyle=':', alpha=0.5, label='Filter(25)')
    axes[1].set_title('RSI指标 (最近30天)')
    axes[1].legend()
    axes[1].grid(True, alpha=0.3)
    axes[1].set_ylim(0, 100)
    
    # 全期价格走势图
    # 为了显示清晰，每天只取一个点
    daily_data = data.resample('D').last().dropna()
    
    axes[2].plot(daily_data.index, daily_data['Close'], label='BTC Price (Daily)', alpha=0.7)
    axes[2].plot(daily_data.index, daily_data['MA120'], label='MA120', alpha=0.8)
    axes[2].plot(daily_data.index, daily_data['MA360'], label='MA360', alpha=0.8)
    
    # 标记所有买卖点
    buy_signals = data[data['Signal'] == 1]
    sell_signals = data[data['Signal'] == -1]
    
    if len(buy_signals) > 0:
        # 每天最多显示一个信号点
        daily_buy = buy_signals.resample('D').first().dropna()
        axes[2].scatter(daily_buy.index, daily_buy['Close'], 
                       color='green', marker='^', s=30, label='Buy Signals', zorder=5)
    if len(sell_signals) > 0:
        daily_sell = sell_signals.resample('D').first().dropna()
        axes[2].scatter(daily_sell.index, daily_sell['Close'], 
                       color='red', marker='v', s=30, label='Sell Signals', zorder=5)
    
    axes[2].set_title('BTC价格与交易信号 (全期)')
    axes[2].legend()
    axes[2].grid(True, alpha=0.3)
    
    # 权益曲线
    if len(equity_curve) > 0:
        # 每天取一个权益点
        equity_daily = pd.Series(equity_curve, index=data.index).resample('D').last()
        
        axes[3].plot(equity_daily.index, equity_daily.values, label='Strategy Equity', color='blue')
        
        # 计算买入持有基准
        daily_closes = data['Close'].resample('D').last()
        buy_hold_value = [100000 * (daily_closes.iloc[i] / daily_closes.iloc[0]) 
                         for i in range(len(daily_closes))]
        axes[3].plot(daily_closes.index, buy_hold_value, label='Buy & Hold', color='orange', alpha=0.7)
        
        axes[3].set_title('权益曲线对比')
        axes[3].legend()
        axes[3].grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig('btc_ma120_ma360_hourly_results.png', dpi=300, bbox_inches='tight')
    plt.show()
    print("图表已保存为: btc_ma120_ma360_hourly_results.png")

def main():
    """主函数"""
    print("正在初始化BTC MA120/MA360策略 - 1小时级别...")
    
    # 参数设置 (1小时级别调整)
    initial_capital = 100000  # 10万美元
    stop_loss = 0.03         # 3%止损
    take_profit = 0.10       # 10%止盈
    
    print("正在获取BTC 1小时历史数据...")
    try:
        # 获取数据 (由于数据量限制，获取近2年数据)
        btc_data = get_btc_hourly_data(start_date='2022-01-01')
        print(f"成功获取数据: {len(btc_data):,} 个小时数据")
        
        print("计算技术指标和信号...")
        data_with_signals = calculate_ma_signals_hourly(btc_data)
        
        # 显示信号统计
        signal_stats = data_with_signals['Signal'].value_counts()
        print(f"信号生成统计: {dict(signal_stats)}")
        
        print("开始回测...")
        results = backtest_hourly_strategy(data_with_signals, initial_capital, stop_loss, take_profit)
        
        print("分析结果...")
        analyze_hourly_results(results, initial_capital)
        
        # 保存交易记录
        if results['trades']:
            trades_df = pd.DataFrame(results['trades'])
            trades_df.to_csv('btc_ma120_ma360_hourly_trades.csv', index=False)
            print(f"\n交易记录已保存至: btc_ma120_ma360_hourly_trades.csv")
            
            # 显示前20笔交易
            print(f"\n前20笔交易记录:")
            print(trades_df.head(20).to_string(index=False))
        else:
            print("\n警告: 没有产生任何交易!")
        
        # 保存详细数据
        data_with_signals['Equity'] = results['equity_curve']
        data_with_signals.to_csv('btc_ma120_ma360_hourly_data.csv')
        print("\n详细数据已保存至: btc_ma120_ma360_hourly_data.csv")
        
        # 绘制图表
        print("正在生成图表...")
        plot_hourly_results(results)
        
    except Exception as e:
        print(f"运行出错: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()