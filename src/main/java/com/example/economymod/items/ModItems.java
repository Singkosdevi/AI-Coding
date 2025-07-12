package com.example.economymod.items;

import com.example.economymod.EconomyMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// 模组物品注册类
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, EconomyMod.MOD_ID);

    // 货币物品
    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register("gold_coin",
            () -> new CoinItem(new Item.Properties().rarity(Rarity.UNCOMMON), 100));

    public static final RegistryObject<Item> SILVER_COIN = ITEMS.register("silver_coin",
            () -> new CoinItem(new Item.Properties().rarity(Rarity.COMMON), 10));

    public static final RegistryObject<Item> COPPER_COIN = ITEMS.register("copper_coin",
            () -> new CoinItem(new Item.Properties().rarity(Rarity.COMMON), 1));

    // 银行卡
    public static final RegistryObject<Item> BANK_CARD = ITEMS.register("bank_card",
            () -> new BankCardItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    // 商店契约
    public static final RegistryObject<Item> SHOP_DEED = ITEMS.register("shop_deed",
            () -> new ShopDeedItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    // 支票
    public static final RegistryObject<Item> CHECK = ITEMS.register("check",
            () -> new CheckItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));

    // 股票证书
    public static final RegistryObject<Item> STOCK_CERTIFICATE = ITEMS.register("stock_certificate",
            () -> new StockCertificateItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}