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

// 交易所方块类 - 全球市场交易
public class ExchangeBlock extends Block {
    public ExchangeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
                                InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            showExchangeMenu(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private void showExchangeMenu(Player player) {
        player.displayClientMessage(
            Component.literal("======= 全球交易所 =======").withStyle(ChatFormatting.DARK_GREEN), false
        );
        player.displayClientMessage(
            Component.literal("欢迎来到全球物品交易市场！").withStyle(ChatFormatting.GREEN), false
        );
        player.displayClientMessage(Component.literal(""), false);
        
        player.displayClientMessage(
            Component.literal("交易功能:").withStyle(ChatFormatting.BLUE), false
        );
        player.displayClientMessage(
            Component.literal("• 全服物品买卖").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• 实时价格系统").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• 拍卖行功能").withStyle(ChatFormatting.GRAY), false
        );
        
        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(
            Component.literal("交易命令:").withStyle(ChatFormatting.BLUE), false
        );
        player.displayClientMessage(
            Component.literal("• /exchange sell <价格> - 出售手持物品").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /exchange buy <物品> <数量> - 购买物品").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /exchange list - 查看市场列表").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /exchange search <物品> - 搜索物品").withStyle(ChatFormatting.GRAY), false
        );
        
        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(
            Component.literal("拍卖功能:").withStyle(ChatFormatting.BLUE), false
        );
        player.displayClientMessage(
            Component.literal("• /auction start <起拍价> <时长> - 开始拍卖").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /auction bid <拍卖ID> <出价> - 竞价").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /auction list - 查看拍卖列表").withStyle(ChatFormatting.GRAY), false
        );
        
        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(
            Component.literal("注意: 所有交易将收取5%的手续费").withStyle(ChatFormatting.YELLOW), false
        );
        player.displayClientMessage(
            Component.literal("=========================").withStyle(ChatFormatting.DARK_GREEN), false
        );
    }
}