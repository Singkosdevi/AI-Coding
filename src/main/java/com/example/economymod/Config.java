package com.example.economymod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// 经济模组配置类
@Mod.EventBusSubscriber(modid = EconomyMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue STARTING_MONEY = BUILDER
            .comment("玩家初始金币数量")
            .defineInRange("startingMoney", 100, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue BANK_INTEREST_RATE = BUILDER
            .comment("银行存款利率（每日）")
            .defineInRange("bankInterestRate", 0.01, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue TRANSACTION_TAX = BUILDER
            .comment("交易税率")
            .defineInRange("transactionTax", 0.05, 0.0, 1.0);

    private static final ForgeConfigSpec.IntValue MAX_SHOPS_PER_PLAYER = BUILDER
            .comment("每个玩家最大商店数量")
            .defineInRange("maxShopsPerPlayer", 5, 1, 100);

    private static final ForgeConfigSpec.BooleanValue ENABLE_DAILY_REWARDS = BUILDER
            .comment("是否启用每日奖励")
            .define("enableDailyRewards", true);

    private static final ForgeConfigSpec.IntValue DAILY_REWARD_AMOUNT = BUILDER
            .comment("每日奖励金额")
            .defineInRange("dailyRewardAmount", 50, 0, Integer.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int startingMoney;
    public static double bankInterestRate;
    public static double transactionTax;
    public static int maxShopsPerPlayer;
    public static boolean enableDailyRewards;
    public static int dailyRewardAmount;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        startingMoney = STARTING_MONEY.get();
        bankInterestRate = BANK_INTEREST_RATE.get();
        transactionTax = TRANSACTION_TAX.get();
        maxShopsPerPlayer = MAX_SHOPS_PER_PLAYER.get();
        enableDailyRewards = ENABLE_DAILY_REWARDS.get();
        dailyRewardAmount = DAILY_REWARD_AMOUNT.get();
    }
}