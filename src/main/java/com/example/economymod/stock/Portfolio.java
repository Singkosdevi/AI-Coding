package com.example.economymod.stock;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// 玩家投资组合类
public class Portfolio {
    private UUID playerId;
    private Map<String, Holding> holdings;      // 股票持仓 (股票代码 -> 持仓信息)
    private List<StockTransaction> transactions; // 交易历史
    private long totalInvested;                 // 总投资金额
    private long totalDividends;                // 总股息收入
    private LocalDateTime lastUpdate;           // 最后更新时间
    
    public Portfolio(UUID playerId) {
        this.playerId = playerId;
        this.holdings = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.totalInvested = 0;
        this.totalDividends = 0;
        this.lastUpdate = LocalDateTime.now();
    }
    
    // 买入股票
    public boolean buyStock(String symbol, long shares, long pricePerShare) {
        if (shares <= 0 || pricePerShare <= 0) return false;
        
        long totalCost = shares * pricePerShare;
        Holding holding = holdings.get(symbol);
        
        if (holding == null) {
            // 新建持仓
            holding = new Holding(symbol, shares, pricePerShare);
            holdings.put(symbol, holding);
        } else {
            // 增加持仓
            holding.addShares(shares, pricePerShare);
        }
        
        // 记录交易
        StockTransaction transaction = new StockTransaction(
            symbol, StockTransaction.TransactionType.BUY, 
            shares, pricePerShare, LocalDateTime.now()
        );
        transactions.add(transaction);
        
        totalInvested += totalCost;
        lastUpdate = LocalDateTime.now();
        
        return true;
    }
    
    // 卖出股票
    public boolean sellStock(String symbol, long shares, long pricePerShare) {
        if (shares <= 0 || pricePerShare <= 0) return false;
        
        Holding holding = holdings.get(symbol);
        if (holding == null || holding.getShares() < shares) {
            return false; // 持仓不足
        }
        
        // 减少持仓
        holding.removeShares(shares);
        
        // 如果持仓为0，移除
        if (holding.getShares() == 0) {
            holdings.remove(symbol);
        }
        
        // 记录交易
        StockTransaction transaction = new StockTransaction(
            symbol, StockTransaction.TransactionType.SELL,
            shares, pricePerShare, LocalDateTime.now()
        );
        transactions.add(transaction);
        
        lastUpdate = LocalDateTime.now();
        return true;
    }
    
    // 计算投资组合总价值
    public long calculateTotalValue(StockMarket stockMarket) {
        long totalValue = 0;
        
        for (Holding holding : holdings.values()) {
            Stock stock = stockMarket.getStock(holding.getSymbol());
            if (stock != null) {
                totalValue += holding.getShares() * stock.getCurrentPrice();
            }
        }
        
        return totalValue;
    }
    
    // 计算总收益/亏损
    public long calculateTotalProfitLoss(StockMarket stockMarket) {
        long currentValue = calculateTotalValue(stockMarket);
        long totalCost = getTotalCost();
        return currentValue - totalCost;
    }
    
    // 计算收益率
    public double calculateReturnRate(StockMarket stockMarket) {
        long totalCost = getTotalCost();
        if (totalCost == 0) return 0;
        
        long profitLoss = calculateTotalProfitLoss(stockMarket);
        return ((double) profitLoss / totalCost) * 100;
    }
    
    // 获取总成本
    public long getTotalCost() {
        return holdings.values().stream()
                .mapToLong(Holding::getTotalCost)
                .sum();
    }
    
    // 收取股息
    public long collectDividends(StockMarket stockMarket) {
        long totalDividend = 0;
        
        for (Holding holding : holdings.values()) {
            Stock stock = stockMarket.getStock(holding.getSymbol());
            if (stock != null) {
                long dividend = (long)(holding.getShares() * stock.getCurrentPrice() * stock.getDividendYield());
                totalDividend += dividend;
                
                // 记录股息交易
                StockTransaction dividendTransaction = new StockTransaction(
                    holding.getSymbol(), StockTransaction.TransactionType.DIVIDEND,
                    0, dividend, LocalDateTime.now()
                );
                transactions.add(dividendTransaction);
            }
        }
        
        totalDividends += totalDividend;
        lastUpdate = LocalDateTime.now();
        
        return totalDividend;
    }
    
    // 获取持仓详情
    public List<HoldingInfo> getHoldingDetails(StockMarket stockMarket) {
        List<HoldingInfo> details = new ArrayList<>();
        
        for (Holding holding : holdings.values()) {
            Stock stock = stockMarket.getStock(holding.getSymbol());
            if (stock != null) {
                long currentValue = holding.getShares() * stock.getCurrentPrice();
                long profitLoss = currentValue - holding.getTotalCost();
                double returnRate = holding.getTotalCost() > 0 ? 
                    ((double) profitLoss / holding.getTotalCost()) * 100 : 0;
                
                HoldingInfo info = new HoldingInfo(
                    stock.getSymbol(),
                    stock.getCompanyName(),
                    holding.getShares(),
                    holding.getAveragePrice(),
                    stock.getCurrentPrice(),
                    holding.getTotalCost(),
                    currentValue,
                    profitLoss,
                    returnRate
                );
                details.add(info);
            }
        }
        
        return details;
    }
    
