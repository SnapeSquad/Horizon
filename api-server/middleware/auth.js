/**
 * Middleware для аутентификации и авторизации
 */
const jwt = require('jsonwebtoken');
const logger = require('../utils/logger');

const JWT_SECRET = process.env.JWT_SECRET || 'horizon_jwt_secret_2024';

/**
 * Проверяет JWT токен пользователя
 */
function verifyToken(req, res, next) {
    const token = req.headers.authorization?.replace('Bearer ', '') || 
                  req.headers['x-auth-token'] ||
                  req.query.token;
    
    if (!token) {
        return res.status(401).json({
            success: false,
            message: 'Токен авторизации не предоставлен.'
        });
    }
    
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        req.user = decoded;
        req.userId = decoded.id;
        req.username = decoded.username;
        next();
    } catch (err) {
        if (err.name === 'TokenExpiredError') {
            return res.status(401).json({
                success: false,
                message: 'Токен авторизации истек.'
            });
        }
        
        logger.debug('Ошибка верификации токена', err);
        return res.status(401).json({
            success: false,
            message: 'Неверный токен авторизации.'
        });
    }
}

/**
 * Опциональная проверка токена (не блокирует запрос, если токена нет)
 */
function optionalToken(req, res, next) {
    const token = req.headers.authorization?.replace('Bearer ', '') || 
                  req.headers['x-auth-token'] ||
                  req.query.token;
    
    if (token) {
        try {
            const decoded = jwt.verify(token, JWT_SECRET);
            req.user = decoded;
            req.userId = decoded.id;
            req.username = decoded.username;
        } catch (err) {
            // Игнорируем ошибки, так как токен опциональный
        }
    }
    
    next();
}

/**
 * Проверяет, что пользователь является владельцем ресурса или админом
 */
function requireOwnershipOrAdmin(req, res, next) {
    if (!req.user) {
        return res.status(401).json({
            success: false,
            message: 'Требуется авторизация.'
        });
    }
    
    const resourceUsername = req.params.username || req.body.username || req.query.username;
    
    // Админы имеют доступ ко всему
    if (req.user.admin || req.user.role === 'admin') {
        return next();
    }
    
    // Пользователь может обращаться только к своим ресурсам
    if (resourceUsername && resourceUsername !== req.user.username) {
        return res.status(403).json({
            success: false,
            message: 'Доступ запрещен. Вы можете обращаться только к своим ресурсам.'
        });
    }
    
    next();
}

/**
 * Проверяет роль пользователя
 */
function requireRole(...allowedRoles) {
    return (req, res, next) => {
        if (!req.user) {
            return res.status(401).json({
                success: false,
                message: 'Требуется авторизация.'
            });
        }
        
        const userRole = req.user.role || 'player';
        
        if (!allowedRoles.includes(userRole) && !req.user.admin) {
            return res.status(403).json({
                success: false,
                message: 'Недостаточно прав доступа.'
            });
        }
        
        next();
    };
}

module.exports = {
    verifyToken,
    optionalToken,
    requireOwnershipOrAdmin,
    requireRole
};
