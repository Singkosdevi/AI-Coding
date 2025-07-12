package com.example.economymod.items;

import com.example.economymod.economy.EconomyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// 银行卡物品类
public class BankCardItem extends Item {
    public BankCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            // 显示玩家的银行余额和账户信息
            long balance = EconomyManager.getMoney(player.getUUID());
            long savings = EconomyManager.getSavings(player.getUUID());
            
            player.displayClientMessage(
                Component.literal("=== 银行账户信息 ===").withStyle(ChatFormatting.GOLD), false
            );
            player.displayClientMessage(
                Component.literal("钱包余额: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(balance)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" 金币")),
                false
            );
            player.displayClientMessage(
                Component.literal("储蓄余额: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(savings)).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" 金币")),
                false
            );
            
            long total = balance + savings;
            player.displayClientMessage(
                Component.literal("总资产: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(total)).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" 金币")),
                false
            );
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        tooltip.add(Component.literal("银行账户卡片").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("右键查看账户信息").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("在ATM或银行处使用").withStyle(ChatFormatting.DARK_GRAY));
    }
}