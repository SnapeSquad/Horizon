/**
 * Middleware для логирования HTTP запросов
 */
const logger = require('../utils/logger');

function requestLogger(req, res, next) {
    const startTime = Date.now();
    
    // Логируем запрос
    logger.debug(`${req.method} ${req.path}`, {
        query: req.query,
        body: req.method !== 'GET' ? req.body : undefined,
        ip: req.ip,
        userAgent: req.get('user-agent')
    });
    
    // Перехватываем ответ
    const originalSend = res.send;
    res.send = function(data) {
        const duration = Date.now() - startTime;
        logger.http(req.method, req.path, res.statusCode, duration);
        originalSend.call(this, data);
    };
    
    next();
}

module.exports = requestLogger;
