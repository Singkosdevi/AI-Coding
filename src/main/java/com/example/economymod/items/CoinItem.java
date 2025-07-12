package com.example.economymod.items;

import com.example.economymod.economy.EconomyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// 货币物品类
public class CoinItem extends Item {
    private final int value;

    public CoinItem(Properties properties, int value) {
        super(properties);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            int amount = stack.getCount();
            int totalValue = value * amount;

            // 将硬币转换为虚拟货币
            EconomyManager.addMoney(player.getUUID(), totalValue);
            player.displayClientMessage(
                Component.literal("已将 ")
                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" 个硬币转换为 "))
                    .append(Component.literal(String.valueOf(totalValue)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" 金币")),
                false
            );

            // 播放声音
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);

            stack.shrink(amount);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        tooltip.add(Component.literal("价值: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" 金币")));
        
        tooltip.add(Component.literal("右键使用转换为虚拟货币")
                .withStyle(ChatFormatting.DARK_GRAY));
        
        if (stack.getCount() > 1) {
            int totalValue = value * stack.getCount();
            tooltip.add(Component.literal("总价值: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(totalValue)).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" 金币")));
        }
    }
}