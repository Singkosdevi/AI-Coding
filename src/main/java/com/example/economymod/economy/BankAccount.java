package com.example.economymod.economy;

import java.time.LocalDateTime;

// 银行账户数据模型
public class BankAccount {
    private long savings;
    private LocalDateTime lastInterestCalculation;
    private long totalInterestEarned;
    private long totalDeposits;
    private long totalWithdrawals;
    
    public BankAccount() {
        this.savings = 0;
        this.lastInterestCalculation = LocalDateTime.now();
        this.totalInterestEarned = 0;
        this.totalDeposits = 0;
        this.totalWithdrawals = 0;
    }
    
    // 储蓄相关方法
    public long getSavings() {
        return savings;
    }
    
    public void addSavings(long amount) {
        if (amount > 0) {
            savings += amount;
            totalDeposits += amount;
        }
    }
    
    public boolean subtractSavings(long amount) {
        if (amount > 0 && savings >= amount) {
            savings -= amount;
            totalWithdrawals += amount;
            return true;
        }
        return false;
    }
    
    public void setSavings(long savings) {
        this.savings = Math.max(0, savings);
    }
    
    // 利息相关
    public void addInterest(long interest) {
        if (interest > 0) {
            savings += interest;
            totalInterestEarned += interest;
            lastInterestCalculation = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getLastInterestCalculation() {
        return lastInterestCalculation;
    }
    
    public long getTotalInterestEarned() {
        return totalInterestEarned;
    }
    
    // 统计相关
    public long getTotalDeposits() {
        return totalDeposits;
    }
    
    public long getTotalWithdrawals() {
        return totalWithdrawals;
    }
    
    public long getNetSavings() {
        return totalDeposits - totalWithdrawals + totalInterestEarned;
    }
    
    // 计算年化收益率
    public double getAnnualReturnRate() {
        if (totalDeposits == 0) return 0.0;
        return (double) totalInterestEarned / totalDeposits;
    }
}