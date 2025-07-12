package com.example.economymod.stock;

import com.example.economymod.economy.EconomyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// 股票市场管理器
public class StockMarket {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockMarket.class);
    
    // 股票数据
    private static final Map<String, Stock> stocks = new ConcurrentHashMap<>();
    
    // 玩家投资组合
    private static final Map<UUID, Portfolio> portfolios = new ConcurrentHashMap<>();
    
    // 市场状态
    private static boolean marketOpen = true;
    private static LocalDateTime marketOpenTime = LocalDateTime.now();
    private static LocalDateTime lastPriceUpdate = LocalDateTime.now();
    
    // 市场统计
    private static long totalMarketCap = 0;
    private static long totalVolume = 0;
    private static int totalListedCompanies = 0;
    
    // 初始化股票市场
    public static void init() {
        LOGGER.info("正在初始化股票市场...");
        
        // 创建默认股票
        createDefaultStocks();
        
        // 启动价格更新任务
        startPriceUpdateTask();
        
        LOGGER.info("股票市场初始化完成，共有 {} 只股票上市", stocks.size());
    }
    
    // 创建默认股票
    private static void createDefaultStocks() {
        // 科技股
        addStock("MCTC", "Minecraft科技", "科技", 150, 10000);
        addStock("REDSTONE", "红石电子", "科技", 80, 15000);
        addStock("ENDER", "末影传送", "科技", 200, 8000);
        
        // 矿业股
        addStock("DIAMOND", "钻石矿业", "矿业", 300, 5000);
        addStock("IRON", "铁矿集团", "矿业", 50, 25000);
        addStock("GOLD", "黄金开采", "矿业", 180, 12000);
        
        // 建筑股
        addStock("COBBLE", "圆石建筑", "建筑", 25, 40000);
        addStock("OAK", "橡木建材", "建筑", 35, 30000);
        addStock("STONE", "石材工程", "建筑", 40, 28000);
        
        // 农业股
        addStock("WHEAT", "小麦农业", "农业", 20, 50000);
        addStock("CARROT", "胡萝卜食品", "农业", 15, 60000);
        addStock("POTATO", "马铃薯集团", "农业", 18, 55000);
        
        // 交通股
        addStock("RAIL", "铁路运输", "交通", 120, 15000);
        addStock("BOAT", "水路运输", "交通", 90, 18000);
        addStock("HORSE", "马匹快递", "交通", 60, 22000);
        
        // 能源股
        addStock("COAL", "煤炭能源", "能源", 45, 35000);
        addStock("LAVA", "岩浆发电", "能源", 75, 20000);
        addStock("WIND", "风力发电", "能源", 55, 25000);
        
        // 娱乐股
        addStock("MUSIC", "音符娱乐", "娱乐", 85, 16000);
        addStock("BOOK", "附魔图书", "娱乐", 110, 12000);
        addStock("ART", "艺术创作", "娱乐", 95, 14000);
        
        // 更新市场统计
        updateMarketStats();
    }
    
    // 添加新股票
    public static boolean addStock(String symbol, String companyName, String industry, 
                                  long initialPrice, long totalShares) {
        if (stocks.containsKey(symbol)) {
            return false; // 股票代码已存在
        }
        
        Stock stock = new Stock(symbol, companyName, industry, initialPrice, totalShares);
        stocks.put(symbol, stock);
        totalListedCompanies++;
        
        LOGGER.info("新股票上市: {} - {}", symbol, companyName);
        updateMarketStats();
        
        return true;
    }
    
    // 获取股票
    public static Stock getStock(String symbol) {
        return stocks.get(symbol.toUpperCase());
    }
    
    // 获取所有股票
    public static Map<String, Stock> getAllStocks() {
        return new HashMap<>(stocks);
    }
    
    // 获取活跃股票列表
    public static List<Stock> getActiveStocks() {
        return stocks.values().stream()
                .filter(Stock::isActive)
                .collect(Collectors.toList());
    }
    
    // 按行业获取股票
    public static List<Stock> getStocksByIndustry(String industry) {
        return stocks.values().stream()
                .filter(stock -> stock.getIndustry().equals(industry))
                .filter(Stock::isActive)
                .collect(Collectors.toList());
    }
    
    // 获取投资组合
    public static Portfolio getPortfolio(UUID playerId) {
        return portfolios.computeIfAbsent(playerId, Portfolio::new);
    }
    
    // 买入股票
    public static boolean buyStock(UUID playerId, String symbol, long shares) {
        if (!marketOpen) return false;
        
        Stock stock = getStock(symbol);
        if (stock == null || !stock.isActive()) return false;
        
        long totalCost = shares * stock.getCurrentPrice();
        long fees = calculateTradingFees(totalCost, StockTransaction.TransactionType.BUY);
        long totalRequired = totalCost + fees;
        
        // 检查玩家资金
        if (EconomyManager.getMoney(playerId) < totalRequired) {
            return false; // 资金不足
        }
        
        // 检查股票可用性
        if (!stock.buyShares(shares)) {
            return false; // 可用股数不足
        }
        
        // 扣除资金
        if (!EconomyManager.removeMoney(playerId, totalRequired)) {
            stock.sellShares(shares); // 回滚股票
            return false;
        }
        
        // 更新投资组合
        Portfolio portfolio = getPortfolio(playerId);
        portfolio.buyStock(symbol, shares, stock.getCurrentPrice());
        
        // 更新股票价格（买入压力）
        stock.setPriceWithVolume(stock.getCurrentPrice() + 1, shares);
        
        updateMarketStats();
        LOGGER.debug("玩家 {} 买入 {} 股票 {} 股", playerId, symbol, shares);
        
        return true;
    }
    
    // 卖出股票
    public static boolean sellStock(UUID playerId, String symbol, long shares) {
        if (!marketOpen) return false;
        
        Stock stock = getStock(symbol);
        if (stock == null || !stock.isActive()) return false;
        
        Portfolio portfolio = getPortfolio(playerId);
        Portfolio.Holding holding = portfolio.getHoldings().get(symbol);
        
        if (holding == null || holding.getShares() < shares) {
            return false; // 持仓不足
        }
        
        long totalValue = shares * stock.getCurrentPrice();
        long fees = calculateTradingFees(totalValue, StockTransaction.TransactionType.SELL);
        long netValue = totalValue - fees;
        
        // 更新投资组合
        if (!portfolio.sellStock(symbol, shares, stock.getCurrentPrice())) {
            return false;
        }
        
        // 增加资金
        EconomyManager.addMoney(playerId, netValue);
        
        // 更新股票数据
        stock.sellShares(shares);
        
        // 更新股票价格（卖出压力）
        long newPrice = Math.max(1, stock.getCurrentPrice() - 1);
        stock.setPriceWithVolume(newPrice, shares);
        
        updateMarketStats();
        LOGGER.debug("玩家 {} 卖出 {} 股票 {} 股", playerId, symbol, shares);
        
        return true;
    }
    
    // 计算交易费用
    private static long calculateTradingFees(long amount, StockTransaction.TransactionType type) {
        long commission = Math.max(1, amount / 1000); // 0.1%手续费，最低1金币
        long tax = 0;
        
        if (type == StockTransaction.TransactionType.SELL) {
            tax = amount / 2000; // 卖出时0.05%印花税
        }
        
        return commission + tax;
    }
    
    // 更新所有股票价格
    public static void updateAllPrices() {
        if (!marketOpen) return;
        
        for (Stock stock : stocks.values()) {
            if (stock.isActive()) {
                stock.updatePrice();
            }
        }
        
        lastPriceUpdate = LocalDateTime.now();
        updateMarketStats();
    }
    
    // 发放股息
    public static void distributeDividends() {
        LOGGER.info("开始发放股息...");
        
        for (UUID playerId : portfolios.keySet()) {
            Portfolio portfolio = portfolios.get(playerId);
            long totalDividends = portfolio.collectDividends(StockMarket::getStock);
            
            if (totalDividends > 0) {
                EconomyManager.addMoney(playerId, totalDividends);
                LOGGER.debug("向玩家 {} 发放股息 {} 金币", playerId, totalDividends);
            }
        }
        
        LOGGER.info("股息发放完成");
    }
    
    // 启动价格更新任务
    private static void startPriceUpdateTask() {
        // 这里应该使用定时任务，简化版本只是记录开始时间
        // 在实际应用中，您可能需要使用ScheduledExecutorService
        LOGGER.info("股票价格更新任务已启动");
    }
    
    // 更新市场统计
    private static void updateMarketStats() {
        totalMarketCap = stocks.values().stream()
                .mapToLong(Stock::getMarketCap)
                .sum();
        
        totalVolume = stocks.values().stream()
                .mapToLong(stock -> stock.getPriceHistory().stream()
                        .filter(history -> history.getTimestamp().isAfter(LocalDateTime.now().minusHours(24)))
                        .mapToLong(Stock.PriceHistory::getVolume)
                        .sum())
                .sum();
        
        totalListedCompanies = (int) stocks.values().stream()
                .filter(Stock::isActive)
                .count();
    }
    
    // 获取市场指数
    public static long getMarketIndex() {
        if (stocks.isEmpty()) return 1000;
        
        // 简化的市场指数计算
        long totalValue = stocks.values().stream()
                .mapToLong(stock -> stock.getCurrentPrice() * 100) // 标准化
                .sum();
        
        return totalValue / stocks.size();
    }
    
    // 获取涨跌幅排行榜
    public static List<Stock> getTopGainers(int limit) {
        return stocks.values().stream()
                .filter(Stock::isActive)
                .sorted((s1, s2) -> Double.compare(s2.getPriceChangePercent(), s1.getPriceChangePercent()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    // 获取跌幅排行榜
    public static List<Stock> getTopLosers(int limit) {
        return stocks.values().stream()
                .filter(Stock::isActive)
                .sorted((s1, s2) -> Double.compare(s1.getPriceChangePercent(), s2.getPriceChangePercent()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    // 获取成交量排行榜
    public static List<Stock> getTopVolumeStocks(int limit) {
        return stocks.values().stream()
                .filter(Stock::isActive)
                .sorted((s1, s2) -> {
                    long v1 = s1.getPriceHistory().stream()
                            .filter(h -> h.getTimestamp().isAfter(LocalDateTime.now().minusHours(24)))
                            .mapToLong(Stock.PriceHistory::getVolume)
                            .sum();
                    long v2 = s2.getPriceHistory().stream()
                            .filter(h -> h.getTimestamp().isAfter(LocalDateTime.now().minusHours(24)))
                            .mapToLong(Stock.PriceHistory::getVolume)
                            .sum();
                    return Long.compare(v2, v1);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    // 搜索股票
    public static List<Stock> searchStocks(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return stocks.values().stream()
                .filter(stock -> stock.getSymbol().toLowerCase().contains(lowerKeyword) ||
                               stock.getCompanyName().toLowerCase().contains(lowerKeyword) ||
                               stock.getIndustry().toLowerCase().contains(lowerKeyword))
                .filter(Stock::isActive)
                .collect(Collectors.toList());
    }
    
    // 开市/闭市
    public static void openMarket() {
        marketOpen = true;
        marketOpenTime = LocalDateTime.now();
        LOGGER.info("股票市场开市");
    }
    
    public static void closeMarket() {
        marketOpen = false;
        LOGGER.info("股票市场闭市");
    }
    
    // 市场状态查询
    public static boolean isMarketOpen() { return marketOpen; }
    public static LocalDateTime getMarketOpenTime() { return marketOpenTime; }
    public static LocalDateTime getLastPriceUpdate() { return lastPriceUpdate; }
    public static long getTotalMarketCap() { return totalMarketCap; }
    public static long getTotalVolume() { return totalVolume; }
    public static int getTotalListedCompanies() { return totalListedCompanies; }
    
    // 获取所有行业
    public static List<String> getAllIndustries() {
        return stocks.values().stream()
                .map(Stock::getIndustry)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    // 股票分割
    public static boolean stockSplit(String symbol, int ratio) {
        Stock stock = getStock(symbol);
        if (stock == null || ratio <= 1) return false;
        
        stock.stockSplit(ratio);
        
        // 更新所有持有该股票的投资组合
        for (Portfolio portfolio : portfolios.values()) {
            Portfolio.Holding holding = portfolio.getHoldings().get(symbol);
            if (holding != null) {
                // 这里需要在Portfolio类中添加分割处理方法
                LOGGER.info("处理股票分割: {} 1:{}", symbol, ratio);
            }
        }
        
        return true;
    }
    
    // 股票回购
    public static boolean stockBuyback(String symbol, long shares, long pricePerShare) {
        Stock stock = getStock(symbol);
        if (stock == null) return false;
        
        stock.buyback(shares, pricePerShare);
        return true;
    }
    
    // 获取市场摘要
    public static String getMarketSummary() {
        if (stocks.isEmpty()) return "市场暂无数据";
        
        int upCount = 0, downCount = 0, flatCount = 0;
        
        for (Stock stock : stocks.values()) {
            if (!stock.isActive()) continue;
            
            double change = stock.getPriceChangePercent();
            if (change > 0) upCount++;
            else if (change < 0) downCount++;
            else flatCount++;
        }
        
        return String.format("上涨:%d 下跌:%d 平盘:%d 总市值:%d金币", 
                upCount, downCount, flatCount, totalMarketCap);
    }
}