package com.horizon.cosmetics;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(HorizonCosmeticsMod.MOD_ID)
public class HorizonCosmeticsMod {
    public static final String MOD_ID = "horizoncosmetics";
    public static final Logger LOGGER = LogManager.getLogger();

    public HorizonCosmeticsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        modEventBus.addListener(this::clientSetup);
        
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CosmeticsRenderer());
        
        LOGGER.info("Horizon Cosmetics Mod initialized!");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Horizon Cosmetics - Client Setup");
        CosmeticsManager.init();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CosmeticsManager.tick();
        }
    }
}

