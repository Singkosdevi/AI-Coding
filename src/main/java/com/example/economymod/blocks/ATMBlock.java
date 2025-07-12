package com.example.economymod.blocks;

import com.example.economymod.economy.EconomyManager;
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

// ATM方块类 - 自动提款机
public class ATMBlock extends Block {
    public ATMBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
                                InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            // 显示ATM菜单
            showATMMenu(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private void showATMMenu(Player player) {
        long balance = EconomyManager.getMoney(player.getUUID());
        long savings = EconomyManager.getSavings(player.getUUID());
        
        player.displayClientMessage(
            Component.literal("======= ATM 自动提款机 =======").withStyle(ChatFormatting.AQUA), false
        );
        player.displayClientMessage(
            Component.literal("欢迎使用ATM服务！").withStyle(ChatFormatting.GREEN), false
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
        player.displayClientMessage(
            Component.literal("可用命令:").withStyle(ChatFormatting.BLUE), false
        );
        player.displayClientMessage(
            Component.literal("• /economy deposit <金额> - 存款").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /economy withdraw <金额> - 取款").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /economy balance - 查看余额").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("============================").withStyle(ChatFormatting.AQUA), false
        );
    }
}