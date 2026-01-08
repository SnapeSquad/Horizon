package com.horizon.launcher.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Утилиты для валидации входных данных
 * AAA-уровень: строгая валидация всех пользовательских данных
 */
public class ValidationUtils {
    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);
    
    // Паттерны валидации
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{6,128}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$");
    
    /**
     * Валидирует имя пользователя
     */
    public static ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.error("Имя пользователя не может быть пустым");
        }
        
        username = username.trim();
        
        if (username.length() < 3) {
            return ValidationResult.error("Имя пользователя должно содержать минимум 3 символа");
        }
        
        if (username.length() > 16) {
            return ValidationResult.error("Имя пользователя не должно превышать 16 символов");
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidationResult.error("Имя пользователя может содержать только буквы, цифры и подчеркивание");
        }
        
        // Проверка на запрещенные слова
        String[] forbiddenWords = {"admin", "administrator", "root", "system", "null", "undefined"};
        String lowerUsername = username.toLowerCase();
        for (String word : forbiddenWords) {
            if (lowerUsername.contains(word)) {
                return ValidationResult.error("Имя пользователя содержит запрещенное слово");
            }
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Валидирует пароль
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.error("Пароль не может быть пустым");
        }
        
        if (password.length() < 6) {
            return ValidationResult.error("Пароль должен содержать минимум 6 символов");
        }
        
        if (password.length() > 128) {
            return ValidationResult.error("Пароль не должен превышать 128 символов");
        }
        
        // Проверка на сложность (опционально, можно усилить)
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        
        if (!hasLetter || !hasDigit) {
            return ValidationResult.warning("Рекомендуется использовать пароль с буквами и цифрами");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Валидирует код 2FA
     */
    public static ValidationResult validate2FACode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return ValidationResult.error("Код 2FA не может быть пустым");
        }
        
        code = code.trim().replaceAll("\\D", ""); // Убираем все не-цифры
        
        if (code.length() != 6) {
            return ValidationResult.error("Код 2FA должен состоять из 6 цифр");
        }
        
        if (!code.matches("\\d{6}")) {
            return ValidationResult.error("Код 2FA должен содержать только цифры");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Валидирует URL
     */
    public static ValidationResult validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return ValidationResult.error("URL не может быть пустым");
        }
        
        url = url.trim();
        
        if (!URL_PATTERN.matcher(url).matches()) {
            return ValidationResult.error("Некорректный формат URL");
        }
        
        // Проверка на безопасные протоколы
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ValidationResult.error("URL должен использовать протокол HTTP или HTTPS");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Валидирует email (если понадобится)
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.error("Email не может быть пустым");
        }
        
        email = email.trim();
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.error("Некорректный формат email");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Простая проверка валидности email (boolean)
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Простая проверка валидности username (boolean)
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username.trim()).matches();
    }
    
    /**
     * Санитизирует строку (убирает опасные символы)
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        
        // Убираем потенциально опасные символы
        return input.replaceAll("[<>\"'&]", "")
                    .replaceAll("\\s+", " ")
                    .trim();
    }
    
    /**
     * Результат валидации
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final boolean isWarning;
        
        private ValidationResult(boolean valid, String message, boolean isWarning) {
            this.valid = valid;
            this.message = message;
            this.isWarning = isWarning;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null, false);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message, false);
        }
        
        public static ValidationResult warning(String message) {
            return new ValidationResult(true, message, true);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public boolean isWarning() {
            return isWarning;
        }
    }
}

