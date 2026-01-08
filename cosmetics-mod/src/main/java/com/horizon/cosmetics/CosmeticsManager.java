package com.horizon.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CosmeticsManager {
    private static final String API_URL = "http://localhost:3000/api/cosmetics";
    private static final Map<UUID, PlayerCosmetics> playerCosmetics = new HashMap<>();
    private static long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 30000; // 30 seconds

    public static void init() {
        HorizonCosmeticsMod.LOGGER.info("Initializing Cosmetics Manager");
        loadCosmetics();
    }

    public static void tick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate > UPDATE_INTERVAL) {
            loadCosmetics();
            lastUpdate = currentTime;
        }
    }

    private static void loadCosmetics() {
        try {
            URL url = new URL(API_URL + "/mods");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                JsonObject response = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                
                if (response.get("success").getAsBoolean()) {
                    JsonArray mods = response.getAsJsonArray("mods");
                    HorizonCosmeticsMod.LOGGER.info("Loaded " + mods.size() + " cosmetics from API");
                    
                    for (JsonElement element : mods) {
                        JsonObject mod = element.getAsJsonObject();
                        // Process cosmetics data
                    }
                }
            }
            
            conn.disconnect();
        } catch (Exception e) {
            HorizonCosmeticsMod.LOGGER.error("Failed to load cosmetics from API", e);
        }
    }

    public static PlayerCosmetics getPlayerCosmetics(UUID playerId) {
        return playerCosmetics.getOrDefault(playerId, new PlayerCosmetics());
    }

    public static void setPlayerCosmetics(UUID playerId, PlayerCosmetics cosmetics) {
        playerCosmetics.put(playerId, cosmetics);
    }

    public static class PlayerCosmetics {
        public boolean hasWings = false;
        public boolean hasCape = false;
        public boolean hasParticles = false;
        public String wingsType = "none";
        public String capeType = "none";
        public String particlesType = "none";
    }
}

