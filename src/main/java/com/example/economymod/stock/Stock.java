package com.example.economymod.stock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// 股票数据模型
public class Stock {
    private String symbol;           // 股票代码
    private String companyName;      // 公司名称
    private String industry;         // 行业分类
    private long currentPrice;       // 当前价格（金币）
    private long openPrice;          // 开盘价
    private long highPrice;          // 最高价
    private long lowPrice;           // 最低价
    private long previousClose;      // 前收盘价
    private long totalShares;        // 总股数
    private long availableShares;    // 可交易股数
    private long marketCap;          // 市值
    private double volatility;       // 波动率
    private List<PriceHistory> priceHistory; // 价格历史
    private LocalDateTime lastUpdate; // 最后更新时间
    private boolean isActive;        // 是否活跃交易
    private String description;      // 公司描述
    
    private static final Random random = new Random();
    
    public Stock(String symbol, String companyName, String industry, long initialPrice, long totalShares) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.industry = industry;
        this.currentPrice = initialPrice;
        this.openPrice = initialPrice;
        this.highPrice = initialPrice;
        this.lowPrice = initialPrice;
        this.previousClose = initialPrice;
        this.totalShares = totalShares;
        this.availableShares = totalShares;
        this.marketCap = initialPrice * totalShares;
        this.volatility = 0.05; // 5%默认波动率
        this.priceHistory = new ArrayList<>();
        this.lastUpdate = LocalDateTime.now();
        this.isActive = true;
        this.description = "一家在" + industry + "领域的知名公司";
        
