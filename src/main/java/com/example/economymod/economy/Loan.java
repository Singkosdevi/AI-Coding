package com.example.economymod.economy;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

// 贷款数据模型
public class Loan {
    private UUID borrowerId;
    private long originalAmount;
    private long remainingAmount;
    private double interestRate;
    private LocalDateTime issueDate;
    private LocalDateTime dueDate;
    private boolean isOverdue;
    private long totalRepaid;
    
    public Loan(UUID borrowerId, long amount, double interestRate, int termDays) {
        this.borrowerId = borrowerId;
        this.originalAmount = amount;
        this.remainingAmount = calculateTotalWithInterest(amount, interestRate);
        this.interestRate = interestRate;
        this.issueDate = LocalDateTime.now();
        this.dueDate = issueDate.plusDays(termDays);
        this.isOverdue = false;
        this.totalRepaid = 0;
    }
    
    // 计算包含利息的总金额
    private long calculateTotalWithInterest(long principal, double rate) {
        return (long) (principal * (1 + rate));
    }
    
    // 还款
    public void repay(long amount) {
        if (amount > 0 && amount <= remainingAmount) {
            remainingAmount -= amount;
            totalRepaid += amount;
        }
    }
    
    // 检查是否已全额还清
    public boolean isFullyRepaid() {
        return remainingAmount <= 0;
    }
    
    // 检查是否逾期
    public boolean checkOverdue() {
        if (LocalDateTime.now().isAfter(dueDate) && !isFullyRepaid()) {
            isOverdue = true;
        }
        return isOverdue;
    }
    
    // 计算逾期天数
    public long getOverdueDays() {
        if (!checkOverdue()) return 0;
        return ChronoUnit.DAYS.between(dueDate, LocalDateTime.now());
    }
    
    // 计算逾期罚金
    public long getOverduePenalty() {
        long overdueDays = getOverdueDays();
        if (overdueDays > 0) {
            // 每天0.1%的逾期罚金
            return (long) (originalAmount * 0.001 * overdueDays);
        }
        return 0;
    }
    
    // 计算剩余天数
    public long getRemainingDays() {
        if (isFullyRepaid()) return 0;
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
        return Math.max(0, days);
    }
    
    // Getter方法
    public UUID getBorrowerId() {
        return borrowerId;
    }
    
    public long getOriginalAmount() {
        return originalAmount;
    }
    
    public long getRemainingAmount() {
        return remainingAmount + getOverduePenalty();
    }
    
    public double getInterestRate() {
        return interestRate;
    }
    
    public LocalDateTime getIssueDate() {
        return issueDate;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public boolean isOverdue() {
        return checkOverdue();
    }
    
    public long getTotalRepaid() {
        return totalRepaid;
    }
    
    // 获取贷款状态描述
    public String getStatusDescription() {
        if (isFullyRepaid()) {
            return "已还清";
        } else if (isOverdue()) {
            return "逾期 (" + getOverdueDays() + " 天)";
        } else {
            return "正常 (剩余 " + getRemainingDays() + " 天)";
        }
    }
}