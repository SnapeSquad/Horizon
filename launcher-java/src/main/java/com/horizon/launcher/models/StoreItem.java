package com.horizon.launcher.models;

import com.google.gson.JsonObject;

/**
 * Модель товара в магазине
 */
public class StoreItem {
    private String id;
    private String name;
    private String description;
    private String category;
    private int price;
    private String rarity; // common, uncommon, rare, epic, legendary
    private String imageUrl;
    private String modelUrl; // URL к Blockbench JSON модели
    private String textureUrl; // URL к текстуре модели
    private String pivotPoint; // head, body, etc.
    private boolean isNew;
    private boolean isDiscounted;
    private int discountPercentage;
    
    public StoreItem() {
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getPrice() {
        return price;
    }
    
    public void setPrice(int price) {
        this.price = price;
    }
    
    public String getRarity() {
        return rarity;
    }
    
    public void setRarity(String rarity) {
        this.rarity = rarity;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getModelUrl() {
        return modelUrl;
    }
    
    public void setModelUrl(String modelUrl) {
        this.modelUrl = modelUrl;
    }
    
    public String getTextureUrl() {
        return textureUrl;
    }
    
    public void setTextureUrl(String textureUrl) {
        this.textureUrl = textureUrl;
    }
    
    public String getPivotPoint() {
        return pivotPoint;
    }
    
    public void setPivotPoint(String pivotPoint) {
        this.pivotPoint = pivotPoint;
    }
    
    public boolean isNew() {
        return isNew;
    }
    
    public void setNew(boolean aNew) {
        isNew = aNew;
    }
    
    public boolean isDiscounted() {
        return isDiscounted;
    }
    
    public void setDiscounted(boolean discounted) {
        isDiscounted = discounted;
    }
    
    public int getDiscountPercentage() {
        return discountPercentage;
    }
    
    public void setDiscountPercentage(int discountPercentage) {
        this.discountPercentage = discountPercentage;
    }
    
    /**
     * Получить CSS класс для редкости товара
     */
    public String getRarityCssClass() {
        if (rarity == null) {
            return "rarity-common";
        }
        
        switch (rarity.toLowerCase()) {
            case "legendary":
                return "rarity-legendary";
            case "epic":
                return "rarity-epic";
            case "rare":
                return "rarity-rare";
            case "uncommon":
                return "rarity-uncommon";
            case "common":
            default:
                return "rarity-common";
        }
    }
    
    /**
     * Получить путь к модели (для совместимости)
     */
    public String getModelPath() {
        return modelUrl;
    }
    
    /**
     * Получить путь к текстуре (для совместимости)
     */
    public String getTexturePath() {
        return textureUrl;
    }
    
    /**
     * Парсить StoreItem из JSON объекта
     */
    public static StoreItem fromJson(JsonObject json) {
        StoreItem item = new StoreItem();
        
        if (json.has("id")) item.setId(json.get("id").getAsString());
        if (json.has("name")) item.setName(json.get("name").getAsString());
        if (json.has("description")) item.setDescription(json.get("description").getAsString());
        if (json.has("category")) item.setCategory(json.get("category").getAsString());
        if (json.has("type")) item.setCategory(json.get("type").getAsString()); // Для совместимости
        if (json.has("price")) item.setPrice(json.get("price").getAsInt());
        if (json.has("rarity")) item.setRarity(json.get("rarity").getAsString());
        if (json.has("image_url")) item.setImageUrl(json.get("image_url").getAsString());
        if (json.has("imageUrl")) item.setImageUrl(json.get("imageUrl").getAsString());
        if (json.has("model_url")) item.setModelUrl(json.get("model_url").getAsString());
        if (json.has("modelUrl")) item.setModelUrl(json.get("modelUrl").getAsString());
        if (json.has("texture_url")) item.setTextureUrl(json.get("texture_url").getAsString());
        if (json.has("textureUrl")) item.setTextureUrl(json.get("textureUrl").getAsString());
        if (json.has("pivot_point")) item.setPivotPoint(json.get("pivot_point").getAsString());
        if (json.has("pivotPoint")) item.setPivotPoint(json.get("pivotPoint").getAsString());
        if (json.has("is_new")) item.setNew(json.get("is_new").getAsBoolean());
        if (json.has("isNew")) item.setNew(json.get("isNew").getAsBoolean());
        if (json.has("is_discounted")) item.setDiscounted(json.get("is_discounted").getAsBoolean());
        if (json.has("isDiscounted")) item.setDiscounted(json.get("isDiscounted").getAsBoolean());
        if (json.has("discount_percentage")) item.setDiscountPercentage(json.get("discount_percentage").getAsInt());
        if (json.has("discountPercentage")) item.setDiscountPercentage(json.get("discountPercentage").getAsInt());
        
        return item;
    }
}
