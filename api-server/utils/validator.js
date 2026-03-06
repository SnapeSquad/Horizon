/**
 * Утилиты для валидации данных
 */

/**
 * Валидация имени пользователя
 */
function validateUsername(username) {
    if (!username || typeof username !== 'string') {
        return { valid: false, error: 'Имя пользователя обязательно.' };
    }

    const trimmed = username.trim();
    
    if (trimmed.length < 3) {
        return { valid: false, error: 'Имя пользователя должно содержать минимум 3 символа.' };
    }
    
    if (trimmed.length > 16) {
        return { valid: false, error: 'Имя пользователя не может быть длиннее 16 символов.' };
    }
    
    if (!/^[a-zA-Z0-9_]+$/.test(trimmed)) {
        return { valid: false, error: 'Имя пользователя может содержать только латинские буквы, цифры и подчеркивания.' };
    }
    
    return { valid: true, value: trimmed };
}

/**
 * Валидация пароля
 */
function validatePassword(password) {
    if (!password || typeof password !== 'string') {
        return { valid: false, error: 'Пароль обязателен.' };
    }
    
    if (password.length < 6) {
        return { valid: false, error: 'Пароль должен содержать минимум 6 символов.' };
    }
    
    if (password.length > 128) {
        return { valid: false, error: 'Пароль не может быть длиннее 128 символов.' };
    }
    
    return { valid: true, value: password };
}

/**
 * Валидация кода 2FA
 */
function validate2FACode(code) {
    if (!code || typeof code !== 'string') {
        return { valid: false, error: 'Код 2FA обязателен.' };
    }
    
    if (code.length !== 6) {
        return { valid: false, error: 'Код 2FA должен содержать 6 цифр.' };
    }
    
    if (!/^\d+$/.test(code)) {
        return { valid: false, error: 'Код 2FA должен содержать только цифры.' };
    }
    
    return { valid: true, value: code };
}

/**
 * Валидация email (если будет использоваться)
 */
function validateEmail(email) {
    if (!email || typeof email !== 'string') {
        return { valid: false, error: 'Email обязателен.' };
    }
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return { valid: false, error: 'Некорректный формат email.' };
    }
    
    return { valid: true, value: email.trim().toLowerCase() };
}

/**
 * Валидация ID (число)
 */
function validateId(id, fieldName = 'ID') {
    if (id === undefined || id === null) {
        return { valid: false, error: `${fieldName} обязателен.` };
    }
    
    const numId = parseInt(id, 10);
    if (isNaN(numId) || numId <= 0) {
        return { valid: false, error: `${fieldName} должен быть положительным числом.` };
    }
    
    return { valid: true, value: numId };
}

/**
 * Валидация строки (не пустая)
 */
function validateString(str, fieldName, minLength = 1, maxLength = null) {
    if (!str || typeof str !== 'string') {
        return { valid: false, error: `${fieldName} обязателен.` };
    }
    
    const trimmed = str.trim();
    
    if (trimmed.length < minLength) {
        return { valid: false, error: `${fieldName} должен содержать минимум ${minLength} символов.` };
    }
    
    if (maxLength && trimmed.length > maxLength) {
        return { valid: false, error: `${fieldName} не может быть длиннее ${maxLength} символов.` };
    }
    
    return { valid: true, value: trimmed };
}

/**
 * Валидация массива
 */
function validateArray(arr, fieldName, minLength = 0) {
    if (!Array.isArray(arr)) {
        return { valid: false, error: `${fieldName} должен быть массивом.` };
    }
    
    if (arr.length < minLength) {
        return { valid: false, error: `${fieldName} должен содержать минимум ${minLength} элементов.` };
    }
    
    return { valid: true, value: arr };
}

/**
 * Валидация объекта
 */
function validateObject(obj, fieldName) {
    if (!obj || typeof obj !== 'object' || Array.isArray(obj)) {
        return { valid: false, error: `${fieldName} должен быть объектом.` };
    }
    
    return { valid: true, value: obj };
}

module.exports = {
    validateUsername,
    validatePassword,
    validate2FACode,
    validateEmail,
    validateId,
    validateString,
    validateArray,
    validateObject
};
