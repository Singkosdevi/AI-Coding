package com.example.economymod.blocks;

import com.example.economymod.EconomyMod;
import com.example.economymod.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

// 模组方块注册类
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EconomyMod.MOD_ID);

    // ATM方块
    public static final RegistryObject<Block> ATM_BLOCK = registerBlock("atm_block",
            () -> new ATMBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(6f).requiresCorrectToolForDrops()));

    // 银行方块
    public static final RegistryObject<Block> BANK_BLOCK = registerBlock("bank_block",
            () -> new BankBlock(BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)
                    .strength(8f).requiresCorrectToolForDrops()));

    // 商店方块
    public static final RegistryObject<Block> SHOP_BLOCK = registerBlock("shop_block",
            () -> new ShopBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
                    .strength(4f)));

    // 保险箱方块
    public static final RegistryObject<Block> VAULT_BLOCK = registerBlock("vault_block",
            () -> new VaultBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)
                    .strength(50f).requiresCorrectToolForDrops()));

    // 交易所方块
    public static final RegistryObject<Block> EXCHANGE_BLOCK = registerBlock("exchange_block",
            () -> new ExchangeBlock(BlockBehaviour.Properties.copy(Blocks.EMERALD_BLOCK)
                    .strength(6f).requiresCorrectToolForDrops()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}