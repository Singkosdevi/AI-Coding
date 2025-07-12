package com.example.economymod.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// 商店契约物品类
public class ShopDeedItem extends Item {
    public ShopDeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        tooltip.add(Component.literal("商店所有权契约").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("允许建立玩家商店").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("在商店方块上使用").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("一次性使用物品").withStyle(ChatFormatting.RED));
    }
}