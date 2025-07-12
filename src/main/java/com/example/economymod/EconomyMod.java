package com.example.economymod;

import com.example.economymod.commands.EconomyCommands;
import com.example.economymod.economy.EconomyManager;
import com.example.economymod.items.ModItems;
import com.example.economymod.blocks.ModBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 主模组类 - Minecraft经济模组
@Mod(EconomyMod.MOD_ID)
public class EconomyMod {
    public static final String MOD_ID = "economymod";
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyMod.class);
    
    public EconomyMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册物品和方块
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        // 注册事件
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        // 注册模组到Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("正在初始化经济模组...");
        
        // 初始化经济管理器
        event.enqueueWork(() -> {
            EconomyManager.init();
            LOGGER.info("经济系统初始化完成");
        });
    }

    // 添加物品到创造模式物品栏
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.GOLD_COIN);
            event.accept(ModItems.SILVER_COIN);
            event.accept(ModItems.COPPER_COIN);
            event.accept(ModItems.BANK_CARD);
        }
        
        if(event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.ATM_BLOCK);
            event.accept(ModBlocks.SHOP_BLOCK);
            event.accept(ModBlocks.BANK_BLOCK);
        }
    }

    // 服务器启动时注册命令
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("经济模组服务器启动中...");
        EconomyManager.loadData();
    }

    // 注册命令
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EconomyCommands.register(event.getDispatcher());
        LOGGER.info("经济命令注册完成");
    }

    // 客户端设置（仅在客户端环境下）
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // 客户端初始化代码
            LOGGER.info("经济模组客户端初始化完成");
        }
    }
}