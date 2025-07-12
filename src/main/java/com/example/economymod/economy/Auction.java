package com.example.economymod.economy;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 拍卖数据模型
public class Auction {
    private int auctionId;
    private UUID sellerId;
    private String sellerName;
    private String itemName;
    private String itemDescription;
    private long startingBid;
    private long currentBid;
    private UUID currentBidderId;
    private String currentBidderName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isActive;
    private boolean isCompleted;
    private Map<UUID, Long> bidHistory;
    
    public Auction(int auctionId, UUID sellerId, String sellerName, String itemName, 
                   long startingBid, int durationHours) {
        this.auctionId = auctionId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemName = itemName;
        this.startingBid = startingBid;
        this.currentBid = startingBid;
        this.startTime = LocalDateTime.now();
        this.endTime = startTime.plusHours(durationHours);
        this.isActive = true;
        this.isCompleted = false;
        this.bidHistory = new HashMap<>();
    }
    
    // 出价
    public boolean placeBid(UUID bidderId, String bidderName, long bidAmount) {
        if (!isActive || isCompleted || bidAmount <= currentBid) {
            return false;
        }
        
        // 检查是否超时
        if (LocalDateTime.now().isAfter(endTime)) {
            endAuction();
            return false;
        }
        
        // 如果是卖家自己出价，拒绝
        if (bidderId.equals(sellerId)) {
            return false;
        }
        
        currentBid = bidAmount;
        currentBidderId = bidderId;
        currentBidderName = bidderName;
        bidHistory.put(bidderId, bidAmount);
        
        return true;
    }
    
    // 结束拍卖
    public void endAuction() {
        isActive = false;
        isCompleted = true;
    }
    
    // 检查拍卖是否过期
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }
    
    // 获取剩余时间（分钟）
    public long getRemainingMinutes() {
        if (isCompleted || isExpired()) return 0;
        
        long minutes = java.time.Duration.between(LocalDateTime.now(), endTime).toMinutes();
        return Math.max(0, minutes);
    }
    
    // 获取拍卖状态
    public String getStatus() {
        if (isCompleted) {
            return currentBidderId != null ? "已成交" : "流拍";
        } else if (isExpired()) {
            return "已过期";
        } else if (isActive) {
            return "进行中";
        } else {
            return "未知状态";
        }
    }
    
    // 是否有获胜者
    public boolean hasWinner() {
        return isCompleted && currentBidderId != null;
    }
    
    // Getter方法
    public int getAuctionId() {
        return auctionId;
    }
    
    public UUID getSellerId() {
        return sellerId;
    }
    
    public String getSellerName() {
        return sellerName;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public String getItemDescription() {
        return itemDescription;
    }
    
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }
    
    public long getStartingBid() {
        return startingBid;
    }
    
    public long getCurrentBid() {
        return currentBid;
    }
    
    public UUID getCurrentBidderId() {
        return currentBidderId;
    }
    
    public String getCurrentBidderName() {
        return currentBidderName;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public boolean isActive() {
        return isActive && !isExpired();
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public Map<UUID, Long> getBidHistory() {
        return new HashMap<>(bidHistory);
    }
    
    public int getBidCount() {
        return bidHistory.size();
    }
    
    // 获取最低出价（当前出价 + 1）
    public long getMinimumBid() {
        return currentBid + 1;
    }
}