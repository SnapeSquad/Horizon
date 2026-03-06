package com.horizon.launcher.utils;

import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;

/**
 * Утилита для улучшения accessibility в JavaFX приложении
 */
public class AccessibilityHelper {
    
    /**
     * Настроить accessibility для кнопки
     */
    public static void setupButtonAccessibility(Button button, String label, String help) {
        button.setAccessibleRole(AccessibleRole.BUTTON);
        button.setAccessibleText(label);
        if (help != null && !help.isEmpty()) {
            button.setAccessibleHelp(help);
        }
        // Убеждаемся, что кнопка может получать фокус
        button.setFocusTraversable(true);
    }
    
    /**
     * Настроить accessibility для текстового поля
     */
    public static void setupTextFieldAccessibility(TextField field, String label, String help) {
        field.setAccessibleRole(AccessibleRole.TEXT_FIELD);
        field.setAccessibleText(label);
        if (help != null && !help.isEmpty()) {
            field.setAccessibleHelp(help);
        }
        field.setFocusTraversable(true);
    }
    
    /**
     * Настроить accessibility для поля пароля
     */
    public static void setupPasswordFieldAccessibility(PasswordField field, String label, String help) {
        field.setAccessibleRole(AccessibleRole.PASSWORD_FIELD);
        field.setAccessibleText(label);
        if (help != null && !help.isEmpty()) {
            field.setAccessibleHelp(help);
        }
        field.setFocusTraversable(true);
    }
    
    /**
     * Настроить accessibility для лейбла
     */
    public static void setupLabelAccessibility(Label label, String text) {
        label.setAccessibleRole(AccessibleRole.TEXT);
        label.setAccessibleText(text);
        // Лейблы обычно не получают фокус
        label.setFocusTraversable(false);
    }
    
    /**
     * Настроить accessibility для элемента управления
     */
    public static void setupControlAccessibility(Control control, AccessibleRole role, String label, String help) {
        control.setAccessibleRole(role);
        control.setAccessibleText(label);
        if (help != null && !help.isEmpty()) {
            control.setAccessibleHelp(help);
        }
        control.setFocusTraversable(true);
    }
    
    /**
     * Установить порядок табуляции для элементов
     * В JavaFX порядок табуляции определяется порядком добавления в сцену
     * Этот метод просто убеждается, что все элементы могут получать фокус
     */
    public static void setTabOrder(Control... controls) {
        for (Control control : controls) {
            control.setFocusTraversable(true);
        }
    }
}
