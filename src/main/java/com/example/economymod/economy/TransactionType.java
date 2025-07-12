package com.example.economymod.economy;

// 交易类型枚举
public enum TransactionType {
    // 基础交易
    DEPOSIT("存款", true),
    WITHDRAWAL("取款", false),
    TRANSFER_IN("转入", true),
    TRANSFER_OUT("转出", false),
    
    // 银行相关
    BANK_DEPOSIT("银行存款", false),
    BANK_WITHDRAWAL("银行取款", true),
    INTEREST("利息收入", true),
    
    // 贷款相关
    LOAN("贷款", true),
    LOAN_REPAYMENT("还款", false),
    
    // 商店交易
    SHOP_PURCHASE("商店购买", false),
    SHOP_SALE("商店销售", true),
    
    // 拍卖相关
    AUCTION_BID("拍卖出价", false),
    AUCTION_WIN("拍卖获胜", false),
    AUCTION_SELL("拍卖销售", true),
    AUCTION_REFUND("拍卖退款", true),
    
    // 系统相关
    INITIAL("初始资金", true),
    DAILY_REWARD("每日奖励", true),
    ADMIN_GIVE("管理员给予", true),
    ADMIN_TAKE("管理员扣除", false),
    TAX("税收", false),
    
    // 其他
    FINE("罚款", false),
    BONUS("奖金", true),
    REFUND("退款", true);
    
    private final String chineseDescription;
    private final boolean isIncome;
    
    TransactionType(String chineseDescription, boolean isIncome) {
        this.chineseDescription = chineseDescription;
        this.isIncome = isIncome;
    }
    
    public String getChineseDescription() {
        return chineseDescription;
    }
    
    public boolean isIncome() {
        return isIncome;
    }
    
    public boolean isExpense() {
        return !isIncome;
    }
}