package com.horizon.launcher.models;

/**
 * Роли пользователей форума
 * Определяет префикс, цвет, стиль шрифта и наличие анимации
 */
public enum UserRole {
    // Роли с анимацией градиента
    OWNER("Владелец", "#FF6B6B", "-fx-font-weight: bold;", true),
    CURATOR("Куратор", "#4ECDC4", "-fx-font-weight: bold;", true),
    
    // Роли без анимации
    ULTA("Ulta", "#9B59B6", "-fx-font-weight: bold;", false),
    VIP("VIP", "#F39C12", "-fx-font-weight: bold;", false),
    PREMIUM("Premium", "#3498DB", "-fx-font-weight: bold;", false),
    DEFAULT("Игрок", "#95A5A6", "-fx-font-weight: normal;", false);
    
    private final String prefix;
    private final String color;
    private final String fontStyle;
    private final boolean hasAnimation;
    
    UserRole(String prefix, String color, String fontStyle, boolean hasAnimation) {
        this.prefix = prefix;
        this.color = color;
        this.fontStyle = fontStyle;
        this.hasAnimation = hasAnimation;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public String getColor() {
        return color;
    }
    
    public String getFontStyle() {
        return fontStyle;
    }
    
    public boolean hasAnimation() {
        return hasAnimation;
    }
    
    /**
     * Получить CSS класс для роли
     */
    public String getCssClass() {
        return "role-" + name().toLowerCase();
    }
    
    /**
     * Применить стиль к текстовому узлу
     */
    public static void applyRoleStyle(javafx.scene.text.Text textNode, UserRole role) {
        if (role == null) {
            role = DEFAULT;
        }
        
        // Устанавливаем CSS класс
        textNode.getStyleClass().clear();
        textNode.getStyleClass().add(role.getCssClass());
        
        // Устанавливаем инлайн стили
        textNode.setStyle(
            "-fx-fill: " + role.getColor() + ";" +
            role.getFontStyle()
        );
        
        // Если есть анимация, добавляем класс анимации
        if (role.hasAnimation()) {
            textNode.getStyleClass().add("role-animated");
        }
    }
    
    /**
     * Определить роль по строке (например, из API)
     */
    public static UserRole fromString(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return DEFAULT;
        }
        
        try {
            return valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Если роль не найдена, пытаемся найти по префиксу
            for (UserRole role : values()) {
                if (role.getPrefix().equalsIgnoreCase(roleName)) {
                    return role;
                }
            }
            return DEFAULT;
        }
    }
}
