package com.example.economymod.economy;

import java.time.LocalDate;

// 玩家账户数据模型
public class PlayerAccount {
    private long balance;
    private LocalDate lastLogin;
    private boolean dailyRewardClaimed;
    private long totalEarned;
    private long totalSpent;
    
    public PlayerAccount() {
        this.balance = 0;
        this.lastLogin = LocalDate.now();
        this.dailyRewardClaimed = false;
        this.totalEarned = 0;
        this.totalSpent = 0;
    }
    
    // 余额相关方法
    public long getBalance() {
        return balance;
    }
    
    public void addBalance(long amount) {
        if (amount > 0) {
            balance += amount;
            totalEarned += amount;
        }
    }
    
    public boolean subtractBalance(long amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
            totalSpent += amount;
            return true;
        }
        return false;
    }
    
    public void setBalance(long balance) {
        this.balance = Math.max(0, balance);
    }
    
    // 每日奖励相关
    public boolean hasClaimedDailyReward() {
        return dailyRewardClaimed && lastLogin.equals(LocalDate.now());
    }
    
    public void setDailyRewardClaimed(boolean claimed) {
        this.dailyRewardClaimed = claimed;
        if (claimed) {
            this.lastLogin = LocalDate.now();
        }
    }
    
    // 统计相关
    public long getTotalEarned() {
        return totalEarned;
    }
    
    public long getTotalSpent() {
        return totalSpent;
    }
    
    public long getNetWorth() {
        return totalEarned - totalSpent;
    }
    
    // 登录相关
    public LocalDate getLastLogin() {
        return lastLogin;
    }
    
    public void updateLastLogin() {
        this.lastLogin = LocalDate.now();
        // 如果是新的一天，重置每日奖励状态
        if (!dailyRewardClaimed || !lastLogin.equals(LocalDate.now())) {
            this.dailyRewardClaimed = false;
        }
    }
}