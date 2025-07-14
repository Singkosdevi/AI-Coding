"""
BTC期货交易策略 - 基于MA120和MA360均线系统
作者: AI Assistant
策略描述:
- 使用120日和360日移动平均线作为交易信号
- MA120上穿MA360时买入(金叉)
- MA120下穿MA360时卖出(死叉)
- 包含风险管理和资金管理
"""

import pandas as pd
import numpy as np
import yfinance as yf
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime, timedelta
import warnings
warnings.filterwarnings('ignore')

class BTCMAStrategy:
    def __init__(self, initial_capital=100000, leverage=1, stop_loss=0.05, take_profit=0.15):
        """
        初始化策略参数
        
        Args:
            initial_capital: 初始资金 (美元)
            leverage: 杠杆倍数
            stop_loss: 止损比例 (5%)
            take_profit: 止盈比例 (15%)
        """
        self.initial_capital = initial_capital
        self.leverage = leverage
        self.stop_loss = stop_loss
        self.take_profit = take_profit
        self.current_capital = initial_capital
        self.position = 0  # 0: 空仓, 1: 多头, -1: 空头
        self.entry_price = 0
        self.trades = []
        self.equity_curve = []
        
    def fetch_btc_data(self, start_date='2020-01-01', end_date=None):
        """
        获取BTC数据
        
        Args:
            start_date: 开始日期
            end_date: 结束日期 (默认为今天)
        """
        if end_date is None:
            end_date = datetime.now().strftime('%Y-%m-%d')
            
        # 获取比特币数据 (使用Bitcoin USD)
        btc = yf.download('BTC-USD', start=start_date, end=end_date)
        
        if btc.empty:
            raise ValueError("无法获取BTC数据，请检查网络连接或日期范围")
            
        return btc
    
    def calculate_indicators(self, data):
        """
        计算技术指标
        
        Args:
            data: OHLCV数据
            
        Returns:
            包含技术指标的DataFrame
        """
        df = data.copy()
        
        # 计算移动平均线
        df['MA120'] = df['Close'].rolling(window=120).mean()
        df['MA360'] = df['Close'].rolling(window=360).mean()
        
        # 计算信号
        df['Signal'] = 0
        df['Position'] = 0
        
        # 金叉死叉信号
        df.loc[df['MA120'] > df['MA360'], 'Signal'] = 1  # 买入信号
        df.loc[df['MA120'] < df['MA360'], 'Signal'] = -1  # 卖出信号
        
        # 计算持仓
        df['Position'] = df['Signal'].shift(1).fillna(0)
        
        # 计算收益率
        df['Returns'] = df['Close'].pct_change()
        df['Strategy_Returns'] = df['Position'] * df['Returns']
        
        # 计算其他技术指标
        df['Volatility'] = df['Returns'].rolling(window=30).std() * np.sqrt(365)
        df['RSI'] = self.calculate_rsi(df['Close'])
        
        return df.dropna()
    
    def calculate_rsi(self, prices, period=14):
        """计算RSI指标"""
        delta = prices.diff()
        gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
        loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
        rs = gain / loss
        rsi = 100 - (100 / (1 + rs))
        return rsi
    
    def generate_signals(self, data):
        """
        生成交易信号
        
        Args:
            data: 包含技术指标的DataFrame
            
        Returns:
            包含信号的DataFrame
        """
        df = data.copy()
        
        # 基础均线信号
        df['MA_Signal'] = 0
        
        # 金叉信号 (MA120上穿MA360)
        ma120_cross_above = (df['MA120'] > df['MA360']) & (df['MA120'].shift(1) <= df['MA360'].shift(1))
        df.loc[ma120_cross_above, 'MA_Signal'] = 1
        
        # 死叉信号 (MA120下穿MA360)
        ma120_cross_below = (df['MA120'] < df['MA360']) & (df['MA120'].shift(1) >= df['MA360'].shift(1))
        df.loc[ma120_cross_below, 'MA_Signal'] = -1
        
        # 过滤信号 - 避免过度交易
        df['Filtered_Signal'] = df['MA_Signal']
        
        # 添加RSI过滤条件
        # 只在RSI不超买/超卖时交易
        df.loc[(df['MA_Signal'] == 1) & (df['RSI'] > 70), 'Filtered_Signal'] = 0
        df.loc[(df['MA_Signal'] == -1) & (df['RSI'] < 30), 'Filtered_Signal'] = 0
        
        return df
    
    def backtest(self, data, verbose=True):
        """
        回测策略
        
        Args:
            data: 价格数据
            verbose: 是否打印详细信息
            
        Returns:
            回测结果
        """
        # 计算指标和信号
        df = self.calculate_indicators(data)
        df = self.generate_signals(df)
        
        self.current_capital = self.initial_capital
        self.position = 0
        self.entry_price = 0
        self.trades = []
        equity = [self.initial_capital]
        
        for i, (date, row) in enumerate(df.iterrows()):
            current_price = row['Close']
            signal = row['Filtered_Signal']
            
            # 确保signal是标量值
            if isinstance(signal, pd.Series):
                signal = signal.iloc[0] if len(signal) > 0 else 0
            if pd.isna(signal):
                signal = 0
            
            # 风险管理 - 止损止盈
            if self.position != 0:
                if self.position == 1:  # 多头持仓
                    pnl_ratio = (current_price - self.entry_price) / self.entry_price
                    if pnl_ratio <= -self.stop_loss or pnl_ratio >= self.take_profit:
                        self._close_position(date, current_price, 'Risk Management')
                elif self.position == -1:  # 空头持仓
                    pnl_ratio = (self.entry_price - current_price) / self.entry_price
                    if pnl_ratio <= -self.stop_loss or pnl_ratio >= self.take_profit:
                        self._close_position(date, current_price, 'Risk Management')
            
            # 处理交易信号
            if pd.notna(signal) and signal == 1 and self.position <= 0:  # 买入信号
                if self.position == -1:  # 先平空头
                    self._close_position(date, current_price, 'Signal Close')
                self._open_position(date, current_price, 1, 'MA Golden Cross')
                
            elif pd.notna(signal) and signal == -1 and self.position >= 0:  # 卖出信号
                if self.position == 1:  # 先平多头
                    self._close_position(date, current_price, 'Signal Close')
                self._open_position(date, current_price, -1, 'MA Death Cross')
            
            # 更新权益曲线
            if self.position != 0:
                if self.position == 1:
                    unrealized_pnl = (current_price - self.entry_price) / self.entry_price * self.current_capital * self.leverage
                else:
                    unrealized_pnl = (self.entry_price - current_price) / self.entry_price * self.current_capital * self.leverage
                current_equity = self.current_capital + unrealized_pnl
            else:
                current_equity = self.current_capital
                
            equity.append(current_equity)
        
        # 最后如果还有持仓，平仓
        if self.position != 0:
            final_price = df.iloc[-1]['Close']
            self._close_position(df.index[-1], final_price, 'Final Close')
        
        df['Equity'] = equity[1:]  # 去掉第一个初始值
        
        if verbose:
            self._print_backtest_summary(df)
            
        return df
    
    def _open_position(self, date, price, direction, reason):
        """开仓"""
        self.position = direction
        self.entry_price = price
        
        trade = {
            'date': date,
            'action': 'OPEN',
            'direction': 'LONG' if direction == 1 else 'SHORT',
            'price': price,
            'reason': reason,
            'capital': self.current_capital
        }
        self.trades.append(trade)
    
    def _close_position(self, date, price, reason):
        """平仓"""
        if self.position == 0:
            return
            
        # 计算盈亏
        if self.position == 1:  # 平多头
            pnl_ratio = (price - self.entry_price) / self.entry_price
        else:  # 平空头
            pnl_ratio = (self.entry_price - price) / self.entry_price
            
        pnl = pnl_ratio * self.current_capital * self.leverage
        self.current_capital += pnl
        
        trade = {
            'date': date,
            'action': 'CLOSE',
            'direction': 'LONG' if self.position == 1 else 'SHORT',
            'price': price,
            'entry_price': self.entry_price,
            'pnl': pnl,
            'pnl_ratio': pnl_ratio,
            'reason': reason,
            'capital': self.current_capital
        }
        self.trades.append(trade)
        
        self.position = 0
        self.entry_price = 0
    
    def _print_backtest_summary(self, data):
        """打印回测摘要"""
        print("=" * 50)
        print("BTC期货MA120/MA360策略回测结果")
        print("=" * 50)
        
        # 基本统计
        total_return = (self.current_capital - self.initial_capital) / self.initial_capital
        total_days = len(data)
        annualized_return = (1 + total_return) ** (365 / total_days) - 1
        
        print(f"回测期间: {data.index[0].strftime('%Y-%m-%d')} 至 {data.index[-1].strftime('%Y-%m-%d')}")
        print(f"总天数: {total_days}")
        print(f"初始资金: ${self.initial_capital:,.2f}")
        print(f"最终资金: ${self.current_capital:,.2f}")
        print(f"总收益率: {total_return:.2%}")
        print(f"年化收益率: {annualized_return:.2%}")
        
        # 交易统计
        trades_df = pd.DataFrame(self.trades)
        if not trades_df.empty:
            close_trades = trades_df[trades_df['action'] == 'CLOSE']
            if not close_trades.empty:
                win_trades = close_trades[close_trades['pnl'] > 0]
                win_rate = len(win_trades) / len(close_trades)
                avg_win = win_trades['pnl'].mean() if len(win_trades) > 0 else 0
                avg_loss = close_trades[close_trades['pnl'] < 0]['pnl'].mean()
                avg_loss = avg_loss if not pd.isna(avg_loss) else 0
                
                print(f"\n交易统计:")
                print(f"总交易次数: {len(close_trades)}")
                print(f"盈利交易: {len(win_trades)}")
                print(f"胜率: {win_rate:.2%}")
                print(f"平均盈利: ${avg_win:.2f}")
                print(f"平均亏损: ${avg_loss:.2f}")
                if avg_loss != 0:
                    print(f"盈亏比: {abs(avg_win/avg_loss):.2f}")
        
        # 风险指标
        if 'Equity' in data.columns:
            equity_returns = data['Equity'].pct_change().dropna()
            if len(equity_returns) > 0:
                volatility = equity_returns.std() * np.sqrt(365)
                sharpe_ratio = annualized_return / volatility if volatility > 0 else 0
                max_drawdown = self._calculate_max_drawdown(data['Equity'])
                
                print(f"\n风险指标:")
                print(f"波动率: {volatility:.2%}")
                print(f"夏普比率: {sharpe_ratio:.2f}")
                print(f"最大回撤: {max_drawdown:.2%}")
        
        # 基准比较
        btc_return = (data['Close'].iloc[-1] - data['Close'].iloc[0]) / data['Close'].iloc[0]
        print(f"\n基准比较:")
        print(f"BTC买入持有收益率: {btc_return:.2%}")
        print(f"策略超额收益: {total_return - btc_return:.2%}")
    
    def _calculate_max_drawdown(self, equity_curve):
        """计算最大回撤"""
        peak = equity_curve.expanding().max()
        drawdown = (equity_curve - peak) / peak
        return drawdown.min()
    
    def plot_results(self, data, save_path=None):
        """绘制回测结果图表"""
        fig, axes = plt.subplots(4, 1, figsize=(15, 20))
        
        # 1. 价格和均线图
        axes[0].plot(data.index, data['Close'], label='BTC Price', alpha=0.7)
        axes[0].plot(data.index, data['MA120'], label='MA120', alpha=0.8)
        axes[0].plot(data.index, data['MA360'], label='MA360', alpha=0.8)
        
        # 标记交易点
        trades_df = pd.DataFrame(self.trades)
        if not trades_df.empty:
            open_trades = trades_df[trades_df['action'] == 'OPEN']
            for _, trade in open_trades.iterrows():
                color = 'green' if trade['direction'] == 'LONG' else 'red'
                marker = '^' if trade['direction'] == 'LONG' else 'v'
                axes[0].scatter(trade['date'], trade['price'], color=color, marker=marker, s=100, alpha=0.8)
        
        axes[0].set_title('BTC价格走势与MA120/MA360')
        axes[0].set_ylabel('价格 (USD)')
        axes[0].legend()
        axes[0].grid(True, alpha=0.3)
        
        # 2. 权益曲线
        if 'Equity' in data.columns:
            axes[1].plot(data.index, data['Equity'], label='策略权益', color='blue')
            axes[1].axhline(y=self.initial_capital, color='gray', linestyle='--', alpha=0.5, label='初始资金')
            
            # BTC买入持有基准
            btc_value = self.initial_capital * (data['Close'] / data['Close'].iloc[0])
            axes[1].plot(data.index, btc_value, label='BTC买入持有', color='orange', alpha=0.7)
        
        axes[1].set_title('权益曲线对比')
        axes[1].set_ylabel('权益 (USD)')
        axes[1].legend()
        axes[1].grid(True, alpha=0.3)
        
        # 3. 回撤图
        if 'Equity' in data.columns:
            peak = data['Equity'].expanding().max()
            drawdown = (data['Equity'] - peak) / peak * 100
            axes[2].fill_between(data.index, drawdown, 0, color='red', alpha=0.3)
            axes[2].plot(data.index, drawdown, color='red')
        
        axes[2].set_title('策略回撤')
        axes[2].set_ylabel('回撤 (%)')
        axes[2].grid(True, alpha=0.3)
        
        # 4. RSI指标
        if 'RSI' in data.columns:
            axes[3].plot(data.index, data['RSI'], color='purple', alpha=0.7)
            axes[3].axhline(y=70, color='red', linestyle='--', alpha=0.5, label='超买线')
            axes[3].axhline(y=30, color='green', linestyle='--', alpha=0.5, label='超卖线')
            axes[3].fill_between(data.index, 70, 100, alpha=0.1, color='red')
            axes[3].fill_between(data.index, 0, 30, alpha=0.1, color='green')
        
        axes[3].set_title('RSI相对强弱指标')
        axes[3].set_ylabel('RSI')
        axes[3].set_xlabel('日期')
        axes[3].legend()
        axes[3].grid(True, alpha=0.3)
        
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            print(f"图表已保存至: {save_path}")
        
        plt.show()
    
    def export_trades(self, filename='btc_ma_trades.csv'):
        """导出交易记录"""
        if self.trades:
            trades_df = pd.DataFrame(self.trades)
            trades_df.to_csv(filename, index=False)
            print(f"交易记录已导出至: {filename}")
        else:
            print("无交易记录可导出")


def main():
    """主函数 - 运行策略回测"""
    print("正在初始化BTC MA120/MA360策略...")
    
    # 创建策略实例
    strategy = BTCMAStrategy(
        initial_capital=100000,  # 10万美元初始资金
        leverage=1,              # 不使用杠杆
        stop_loss=0.05,          # 5%止损
        take_profit=0.15         # 15%止盈
    )
    
    print("正在获取BTC历史数据...")
    try:
        # 获取过去4年的数据
        start_date = '2020-01-01'
        btc_data = strategy.fetch_btc_data(start_date=start_date)
        print(f"成功获取数据: {len(btc_data)} 个交易日")
        
        print("开始回测...")
        results = strategy.backtest(btc_data, verbose=True)
        
        print("\n正在生成图表...")
        strategy.plot_results(results, save_path='btc_ma_strategy_results.png')
        
        print("导出交易记录...")
        strategy.export_trades()
        
        # 保存详细结果
        results.to_csv('btc_ma_strategy_data.csv')
        print("详细数据已保存至: btc_ma_strategy_data.csv")
        
    except Exception as e:
        print(f"运行出错: {str(e)}")
        print("请检查网络连接或重试")


if __name__ == "__main__":
    main()