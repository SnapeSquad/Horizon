package com.horizon.launcher.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с косметикой
 */
public class CosmeticService {
    private static final Logger logger = LoggerFactory.getLogger(CosmeticService.class);
    private final ApiClient apiClient;

    public CosmeticService() {
        this.apiClient = ApiClient.getInstance();
    }

    /**
     * Получает косметику пользователя
     */
    public List<Cosmetic> getCosmetics(String username) {
        logger.info("Получение косметики для: {}", username);
        
        ApiClient.ApiResponse response = apiClient.get("/api/user/cosmetics?username=" + username);
        
        if (response.isSuccess() && response.getBody().has("cosmetics")) {
            JsonArray cosmeticsArray = response.getBody().getAsJsonArray("cosmetics");
            List<Cosmetic> cosmetics = new ArrayList<>();
            
            for (var element : cosmeticsArray) {
                JsonObject cosmeticJson = element.getAsJsonObject();
                Cosmetic cosmetic = new Cosmetic(
                    cosmeticJson.get("id").getAsString(),
                    cosmeticJson.get("name").getAsString(),
                    cosmeticJson.get("type").getAsString(),
                    cosmeticJson.has("icon") ? cosmeticJson.get("icon").getAsString() : null,
                    cosmeticJson.has("description") ? cosmeticJson.get("description").getAsString() : null
                );
                cosmetics.add(cosmetic);
            }
            
            return cosmetics;
        }
        
        return new ArrayList<>();
    }

    /**
     * Получает список доступной косметики для покупки
     */
    public List<Cosmetic> getAvailableCosmetics() {
        // Временная заглушка - в реальности это должно приходить с сервера
        List<Cosmetic> cosmetics = new ArrayList<>();
        
        cosmetics.add(new Cosmetic("vip_badge", "VIP Значок", "badge", null, 
            "Эксклюзивный VIP значок", 500));
        cosmetics.add(new Cosmetic("golden_crown", "Золотая Корона", "hat", null,
            "Корона из чистого золота", 1000));
        cosmetics.add(new Cosmetic("rainbow_trail", "Радужный След", "trail", null,
            "Оставляет радужный след за вами", 750));
        cosmetics.add(new Cosmetic("diamond_sword", "Алмазный Меч", "weapon", null,
            "Косметический алмазный меч", 1200));
        
        return cosmetics;
    }

    /**
     * Добавляет косметику пользователю
     */
    public boolean addCosmetic(String username, Cosmetic cosmetic) {
        logger.info("Добавление косметики {} для: {}", cosmetic.getId(), username);
        
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        
        JsonObject cosmeticJson = new JsonObject();
        cosmeticJson.addProperty("id", cosmetic.getId());
        cosmeticJson.addProperty("name", cosmetic.getName());
        cosmeticJson.addProperty("type", cosmetic.getType());
        if (cosmetic.getIcon() != null) {
            cosmeticJson.addProperty("icon", cosmetic.getIcon());
        }
        if (cosmetic.getDescription() != null) {
            cosmeticJson.addProperty("description", cosmetic.getDescription());
        }
        
        data.add("cosmetic", cosmeticJson);

        ApiClient.ApiResponse response = apiClient.post("/api/user/cosmetics/add", data);
        return response.isSuccess();
    }

    public static class Cosmetic {
        private final String id;
        private final String name;
        private final String type;
        private final String icon;
        private final String description;
        private final Integer price;

        public Cosmetic(String id, String name, String type, String icon, String description) {
            this(id, name, type, icon, description, null);
        }

        public Cosmetic(String id, String name, String type, String icon, String description, Integer price) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.icon = icon;
            this.description = description;
            this.price = price;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getIcon() {
            return icon;
        }

        public String getDescription() {
            return description;
        }

        public Integer getPrice() {
            return price;
        }
    }
}





