package com.example.economymod.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// 保险箱方块类 - 高级存储系统
public class VaultBlock extends Block {
    public VaultBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
                                InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            showVaultMenu(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private void showVaultMenu(Player player) {
        player.displayClientMessage(
            Component.literal("======= 私人保险箱 =======").withStyle(ChatFormatting.DARK_PURPLE), false
        );
        player.displayClientMessage(
            Component.literal("欢迎使用高级保险箱服务！").withStyle(ChatFormatting.LIGHT_PURPLE), false
        );
        player.displayClientMessage(Component.literal(""), false);
        
        player.displayClientMessage(
            Component.literal("保险箱功能:").withStyle(ChatFormatting.BLUE), false
        );
        player.displayClientMessage(
            Component.literal("• 安全存储贵重物品").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• 防盗保护系统").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• 大容量存储空间").withStyle(ChatFormatting.GRAY), false
        );
        
        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(
            Component.literal("使用方法:").withStyle(ChatFormatting.BLUE), false
        );
        player.displayClientMessage(
            Component.literal("• /vault open - 打开个人保险箱").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /vault share <玩家> - 共享访问权限").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /vault lock - 锁定保险箱").withStyle(ChatFormatting.GRAY), false
        );
        
        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(
            Component.literal("注意: 保险箱具有最高级别的安全保护").withStyle(ChatFormatting.RED), false
        );
        player.displayClientMessage(
            Component.literal("========================").withStyle(ChatFormatting.DARK_PURPLE), false
        );
    }
}