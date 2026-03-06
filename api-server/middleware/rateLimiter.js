/**
 * Rate Limiting middleware для защиты от DDoS и брутфорса
 */
const logger = require('../utils/logger');

// Хранилище для счетчиков запросов (в продакшене лучше использовать Redis)
const requestCounts = new Map();

// Очистка старых записей каждые 5 минут
setInterval(() => {
    const now = Date.now();
    for (const [key, data] of requestCounts.entries()) {
        if (now - data.resetTime > 0) {
            requestCounts.delete(key);
        }
    }
}, 5 * 60 * 1000);

/**
 * Создает ключ для идентификации клиента
 */
function getClientKey(req) {
    // Используем IP адрес или комбинацию IP + User-Agent
    const ip = req.ip || req.connection.remoteAddress || 'unknown';
    const userAgent = req.get('user-agent') || '';
    return `${ip}:${userAgent.substring(0, 50)}`;
}

/**
 * Rate limiter для общих эндпоинтов
 */
function generalRateLimiter(maxRequests = 100, windowMs = 15 * 60 * 1000) {
    return (req, res, next) => {
        const key = getClientKey(req);
        const now = Date.now();
        
        let clientData = requestCounts.get(key);
        
        if (!clientData || now > clientData.resetTime) {
            clientData = {
                count: 0,
                resetTime: now + windowMs
            };
        }
        
        clientData.count++;
        requestCounts.set(key, clientData);
        
        // Устанавливаем заголовки с информацией о лимитах
        res.setHeader('X-RateLimit-Limit', maxRequests);
        res.setHeader('X-RateLimit-Remaining', Math.max(0, maxRequests - clientData.count));
        res.setHeader('X-RateLimit-Reset', new Date(clientData.resetTime).toISOString());
        
        if (clientData.count > maxRequests) {
            logger.warn('Rate limit exceeded', { 
                ip: req.ip, 
                path: req.path,
                count: clientData.count 
            });
            return res.status(429).json({
                success: false,
                message: 'Слишком много запросов. Попробуйте позже.',
                retryAfter: Math.ceil((clientData.resetTime - now) / 1000)
            });
        }
        
        next();
    };
}

/**
 * Строгий rate limiter для эндпоинтов аутентификации
 */
function authRateLimiter(maxRequests = 5, windowMs = 15 * 60 * 1000) {
    return (req, res, next) => {
        const key = `auth:${getClientKey(req)}`;
        const now = Date.now();
        
        let clientData = requestCounts.get(key);
        
        if (!clientData || now > clientData.resetTime) {
            clientData = {
                count: 0,
                resetTime: now + windowMs
            };
        }
        
        clientData.count++;
        requestCounts.set(key, clientData);
        
        res.setHeader('X-RateLimit-Limit', maxRequests);
        res.setHeader('X-RateLimit-Remaining', Math.max(0, maxRequests - clientData.count));
        res.setHeader('X-RateLimit-Reset', new Date(clientData.resetTime).toISOString());
        
        if (clientData.count > maxRequests) {
            logger.warn('Auth rate limit exceeded', { 
                ip: req.ip, 
                path: req.path,
                username: req.body?.username,
                count: clientData.count 
            });
            return res.status(429).json({
                success: false,
                message: 'Слишком много попыток входа. Попробуйте через 15 минут.',
                retryAfter: Math.ceil((clientData.resetTime - now) / 1000)
            });
        }
        
        next();
    };
}

/**
 * Rate limiter для 2FA эндпоинтов
 */
function twoFARateLimiter(maxRequests = 3, windowMs = 5 * 60 * 1000) {
    return (req, res, next) => {
        const key = `2fa:${getClientKey(req)}`;
        const now = Date.now();
        
        let clientData = requestCounts.get(key);
        
        if (!clientData || now > clientData.resetTime) {
            clientData = {
                count: 0,
                resetTime: now + windowMs
            };
        }
        
        clientData.count++;
        requestCounts.set(key, clientData);
        
        res.setHeader('X-RateLimit-Limit', maxRequests);
        res.setHeader('X-RateLimit-Remaining', Math.max(0, maxRequests - clientData.count));
        res.setHeader('X-RateLimit-Reset', new Date(clientData.resetTime).toISOString());
        
        if (clientData.count > maxRequests) {
            logger.warn('2FA rate limit exceeded', { 
                ip: req.ip, 
                path: req.path,
                count: clientData.count 
            });
            return res.status(429).json({
                success: false,
                message: 'Слишком много попыток верификации. Попробуйте через 5 минут.',
                retryAfter: Math.ceil((clientData.resetTime - now) / 1000)
            });
        }
        
        next();
    };
}

module.exports = {
    generalRateLimiter,
    authRateLimiter,
    twoFARateLimiter
};
