/**
 * Централизованная обработка ошибок
 */
const logger = require('./logger');

/**
 * Создает стандартизированный ответ об ошибке
 */
function createErrorResponse(message, statusCode = 400, details = null) {
    const response = {
        success: false,
        message: message
    };
    
    if (details && process.env.NODE_ENV === 'development') {
        response.details = details;
    }
    
    return { response, statusCode };
}

/**
 * Обработчик ошибок для Express
 */
function errorHandler(err, req, res, next) {
    logger.error('Ошибка обработки запроса', err, {
        method: req.method,
        path: req.path,
        body: req.body,
        query: req.query
    });

    // Ошибки валидации
    if (err.name === 'ValidationError') {
        const { response, statusCode } = createErrorResponse(
            err.message || 'Ошибка валидации данных',
            400,
            err.details
        );
        return res.status(statusCode).json(response);
    }

    // Ошибки JWT
    if (err.name === 'JsonWebTokenError') {
        const { response, statusCode } = createErrorResponse(
            'Неверный токен авторизации.',
            401
        );
        return res.status(statusCode).json(response);
    }

    if (err.name === 'TokenExpiredError') {
        const { response, statusCode } = createErrorResponse(
            'Токен авторизации истек.',
            401
        );
        return res.status(statusCode).json(response);
    }

    // Ошибки Multer (загрузка файлов)
    if (err.code === 'LIMIT_FILE_SIZE') {
        const { response, statusCode } = createErrorResponse(
            'Размер файла превышает допустимый лимит.',
            413
        );
        return res.status(statusCode).json(response);
    }

    if (err.code === 'LIMIT_FILE_COUNT') {
        const { response, statusCode } = createErrorResponse(
            'Превышено максимальное количество файлов.',
            413
        );
        return res.status(statusCode).json(response);
    }

    // Ошибки базы данных
    if (err.code === 'SQLITE_CONSTRAINT') {
        const { response, statusCode } = createErrorResponse(
            'Нарушение ограничений базы данных.',
            409,
            process.env.NODE_ENV === 'development' ? err.message : null
        );
        return res.status(statusCode).json(response);
    }

    // Общая ошибка сервера
    const { response, statusCode } = createErrorResponse(
        'Внутренняя ошибка сервера.',
        500,
        process.env.NODE_ENV === 'development' ? err.message : null
    );
    
    res.status(statusCode).json(response);
}

/**
 * Middleware для обработки асинхронных ошибок
 */
function asyncHandler(fn) {
    return (req, res, next) => {
        Promise.resolve(fn(req, res, next)).catch(next);
    };
}

/**
 * Создает ошибку валидации
 */
function createValidationError(message, details = null) {
    const error = new Error(message);
    error.name = 'ValidationError';
    error.details = details;
    return error;
}

module.exports = {
    errorHandler,
    asyncHandler,
    createErrorResponse,
    createValidationError
};
