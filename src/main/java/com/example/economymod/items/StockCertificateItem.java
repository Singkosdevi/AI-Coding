package com.example.economymod.items;

import com.example.economymod.stock.StockMarket;
import com.example.economymod.stock.Stock;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 股票证书物品类
public class StockCertificateItem extends Item {
    private static final DecimalFormat df = new DecimalFormat("#.##");
    
    public StockCertificateItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("symbol") && tag.contains("shares")) {
            String symbol = tag.getString("symbol");
            long shares = tag.getLong("shares");
            long purchasePrice = tag.getLong("purchasePrice");
            String purchaseDate = tag.getString("purchaseDate");
            
            Stock stock = StockMarket.getStock(symbol);
            
            tooltip.add(Component.literal("股票证书").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("股票代码: " + symbol).withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.literal("持有股数: " + shares).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("买入价格: " + purchasePrice + " 金币/股").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("买入日期: " + purchaseDate).withStyle(ChatFormatting.GRAY));
            
            if (stock != null) {
                long currentPrice = stock.getCurrentPrice();
                long totalCost = shares * purchasePrice;
                long currentValue = shares * currentPrice;
                long profitLoss = currentValue - totalCost;
                double returnRate = totalCost > 0 ? ((double) profitLoss / totalCost) * 100 : 0;
                
                tooltip.add(Component.literal("公司名称: " + stock.getCompanyName()).withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("当前价格: " + currentPrice + " 金币/股").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("当前价值: " + currentValue + " 金币").withStyle(ChatFormatting.YELLOW));
                
                ChatFormatting profitColor = profitLoss >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                tooltip.add(Component.literal("盈亏: " + (profitLoss >= 0 ? "+" : "") + profitLoss + 
                        " 金币 (" + (returnRate >= 0 ? "+" : "") + df.format(returnRate) + "%)")
                        .withStyle(profitColor));
                
                tooltip.add(Component.literal("股票状态: " + stock.getStatusDescription()).withStyle(ChatFormatting.BLUE));
            } else {
                tooltip.add(Component.literal("⚠️ 股票已退市").withStyle(ChatFormatting.RED));
            }
            
        } else {
            tooltip.add(Component.literal("空白股票证书").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("通过股票交易获得").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // 创建股票证书的静态方法
    public static ItemStack createCertificate(String symbol, long shares, long purchasePrice) {
        ItemStack stack = new ItemStack(ModItems.STOCK_CERTIFICATE.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("symbol", symbol);
        tag.putLong("shares", shares);
        tag.putLong("purchasePrice", purchasePrice);
        tag.putString("purchaseDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        tag.putLong("timestamp", System.currentTimeMillis());
        stack.setTag(tag);
        
        // 设置自定义名称
        Stock stock = StockMarket.getStock(symbol);
        String companyName = stock != null ? stock.getCompanyName() : symbol;
        stack.setHoverName(Component.literal(companyName + " 股票证书")
                .withStyle(ChatFormatting.GOLD));
        
        return stack;
    }
    
    // 获取股票代码
    public static String getSymbol(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString("symbol") : "";
    }
    
    // 获取股数
    public static long getShares(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getLong("shares") : 0;
    }
    
    // 获取买入价格
    public static long getPurchasePrice(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getLong("purchasePrice") : 0;
    }
    
    // 验证证书有效性
    public static boolean isValidCertificate(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("symbol") && tag.contains("shares") && 
               !tag.getString("symbol").isEmpty() && tag.getLong("shares") > 0;
    }
}