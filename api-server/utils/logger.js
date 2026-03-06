/**
 * Улучшенная система логирования для API сервера
 */
const fs = require('fs');
const path = require('path');

class Logger {
    constructor() {
        this.logDir = path.join(__dirname, '../logs');
        this.ensureLogDirectory();
    }

    ensureLogDirectory() {
        if (!fs.existsSync(this.logDir)) {
            fs.mkdirSync(this.logDir, { recursive: true });
        }
    }

    getLogFileName() {
        const date = new Date().toISOString().split('T')[0];
        return path.join(this.logDir, `api-${date}.log`);
    }

    formatMessage(level, message, data = null) {
        const timestamp = new Date().toISOString();
        const dataStr = data ? ` | Data: ${JSON.stringify(data)}` : '';
        return `[${timestamp}] [${level}] ${message}${dataStr}\n`;
    }

    writeToFile(message) {
        try {
            fs.appendFileSync(this.getLogFileName(), message, 'utf8');
        } catch (err) {
            console.error('Ошибка записи в лог файл:', err.message);
        }
    }

    info(message, data = null) {
        const formatted = this.formatMessage('INFO', message, data);
        console.log(`✅ ${message}`, data || '');
        this.writeToFile(formatted);
    }

    error(message, error = null, data = null) {
        const errorData = error ? {
            message: error.message,
            stack: error.stack,
            ...data
        } : data;
        const formatted = this.formatMessage('ERROR', message, errorData);
        console.error(`❌ ${message}`, error || data || '');
        this.writeToFile(formatted);
    }

    warn(message, data = null) {
        const formatted = this.formatMessage('WARN', message, data);
        console.warn(`⚠️  ${message}`, data || '');
        this.writeToFile(formatted);
    }

    debug(message, data = null) {
        if (process.env.NODE_ENV === 'development') {
            const formatted = this.formatMessage('DEBUG', message, data);
            console.debug(`🔍 ${message}`, data || '');
            this.writeToFile(formatted);
        }
    }

    http(method, path, statusCode, duration = null) {
        const message = `${method} ${path} - ${statusCode}${duration ? ` (${duration}ms)` : ''}`;
        const formatted = this.formatMessage('HTTP', message);
        console.log(`📡 ${message}`);
        this.writeToFile(formatted);
    }
}

module.exports = new Logger();
