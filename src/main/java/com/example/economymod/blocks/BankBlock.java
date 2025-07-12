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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 银行方块类
public class BankBlock extends Block {
    public BankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
                                InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            showBankMenu(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private void showBankMenu(Player player) {
        long balance = EconomyManager.getMoney(player.getUUID());
        long savings = EconomyManager.getSavings(player.getUUID());
        long totalTransactions = EconomyManager.getTransactionCount(player.getUUID());
        
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        
        player.displayClientMessage(
            Component.literal("======= 经济银行 =======").withStyle(ChatFormatting.GOLD), false
        );
        player.displayClientMessage(
            Component.literal("尊敬的客户，欢迎来到经济银行！").withStyle(ChatFormatting.GREEN), false
        );
        player.displayClientMessage(
            Component.literal("当前时间: " + currentTime).withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(Component.literal(""), false);
        
        player.displayClientMessage(
            Component.literal("账户信息:").withStyle(ChatFormatting.BLUE), false
        );
        player.displayClientMessage(
            Component.literal("  钱包余额: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(balance)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 金币")),
            false
        );
        player.displayClientMessage(
            Component.literal("  储蓄余额: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(savings)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" 金币")),
            false
        );
        player.displayClientMessage(
            Component.literal("  交易次数: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(totalTransactions)).withStyle(ChatFormatting.AQUA)),
            false
        );
        
        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(
            Component.literal("银行服务:").withStyle(ChatFormatting.BLUE), false
        );
        player.displayClientMessage(
            Component.literal("• /economy deposit <金额> - 存入储蓄账户").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /economy withdraw <金额> - 从储蓄账户取出").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /economy transfer <玩家> <金额> - 转账给其他玩家").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /economy loan <金额> - 申请贷款").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /economy history - 查看交易历史").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("========================").withStyle(ChatFormatting.GOLD), false
        );
    }
}