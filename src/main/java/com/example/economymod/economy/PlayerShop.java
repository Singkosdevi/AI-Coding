package com.example.economymod.economy;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 玩家商店数据模型
public class PlayerShop {
    private String shopName;
    private UUID ownerId;
    private String ownerName;
    private Map<String, ShopItem> items;
    private long totalRevenue;
    private long totalSales;
    private boolean isOpen;
    private String description;
    
    public PlayerShop(String shopName, UUID ownerId, String ownerName) {
        this.shopName = shopName;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.items = new HashMap<>();
        this.totalRevenue = 0;
        this.totalSales = 0;
        this.isOpen = true;
        this.description = "欢迎来到我的商店！";
    }
    
    // 添加商品
    public void addItem(String itemName, long price, int quantity) {
        ShopItem shopItem = items.get(itemName);
        if (shopItem != null) {
            shopItem.addQuantity(quantity);
        } else {
            items.put(itemName, new ShopItem(itemName, price, quantity));
        }
    }
    
    // 移除商品
    public boolean removeItem(String itemName, int quantity) {
        ShopItem shopItem = items.get(itemName);
        if (shopItem != null) {
            boolean removed = shopItem.removeQuantity(quantity);
            if (shopItem.getQuantity() <= 0) {
                items.remove(itemName);
            }
            return removed;
        }
        return false;
    }
    
    // 购买商品
    public boolean purchaseItem(String itemName, int quantity, UUID buyerId) {
        ShopItem shopItem = items.get(itemName);
        if (shopItem != null && shopItem.getQuantity() >= quantity) {
            long totalCost = shopItem.getPrice() * quantity;
            
            // 扣除商品数量
            shopItem.removeQuantity(quantity);
            if (shopItem.getQuantity() <= 0) {
                items.remove(itemName);
            }
            
            // 增加收入统计
            totalRevenue += totalCost;
            totalSales++;
            
            return true;
        }
        return false;
    }
    
    // 更新商品价格
    public boolean updatePrice(String itemName, long newPrice) {
        ShopItem shopItem = items.get(itemName);
        if (shopItem != null) {
            shopItem.setPrice(newPrice);
            return true;
        }
        return false;
    }
    
    // Getter和Setter方法
    public String getShopName() {
        return shopName;
    }
    
    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
    
    public UUID getOwnerId() {
        return ownerId;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public Map<String, ShopItem> getItems() {
        return new HashMap<>(items);
    }
    
    public long getTotalRevenue() {
        return totalRevenue;
    }
    
    public long getTotalSales() {
        return totalSales;
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    public void setOpen(boolean open) {
        isOpen = open;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    // 获取商品数量
    public int getItemCount() {
        return items.size();
    }
    
    // 检查是否有特定商品
    public boolean hasItem(String itemName) {
        return items.containsKey(itemName) && items.get(itemName).getQuantity() > 0;
    }
    
    // 获取商品信息
    public ShopItem getItem(String itemName) {
        return items.get(itemName);
    }
    
    // 商品内部类
    public static class ShopItem {
        private String itemName;
        private long price;
        private int quantity;
        
        public ShopItem(String itemName, long price, int quantity) {
            this.itemName = itemName;
            this.price = price;
            this.quantity = quantity;
        }
        
        public String getItemName() {
            return itemName;
        }
        
        public long getPrice() {
            return price;
        }
        
        public void setPrice(long price) {
            this.price = Math.max(0, price);
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void addQuantity(int amount) {
            if (amount > 0) {
                quantity += amount;
            }
        }
        
        public boolean removeQuantity(int amount) {
            if (amount > 0 && quantity >= amount) {
                quantity -= amount;
                return true;
            }
            return false;
        }
        
        public long getTotalValue() {
            return price * quantity;
        }
    }
}