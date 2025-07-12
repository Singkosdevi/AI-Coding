package com.example.economymod.economy;

import java.time.LocalDateTime;

// 经济统计数据模型
public class EconomyStats {
    private long totalTransactions;
    private long totalTransactionValue;
    private long totalTaxCollected;
    private long totalLoansIssued;
    private long totalLoanValue;
    private long totalInterestPaid;
    private long totalShopsCreated;
    private long totalAuctionsCompleted;
    private LocalDateTime lastReset;
    
    public EconomyStats() {
        this.totalTransactions = 0;
        this.totalTransactionValue = 0;
        this.totalTaxCollected = 0;
        this.totalLoansIssued = 0;
        this.totalLoanValue = 0;
        this.totalInterestPaid = 0;
        this.totalShopsCreated = 0;
        this.totalAuctionsCompleted = 0;
        this.lastReset = LocalDateTime.now();
    }
    
    // 添加交易统计
    public void addTransaction(long value) {
        totalTransactions++;
        totalTransactionValue += value;
    }
    
    // 添加税收统计
    public void addTax(long tax) {
        totalTaxCollected += tax;
    }
    
    // 添加贷款统计
    public void addLoan(long amount) {
        totalLoansIssued++;
        totalLoanValue += amount;
    }
    
    // 添加利息统计
    public void addInterest(long interest) {
        totalInterestPaid += interest;
    }
    
    // 添加商店统计
    public void addShop() {
        totalShopsCreated++;
    }
    
    // 添加拍卖统计
    public void addAuction() {
        totalAuctionsCompleted++;
    }
    
    // 重置统计
    public void reset() {
        totalTransactions = 0;
        totalTransactionValue = 0;
        totalTaxCollected = 0;
        totalLoansIssued = 0;
        totalLoanValue = 0;
        totalInterestPaid = 0;
        totalShopsCreated = 0;
        totalAuctionsCompleted = 0;
        lastReset = LocalDateTime.now();
    }
    
    // 计算平均交易价值
    public double getAverageTransactionValue() {
        if (totalTransactions == 0) return 0.0;
        return (double) totalTransactionValue / totalTransactions;
    }
    
    // 计算平均贷款金额
    public double getAverageLoanAmount() {
        if (totalLoansIssued == 0) return 0.0;
        return (double) totalLoanValue / totalLoansIssued;
    }
    
    // Getter方法
    public long getTotalTransactions() {
        return totalTransactions;
    }
    
    public long getTotalTransactionValue() {
        return totalTransactionValue;
    }
    
    public long getTotalTaxCollected() {
        return totalTaxCollected;
    }
    
    public long getTotalLoansIssued() {
        return totalLoansIssued;
    }
    
    public long getTotalLoanValue() {
        return totalLoanValue;
    }
    
    public long getTotalInterestPaid() {
        return totalInterestPaid;
    }
    
    public long getTotalShopsCreated() {
        return totalShopsCreated;
    }
    
    public long getTotalAuctionsCompleted() {
        return totalAuctionsCompleted;
    }
    
    public LocalDateTime getLastReset() {
        return lastReset;
    }
}