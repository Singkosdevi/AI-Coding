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

// 商店方块类
public class ShopBlock extends Block {
    public ShopBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
                                InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            showShopMenu(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private void showShopMenu(Player player) {
        player.displayClientMessage(
            Component.literal("======= 玩家商店 =======").withStyle(ChatFormatting.GREEN), false
        );
        player.displayClientMessage(
            Component.literal("欢迎来到玩家商店系统！").withStyle(ChatFormatting.YELLOW), false
        );
        player.displayClientMessage(Component.literal(""), false);
        
        player.displayClientMessage(
            Component.literal("商店管理:").withStyle(ChatFormatting.BLUE), false
        );
        player.displayClientMessage(
            Component.literal("• /shop create <商店名> - 创建新商店").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /shop list - 查看所有商店").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /shop info <商店名> - 查看商店信息").withStyle(ChatFormatting.GRAY), false
        );
        
        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(
            Component.literal("商品管理:").withStyle(ChatFormatting.BLUE), false
        );
        player.displayClientMessage(
            Component.literal("• /shop add <价格> - 添加手持物品到商店").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /shop remove <物品> - 从商店移除物品").withStyle(ChatFormatting.GRAY), false
        );
        player.displayClientMessage(
            Component.literal("• /shop buy <商店名> <物品> <数量> - 购买物品").withStyle(ChatFormatting.GRAY), false
        );
        
        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(
            Component.literal("提示: 需要商店契约才能创建商店").withStyle(ChatFormatting.GOLD), false
        );
        player.displayClientMessage(
            Component.literal("=======================").withStyle(ChatFormatting.GREEN), false
        );
    }
}