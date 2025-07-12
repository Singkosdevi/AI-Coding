package com.example.economymod.stock;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.text.DecimalFormat;
import java.util.List;

// 股票命令系统
public class StockCommands {
    private static final DecimalFormat df = new DecimalFormat("#.##");
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stock")
                .then(Commands.literal("list")
                        .executes(context -> listStocks(context.getSource(), 10))
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 50))
                                .executes(context -> listStocks(context.getSource(), 
                                        IntegerArgumentType.getInteger(context, "limit")))))
                
                .then(Commands.literal("info")
                        .then(Commands.argument("symbol", StringArgumentType.word())
                                .executes(context -> showStockInfo(context.getSource(),
                                        StringArgumentType.getString(context, "symbol")))))
                
                .then(Commands.literal("buy")
                        .then(Commands.argument("symbol", StringArgumentType.word())
                                .then(Commands.argument("shares", LongArgumentType.longArg(1))
                                        .executes(context -> buyStock(context.getSource(),
                                                StringArgumentType.getString(context, "symbol"),
                                                LongArgumentType.getLong(context, "shares"))))))
                
                .then(Commands.literal("sell")
                        .then(Commands.argument("symbol", StringArgumentType.word())
                                .then(Commands.argument("shares", LongArgumentType.longArg(1))
                                        .executes(context -> sellStock(context.getSource(),
                                                StringArgumentType.getString(context, "symbol"),
                                                LongArgumentType.getLong(context, "shares"))))))
                
                .then(Commands.literal("portfolio")
                        .executes(context -> showPortfolio(context.getSource())))
                
                .then(Commands.literal("market")
                        .executes(context -> showMarketOverview(context.getSource())))
                
                .then(Commands.literal("gainers")
                        .executes(context -> showTopGainers(context.getSource(), 5))
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 20))
                                .executes(context -> showTopGainers(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "limit")))))
                
                .then(Commands.literal("losers")
                        .executes(context -> showTopLosers(context.getSource(), 5))
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 20))
                                .executes(context -> showTopLosers(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "limit")))))
                
                .then(Commands.literal("search")
                        .then(Commands.argument("keyword", StringArgumentType.greedyString())
                                .executes(context -> searchStocks(context.getSource(),
                                        StringArgumentType.getString(context, "keyword")))))
                
                .then(Commands.literal("industry")
                        .then(Commands.argument("industry", StringArgumentType.word())
                                .executes(context -> showIndustryStocks(context.getSource(),
                                        StringArgumentType.getString(context, "industry")))))
                
                .then(Commands.literal("industries")
                        .executes(context -> showIndustries(context.getSource())))
                
                .then(Commands.literal("transactions")
                        .executes(context -> showTransactionHistory(context.getSource(), 10))
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 50))
                                .executes(context -> showTransactionHistory(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "limit")))))
                
                .then(Commands.literal("dividends")
                        .executes(context -> collectDividends(context.getSource())))
                
                // 管理员命令
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("update")
                                .executes(context -> adminUpdatePrices(context.getSource())))
                        
                        .then(Commands.literal("add")
                                .then(Commands.argument("symbol", StringArgumentType.word())
                                        .then(Commands.argument("company", StringArgumentType.word())
                                                .then(Commands.argument("industry", StringArgumentType.word())
                                                        .then(Commands.argument("price", LongArgumentType.longArg(1))
                                                                .then(Commands.argument("shares", LongArgumentType.longArg(1))
                                                                        .executes(context -> adminAddStock(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "symbol"),
                                                                                StringArgumentType.getString(context, "company"),
                                                                                StringArgumentType.getString(context, "industry"),
                                                                                LongArgumentType.getLong(context, "price"),
                                                                                LongArgumentType.getLong(context, "shares")))))))))
                        
                        .then(Commands.literal("dividends")
                                .executes(context -> adminDistributeDividends(context.getSource())))
                        
                        .then(Commands.literal("split")
                                .then(Commands.argument("symbol", StringArgumentType.word())
                                        .then(Commands.argument("ratio", IntegerArgumentType.integer(2, 10))
                                                .executes(context -> adminStockSplit(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "symbol"),
                                                        IntegerArgumentType.getInteger(context, "ratio"))))))
                        
                        .then(Commands.literal("open")
                                .executes(context -> adminOpenMarket(context.getSource())))
                        
                        .then(Commands.literal("close")
                                .executes(context -> adminCloseMarket(context.getSource())))));
    }

    // 显示股票列表
    private static int listStocks(CommandSourceStack source, int limit) {
        List<Stock> stocks = StockMarket.getActiveStocks();
        
        if (stocks.isEmpty()) {
            source.sendFailure(Component.literal("当前没有可交易的股票"));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== 股票列表 ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("代码    公司名称        当前价格    涨跌幅").withStyle(ChatFormatting.GRAY), false);
        
        stocks.stream().limit(limit).forEach(stock -> {
            double changePercent = stock.getPriceChangePercent();
            ChatFormatting color = changePercent > 0 ? ChatFormatting.GREEN : 
                                  changePercent < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW;
            String changeStr = (changePercent > 0 ? "+" : "") + df.format(changePercent) + "%";
            
            source.sendSuccess(() -> Component.literal(String.format("%-8s %-12s %8d    %s",
                    stock.getSymbol(),
                    stock.getCompanyName().length() > 12 ? 
                        stock.getCompanyName().substring(0, 12) : stock.getCompanyName(),
                    stock.getCurrentPrice(),
                    changeStr)).withStyle(color), false);
        });
        
        return 1;
    }

    // 显示股票详细信息
    private static int showStockInfo(CommandSourceStack source, String symbol) {
        Stock stock = StockMarket.getStock(symbol);
        if (stock == null) {
            source.sendFailure(Component.literal("未找到股票: " + symbol));
            return 0;
        }
        
        double changePercent = stock.getPriceChangePercent();
        ChatFormatting priceColor = changePercent > 0 ? ChatFormatting.GREEN : 
                                   changePercent < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW;
        
        source.sendSuccess(() -> Component.literal("=== " + stock.getSymbol() + " - " + stock.getCompanyName() + " ===")
                .withStyle(ChatFormatting.GOLD), false);
        
        source.sendSuccess(() -> Component.literal("行业: " + stock.getIndustry()).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("当前价格: " + stock.getCurrentPrice() + " 金币").withStyle(priceColor), false);
        source.sendSuccess(() -> Component.literal("涨跌: " + stock.getPriceChange() + " (" + 
                (changePercent > 0 ? "+" : "") + df.format(changePercent) + "%)").withStyle(priceColor), false);
        source.sendSuccess(() -> Component.literal("开盘价: " + stock.getOpenPrice() + " 金币").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("最高价: " + stock.getHighPrice() + " 金币").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("最低价: " + stock.getLowPrice() + " 金币").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("市值: " + stock.getMarketCap() + " 金币").withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("可用股数: " + stock.getAvailableShares()).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("总股数: " + stock.getTotalShares()).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("状态: " + stock.getStatusDescription()).withStyle(ChatFormatting.BLUE), false);
        source.sendSuccess(() -> Component.literal("风险等级: " + stock.getRiskLevel()).withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("股息收益率: " + df.format(stock.getDividendYield() * 100) + "%")
                .withStyle(ChatFormatting.GREEN), false);
        
        return 1;
    }

    // 买入股票
    private static int buyStock(CommandSourceStack source, String symbol, long shares) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        if (!StockMarket.isMarketOpen()) {
            source.sendFailure(Component.literal("市场已闭市，无法交易"));
            return 0;
        }
        
        Stock stock = StockMarket.getStock(symbol);
        if (stock == null) {
            source.sendFailure(Component.literal("未找到股票: " + symbol));
            return 0;
        }
        
        long totalCost = shares * stock.getCurrentPrice();
        source.sendSuccess(() -> Component.literal("准备买入 " + shares + " 股 " + symbol + 
                "，总费用约 " + totalCost + " 金币（不含手续费）"), false);
        
        if (StockMarket.buyStock(player.getUUID(), symbol, shares)) {
            source.sendSuccess(() -> Component.literal("成功买入 " + shares + " 股 " + symbol)
                    .withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendFailure(Component.literal("买入失败，可能是资金不足或股票不可用"));
        }
        
        return 1;
    }

    // 卖出股票
    private static int sellStock(CommandSourceStack source, String symbol, long shares) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        if (!StockMarket.isMarketOpen()) {
            source.sendFailure(Component.literal("市场已闭市，无法交易"));
            return 0;
        }
        
        if (StockMarket.sellStock(player.getUUID(), symbol, shares)) {
            source.sendSuccess(() -> Component.literal("成功卖出 " + shares + " 股 " + symbol)
                    .withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendFailure(Component.literal("卖出失败，可能是持仓不足"));
        }
        
        return 1;
    }

    // 显示投资组合
    private static int showPortfolio(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        Portfolio portfolio = StockMarket.getPortfolio(player.getUUID());
        List<Portfolio.HoldingInfo> holdings = portfolio.getHoldingDetails(StockMarket::getStock);
        
        if (holdings.isEmpty()) {
            source.sendSuccess(() -> Component.literal("您还没有任何股票投资").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        
        long totalValue = portfolio.calculateTotalValue(StockMarket::getStock);
        long totalCost = portfolio.getTotalCost();
        long profitLoss = totalValue - totalCost;
        double returnRate = portfolio.calculateReturnRate(StockMarket::getStock);
        
        source.sendSuccess(() -> Component.literal("=== 投资组合 ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("总投资: " + totalCost + " 金币").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("当前价值: " + totalValue + " 金币").withStyle(ChatFormatting.GRAY), false);
        
        ChatFormatting profitColor = profitLoss >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> Component.literal("盈亏: " + (profitLoss >= 0 ? "+" : "") + profitLoss + 
                " 金币 (" + (returnRate >= 0 ? "+" : "") + df.format(returnRate) + "%)")
                .withStyle(profitColor), false);
        
        source.sendSuccess(() -> Component.literal("风险评估: " + portfolio.getRiskAssessment(StockMarket::getStock))
                .withStyle(ChatFormatting.YELLOW), false);
        
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("持仓详情:").withStyle(ChatFormatting.BLUE), false);
        source.sendSuccess(() -> Component.literal("代码    股数    成本价  当前价  盈亏%").withStyle(ChatFormatting.GRAY), false);
        
        for (Portfolio.HoldingInfo holding : holdings) {
            ChatFormatting color = holding.getReturnRate() >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
            source.sendSuccess(() -> Component.literal(String.format("%-8s %6d  %6d  %6d  %s",
                    holding.getSymbol(),
                    holding.getShares(),
                    holding.getAveragePrice(),
                    holding.getCurrentPrice(),
                    (holding.getReturnRate() >= 0 ? "+" : "") + df.format(holding.getReturnRate()) + "%"))
                    .withStyle(color), false);
        }
        
        return 1;
    }

    // 显示市场概览
    private static int showMarketOverview(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== 股票市场概览 ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("市场状态: " + (StockMarket.isMarketOpen() ? "开市" : "闭市"))
                .withStyle(StockMarket.isMarketOpen() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        source.sendSuccess(() -> Component.literal("市场指数: " + StockMarket.getMarketIndex()).withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("总市值: " + StockMarket.getTotalMarketCap() + " 金币").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("上市公司数: " + StockMarket.getTotalListedCompanies()).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("24h成交量: " + StockMarket.getTotalVolume()).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("市场摘要: " + StockMarket.getMarketSummary()).withStyle(ChatFormatting.YELLOW), false);
        
        return 1;
    }

    // 显示涨幅榜
    private static int showTopGainers(CommandSourceStack source, int limit) {
        List<Stock> gainers = StockMarket.getTopGainers(limit);
        
        source.sendSuccess(() -> Component.literal("=== 涨幅榜 ===").withStyle(ChatFormatting.GREEN), false);
        
        for (int i = 0; i < gainers.size(); i++) {
            Stock stock = gainers.get(i);
            double change = stock.getPriceChangePercent();
            source.sendSuccess(() -> Component.literal(String.format("%d. %s (%s) %d金币 +%.2f%%",
                    i + 1, stock.getSymbol(), stock.getCompanyName(), 
                    stock.getCurrentPrice(), change))
                    .withStyle(ChatFormatting.GREEN), false);
        }
        
        return 1;
    }

    // 显示跌幅榜
    private static int showTopLosers(CommandSourceStack source, int limit) {
        List<Stock> losers = StockMarket.getTopLosers(limit);
        
        source.sendSuccess(() -> Component.literal("=== 跌幅榜 ===").withStyle(ChatFormatting.RED), false);
        
        for (int i = 0; i < losers.size(); i++) {
            Stock stock = losers.get(i);
            double change = stock.getPriceChangePercent();
            source.sendSuccess(() -> Component.literal(String.format("%d. %s (%s) %d金币 %.2f%%",
                    i + 1, stock.getSymbol(), stock.getCompanyName(),
                    stock.getCurrentPrice(), change))
                    .withStyle(ChatFormatting.RED), false);
        }
        
        return 1;
    }

    // 搜索股票
    private static int searchStocks(CommandSourceStack source, String keyword) {
        List<Stock> results = StockMarket.searchStocks(keyword);
        
        if (results.isEmpty()) {
            source.sendFailure(Component.literal("未找到匹配的股票: " + keyword));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== 搜索结果: " + keyword + " ===").withStyle(ChatFormatting.YELLOW), false);
        
        for (Stock stock : results) {
            double change = stock.getPriceChangePercent();
            ChatFormatting color = change > 0 ? ChatFormatting.GREEN : 
                                  change < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW;
            
            source.sendSuccess(() -> Component.literal(String.format("%s - %s (%s) %d金币 %s%.2f%%",
                    stock.getSymbol(), stock.getCompanyName(), stock.getIndustry(),
                    stock.getCurrentPrice(), change >= 0 ? "+" : "", change))
                    .withStyle(color), false);
        }
        
        return 1;
    }

    // 显示行业股票
    private static int showIndustryStocks(CommandSourceStack source, String industry) {
        List<Stock> stocks = StockMarket.getStocksByIndustry(industry);
        
        if (stocks.isEmpty()) {
            source.sendFailure(Component.literal("未找到 " + industry + " 行业的股票"));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== " + industry + " 行业股票 ===").withStyle(ChatFormatting.BLUE), false);
        
        for (Stock stock : stocks) {
            double change = stock.getPriceChangePercent();
            ChatFormatting color = change > 0 ? ChatFormatting.GREEN : 
                                  change < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW;
            
            source.sendSuccess(() -> Component.literal(String.format("%s - %s %d金币 %s%.2f%%",
                    stock.getSymbol(), stock.getCompanyName(),
                    stock.getCurrentPrice(), change >= 0 ? "+" : "", change))
                    .withStyle(color), false);
        }
        
        return 1;
    }

    // 显示所有行业
    private static int showIndustries(CommandSourceStack source) {
        List<String> industries = StockMarket.getAllIndustries();
        
        source.sendSuccess(() -> Component.literal("=== 行业分类 ===").withStyle(ChatFormatting.BLUE), false);
        
        for (String industry : industries) {
            int count = StockMarket.getStocksByIndustry(industry).size();
            source.sendSuccess(() -> Component.literal(industry + " (" + count + " 只股票)")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        
        return 1;
    }

    // 显示交易历史
    private static int showTransactionHistory(CommandSourceStack source, int limit) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        Portfolio portfolio = StockMarket.getPortfolio(player.getUUID());
        List<StockTransaction> transactions = portfolio.getTransactionHistory(limit);
        
        if (transactions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("暂无股票交易记录").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("=== 股票交易历史 ===").withStyle(ChatFormatting.GOLD), false);
        
        for (StockTransaction transaction : transactions) {
            ChatFormatting color = transaction.getType().isIncome() ? ChatFormatting.GREEN : ChatFormatting.RED;
            source.sendSuccess(() -> Component.literal(transaction.toString()).withStyle(color), false);
        }
        
        return 1;
    }

    // 收取股息
    private static int collectDividends(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        Portfolio portfolio = StockMarket.getPortfolio(player.getUUID());
        long dividends = portfolio.collectDividends(StockMarket::getStock);
        
        if (dividends > 0) {
            source.sendSuccess(() -> Component.literal("收到股息 " + dividends + " 金币").withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendSuccess(() -> Component.literal("当前没有可收取的股息").withStyle(ChatFormatting.YELLOW), false);
        }
        
        return 1;
    }

    // 管理员命令：更新价格
    private static int adminUpdatePrices(CommandSourceStack source) {
        StockMarket.updateAllPrices();
        source.sendSuccess(() -> Component.literal("已更新所有股票价格"), false);
        return 1;
    }

    // 管理员命令：添加股票
    private static int adminAddStock(CommandSourceStack source, String symbol, String company, 
                                   String industry, long price, long shares) {
        if (StockMarket.addStock(symbol, company, industry, price, shares)) {
            source.sendSuccess(() -> Component.literal("成功添加股票: " + symbol + " - " + company), false);
        } else {
            source.sendFailure(Component.literal("添加股票失败，可能是代码已存在"));
        }
        return 1;
    }

    // 管理员命令：发放股息
    private static int adminDistributeDividends(CommandSourceStack source) {
        StockMarket.distributeDividends();
        source.sendSuccess(() -> Component.literal("已发放所有股息"), false);
        return 1;
    }

    // 管理员命令：股票分割
    private static int adminStockSplit(CommandSourceStack source, String symbol, int ratio) {
        if (StockMarket.stockSplit(symbol, ratio)) {
            source.sendSuccess(() -> Component.literal("股票 " + symbol + " 已执行 1:" + ratio + " 分割"), false);
        } else {
            source.sendFailure(Component.literal("股票分割失败"));
        }
        return 1;
    }

    // 管理员命令：开市
    private static int adminOpenMarket(CommandSourceStack source) {
        StockMarket.openMarket();
        source.sendSuccess(() -> Component.literal("股票市场已开市"), false);
        return 1;
    }

    // 管理员命令：闭市
    private static int adminCloseMarket(CommandSourceStack source) {
        StockMarket.closeMarket();
        source.sendSuccess(() -> Component.literal("股票市场已闭市"), false);
        return 1;
    }
}