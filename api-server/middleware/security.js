/**
 * Middleware для безопасности
 */

/**
 * Устанавливает безопасные HTTP заголовки
 */
function securityHeaders(req, res, next) {
    // Защита от XSS
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    
    // Защита от MIME type sniffing
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    
    // Скрываем информацию о сервере
    res.removeHeader('X-Powered-By');
    
    next();
}

/**
 * Валидация и санитизация входных данных
 */
function sanitizeInput(req, res, next) {
    // Рекурсивная функция для очистки объекта
    function sanitize(obj) {
        if (typeof obj !== 'object' || obj === null) {
            return typeof obj === 'string' ? obj.trim() : obj;
        }
        
        if (Array.isArray(obj)) {
            return obj.map(item => sanitize(item));
        }
        
        const sanitized = {};
        for (const [key, value] of Object.entries(obj)) {
            // Удаляем потенциально опасные ключи
            if (key.startsWith('__') || key.startsWith('$')) {
                continue;
            }
            
            sanitized[key] = sanitize(value);
        }
        
        return sanitized;
    }
    
    // Санитизируем body, query и params
    if (req.body) {
        req.body = sanitize(req.body);
    }
    
    if (req.query) {
        req.query = sanitize(req.query);
    }
    
    next();
}

/**
 * Валидация размера тела запроса
 */
function validateBodySize(maxSize = 10 * 1024 * 1024) { // 10MB по умолчанию
    return (req, res, next) => {
        const contentLength = parseInt(req.get('content-length') || '0', 10);
        
        if (contentLength > maxSize) {
            return res.status(413).json({
                success: false,
                message: `Размер запроса превышает максимально допустимый (${maxSize / 1024 / 1024}MB).`
            });
        }
        
        next();
    };
}

/**
 * Защита от CSRF (базовая проверка Origin/Referer)
 */
function csrfProtection(req, res, next) {
    // Пропускаем GET запросы и запросы без тела
    if (req.method === 'GET' || req.method === 'HEAD' || req.method === 'OPTIONS') {
        return next();
    }
    
    // В development режиме пропускаем проверку
    if (process.env.NODE_ENV === 'development') {
        return next();
    }
    
    const origin = req.get('origin');
    const referer = req.get('referer');
    const allowedOrigin = process.env.CORS_ORIGIN;
    
    // Если указан CORS_ORIGIN, проверяем соответствие
    if (allowedOrigin && allowedOrigin !== '*') {
        if (origin && !origin.startsWith(allowedOrigin)) {
            return res.status(403).json({
                success: false,
                message: 'Запрос отклонен из-за несоответствия Origin.'
            });
        }
    }
    
    next();
}

/**
 * Логирование подозрительной активности
 */
function suspiciousActivityLogger(req, res, next) {
    const suspiciousPatterns = [
        /<script/i,
        /javascript:/i,
        /on\w+\s*=/i,
        /union\s+select/i,
        /drop\s+table/i,
        /exec\s*\(/i,
        /eval\s*\(/i
    ];
    
    const checkString = (str) => {
        if (typeof str !== 'string') return false;
        return suspiciousPatterns.some(pattern => pattern.test(str));
    };
    
    const checkObject = (obj) => {
        if (typeof obj !== 'object' || obj === null) return false;
        
        for (const value of Object.values(obj)) {
            if (typeof value === 'string' && checkString(value)) {
                return true;
            }
            if (typeof value === 'object' && checkObject(value)) {
                return true;
            }
        }
        
        return false;
    };
    
    if (req.body && checkObject(req.body)) {
        const logger = require('../utils/logger');
        logger.warn('Обнаружена подозрительная активность', {
            ip: req.ip,
            path: req.path,
            method: req.method,
            userAgent: req.get('user-agent')
        });
    }
    
    next();
}

module.exports = {
    securityHeaders,
    sanitizeInput,
    validateBodySize,
    csrfProtection,
    suspiciousActivityLogger
};
