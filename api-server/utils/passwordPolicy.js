/**
 * Политика безопасности паролей
 */

/**
 * Проверяет сложность пароля
 */
function checkPasswordStrength(password) {
    const issues = [];
    
    if (password.length < 8) {
        issues.push('Пароль должен содержать минимум 8 символов.');
    }
    
    if (!/[a-z]/.test(password)) {
        issues.push('Пароль должен содержать хотя бы одну строчную букву.');
    }
    
    if (!/[A-Z]/.test(password)) {
        issues.push('Пароль должен содержать хотя бы одну заглавную букву.');
    }
    
    if (!/[0-9]/.test(password)) {
        issues.push('Пароль должен содержать хотя бы одну цифру.');
    }
    
    if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
        issues.push('Пароль должен содержать хотя бы один специальный символ.');
    }
    
    // Проверка на распространенные пароли
    const commonPasswords = [
        'password', '12345678', 'qwerty', 'abc123', 'password123',
        'admin', 'letmein', 'welcome', 'monkey', '1234567890'
    ];
    
    if (commonPasswords.some(common => password.toLowerCase().includes(common))) {
        issues.push('Пароль слишком простой. Используйте более сложный пароль.');
    }
    
    return {
        valid: issues.length === 0,
        issues: issues,
        strength: calculateStrength(password, issues)
    };
}

/**
 * Вычисляет силу пароля (0-100)
 */
function calculateStrength(password, issues) {
    let strength = 100;
    
    // Штрафы за проблемы
    strength -= issues.length * 15;
    
    // Бонусы за длину
    if (password.length >= 12) strength += 10;
    if (password.length >= 16) strength += 10;
    
    // Бонусы за разнообразие символов
    const uniqueChars = new Set(password).size;
    if (uniqueChars >= password.length * 0.7) strength += 10;
    
    return Math.max(0, Math.min(100, strength));
}

/**
 * Валидация пароля с учетом политики безопасности
 */
function validatePasswordWithPolicy(password, requireStrong = false) {
    const basicValidation = require('../utils/validator').validatePassword(password);
    
    if (!basicValidation.valid) {
        return basicValidation;
    }
    
    const strengthCheck = checkPasswordStrength(password);
    
    if (requireStrong && !strengthCheck.valid) {
        return {
            valid: false,
            error: strengthCheck.issues.join(' ')
        };
    }
    
    // Предупреждение о слабом пароле (но не блокируем)
    if (!strengthCheck.valid && !requireStrong) {
        return {
            valid: true,
            value: password,
            warning: strengthCheck.issues.join(' '),
            strength: strengthCheck.strength
        };
    }
    
    return {
        valid: true,
        value: password,
        strength: strengthCheck.strength
    };
}

module.exports = {
    checkPasswordStrength,
    validatePasswordWithPolicy,
    calculateStrength
};
