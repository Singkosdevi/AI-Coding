package com.example.economymod.items;

import com.example.economymod.economy.EconomyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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

// 支票物品类
public class CheckItem extends Item {
    public CheckItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("amount") && tag.contains("issuer")) {
                long amount = tag.getLong("amount");
                String issuer = tag.getString("issuer");
                
                // 兑现支票
                EconomyManager.addMoney(player.getUUID(), amount);
                player.displayClientMessage(
                    Component.literal("成功兑现支票！获得 ")
                        .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" 金币"))
                        .append(Component.literal(" (来自: " + issuer + ")")),
                    false
                );
                
                stack.shrink(1);
            } else {
                player.displayClientMessage(
                    Component.literal("这是一张空白支票，无法兑现").withStyle(ChatFormatting.RED),
                    false
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("amount") && tag.contains("issuer")) {
            long amount = tag.getLong("amount");
            String issuer = tag.getString("issuer");
            
            tooltip.add(Component.literal("金额: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" 金币")));
            tooltip.add(Component.literal("发行人: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(issuer).withStyle(ChatFormatting.BLUE)));
            tooltip.add(Component.literal("右键兑现").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("空白支票").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("使用 /economy check <金额> <玩家> 开具").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // 创建支票的静态方法
    public static ItemStack createCheck(long amount, String issuer, String recipient) {
        ItemStack stack = new ItemStack(ModItems.CHECK.get());
        CompoundTag tag = new CompoundTag();
        tag.putLong("amount", amount);
        tag.putString("issuer", issuer);
        tag.putString("recipient", recipient);
        tag.putLong("timestamp", System.currentTimeMillis());
        stack.setTag(tag);
        
        // 设置自定义名称
        stack.setHoverName(Component.literal("支票 - " + amount + " 金币")
                .withStyle(ChatFormatting.YELLOW));
        
        return stack;
    }
}