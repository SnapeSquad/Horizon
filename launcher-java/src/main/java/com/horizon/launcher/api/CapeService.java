package com.horizon.launcher.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с плащами
 */
public class CapeService {
    private static final Logger logger = LoggerFactory.getLogger(CapeService.class);
    private final ApiClient apiClient;

    public CapeService() {
        this.apiClient = ApiClient.getInstance();
    }

    /**
     * Получает список доступных плащей
     */
    public CapeList getCapes(String username) {
        try {
            ApiClient.ApiResponse response = apiClient.get("/api/user/capes?username=" + username);
            
            if (response.isSuccess()) {
                JsonObject body = response.getBody();
                List<Cape> capes = new ArrayList<>();
                List<String> owned = new ArrayList<>();
                String selected = null;
                
                if (body.has("capes")) {
                    JsonArray capesArray = body.getAsJsonArray("capes");
                    for (var element : capesArray) {
                        JsonObject capeJson = element.getAsJsonObject();
                        Cape cape = new Cape(
                            capeJson.get("id").getAsString(),
                            capeJson.get("name").getAsString(),
                            capeJson.has("icon") ? capeJson.get("icon").getAsString() : null,
                            capeJson.has("description") ? capeJson.get("description").getAsString() : null
                        );
                        capes.add(cape);
                    }
                }
                
                if (body.has("owned")) {
                    JsonArray ownedArray = body.getAsJsonArray("owned");
                    for (var element : ownedArray) {
                        owned.add(element.getAsString());
                    }
                }
                
                if (body.has("selected")) {
                    selected = body.get("selected").getAsString();
                }
                
                return new CapeList(capes, owned, selected);
            }
        } catch (Exception e) {
            logger.error("Ошибка получения плащей", e);
        }
        
        return new CapeList(new ArrayList<>(), new ArrayList<>(), null);
    }

    /**
     * Выбирает плащ
     */
    public boolean selectCape(String username, String capeId) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("username", username);
            data.addProperty("capeId", capeId);

            ApiClient.ApiResponse response = apiClient.post("/api/user/cape/select", data);
            return response.isSuccess();
        } catch (Exception e) {
            logger.error("Ошибка выбора плаща", e);
            return false;
        }
    }

    public static class Cape {
        private final String id;
        private final String name;
        private final String icon;
        private final String description;

        public Cape(String id, String name, String icon, String description) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    public static class CapeList {
        private final List<Cape> capes;
        private final List<String> owned;
        private final String selected;

        public CapeList(List<Cape> capes, List<String> owned, String selected) {
            this.capes = capes;
            this.owned = owned;
            this.selected = selected;
        }

        public List<Cape> getCapes() { return capes; }
        public List<String> getOwned() { return owned; }
        public String getSelected() { return selected; }
    }
}





