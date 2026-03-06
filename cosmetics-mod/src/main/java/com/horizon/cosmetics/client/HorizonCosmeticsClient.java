package com.horizon.cosmetics.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс клиентской части мода Horizon Cosmetics
 */
@Environment(EnvType.CLIENT)
public class HorizonCosmeticsClient implements ClientModInitializer {
    private static final Logger logger = LoggerFactory.getLogger(HorizonCosmeticsClient.class);
    
    @Override
    public void onInitializeClient() {
        logger.info("Horizon Cosmetics Mod загружается...");
        
        // Инициализация менеджера косметики
        CosmeticManager cosmeticManager = CosmeticManager.getInstance();
        
        logger.info("Horizon Cosmetics Mod успешно загружен!");
    }
}
