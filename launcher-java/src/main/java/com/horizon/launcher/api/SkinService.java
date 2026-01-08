package com.horizon.launcher.api;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

/**
 * Сервис для работы со скинами
 */
public class SkinService {
    private static final Logger logger = LoggerFactory.getLogger(SkinService.class);
    private final ApiClient apiClient;
    private final HttpClient httpClient;

    public SkinService() {
        this.apiClient = ApiClient.getInstance();
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Загружает скин пользователя
     */
    public boolean uploadSkin(String username, String skinBase64) {
        logger.info("Загрузка скина для: {}", username);
        
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("skin", skinBase64);

        ApiClient.ApiResponse response = apiClient.post("/api/user/skin", data);
        return response.isSuccess();
    }

    /**
     * Получает URL скина по никнейму
     */
    public String getSkinUrlByUsername(String username) {
        try {
            // Шаг 1: Получаем UUID
            String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + username;
            HttpRequest uuidRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uuidUrl))
                    .GET()
                    .build();
            
            HttpResponse<String> uuidResponse = httpClient.send(uuidRequest, HttpResponse.BodyHandlers.ofString());
            
            if (uuidResponse.statusCode() != 200) {
                logger.warn("Пользователь {} не найден в Mojang, используем дефолтный скин", username);
                return "https://minecraft-api.com/api/skins/steve.png";
            }
            
            JsonObject uuidData = com.google.gson.JsonParser.parseString(uuidResponse.body()).getAsJsonObject();
            String uuid = uuidData.get("id").getAsString();
            
            // Шаг 2: Получаем профиль
            String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
            HttpRequest profileRequest = HttpRequest.newBuilder()
                    .uri(URI.create(profileUrl))
                    .GET()
                    .build();
            
            HttpResponse<String> profileResponse = httpClient.send(profileRequest, HttpResponse.BodyHandlers.ofString());
            
            if (profileResponse.statusCode() != 200) {
                return "https://minecraft-api.com/api/skins/steve.png";
            }
            
            JsonObject profileData = com.google.gson.JsonParser.parseString(profileResponse.body()).getAsJsonObject();
            var properties = profileData.getAsJsonArray("properties");
            
            for (var prop : properties) {
                JsonObject property = prop.getAsJsonObject();
                if ("textures".equals(property.get("name").getAsString())) {
                    String texturesBase64 = property.get("value").getAsString();
                    String texturesJson = new String(Base64.getDecoder().decode(texturesBase64));
                    JsonObject textures = com.google.gson.JsonParser.parseString(texturesJson).getAsJsonObject();
                    
                    if (textures.has("textures") && textures.getAsJsonObject("textures").has("SKIN")) {
                        return textures.getAsJsonObject("textures")
                                .getAsJsonObject("SKIN")
                                .get("url").getAsString();
                    }
                }
            }
            
            return "https://minecraft-api.com/api/skins/steve.png";
        } catch (Exception e) {
            logger.error("Ошибка получения URL скина для {}", username, e);
            return "https://minecraft-api.com/api/skins/steve.png";
        }
    }

    /**
     * Получает скин по никнейму через Minecraft API
     */
    public BufferedImage getSkinByUsername(String username) throws IOException {
        logger.info("Получение скина по никнейму: {}", username);
        
        try {
            // Шаг 1: Получаем UUID
            String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + username;
            HttpRequest uuidRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uuidUrl))
                    .GET()
                    .build();
            
            HttpResponse<String> uuidResponse = httpClient.send(uuidRequest, HttpResponse.BodyHandlers.ofString());
            
            if (uuidResponse.statusCode() != 200) {
                logger.warn("Пользователь {} не найден в Mojang", username);
                return null;
            }
            
            JsonObject uuidData = com.google.gson.JsonParser.parseString(uuidResponse.body()).getAsJsonObject();
            String uuid = uuidData.get("id").getAsString();
            
            // Шаг 2: Получаем профиль
            String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
            HttpRequest profileRequest = HttpRequest.newBuilder()
                    .uri(URI.create(profileUrl))
                    .GET()
                    .build();
            
            HttpResponse<String> profileResponse = httpClient.send(profileRequest, HttpResponse.BodyHandlers.ofString());
            
            if (profileResponse.statusCode() != 200) {
                return null;
            }
            
            JsonObject profileData = com.google.gson.JsonParser.parseString(profileResponse.body()).getAsJsonObject();
            var properties = profileData.getAsJsonArray("properties");
            
            for (var prop : properties) {
                JsonObject property = prop.getAsJsonObject();
                if ("textures".equals(property.get("name").getAsString())) {
                    String texturesBase64 = property.get("value").getAsString();
                    String texturesJson = new String(Base64.getDecoder().decode(texturesBase64));
                    JsonObject textures = com.google.gson.JsonParser.parseString(texturesJson).getAsJsonObject();
                    
                    if (textures.has("textures") && textures.getAsJsonObject("textures").has("SKIN")) {
                        String skinUrl = textures.getAsJsonObject("textures")
                                .getAsJsonObject("SKIN")
                                .get("url").getAsString();
                        
                        // Шаг 3: Загружаем изображение скина
                        HttpRequest skinRequest = HttpRequest.newBuilder()
                                .uri(URI.create(skinUrl))
                                .GET()
                                .build();
                        
                        HttpResponse<byte[]> skinResponse = httpClient.send(
                                skinRequest, HttpResponse.BodyHandlers.ofByteArray());
                        
                        if (skinResponse.statusCode() == 200) {
                            return ImageIO.read(new java.io.ByteArrayInputStream(skinResponse.body()));
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Ошибка получения скина для {}", username, e);
            return null;
        }
    }

    /**
     * Получает дефолтный скин Стива
     */
    public BufferedImage getDefaultSteveSkin() {
        try {
            // Используем публичный URL дефолтного скина
            String defaultSkinUrl = "https://minecraft-api.com/api/skins/steve.png";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(defaultSkinUrl))
                    .GET()
                    .build();
            
            HttpResponse<byte[]> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() == 200) {
                return ImageIO.read(new java.io.ByteArrayInputStream(response.body()));
            }
        } catch (Exception e) {
            logger.error("Ошибка загрузки дефолтного скина", e);
        }
        
        // Создаем простой дефолтный скин программно
        return createDefaultSkin();
    }

    private BufferedImage createDefaultSkin() {
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = skin.createGraphics();
        
        // Простой Стив - голова (верхний левый угол)
        g.setColor(new java.awt.Color(199, 159, 101)); // Цвет кожи
        g.fillRect(8, 8, 8, 8);
        g.setColor(new java.awt.Color(0, 0, 0)); // Волосы
        g.fillRect(8, 0, 8, 8);
        
        // Тело
        g.setColor(new java.awt.Color(113, 188, 120)); // Рубашка
        g.fillRect(20, 20, 8, 12);
        g.setColor(new java.awt.Color(0, 0, 0)); // Рукава
        g.fillRect(16, 20, 4, 12);
        g.fillRect(28, 20, 4, 12);
        
        // Ноги
        g.setColor(new java.awt.Color(60, 67, 170)); // Штаны
        g.fillRect(20, 32, 4, 12);
        g.fillRect(24, 32, 4, 12);
        
        g.dispose();
        return skin;
    }

    /**
     * Конвертирует BufferedImage в base64
     */
    public String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}





