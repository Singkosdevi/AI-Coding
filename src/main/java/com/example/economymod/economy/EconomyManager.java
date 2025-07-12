package com.example.economymod.economy;

import com.example.economymod.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// 经济管理器 - 核心经济系统
public class EconomyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // 玩家账户数据
    private static final Map<UUID, PlayerAccount> playerAccounts = new ConcurrentHashMap<>();
    
    // 银行数据
    private static final Map<UUID, BankAccount> bankAccounts = new ConcurrentHashMap<>();
    
    // 交易历史
    private static final Map<UUID, List<Transaction>> transactionHistory = new ConcurrentHashMap<>();
    
    // 商店数据
    private static final Map<String, PlayerShop> playerShops = new ConcurrentHashMap<>();
    
    // 拍卖数据
    private static final Map<Integer, Auction> activeAuctions = new ConcurrentHashMap<>();
    private static int nextAuctionId = 1;
    
    // 贷款数据
    private static final Map<UUID, Loan> playerLoans = new ConcurrentHashMap<>();
    
    // 系统统计
    private static EconomyStats economyStats = new EconomyStats();

    // 初始化经济系统
    public static void init() {
        LOGGER.info("正在初始化经济管理系统...");
        loadData();
        
        // 启动定时任务
        startDailyTasks();
        LOGGER.info("经济管理系统初始化完成");
    }

    // 获取玩家钱包余额
    public static long getMoney(UUID playerId) {
        return playerAccounts.computeIfAbsent(playerId, k -> new PlayerAccount()).getBalance();
    }

    // 获取玩家储蓄余额
    public static long getSavings(UUID playerId) {
        return bankAccounts.computeIfAbsent(playerId, k -> new BankAccount()).getSavings();
    }

    // 获取交易次数
    public static long getTransactionCount(UUID playerId) {
        return transactionHistory.getOrDefault(playerId, new ArrayList<>()).size();
    }

    // 添加金钱到钱包
    public static boolean addMoney(UUID playerId, long amount) {
        if (amount <= 0) return false;
        
        PlayerAccount account = playerAccounts.computeIfAbsent(playerId, k -> new PlayerAccount());
        account.addBalance(amount);
        
        // 记录交易
        recordTransaction(playerId, TransactionType.DEPOSIT, amount, "系统添加金钱");
        
        saveData();
        return true;
    }

    // 从钱包扣除金钱
    public static boolean removeMoney(UUID playerId, long amount) {
        if (amount <= 0) return false;
        
        PlayerAccount account = playerAccounts.get(playerId);
        if (account == null || account.getBalance() < amount) {
            return false;
        }
        
        account.subtractBalance(amount);
        
        // 记录交易
        recordTransaction(playerId, TransactionType.WITHDRAWAL, amount, "系统扣除金钱");
        
        saveData();
        return true;
    }

    // 转账功能
    public static boolean transferMoney(UUID fromPlayer, UUID toPlayer, long amount, String memo) {
        if (amount <= 0) return false;
        
        PlayerAccount fromAccount = playerAccounts.get(fromPlayer);
        if (fromAccount == null || fromAccount.getBalance() < amount) {
            return false;
        }
        
        // 计算税费
        long tax = (long) (amount * Config.transactionTax);
        long actualAmount = amount - tax;
        
        // 执行转账
        fromAccount.subtractBalance(amount);
        PlayerAccount toAccount = playerAccounts.computeIfAbsent(toPlayer, k -> new PlayerAccount());
        toAccount.addBalance(actualAmount);
        
        // 记录交易
        recordTransaction(fromPlayer, TransactionType.TRANSFER_OUT, amount, 
                "转账给玩家 (税费: " + tax + ", 备注: " + memo + ")");
        recordTransaction(toPlayer, TransactionType.TRANSFER_IN, actualAmount, 
                "来自玩家的转账 (备注: " + memo + ")");
        
        // 更新统计
        economyStats.addTransaction(amount);
        economyStats.addTax(tax);
        
        saveData();
        return true;
    }

    // 存款到银行
    public static boolean deposit(UUID playerId, long amount) {
        if (amount <= 0) return false;
        
        PlayerAccount account = playerAccounts.get(playerId);
        if (account == null || account.getBalance() < amount) {
            return false;
        }
        
        BankAccount bankAccount = bankAccounts.computeIfAbsent(playerId, k -> new BankAccount());
        
        account.subtractBalance(amount);
        bankAccount.addSavings(amount);
        
        recordTransaction(playerId, TransactionType.BANK_DEPOSIT, amount, "存入银行储蓄账户");
        
        saveData();
        return true;
    }

    // 从银行取款
    public static boolean withdraw(UUID playerId, long amount) {
        if (amount <= 0) return false;
        
        BankAccount bankAccount = bankAccounts.get(playerId);
        if (bankAccount == null || bankAccount.getSavings() < amount) {
            return false;
        }
        
        PlayerAccount account = playerAccounts.computeIfAbsent(playerId, k -> new PlayerAccount());
        
        bankAccount.subtractSavings(amount);
        account.addBalance(amount);
        
        recordTransaction(playerId, TransactionType.BANK_WITHDRAWAL, amount, "从银行储蓄账户取出");
        
        saveData();
        return true;
    }

    // 申请贷款
    public static boolean requestLoan(UUID playerId, long amount) {
        if (amount <= 0 || amount > 10000) return false; // 最大贷款限额
        
        if (playerLoans.containsKey(playerId)) {
            return false; // 已有贷款
        }
        
        PlayerAccount account = playerAccounts.computeIfAbsent(playerId, k -> new PlayerAccount());
        account.addBalance(amount);
        
        // 创建贷款记录
        Loan loan = new Loan(playerId, amount, 0.1, 30); // 10%利率，30天期限
        playerLoans.put(playerId, loan);
        
        recordTransaction(playerId, TransactionType.LOAN, amount, "银行贷款");
        
        saveData();
        return true;
    }

    // 还款
    public static boolean repayLoan(UUID playerId, long amount) {
        Loan loan = playerLoans.get(playerId);
        if (loan == null) return false;
        
        PlayerAccount account = playerAccounts.get(playerId);
        if (account == null || account.getBalance() < amount) {
            return false;
        }
        
        account.subtractBalance(amount);
        loan.repay(amount);
        
        if (loan.isFullyRepaid()) {
            playerLoans.remove(playerId);
        }
        
        recordTransaction(playerId, TransactionType.LOAN_REPAYMENT, amount, "贷款还款");
        
        saveData();
        return true;
    }

    // 获取贷款信息
    public static Loan getLoan(UUID playerId) {
        return playerLoans.get(playerId);
    }

    // 获取交易历史
    public static List<Transaction> getTransactionHistory(UUID playerId) {
        return new ArrayList<>(transactionHistory.getOrDefault(playerId, new ArrayList<>()));
    }

    // 记录交易
    private static void recordTransaction(UUID playerId, TransactionType type, long amount, String description) {
        Transaction transaction = new Transaction(type, amount, description, LocalDateTime.now());
        transactionHistory.computeIfAbsent(playerId, k -> new ArrayList<>()).add(transaction);
        
        // 限制历史记录数量
        List<Transaction> history = transactionHistory.get(playerId);
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    // 每日任务
    private static void startDailyTasks() {
        // 这里可以添加定时任务，如利息计算、每日奖励等
        // 在实际实现中，您可能需要使用调度器
    }

    // 计算并发放银行利息
    public static void calculateInterest() {
        for (Map.Entry<UUID, BankAccount> entry : bankAccounts.entrySet()) {
            UUID playerId = entry.getKey();
            BankAccount bankAccount = entry.getValue();
            
            long savings = bankAccount.getSavings();
            if (savings > 0) {
                long interest = (long) (savings * Config.bankInterestRate);
                bankAccount.addSavings(interest);
                
                recordTransaction(playerId, TransactionType.INTEREST, interest, "银行存款利息");
            }
        }
        saveData();
    }

    // 发放每日奖励
    public static boolean claimDailyReward(UUID playerId) {
        if (!Config.enableDailyRewards) return false;
        
        PlayerAccount account = playerAccounts.computeIfAbsent(playerId, k -> new PlayerAccount());
        
        // 检查是否已经领取今日奖励
        if (account.hasClaimedDailyReward()) {
            return false;
        }
        
        account.addBalance(Config.dailyRewardAmount);
        account.setDailyRewardClaimed(true);
        
        recordTransaction(playerId, TransactionType.DAILY_REWARD, Config.dailyRewardAmount, "每日登录奖励");
        
        saveData();
        return true;
    }

    // 重置每日奖励状态
    public static void resetDailyRewards() {
        for (PlayerAccount account : playerAccounts.values()) {
            account.setDailyRewardClaimed(false);
        }
        saveData();
    }

    // 获取经济统计
    public static EconomyStats getEconomyStats() {
        return economyStats;
    }

    // 保存数据
    public static void saveData() {
        try {
            File dataDir = new File("world/economymod");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            // 保存玩家账户
            saveToFile(playerAccounts, new File(dataDir, "player_accounts.json"));
            
            // 保存银行账户
            saveToFile(bankAccounts, new File(dataDir, "bank_accounts.json"));
            
            // 保存交易历史
            saveToFile(transactionHistory, new File(dataDir, "transaction_history.json"));
            
            // 保存商店数据
            saveToFile(playerShops, new File(dataDir, "player_shops.json"));
            
            // 保存拍卖数据
            saveToFile(activeAuctions, new File(dataDir, "auctions.json"));
            
            // 保存贷款数据
            saveToFile(playerLoans, new File(dataDir, "loans.json"));
            
            // 保存统计数据
            saveToFile(economyStats, new File(dataDir, "economy_stats.json"));
            
        } catch (Exception e) {
            LOGGER.error("保存经济数据时发生错误", e);
        }
    }

    // 加载数据
    public static void loadData() {
        try {
            File dataDir = new File("world/economymod");
            if (!dataDir.exists()) {
                LOGGER.info("经济数据目录不存在，将创建新的数据");
                return;
            }
            
            // 加载玩家账户
            loadFromFile(new File(dataDir, "player_accounts.json"), 
                    new TypeToken<Map<UUID, PlayerAccount>>(){}.getType(), playerAccounts);
            
            // 加载银行账户
            loadFromFile(new File(dataDir, "bank_accounts.json"), 
                    new TypeToken<Map<UUID, BankAccount>>(){}.getType(), bankAccounts);
            
            // 加载交易历史
            loadFromFile(new File(dataDir, "transaction_history.json"), 
                    new TypeToken<Map<UUID, List<Transaction>>>(){}.getType(), transactionHistory);
            
            // 加载商店数据
            loadFromFile(new File(dataDir, "player_shops.json"), 
                    new TypeToken<Map<String, PlayerShop>>(){}.getType(), playerShops);
            
            // 加载拍卖数据
            loadFromFile(new File(dataDir, "auctions.json"), 
                    new TypeToken<Map<Integer, Auction>>(){}.getType(), activeAuctions);
            
            // 加载贷款数据
            loadFromFile(new File(dataDir, "loans.json"), 
                    new TypeToken<Map<UUID, Loan>>(){}.getType(), playerLoans);
            
            // 加载统计数据
            EconomyStats loadedStats = loadSingleFromFile(new File(dataDir, "economy_stats.json"), EconomyStats.class);
            if (loadedStats != null) {
                economyStats = loadedStats;
            }
            
            LOGGER.info("经济数据加载完成");
            
        } catch (Exception e) {
            LOGGER.error("加载经济数据时发生错误", e);
        }
    }

    // 保存单个对象到文件
    private static void saveToFile(Object data, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        }
    }

    // 从文件加载到Map
    private static <T> void loadFromFile(File file, Type type, Map<?, ?> targetMap) {
        if (!file.exists()) return;
        
        try (FileReader reader = new FileReader(file)) {
            Map<?, ?> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                targetMap.clear();
                targetMap.putAll(loaded);
            }
        } catch (Exception e) {
            LOGGER.error("加载文件时发生错误: " + file.getName(), e);
        }
    }

    // 从文件加载单个对象
    private static <T> T loadSingleFromFile(File file, Class<T> clazz) {
        if (!file.exists()) return null;
        
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, clazz);
        } catch (Exception e) {
            LOGGER.error("加载文件时发生错误: " + file.getName(), e);
            return null;
        }
    }

    // 初始化新玩家账户
    public static void initializePlayer(UUID playerId) {
        if (!playerAccounts.containsKey(playerId)) {
            PlayerAccount account = new PlayerAccount();
            account.addBalance(Config.startingMoney);
            playerAccounts.put(playerId, account);
            
            recordTransaction(playerId, TransactionType.INITIAL, Config.startingMoney, "新玩家初始资金");
            
            LOGGER.info("为新玩家初始化账户: " + playerId);
            saveData();
        }
    }

    // 获取玩家名称
    private static String getPlayerName(UUID playerId) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
        return player != null ? player.getName().getString() : "Unknown Player";
    }
}