        // 记录初始价格
        recordPrice(initialPrice, 0);
    }
    
    // 更新股票价格（模拟市场波动）
    public void updatePrice() {
        if (!isActive) return;
        
        // 基于波动率和随机因素计算新价格
        double changePercent = (random.nextGaussian() * volatility);
        
        // 添加一些市场趋势影响
        double trendFactor = calculateTrendFactor();
        changePercent += trendFactor;
        
        // 限制单次波动幅度（-20% 到 +20%）
        changePercent = Math.max(-0.2, Math.min(0.2, changePercent));
        
        long newPrice = Math.max(1, (long)(currentPrice * (1 + changePercent)));
        long volume = Math.abs((long)(random.nextGaussian() * 1000)) + 100;
        
        setPriceWithVolume(newPrice, volume);
    }
    
    // 设置新价格和交易量
    public void setPriceWithVolume(long newPrice, long volume) {
        previousClose = currentPrice;
        currentPrice = newPrice;
        
        // 更新日内高低价
        if (isNewTradingDay()) {
            openPrice = newPrice;
            highPrice = newPrice;
            lowPrice = newPrice;
        } else {
            highPrice = Math.max(highPrice, newPrice);
            lowPrice = Math.min(lowPrice, newPrice);
        }
        
        // 记录价格历史
        recordPrice(newPrice, volume);
        
        // 更新市值
        marketCap = newPrice * totalShares;
        lastUpdate = LocalDateTime.now();
    }
    
    // 记录价格历史
    private void recordPrice(long price, long volume) {
        priceHistory.add(new PriceHistory(price, volume, LocalDateTime.now()));
        
        // 限制历史记录数量（保留最近1000条）
        if (priceHistory.size() > 1000) {
            priceHistory.remove(0);
        }
    }
    
    // 计算趋势因子
    private double calculateTrendFactor() {
        if (priceHistory.size() < 10) return 0;
        
        // 基于最近的价格趋势计算
        List<PriceHistory> recent = priceHistory.subList(
            Math.max(0, priceHistory.size() - 10), priceHistory.size());
        
        double totalChange = 0;
        for (int i = 1; i < recent.size(); i++) {
            double change = (double)(recent.get(i).getPrice() - recent.get(i-1).getPrice()) 
                           / recent.get(i-1).getPrice();
            totalChange += change;
        }
        
        // 返回平均变化的10%作为趋势影响
        return (totalChange / recent.size()) * 0.1;
    }
    
    // 检查是否为新交易日
    private boolean isNewTradingDay() {
        if (priceHistory.isEmpty()) return true;
        
        LocalDateTime lastRecord = priceHistory.get(priceHistory.size() - 1).getTimestamp();
        LocalDateTime now = LocalDateTime.now();
        
        return !lastRecord.toLocalDate().equals(now.toLocalDate());
    }
    
    // 计算价格变化
    public long getPriceChange() {
        return currentPrice - previousClose;
    }
    
    // 计算价格变化百分比
    public double getPriceChangePercent() {
        if (previousClose == 0) return 0;
        return ((double)getPriceChange() / previousClose) * 100;
    }
    
    // 获取市盈率（简化计算）
    public double getPERatio() {
        // 简化的市盈率计算，基于市值和一个假设的年收益
        long assumedEarnings = marketCap / 20; // 假设收益为市值的5%
        return assumedEarnings > 0 ? (double)marketCap / assumedEarnings : 0;
    }
    
    // 获取股息收益率（简化）
    public double getDividendYield() {
        // 基于价格的简化股息计算
        return Math.max(0.01, 0.05 - (volatility * 10));
    }
    
    // 计算移动平均价格
    public long getMovingAverage(int days) {
        if (priceHistory.size() < days) return currentPrice;
        
        List<PriceHistory> recent = priceHistory.subList(
            Math.max(0, priceHistory.size() - days), priceHistory.size());
        
        long sum = recent.stream().mapToLong(PriceHistory::getPrice).sum();
        return sum / recent.size();
    }
    
    // 买入股票（减少可用股数）
    public boolean buyShares(long shares) {
        if (shares <= 0 || shares > availableShares) return false;
        
        availableShares -= shares;
        return true;
    }
    
    // 卖出股票（增加可用股数）
    public void sellShares(long shares) {
        if (shares > 0) {
            availableShares += shares;
            availableShares = Math.min(availableShares, totalShares);
        }
    }
    
    // 股票分割
    public void stockSplit(int ratio) {
        if (ratio <= 1) return;
        
        totalShares *= ratio;
        availableShares *= ratio;
        currentPrice /= ratio;
        openPrice /= ratio;
        highPrice /= ratio;
        lowPrice /= ratio;
        previousClose /= ratio;
        
        // 调整价格历史
        for (PriceHistory history : priceHistory) {
            history.adjustPrice(ratio);
        }
    }
    
    // 股票回购
    public void buyback(long shares, long pricePerShare) {
        if (shares > 0 && shares <= availableShares) {
            totalShares -= shares;
            availableShares -= shares;
            
            // 回购通常会推高股价
            long newPrice = (long)(currentPrice * 1.02); // 2%溢价
            setPriceWithVolume(newPrice, shares);
        }
    }
    
    // 获取股票状态描述
    public String getStatusDescription() {
        double change = getPriceChangePercent();
        if (change > 5) return "强势上涨";
        else if (change > 2) return "稳步上涨";
        else if (change > 0) return "微幅上涨";
        else if (change > -2) return "微幅下跌";
        else if (change > -5) return "稳步下跌";
        else return "大幅下跌";
    }
    
    // 获取风险等级
    public String getRiskLevel() {
        if (volatility < 0.02) return "低风险";
        else if (volatility < 0.05) return "中等风险";
        else if (volatility < 0.1) return "高风险";
        else return "极高风险";
    }
    
    // Getter和Setter方法
    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public String getIndustry() { return industry; }
    public long getCurrentPrice() { return currentPrice; }
    public long getOpenPrice() { return openPrice; }
    public long getHighPrice() { return highPrice; }
    public long getLowPrice() { return lowPrice; }
    public long getPreviousClose() { return previousClose; }
    public long getTotalShares() { return totalShares; }
    public long getAvailableShares() { return availableShares; }
    public long getMarketCap() { return marketCap; }
    public double getVolatility() { return volatility; }
    public List<PriceHistory> getPriceHistory() { return new ArrayList<>(priceHistory); }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public boolean isActive() { return isActive; }
    public String getDescription() { return description; }
    
    public void setVolatility(double volatility) {
        this.volatility = Math.max(0.001, Math.min(1.0, volatility));
    }
    
    public void setActive(boolean active) { this.isActive = active; }
    public void setDescription(String description) { this.description = description; }
    
    // 价格历史记录内部类
    public static class PriceHistory {
        private long price;
        private long volume;
        private LocalDateTime timestamp;
        
        public PriceHistory(long price, long volume, LocalDateTime timestamp) {
            this.price = price;
            this.volume = volume;
            this.timestamp = timestamp;
        }
        
        public void adjustPrice(int ratio) {
            this.price /= ratio;
        }
        
        public long getPrice() { return price; }
        public long getVolume() { return volume; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}