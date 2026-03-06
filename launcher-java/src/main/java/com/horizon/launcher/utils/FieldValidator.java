package com.horizon.launcher.utils;

import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Утилита для валидации полей ввода в реальном времени
 */
public class FieldValidator {
    private static final Logger logger = LoggerFactory.getLogger(FieldValidator.class);
    
    // Минимальная длина имени пользователя
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 16;
    
    // Минимальная длина пароля
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;
    
    // Паттерны для валидации
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]+$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    /**
     * Результат валидации
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
    
    /**
     * Валидация имени пользователя
     */
    public static ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.error("Имя пользователя не может быть пустым");
        }
        
        username = username.trim();
        
        if (username.length() < MIN_USERNAME_LENGTH) {
            return ValidationResult.error(String.format("Имя пользователя должно содержать минимум %d символов", MIN_USERNAME_LENGTH));
        }
        
        if (username.length() > MAX_USERNAME_LENGTH) {
            return ValidationResult.error(String.format("Имя пользователя не должно превышать %d символов", MAX_USERNAME_LENGTH));
        }
        
        if (!username.matches(USERNAME_PATTERN)) {
            return ValidationResult.error("Имя пользователя может содержать только буквы, цифры и символ подчеркивания");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Валидация пароля
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.error("Пароль не может быть пустым");
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return ValidationResult.error(String.format("Пароль должен содержать минимум %d символов", MIN_PASSWORD_LENGTH));
        }
        
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return ValidationResult.error(String.format("Пароль не должен превышать %d символов", MAX_PASSWORD_LENGTH));
        }
        
        // Проверка на наличие хотя бы одной буквы и одной цифры
        boolean hasLetter = password.matches(".*[a-zA-Zа-яА-Я].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        
        if (!hasLetter || !hasDigit) {
            return ValidationResult.error("Пароль должен содержать хотя бы одну букву и одну цифру");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Валидация подтверждения пароля
     */
    public static ValidationResult validatePasswordConfirmation(String password, String confirmPassword) {
        ValidationResult passwordResult = validatePassword(password);
        if (!passwordResult.isValid()) {
            return passwordResult;
        }
        
        if (!password.equals(confirmPassword)) {
            return ValidationResult.error("Пароли не совпадают");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Валидация 2FA кода
     */
    public static ValidationResult validate2FACode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return ValidationResult.error("Код не может быть пустым");
        }
        
        code = code.trim();
        
        if (code.length() != 6) {
            return ValidationResult.error("Код должен содержать 6 цифр");
        }
        
        if (!code.matches("^[0-9]{6}$")) {
            return ValidationResult.error("Код должен содержать только цифры");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Валидация email (если будет использоваться)
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.error("Email не может быть пустым");
        }
        
        email = email.trim();
        
        if (!email.matches(EMAIL_PATTERN)) {
            return ValidationResult.error("Некорректный формат email");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Настройка валидации в реальном времени для TextField
     */
    public static void setupRealtimeValidation(TextField field, ValidationFunction validator, Label errorLabel) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            ValidationResult result = validator.validate(newValue);
            updateFieldStyle(field, result.isValid());
            if (errorLabel != null) {
                updateErrorLabel(errorLabel, result);
            }
        });
        
        // Валидация при потере фокуса
        field.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && !field.getText().isEmpty()) {
                ValidationResult result = validator.validate(field.getText());
                updateFieldStyle(field, result.isValid());
                if (errorLabel != null) {
                    updateErrorLabel(errorLabel, result);
                }
            }
        });
    }
    
    /**
     * Настройка валидации в реальном времени для PasswordField
     */
    public static void setupRealtimeValidation(PasswordField field, ValidationFunction validator, Label errorLabel) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            ValidationResult result = validator.validate(newValue);
            updateFieldStyle(field, result.isValid());
            if (errorLabel != null) {
                updateErrorLabel(errorLabel, result);
            }
        });
        
        field.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && !field.getText().isEmpty()) {
                ValidationResult result = validator.validate(field.getText());
                updateFieldStyle(field, result.isValid());
                if (errorLabel != null) {
                    updateErrorLabel(errorLabel, result);
                }
            }
        });
    }
    
    /**
     * Обновление стиля поля в зависимости от валидности
     */
    private static void updateFieldStyle(javafx.scene.control.Control field, boolean isValid) {
        if (isValid) {
            // Убираем красную границу, если поле валидно
            field.setStyle(field.getStyle().replaceAll("-fx-border-color:\\s*#[fF][fF]?[0-9a-fA-F]{4,6};?", ""));
        } else {
            // Добавляем красную границу для невалидного поля
            String currentStyle = field.getStyle();
            if (!currentStyle.contains("-fx-border-color: #FF6B6B")) {
                field.setStyle(currentStyle + " -fx-border-color: #FF6B6B; -fx-border-width: 2px;");
            }
        }
    }
    
    /**
     * Обновление лейбла ошибки
     */
    private static void updateErrorLabel(Label errorLabel, ValidationResult result) {
        if (result.isValid()) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        } else {
            errorLabel.setText(result.getErrorMessage());
            errorLabel.setStyle(
                "-fx-text-fill: #FF6B6B; " +
                "-fx-font-size: 12px; " +
                "-fx-wrap-text: true;"
            );
            errorLabel.setVisible(true);
        }
    }
    
    /**
     * Функциональный интерфейс для валидации
     */
    @FunctionalInterface
    public interface ValidationFunction {
        ValidationResult validate(String value);
    }
    
    /**
     * Проверка силы пароля и возврат уровня сложности
     */
    public static PasswordStrength getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.EMPTY;
        }
        
        int strength = 0;
        
        // Длина
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        
        // Буквы
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        
        // Цифры
        if (password.matches(".*[0-9].*")) strength++;
        
        // Специальные символы
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) strength++;
        
        if (strength <= 2) return PasswordStrength.WEAK;
        if (strength <= 4) return PasswordStrength.MEDIUM;
        if (strength <= 5) return PasswordStrength.STRONG;
        return PasswordStrength.VERY_STRONG;
    }
    
    /**
     * Уровни сложности пароля
     */
    public enum PasswordStrength {
        EMPTY(""),
        WEAK("Слабый"),
        MEDIUM("Средний"),
        STRONG("Сильный"),
        VERY_STRONG("Очень сильный");
        
        private final String displayName;
        
        PasswordStrength(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getColor() {
            switch (this) {
                case WEAK:
                    return "#FF6B6B";
                case MEDIUM:
                    return "#FFA726";
                case STRONG:
                    return "#66BB6A";
                case VERY_STRONG:
                    return "#42A5F5";
                default:
                    return "#A0A0B0";
            }
        }
    }
}
