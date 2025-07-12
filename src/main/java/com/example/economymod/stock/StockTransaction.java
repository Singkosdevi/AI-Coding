package com.example.economymod.stock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 股票交易记录类
public class StockTransaction {
    private String symbol;              // 股票代码
    private TransactionType type;       // 交易类型
    private long shares;               // 股票数量
    private long pricePerShare;        // 每股价格
    private long totalAmount;          // 总金额
    private LocalDateTime timestamp;    // 交易时间
    private String transactionId;      // 交易ID
    private String notes;              // 备注
    
    public StockTransaction(String symbol, TransactionType type, long shares, 
                           long pricePerShare, LocalDateTime timestamp) {
        this.symbol = symbol;
        this.type = type;
        this.shares = shares;
        this.pricePerShare = pricePerShare;
        this.totalAmount = shares * pricePerShare;
        this.timestamp = timestamp;
        this.transactionId = generateTransactionId();
        this.notes = "";
    }
    
    // 股票交易类型枚举
    public enum TransactionType {
        BUY("买入", true),
        SELL("卖出", false),
        DIVIDEND("股息", true),
        SPLIT("股票分割", false),
        BONUS("红股", true),
        RIGHTS("配股", true);
        
        private final String description;
        private final boolean isIncome;
        
        TransactionType(String description, boolean isIncome) {
            this.description = description;
            this.isIncome = isIncome;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isIncome() {
            return isIncome;
        }
    }
    
    // 生成交易ID
    private String generateTransactionId() {
        return String.format("STK-%d-%s", 
                System.currentTimeMillis(), 
                Integer.toHexString(hashCode()).toUpperCase().substring(0, 4));
    }
    
    // 格式化时间显示
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    // 获取交易方向（买入/卖出）
    public String getDirection() {
        switch (type) {
            case BUY:
            case DIVIDEND:
            case BONUS:
            case RIGHTS:
                return "买入";
            case SELL:
                return "卖出";
            case SPLIT:
                return "分割";
            default:
                return "其他";
        }
    }
    
    // 获取交易描述
    public String getTransactionDescription() {
        switch (type) {
            case BUY:
                return String.format("买入 %s %d股，每股%d金币", symbol, shares, pricePerShare);
            case SELL:
                return String.format("卖出 %s %d股，每股%d金币", symbol, shares, pricePerShare);
            case DIVIDEND:
                return String.format("%s 股息收入 %d金币", symbol, pricePerShare);
            case SPLIT:
                return String.format("%s 股票分割 %d股", symbol, shares);
            case BONUS:
                return String.format("%s 红股 %d股", symbol, shares);
            case RIGHTS:
                return String.format("%s 配股 %d股，每股%d金币", symbol, shares, pricePerShare);
            default:
                return String.format("%s %s", symbol, type.getDescription());
        }
    }
    
    // 计算手续费（简化版本）
    public long getCommissionFee() {
        if (type == TransactionType.BUY || type == TransactionType.SELL) {
            // 手续费为交易金额的0.1%，最低1金币
            return Math.max(1, totalAmount / 1000);
        }
        return 0;
    }
    
    // 计算税费（简化版本）
    public long getTaxFee() {
        if (type == TransactionType.SELL) {
            // 卖出时收取印花税，税率0.05%
            return Math.max(0, totalAmount / 2000);
        }
        return 0;
    }
    
    // 获取总费用（手续费+税费）
    public long getTotalFees() {
        return getCommissionFee() + getTaxFee();
    }
    
    // 获取净交易金额（扣除费用后）
    public long getNetAmount() {
        if (type == TransactionType.BUY) {
            return totalAmount + getTotalFees(); // 买入时加上费用
        } else if (type == TransactionType.SELL) {
            return totalAmount - getTotalFees(); // 卖出时扣除费用
        } else {
            return totalAmount; // 其他交易不收费
        }
    }
    
    // 判断是否为当日交易
    public boolean isToday() {
        return timestamp.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }
    
    // 判断是否为本周交易
    public boolean isThisWeek() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        return timestamp.isAfter(weekAgo);
    }
    
    // 判断是否为本月交易
    public boolean isThisMonth() {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        return timestamp.isAfter(monthAgo);
    }
    
    // 获取交易状态（成功/失败）
    public String getStatus() {
        // 在实际应用中，这里可能会有更复杂的状态逻辑
        return "成功";
    }
    
    // 获取交易影响（对投资组合的影响）
    public String getImpact() {
        switch (type) {
            case BUY:
                return "增加持仓";
            case SELL:
                return "减少持仓";
            case DIVIDEND:
                return "增加收入";
            case SPLIT:
                return "股数增加，价格调整";
            case BONUS:
                return "免费获得股票";
            case RIGHTS:
                return "优先购买权";
            default:
                return "其他影响";
        }
    }
    
    // 添加备注
    public void addNote(String note) {
        if (this.notes.isEmpty()) {
            this.notes = note;
        } else {
            this.notes += "; " + note;
        }
    }
    
    // Getter方法
    public String getSymbol() { return symbol; }
    public TransactionType getType() { return type; }
    public long getShares() { return shares; }
    public long getPricePerShare() { return pricePerShare; }
    public long getTotalAmount() { return totalAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getTransactionId() { return transactionId; }
    public String getNotes() { return notes; }
    
    // Setter方法
    public void setNotes(String notes) { this.notes = notes; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s", 
                getFormattedTimestamp(), 
                getTransactionDescription(),
                getStatus());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        StockTransaction that = (StockTransaction) obj;
        return transactionId.equals(that.transactionId);
    }
    
    @Override
    public int hashCode() {
        return transactionId.hashCode();
    }
}