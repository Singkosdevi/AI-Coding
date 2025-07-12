package com.example.economymod.economy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 交易记录数据模型
public class Transaction {
    private TransactionType type;
    private long amount;
    private String description;
    private LocalDateTime timestamp;
    private String transactionId;
    
    public Transaction(TransactionType type, long amount, String description, LocalDateTime timestamp) {
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.transactionId = generateTransactionId();
    }
    
    // Getter方法
    public TransactionType getType() {
        return type;
    }
    
    public long getAmount() {
        return amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    // 生成交易ID
    private String generateTransactionId() {
        return String.format("TXN-%d-%s", 
                System.currentTimeMillis(), 
                Integer.toHexString(hashCode()).toUpperCase());
    }
    
    // 格式化时间显示
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    // 获取交易类型的中文描述
    public String getTypeDescription() {
        return type.getChineseDescription();
    }
    
    // 判断是否为收入交易
    public boolean isIncome() {
        return type.isIncome();
    }
    
    // 判断是否为支出交易
    public boolean isExpense() {
        return type.isExpense();
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %+d 金币 - %s", 
                getFormattedTimestamp(), 
                getTypeDescription(), 
                isIncome() ? amount : -amount, 
                description);
    }
}