    // 获取交易历史
    public List<StockTransaction> getTransactionHistory(int limit) {
        return transactions.stream()
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    // 获取特定股票的交易历史
    public List<StockTransaction> getStockTransactionHistory(String symbol) {
        return transactions.stream()
                .filter(t -> t.getSymbol().equals(symbol))
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // 计算投资组合多样化指数
    public double getDiversificationIndex(StockMarket stockMarket) {
        if (holdings.isEmpty()) return 0;
        
        long totalValue = calculateTotalValue(stockMarket);
        if (totalValue == 0) return 0;
        
        // 计算赫芬达尔指数 (Herfindahl Index)
        double hhi = 0;
        for (Holding holding : holdings.values()) {
            Stock stock = stockMarket.getStock(holding.getSymbol());
            if (stock != null) {
                double weight = (double)(holding.getShares() * stock.getCurrentPrice()) / totalValue;
                hhi += weight * weight;
            }
        }
        
        // 返回多样化指数 (1 - HHI)，值越高越多样化
        return 1 - hhi;
    }
    
    // 获取投资组合风险评估
    public String getRiskAssessment(StockMarket stockMarket) {
        if (holdings.isEmpty()) return "无投资";
        
        double diversification = getDiversificationIndex(stockMarket);
        double avgVolatility = holdings.values().stream()
                .mapToDouble(holding -> {
                    Stock stock = stockMarket.getStock(holding.getSymbol());
                    return stock != null ? stock.getVolatility() : 0;
                })
                .average()
                .orElse(0);
        
        if (diversification > 0.7 && avgVolatility < 0.05) return "低风险";
        else if (diversification > 0.5 && avgVolatility < 0.1) return "中等风险";
        else if (diversification > 0.3) return "中高风险";
        else return "高风险";
    }
    
    // 获取推荐操作
    public List<String> getRecommendations(StockMarket stockMarket) {
        List<String> recommendations = new ArrayList<>();
        
        if (holdings.isEmpty()) {
            recommendations.add("建议开始投资以实现财富增长");
            return recommendations;
        }
        
        double diversification = getDiversificationIndex(stockMarket);
        if (diversification < 0.5) {
            recommendations.add("投资组合集中度较高，建议增加多样性");
        }
        
        // 检查长期持有的亏损股票
        for (Holding holding : holdings.values()) {
            Stock stock = stockMarket.getStock(holding.getSymbol());
            if (stock != null) {
                long currentValue = holding.getShares() * stock.getCurrentPrice();
                if (currentValue < holding.getTotalCost() * 0.8) { // 亏损超过20%
                    recommendations.add("考虑止损 " + stock.getSymbol() + "，当前亏损较大");
                }
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("投资组合表现良好，继续保持");
        }
        
        return recommendations;
    }
    
    // Getter方法
    public UUID getPlayerId() { return playerId; }
    public Map<String, Holding> getHoldings() { return new HashMap<>(holdings); }
    public List<StockTransaction> getTransactions() { return new ArrayList<>(transactions); }
    public long getTotalInvested() { return totalInvested; }
    public long getTotalDividends() { return totalDividends; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    
    // 持仓信息类
    public static class Holding {
        private String symbol;
        private long shares;
        private long totalCost;
        private LocalDateTime firstPurchase;
        
        public Holding(String symbol, long shares, long pricePerShare) {
            this.symbol = symbol;
            this.shares = shares;
            this.totalCost = shares * pricePerShare;
            this.firstPurchase = LocalDateTime.now();
        }
        
        public void addShares(long newShares, long pricePerShare) {
            this.shares += newShares;
            this.totalCost += newShares * pricePerShare;
        }
        
        public void removeShares(long sharesToRemove) {
            if (sharesToRemove >= shares) {
                shares = 0;
                totalCost = 0;
            } else {
                double costPerShare = (double) totalCost / shares;
                shares -= sharesToRemove;
                totalCost = (long) (shares * costPerShare);
            }
        }
        
        public long getAveragePrice() {
            return shares > 0 ? totalCost / shares : 0;
        }
        
        // Getter方法
        public String getSymbol() { return symbol; }
        public long getShares() { return shares; }
        public long getTotalCost() { return totalCost; }
        public LocalDateTime getFirstPurchase() { return firstPurchase; }
    }
    
    // 持仓详情信息类
    public static class HoldingInfo {
        private String symbol;
        private String companyName;
        private long shares;
        private long averagePrice;
        private long currentPrice;
        private long totalCost;
        private long currentValue;
        private long profitLoss;
        private double returnRate;
        
        public HoldingInfo(String symbol, String companyName, long shares, long averagePrice,
                          long currentPrice, long totalCost, long currentValue, 
                          long profitLoss, double returnRate) {
            this.symbol = symbol;
            this.companyName = companyName;
            this.shares = shares;
            this.averagePrice = averagePrice;
            this.currentPrice = currentPrice;
            this.totalCost = totalCost;
            this.currentValue = currentValue;
            this.profitLoss = profitLoss;
            this.returnRate = returnRate;
        }
        
        // Getter方法
        public String getSymbol() { return symbol; }
        public String getCompanyName() { return companyName; }
        public long getShares() { return shares; }
        public long getAveragePrice() { return averagePrice; }
        public long getCurrentPrice() { return currentPrice; }
        public long getTotalCost() { return totalCost; }
        public long getCurrentValue() { return currentValue; }
        public long getProfitLoss() { return profitLoss; }
        public double getReturnRate() { return returnRate; }
    }
}