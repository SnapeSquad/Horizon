// api-server/server.js
require('dotenv').config(); 

const express = require('express');
const TelegramBotClient = require('./utils/telegramBotClient');
const fs = require('fs');
const path = require('path');
const bcrypt = require('bcrypt');
const sqlite3 = require('sqlite3').verbose();
const mcu = require('minecraft-server-util');
const cors = require('cors');
const multer = require('multer');
const jwt = require('jsonwebtoken');

// Импортируем утилиты
const logger = require('./utils/logger');
const { errorHandler, asyncHandler } = require('./utils/errorHandler');
const requestLogger = require('./middleware/requestLogger');
const { validateUsername, validatePassword, validate2FACode, validateId, validateString } = require('./utils/validator');

// Импортируем middleware безопасности
const { generalRateLimiter, authRateLimiter, twoFARateLimiter } = require('./middleware/rateLimiter');
const { securityHeaders, sanitizeInput, validateBodySize, csrfProtection, suspiciousActivityLogger } = require('./middleware/security');

const app = express();
const PORT = 3000; 
const DB_PATH = path.join(__dirname, 'users.db');
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || 'horizon_admin_2024';
const JWT_SECRET = process.env.JWT_SECRET || 'horizon_jwt_secret_2024';

// Директории для файлов
const UPLOADS_DIR = path.join(__dirname, 'uploads');
const COSMETICS_DIR = path.join(UPLOADS_DIR, 'cosmetics');

// Создаем директории
[UPLOADS_DIR, COSMETICS_DIR].forEach(dir => {
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }
});

// Настройка multer для загрузки файлов
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, COSMETICS_DIR);
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, file.fieldname + '-' + uniqueSuffix + path.extname(file.originalname));
    }
});

const upload = multer({ 
    storage: storage,
    limits: { fileSize: 10 * 1024 * 1024 }, // 10MB лимит
    fileFilter: (req, file, cb) => {
        const allowedTypes = {
            'model': ['.json'],
            'texture': ['.png', '.jpg', '.jpeg']
        };
        const ext = path.extname(file.originalname).toLowerCase();
        const fieldName = file.fieldname;
        
        if (allowedTypes[fieldName] && allowedTypes[fieldName].includes(ext)) {
            cb(null, true);
        } else {
            cb(new Error(`Неподдерживаемый тип файла для ${fieldName}: ${ext}`));
        }
    }
});

// Middleware для проверки JWT токена (для админских эндпоинтов)
function verifyAdminToken(req, res, next) {
    const token = req.headers.authorization?.replace('Bearer ', '') || req.headers['x-admin-token'];
    
    if (!token) {
        return res.status(401).json({ success: false, message: 'Токен не предоставлен.' });
    }
    
    // Проверяем как JWT токен, так и простой admin токен
    if (token === ADMIN_TOKEN) {
        req.admin = true;
        return next();
    }
    
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        if (decoded.admin) {
            req.admin = true;
            req.user = decoded;
            return next();
        }
    } catch (err) {
        // Игнорируем ошибки JWT, используем простой токен
    }
    
    return res.status(403).json({ success: false, message: 'Неверный токен авторизации.' });
}

// --- Инициализация SQLite БД ---
const db = new sqlite3.Database(DB_PATH, (err) => {
    if (err) {
        logger.error('Ошибка подключения к БД', err);
    } else {
        logger.info('Подключено к SQLite БД');
        // Создаем таблицы
        db.serialize(() => {
            db.run(`CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                telegram_chat_id TEXT,
                two_factor_enabled INTEGER DEFAULT 0,
                cosmetics TEXT,
                skin TEXT,
                currency INTEGER DEFAULT 0,
                skin_model TEXT DEFAULT 'classic',
                cape TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            // Создаем таблицы форума
            db.run(`CREATE TABLE IF NOT EXISTS forum_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                icon TEXT,
                order_index INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            db.run(`CREATE TABLE IF NOT EXISTS forum_topics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_id INTEGER NOT NULL,
                author_username TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT,
                is_pinned INTEGER DEFAULT 0,
                is_locked INTEGER DEFAULT 0,
                views INTEGER DEFAULT 0,
                replies_count INTEGER DEFAULT 0,
                last_reply_at DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            db.run(`CREATE TABLE IF NOT EXISTS forum_posts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                topic_id INTEGER NOT NULL,
                author_username TEXT NOT NULL,
                content TEXT NOT NULL,
                is_edited INTEGER DEFAULT 0,
                edited_at DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            db.run(`CREATE TABLE IF NOT EXISTS forum_likes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                post_id INTEGER NOT NULL,
                username TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(post_id, username)
            )`);
            
            db.run(`CREATE TABLE IF NOT EXISTS cosmetic_mods (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                version TEXT NOT NULL,
                description TEXT,
                author TEXT NOT NULL,
                file_path TEXT NOT NULL,
                is_active INTEGER DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            db.run(`CREATE TABLE IF NOT EXISTS cosmetic_animations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cosmetic_id TEXT NOT NULL,
                animation_type TEXT NOT NULL,
                frames TEXT NOT NULL,
                duration INTEGER DEFAULT 1000,
                loop INTEGER DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            // Таблица косметики для админ-панели
            db.run(`CREATE TABLE IF NOT EXISTS cosmetics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                pivot_point TEXT NOT NULL,
                price INTEGER DEFAULT 0,
                rarity TEXT DEFAULT 'common',
                model_file_path TEXT NOT NULL,
                texture_file_path TEXT NOT NULL,
                is_active INTEGER DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            // Таблица новостей
            db.run(`CREATE TABLE IF NOT EXISTS news (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                image_url TEXT,
                author TEXT,
                views INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            // Таблица банов по HWID
            db.run(`CREATE TABLE IF NOT EXISTS banned_hwid (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hwid TEXT NOT NULL UNIQUE,
                reason TEXT,
                banned_by TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            // Добавляем поле hwid в таблицу users, если его нет
            db.run(`ALTER TABLE users ADD COLUMN hwid TEXT`, (err) => {
                // Игнорируем ошибку, если колонка уже существует
            });

            // Добавляем роль пользователя, если поля еще нет
            db.run(`ALTER TABLE users ADD COLUMN role TEXT DEFAULT 'player'`, (err) => {
                // Игнорируем ошибку, если колонка уже существует
            });
            
            // Инициализируем категории форума
            db.get('SELECT COUNT(*) as count FROM forum_categories', (err, row) => {
                if (!err && row && row.count === 0) {
                    const categories = [
                        ['Общие обсуждения', 'Обсуждения сервера и лаунчера', '💬', 0],
                        ['Техническая поддержка', 'Помощь и решение проблем', '🔧', 1],
                        ['Предложения', 'Ваши идеи и предложения', '💡', 2],
                        ['Косметика', 'Обсуждение косметики и скинов', '👕', 3],
                        ['События', 'Новости и события сервера', '🎉', 4]
                    ];
                    
                    const stmt = db.prepare('INSERT INTO forum_categories (name, description, icon, order_index) VALUES (?, ?, ?, ?)');
                    categories.forEach(cat => stmt.run(...cat));
                    stmt.finalize();
                }
            });
        });
    }
});

// --- Инициализация Telegram ---
const BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const BOT_PLACEHOLDER = 'ВАШ_СЕКРЕТНЫЙ_ТОКЕН_БОТА';
const TELEGRAM_SILENT_MODE = process.env.HORIZON_SILENT_TELEGRAM === '1' || process.env.NODE_ENV === 'test';

if (BOT_TOKEN === BOT_PLACEHOLDER) {
    logger.error("ОШИБКА: Замените 'ВАШ_СЕКРЕТНЫЙ_ТОКЕН_БОТА' на реальный токен в файле .env!");
} else if (!BOT_TOKEN) {
    if (!TELEGRAM_SILENT_MODE) {
        logger.warn('TELEGRAM_BOT_TOKEN не задан. Telegram Bot будет отключен.');
    }
}

const bot = BOT_TOKEN && BOT_TOKEN !== BOT_PLACEHOLDER
    ? new TelegramBotClient(BOT_TOKEN, { polling: true })
    : null;

// --- Временное хранилище для ожидания входа ---
const pendingTgLogins = {};
const pendingTgLoginsByChatId = {};

// --- ЛОГИКА ОБРАБОТКИ СООБЩЕНИЙ ОТ TELEGRAM ---
if (bot) {
    bot.on('message', (msg) => {
        const chatId = msg.chat.id;
        const text = msg.text ? msg.text.trim() : '';
        const textLower = text.toLowerCase();

        console.log(`[TG MSG] Received from ${chatId}: ${text}`);

        // Проверяем коды для входа или привязки 2FA
        const codeKey = textLower;
        const codeKeyUpper = text.toUpperCase();
        const codeToCheck = pendingTgLogins[codeKey] || pendingTgLogins[codeKeyUpper];
        
        if (codeToCheck) {
            const loginData = codeToCheck;
            const actualCodeKey = pendingTgLogins[codeKey] ? codeKey : codeKeyUpper;
            
            if (Date.now() > loginData.timestamp) {
                delete pendingTgLogins[codeKey];
                delete pendingTgLogins[codeKeyUpper];
                bot.sendMessage(chatId, `❌ Срок действия кода истек. Попробуйте снова.`, { parse_mode: 'Markdown' });
                return;
            }

            // Если это привязка 2FA
            if (loginData.type === '2fa_setup') {
                db.run('UPDATE users SET telegram_chat_id = ?, two_factor_enabled = 1 WHERE username = ?', 
                    [chatId.toString(), loginData.username], (err) => {
                    if (err) {
                        console.error('[TG 2FA] Ошибка сохранения chat_id:', err);
                        bot.sendMessage(chatId, `❌ Ошибка при привязке Telegram. Попробуйте позже.`);
                        return;
                    }
                    
                    delete pendingTgLogins[codeKey];
                    delete pendingTgLogins[codeKeyUpper];
                    bot.sendMessage(chatId, 
                        `✅ Telegram 2FA успешно привязана для аккаунта *${loginData.username}*!`,
                        { parse_mode: 'Markdown' }
                    );
                });
                return;
            }

            // Если это код для входа (login_2fa)
            if (loginData.type === 'login_2fa') {
                const textTrimmed = String(text).trim().replace(/\s/g, '');
                const codeMatch = String(loginData.code).trim() === textTrimmed;
                
                if (codeMatch) {
                    bot.sendMessage(chatId, `✅ Код подтвержден! Вы можете войти в лаунчер.`, { parse_mode: 'Markdown' });
                    console.log(`[TG LOGIN] Код подтвержден для ${loginData.username} через бота`);
                } else {
                    bot.sendMessage(chatId, `❌ Код неверный.`, { parse_mode: 'Markdown' });
                }
                return;
            }

            // Обычный вход (старый формат)
            loginData.chatId = chatId;
            loginData.tgUsername = msg.from.username; 
            
            delete pendingTgLogins[codeKey];
            delete pendingTgLogins[codeKeyUpper];
            pendingTgLoginsByChatId[chatId] = loginData;

            bot.sendMessage(chatId, `✅ Аккаунт *${loginData.username}* верифицирован!`, { parse_mode: 'Markdown' });
            console.log(`[TG AUTH] Код ${text} успешно верифицирован для ${loginData.username}`);
            return;
        }

        // Команды бота
        if (textLower === '/start') {
            bot.sendMessage(chatId, 
                `👋 Привет! Я бот для двухфакторной аутентификации Horizon Launcher.\n\n` +
                `📋 Доступные команды:\n` +
                `• Отправьте код из лаунчера для входа\n` +
                `• Отправьте код для привязки Telegram 2FA\n\n` +
                `🔒 Для защиты аккаунта рекомендуется включить 2FA.`,
                { parse_mode: 'Markdown' }
            );
        } else if (textLower === '/help') {
            bot.sendMessage(chatId,
                `📖 Помощь по использованию бота:\n\n` +
                `1️⃣ **Вход в лаунчер:**\n` +
                `   Введите свой никнейм в лаунчере, затем отправьте полученный код боту.\n\n` +
                `2️⃣ **Привязка Telegram 2FA:**\n` +
                `   В настройках лаунчера выберите "Telegram 2FA" и отправьте полученный код боту.\n\n` +
                `3️⃣ **Получение кодов 2FA:**\n` +
                `   После привязки вы будете получать коды подтверждения при каждом входе.`,
                { parse_mode: 'Markdown' }
            );
        } else {
            bot.sendMessage(chatId, `❓ Неизвестная команда. Используйте /help для справки.`, { parse_mode: 'Markdown' });
        }
    });

    bot.on('polling_error', (error) => {
        console.error('[TG BOT] Polling error:', error);
    });

    console.log(`🤖 Telegram Bot запущен и слушает входящие сообщения (Polling).`);
} else {
    if (!TELEGRAM_SILENT_MODE) {
        console.warn(`⚠️ Telegram Bot не запущен, так как не найден токен в .env.`);
    }
}

// --- Middleware ---
// Безопасность: заголовки безопасности (должны быть первыми)
app.use(securityHeaders);

// Валидация размера тела запроса
app.use(validateBodySize(10 * 1024 * 1024)); // 10MB

// Парсинг JSON и URL-encoded
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Санитизация входных данных
app.use(sanitizeInput);

// Защита от подозрительной активности
app.use(suspiciousActivityLogger);

// CORS настройки
const corsOptions = {
    origin: process.env.CORS_ORIGIN || (process.env.NODE_ENV === 'production' ? false : '*'),
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Admin-Token'],
    maxAge: 86400 // 24 часа
};
app.use(cors(corsOptions));

// CSRF защита (только для production)
if (process.env.NODE_ENV === 'production') {
    app.use(csrfProtection);
}

// Логирование запросов (только в development)
if (process.env.NODE_ENV === 'development') {
    app.use(requestLogger);
}

// Общий rate limiter для всех запросов
app.use(generalRateLimiter(100, 15 * 60 * 1000)); // 100 запросов за 15 минут

app.use('/uploads', express.static(UPLOADS_DIR)); // Статические файлы для загрузок

app.get('/', (req, res) => {
    res.send('API Server запущен и ждет запросов от лаунчера!');
});

// --- Маршрут: Статус сервера Minecraft ---
app.get('/api/server/status', async (req, res) => {
    const MC_SERVER_HOST = 'hor1zon.fun'; 
    const MC_SERVER_PORT = 25565; 
    
    try {
        const response = await mcu.status(MC_SERVER_HOST, MC_SERVER_PORT, { timeout: 5000, enableSRV: true });
        return res.json({
            online: true,
            motd: response.motd.clean, 
            version: response.version.name,
            players: {
                online: response.players.online,
                max: response.players.max
            }
        });
    } catch (error) {
        console.warn(`[MC STATUS] Не удалось подключиться к серверу ${MC_SERVER_HOST}:${MC_SERVER_PORT}.`);
        return res.json({
            online: false,
            motd: 'Сервер отключен или недоступен.',
            version: 'N/A',
            players: { online: 0, max: 0 }
        });
    }
});

// Утилиты валидации импортированы из utils/validator.js

// --- Регистрация ---
app.post('/api/auth/register', authRateLimiter(5, 15 * 60 * 1000), (req, res) => {
    const { username, password, hwid } = req.body;

    // Валидация username
    const usernameValidation = validateUsername(username);
    if (!usernameValidation.valid) {
        return res.status(400).json({ success: false, message: usernameValidation.error });
    }
    const validUsername = usernameValidation.value;

    // Валидация password
    const passwordValidation = validatePassword(password);
    if (!passwordValidation.valid) {
        return res.status(400).json({ success: false, message: passwordValidation.error });
    }

    // Проверка HWID на бан (внутри запроса к пользователю для правильной последовательности)
    db.get('SELECT id FROM users WHERE username = ?', [validUsername], (err, row) => {
        if (err) {
            console.error('[REGISTER] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера при проверке пользователя.' });
        }

        if (row) {
            return res.status(409).json({ success: false, message: 'Пользователь с таким именем уже существует.' });
        }

        // Проверка HWID на бан (перед регистрацией)
        if (hwid) {
            db.get('SELECT * FROM banned_hwid WHERE hwid = ?', [hwid], (err, ban) => {
                if (err) {
                    console.error('[REGISTER] Ошибка проверки HWID:', err);
                    // При ошибке проверки HWID продолжаем регистрацию (логируем, но не блокируем)
                    continueRegistration();
                } else if (ban) {
                    return res.status(403).json({ 
                        success: false, 
                        message: 'Ваше устройство заблокировано. Причина: ' + (ban.reason || 'Не указана') + '.' 
                    });
                } else {
                    // Если HWID не забанен, продолжаем регистрацию
                    continueRegistration();
                }
            });
        } else {
            // Если HWID не предоставлен, продолжаем регистрацию
            continueRegistration();
        }

        function continueRegistration() {
            bcrypt.hash(password, 10, (err, hashedPassword) => {
            if (err) {
                console.error('[REGISTER] Ошибка хэширования пароля:', err);
                return res.status(500).json({ success: false, message: 'Ошибка сервера при регистрации.' });
            }

            // Сохраняем пользователя с HWID
            const hwidValue = hwid || null;
            db.run('INSERT INTO users (username, password, hwid) VALUES (?, ?, ?)', [validUsername, hashedPassword, hwidValue], function(err) {
                if (err) {
                    console.error('[REGISTER] Ошибка сохранения в БД:', err);
                    // Проверяем на дубликат username (на случай race condition)
                    if (err.message && err.message.includes('UNIQUE constraint failed')) {
                        return res.status(409).json({ success: false, message: 'Пользователь с таким именем уже существует.' });
                    }
                    return res.status(500).json({ success: false, message: 'Ошибка сервера при сохранении пользователя.' });
                }
                
                // Генерируем токен для нового пользователя
                const token = jwt.sign(
                    { username: validUsername, id: this.lastID, role: 'player', admin: false },
                    JWT_SECRET,
                    { expiresIn: '7d' }
                );
                
                console.log(`[REGISTER] Пользователь ${validUsername} успешно зарегистрирован.`);
                res.json({ success: true, message: 'Регистрация прошла успешно!', token: token });
            });
        });
        }
    });
});

// --- Вход ---
app.post('/api/auth/login', authRateLimiter(5, 15 * 60 * 1000), (req, res) => {
    let { username, password, twoFactorCode, hwid } = req.body;
    
    // Валидация username
    const usernameValidation = validateUsername(username);
    if (!usernameValidation.valid) {
        return res.status(400).json({ success: false, message: 'Неверный формат имени пользователя.' });
    }
    const validUsername = usernameValidation.value;

    if (!password || typeof password !== 'string' || password.trim().length === 0) {
        return res.status(400).json({ success: false, message: 'Пароль обязателен.' });
    }
    
    if (twoFactorCode) {
        twoFactorCode = String(twoFactorCode).trim().replace(/\D/g, '');
        if (twoFactorCode.length !== 6) {
            return res.status(400).json({ success: false, message: 'Код двухфакторной аутентификации должен содержать 6 цифр.' });
        }
    }

    db.get('SELECT * FROM users WHERE username = ?', [validUsername], (err, user) => {
        if (err) {
            console.error('[LOGIN] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера при проверке пользователя.' });
        }

        if (!user) {
            return res.status(401).json({ success: false, message: 'Пользователь с таким логином не найден. Проверьте правильность ввода.' });
        }

        // Проверка HWID на бан (перед проверкой пароля)
        if (hwid) {
            db.get('SELECT * FROM banned_hwid WHERE hwid = ?', [hwid], (err, bannedHwid) => {
                if (err) {
                    console.error('[LOGIN] Ошибка проверки HWID:', err);
                    // При ошибке проверки HWID продолжаем (логируем, но не блокируем)
                    checkPassword();
                } else if (bannedHwid) {
                    return res.status(403).json({ 
                        success: false, 
                        message: 'Ваше устройство заблокировано. Причина: ' + (bannedHwid.reason || 'Не указана') + '.' 
                    });
                } else {
                    // Если HWID не забанен, проверяем пароль
                    checkPassword();
                }
            });
        } else {
            // Если HWID не предоставлен, проверяем пароль
            checkPassword();
        }

        function checkPassword() {
            bcrypt.compare(password, user.password, (err, isMatch) => {
                if (err) {
                    console.error('[LOGIN] Ошибка сравнения пароля:', err);
                    return res.status(500).json({ success: false, message: 'Ошибка сервера при проверке пароля.' });
                }
            
            if (!isMatch) {
                return res.status(401).json({ success: false, message: 'Неверный пароль. Проверьте правильность ввода.' });
            }

            // Проверяем 2FA
            if (user.two_factor_enabled) {
                if (!twoFactorCode) {
                    if (user.telegram_chat_id && bot) {
                        const loginCode = String(Math.floor(100000 + Math.random() * 900000));
                        const expiresAt = Date.now() + 5 * 60 * 1000;
                        
                        const loginData = {
                            username: user.username,
                            code: loginCode,
                            timestamp: expiresAt,
                            type: 'login_2fa',
                            chatId: String(user.telegram_chat_id)
                        };
                        
                        pendingTgLogins[loginCode] = loginData;
                        
                        bot.sendMessage(user.telegram_chat_id,
                            `🔐 Код для входа в аккаунт *${user.username}*:\n\n*${loginCode}*\n\nКод действителен 5 минут.`,
                            { parse_mode: 'Markdown' }
                        ).catch(err => console.error('[LOGIN 2FA] Ошибка отправки кода:', err));
                        
                        return res.status(200).json({ 
                            success: false, 
                            status: 'NEED_2FA',
                            requires2FA: true,
                            message: 'Код отправлен в Telegram. Введите его для входа.' 
                        });
                    } else {
                        // Если 2FA включена, но Telegram не привязан или бот недоступен
                        return res.status(400).json({ 
                            success: false, 
                            message: 'Двухфакторная аутентификация включена, но Telegram не привязан к аккаунту или бот недоступен. Обратитесь к администратору.' 
                        });
                    }
                }

                // Проверяем код 2FA
                let codeValid = false;
                if (user.telegram_chat_id) {
                    const codeKey = String(twoFactorCode).trim().replace(/\D/g, '');
                    const loginData = pendingTgLogins[codeKey];
                    
                    if (loginData && loginData.type === 'login_2fa' && 
                        loginData.username === user.username && 
                        Date.now() <= loginData.timestamp &&
                        String(loginData.code) === codeKey) {
                        codeValid = true;
                        delete pendingTgLogins[codeKey];
                    }
                }

                if (!codeValid) {
                    return res.status(401).json({ success: false, message: 'Неверный код двухфакторной аутентификации.' });
                }
            }

            const resolvedRole = user.role || 'player';
            const isAdmin = resolvedRole === 'admin' || resolvedRole === 'owner';

            console.log(`[LOGIN] Пользователь ${user.username} успешно вошел в систему.`);
            res.json({ 
                success: true, 
                token: jwt.sign(
                    { username: user.username, id: user.id, role: resolvedRole, admin: isAdmin },
                    JWT_SECRET,
                    { expiresIn: '7d' }
                ), 
                username: user.username,
                has2FA: user.two_factor_enabled === 1
            });
            });
        }
    });
});

// --- Проверка 2FA кода (отдельный эндпоинт) ---
app.post('/api/auth/verify-2fa', twoFARateLimiter(3, 5 * 60 * 1000), (req, res) => {
    const { username, code, hwid } = req.body;
    
    // Валидация username
    const usernameValidation = validateUsername(username);
    if (!usernameValidation.valid) {
        return res.status(400).json({ success: false, message: 'Неверный формат имени пользователя.' });
    }
    const validUsername = usernameValidation.value;
    
    // Валидация кода
    if (!code || typeof code !== 'string') {
        return res.status(400).json({ success: false, message: 'Код обязателен.' });
    }
    const codeKey = String(code).trim().replace(/\D/g, '');
    if (codeKey.length !== 6) {
        return res.status(400).json({ success: false, message: 'Код должен содержать 6 цифр.' });
    }
    
    db.get('SELECT * FROM users WHERE username = ?', [validUsername], (err, user) => {
        if (err) {
            console.error('[VERIFY-2FA] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера при проверке пользователя.' });
        }
        
        if (!user) {
            return res.status(401).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        if (!user.two_factor_enabled) {
            return res.status(400).json({ success: false, message: 'Двухфакторная аутентификация не включена для этого аккаунта.' });
        }
        
        if (!user.telegram_chat_id) {
            return res.status(400).json({ success: false, message: 'Telegram не привязан к аккаунту.' });
        }
        
        // Проверка HWID на бан
        if (hwid) {
            db.get('SELECT * FROM banned_hwid WHERE hwid = ?', [hwid], (err, bannedHwid) => {
                if (err) {
                    console.error('[VERIFY-2FA] Ошибка проверки HWID:', err);
                } else if (bannedHwid) {
                    return res.status(403).json({ 
                        success: false, 
                        message: 'Ваше устройство заблокировано. Причина: ' + (bannedHwid.reason || 'Не указана') + '.' 
                    });
                }
                // Продолжаем проверку кода
                checkCode();
            });
        } else {
            checkCode();
        }
        
        function checkCode() {
            const loginData = pendingTgLogins[codeKey];
            
            if (!loginData || loginData.type !== 'login_2fa' || 
                loginData.username !== validUsername || 
                Date.now() > loginData.timestamp ||
                String(loginData.code) !== codeKey) {
                return res.status(401).json({ success: false, message: 'Неверный или истекший код двухфакторной аутентификации.' });
            }
            
            // Код валиден, удаляем его и выдаем токен
            delete pendingTgLogins[codeKey];
            
            const resolvedRole = user.role || 'player';
            const isAdmin = resolvedRole === 'admin' || resolvedRole === 'owner';

            console.log(`[VERIFY-2FA] Пользователь ${validUsername} успешно прошел 2FA.`);
            res.json({ 
                success: true, 
                token: jwt.sign(
                    { username: user.username, id: user.id, role: resolvedRole, admin: isAdmin },
                    JWT_SECRET,
                    { expiresIn: '7d' }
                ), 
                username: user.username,
                has2FA: true
            });
        }
    });
});

// --- Восстановление пароля: Запрос кода ---
app.post('/api/auth/recovery/request', authRateLimiter(3, 15 * 60 * 1000), (req, res) => {
    const { username, hwid } = req.body;

    // Валидация username
    const usernameValidation = validateUsername(username);
    if (!usernameValidation.valid) {
        return res.status(400).json({ success: false, message: 'Неверный формат имени пользователя.' });
    }
    const validUsername = usernameValidation.value;
    
    db.get('SELECT * FROM users WHERE username = ?', [validUsername], (err, user) => {
        if (err) {
            console.error('[RECOVERY] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        if (!user.telegram_chat_id) {
            return res.status(400).json({ success: false, message: 'Telegram не привязан к аккаунту.' });
        }
        
        // Генерируем код восстановления
        const recoveryCode = String(Math.floor(100000 + Math.random() * 900000));
        const expiresAt = Date.now() + 10 * 60 * 1000; // 10 минут
        
        // Сохраняем код во временное хранилище
        pendingTgLogins[recoveryCode] = {
            username: user.username,
            code: recoveryCode,
            timestamp: expiresAt,
            type: 'password_recovery',
            chatId: String(user.telegram_chat_id)
        };
        
        // Отправляем код в Telegram
        if (bot) {
            bot.sendMessage(user.telegram_chat_id,
                `🔐 Код для восстановления пароля аккаунта *${user.username}*:\n\n*${recoveryCode}*\n\nКод действителен 10 минут.`,
                { parse_mode: 'Markdown' }
            ).catch(err => console.error('[RECOVERY] Ошибка отправки кода:', err));
        }
        
        console.log(`[RECOVERY] Код восстановления отправлен для ${validUsername}`);
        res.json({ success: true, message: 'Код отправлен в Telegram.' });
    });
});

// --- Восстановление пароля: Сброс пароля ---
app.post('/api/auth/recovery/reset', authRateLimiter(3, 15 * 60 * 1000), (req, res) => {
    const { username, code, newPassword, hwid } = req.body;
    
    // Валидация username
    const usernameValidation = validateUsername(username);
    if (!usernameValidation.valid) {
        return res.status(400).json({ success: false, message: 'Неверный формат имени пользователя.' });
    }
    const validUsername = usernameValidation.value;

    // Валидация кода
    if (!code || typeof code !== 'string') {
        return res.status(400).json({ success: false, message: 'Код подтверждения обязателен.' });
    }
    const codeKey = String(code).trim().replace(/\D/g, '');
    if (codeKey.length !== 6) {
        return res.status(400).json({ success: false, message: 'Код должен содержать 6 цифр.' });
    }
    
    // Валидация пароля
    const passwordValidation = validatePassword(newPassword);
    if (!passwordValidation.valid) {
        return res.status(400).json({ success: false, message: passwordValidation.error });
    }
    
    const recoveryData = pendingTgLogins[codeKey];
    
    if (!recoveryData || recoveryData.type !== 'password_recovery' || 
        recoveryData.username !== validUsername || Date.now() > recoveryData.timestamp) {
        return res.status(401).json({ success: false, message: 'Неверный или истекший код восстановления.' });
    }
    
    // Удаляем код сразу после валидации (предотвращает повторное использование)
    delete pendingTgLogins[codeKey];
    
    // Хэшируем новый пароль
    bcrypt.hash(newPassword, 10, (err, hashedPassword) => {
        if (err) {
            console.error('[RECOVERY] Ошибка хэширования пароля:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера при сбросе пароля.' });
        }
        
        // Обновляем пароль
        db.run('UPDATE users SET password = ? WHERE username = ?', [hashedPassword, validUsername], function(err) {
            if (err) {
                console.error('[RECOVERY] Ошибка обновления пароля:', err);
                return res.status(500).json({ success: false, message: 'Ошибка сервера при обновлении пароля.' });
            }
            
            console.log(`[RECOVERY] Пароль успешно восстановлен для ${validUsername}`);
            res.json({ success: true, message: 'Пароль успешно восстановлен.' });
        });
    });
});

// --- Проверка токена (для автоматического входа) ---
app.post('/api/auth/verify', (req, res) => {
    const { token, username, hwid } = req.body;
    
    if (!token) {
        return res.status(400).json({ success: false, message: 'Токен не предоставлен.' });
    }

    // Валидация username, если предоставлен
    if (username) {
        const usernameValidation = validateUsername(username);
        if (!usernameValidation.valid) {
            return res.status(400).json({ success: false, message: 'Неверный формат имени пользователя.' });
        }
    }
    
    // Пытаемся проверить токен как JWT
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        
        // Если предоставлен username, проверяем совпадение
        if (username && decoded.username !== username) {
            return res.status(403).json({ success: false, message: 'Токен не принадлежит указанному пользователю.' });
        }
        
        // Проверяем HWID, если предоставлен
        if (hwid && decoded.hwid && decoded.hwid !== hwid) {
            return res.status(403).json({ success: false, message: 'Токен не валиден для этого устройства.' });
        }
        
        return res.json({ 
            success: true, 
            message: 'Токен валиден.',
            username: decoded.username 
        });
    } catch (err) {
        // Если это не JWT токен, проверяем простой токен (для обратной совместимости)
        if (token && token.length > 10) {
            // Для простых токенов проверяем в БД, если предоставлен username
            if (username) {
                db.get('SELECT token FROM users WHERE username = ?', [username], (err, user) => {
                    if (err || !user) {
                        return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
                    }
                    // Здесь должна быть более сложная логика проверки токена
                    return res.json({ success: true, message: 'Токен валиден.' });
                });
                return;
            }
            return res.json({ success: true, message: 'Токен валиден.' });
        }
        
        return res.status(401).json({ success: false, message: 'Токен невалиден или истек.' });
    }
});

// --- Telegram 2FA Setup ---
app.post('/api/user/2fa/telegram/setup', (req, res) => {
    const { username } = req.body;
    
    if (!username || !bot) {
        return res.status(400).json({ success: false, message: 'Имя пользователя обязательно.' });
    }
    
    const linkCode = Math.random().toString(36).substring(2, 10).toUpperCase();
    const expiresAt = Date.now() + 10 * 60 * 1000;
    
    pendingTgLogins[linkCode.toLowerCase()] = {
        username: username,
        timestamp: expiresAt,
        type: '2fa_setup'
    };
    
    bot.getMe().then((botInfo) => {
        res.json({
            success: true,
            linkCode: linkCode,
            botUsername: botInfo.username,
            message: `Отправьте код "${linkCode}" боту @${botInfo.username} для привязки Telegram 2FA.`
        });
    }).catch((err) => {
        res.json({
            success: true,
            linkCode: linkCode,
            message: `Отправьте код "${linkCode}" боту для привязки Telegram 2FA.`
        });
    });
});

// Функция получения косметики
function getDefaultCosmetics() {
    return [
        {
            id: 'cap_blue',
            name: 'Синяя кепка',
            type: 'hat',
            description: 'Стильная синяя кепка',
            price: 100
        },
        {
            id: 'cape_red',
            name: 'Красный плащ',
            type: 'cape',
            description: 'Элегантный красный плащ',
            price: 200
        },
        {
            id: 'badge_vip',
            name: 'VIP Значок',
            type: 'badge',
            description: 'Эксклюзивный VIP значок',
            price: 500
        }
    ];
}

function getAvailableCosmetics() {
    return getDefaultCosmetics();
}

// --- Получить косметику пользователя ---
app.get('/api/user/cosmetics', (req, res) => {
    const { username } = req.query;
    
    if (!username) {
        return res.status(400).json({ success: false, message: 'Имя пользователя обязательно.' });
    }
    
    db.get('SELECT cosmetics FROM users WHERE username = ?', [username], (err, user) => {
        if (err || !user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        let cosmetics = [];
        if (user.cosmetics) {
            try {
                cosmetics = JSON.parse(user.cosmetics);
            } catch (e) {
                cosmetics = [];
            }
        }
        
        if (cosmetics.length === 0) {
            cosmetics = getDefaultCosmetics();
            db.run('UPDATE users SET cosmetics = ? WHERE username = ?', 
                [JSON.stringify(cosmetics), username], () => {});
        }
        
        res.json({ success: true, cosmetics: cosmetics });
    });
});

// --- Получить доступную косметику для магазина ---
app.get('/api/cosmetics/available', (req, res) => {
    res.json({ success: true, cosmetics: getAvailableCosmetics() });
});

// --- Покупка косметики ---
app.post('/api/user/currency/purchase', (req, res) => {
    const { username, cosmeticId, price } = req.body;
    
    if (!username || !cosmeticId || !price) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    db.get('SELECT currency, cosmetics FROM users WHERE username = ?', [username], (err, user) => {
        if (err || !user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        const balance = user.currency || 0;
        if (balance < price) {
            return res.status(400).json({ success: false, message: 'Недостаточно средств.' });
        }
        
        const cosmetic = getAvailableCosmetics().find(c => c.id === cosmeticId);
        if (!cosmetic) {
            return res.status(404).json({ success: false, message: 'Косметика не найдена.' });
        }
        
        let cosmetics = [];
        if (user.cosmetics) {
            try {
                cosmetics = JSON.parse(user.cosmetics);
            } catch (e) {
                cosmetics = [];
            }
        }
        
        if (cosmetics.find(c => c.id === cosmeticId)) {
            return res.status(409).json({ success: false, message: 'Косметика уже куплена.' });
        }
        
        const newBalance = balance - price;
        cosmetics.push(cosmetic);
        
        db.run('UPDATE users SET currency = ?, cosmetics = ? WHERE username = ?', 
            [newBalance, JSON.stringify(cosmetics), username], (err) => {
            if (err) {
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }
            res.json({ success: true, message: 'Косметика успешно куплена!', balance: newBalance });
        });
    });
});

// --- Получить баланс валюты ---
app.get('/api/user/currency', (req, res) => {
    const { username } = req.query;
    
    if (!username) {
        return res.status(400).json({ success: false, message: 'Имя пользователя обязательно.' });
    }
    
    db.get('SELECT currency FROM users WHERE username = ?', [username], (err, user) => {
        if (err || !user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        res.json({ success: true, balance: user.currency || 0 });
    });
});

// --- Генерация ссылки для пополнения баланса ---
app.post('/api/payment/generate', (req, res) => {
    const { username, amount } = req.body;

    if (!username || !amount || Number(amount) <= 0) {
        return res.status(400).json({ success: false, message: 'Имя пользователя и положительная сумма обязательны.' });
    }

    const normalizedAmount = Math.floor(Number(amount));
    const encodedPayload = encodeURIComponent(`topup_${username}_${normalizedAmount}`);

    const respondWithUrl = (baseUrl) => {
        const paymentUrl = `${baseUrl}${encodedPayload}`;
        return res.json({
            success: true,
            message: 'Ссылка для оплаты успешно создана.',
            paymentUrl
        });
    };

    // Приоритет: явная конфигурация из .env
    if (process.env.TELEGRAM_BOT_URL) {
        return respondWithUrl(process.env.TELEGRAM_BOT_URL);
    }

    // Фолбэк: генерируем ссылку по username бота, если бот инициализирован
    if (bot) {
        return bot.getMe()
            .then((botInfo) => {
                respondWithUrl(`https://t.me/${botInfo.username}?start=`);
            })
            .catch(() => {
                // Финальный фолбэк для локальной разработки
                respondWithUrl('https://t.me/your_bot?start=');
            });
    }

    // Финальный фолбэк для локальной разработки
    return respondWithUrl('https://t.me/your_bot?start=');
});

// ==================== АДМИНСКИЕ ЭНДПОИНТЫ ====================

// --- Выдать валюту (админ) ---
app.post('/api/admin/currency/give', verifyAdminToken, (req, res) => {
    const { username, amount } = req.body;
    
    if (!username || !amount || amount <= 0) {
        return res.status(400).json({ success: false, message: 'Имя пользователя и сумма обязательны.' });
    }
    
    db.get('SELECT currency FROM users WHERE username = ?', [username], (err, user) => {
        if (err || !user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        const newBalance = (user.currency || 0) + amount;
        
        db.run('UPDATE users SET currency = ? WHERE username = ?', [newBalance, username], (err) => {
            if (err) {
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }
            res.json({ success: true, message: `Выдано ${amount} валюты. Новый баланс: ${newBalance}`, balance: newBalance });
        });
    });
});

// --- Получить список пользователей (админ) ---
app.get('/api/admin/users', verifyAdminToken, (req, res) => {
    db.all(`SELECT id, username, role, currency, hwid, created_at 
            FROM users 
            ORDER BY created_at DESC`, [], (err, users) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, users: users || [] });
    });
});

// --- Забанить пользователя по HWID (админ) ---
app.post('/api/admin/users/ban', verifyAdminToken, (req, res) => {
    const { hwid, reason, username } = req.body;
    
    if (!hwid) {
        return res.status(400).json({ success: false, message: 'HWID обязателен.' });
    }
    
    db.run(`INSERT OR REPLACE INTO banned_hwid (hwid, reason, banned_by) 
            VALUES (?, ?, ?)`, 
            [hwid, reason || 'Не указана', username || 'Admin'], function(err) {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, message: `HWID ${hwid} добавлен в черный список.` });
    });
});

// --- Получить список банов (админ) ---
app.get('/api/admin/bans', verifyAdminToken, (req, res) => {
    db.all(`SELECT * FROM banned_hwid ORDER BY created_at DESC`, [], (err, bans) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, bans: bans || [] });
    });
});

// --- Разбанить пользователя по HWID (админ) ---
app.delete('/api/admin/users/unban/:hwid', verifyAdminToken, (req, res) => {
    const { hwid } = req.params;
    
    db.run('DELETE FROM banned_hwid WHERE hwid = ?', [hwid], function(err) {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        if (this.changes === 0) {
            return res.status(404).json({ success: false, message: 'HWID не найден в черном списке.' });
        }
        res.json({ success: true, message: `HWID ${hwid} удален из черного списка.` });
    });
});

// --- Загрузить косметику (админ) ---
app.post('/api/admin/cosmetics', verifyAdminToken, upload.fields([
    { name: 'model', maxCount: 1 },
    { name: 'texture', maxCount: 1 }
]), (req, res) => {
    try {
        const { name, description, pivot_point, price, rarity } = req.body;
        
        if (!name || !pivot_point) {
            if (req.files) {
                Object.values(req.files).forEach(files => {
                    files.forEach(file => {
                        if (fs.existsSync(file.path)) fs.unlinkSync(file.path);
                    });
                });
            }
            return res.status(400).json({ success: false, message: 'Имя и pivot_point обязательны.' });
        }
        
        if (!req.files || !req.files.model || !req.files.texture) {
            return res.status(400).json({ success: false, message: 'Модель и текстура обязательны.' });
        }
        
        const modelFile = req.files.model[0];
        const textureFile = req.files.texture[0];
        
        // Валидация JSON модели BlockBench
        try {
            const modelContent = fs.readFileSync(modelFile.path, 'utf8');
            const modelJson = JSON.parse(modelContent);
            
            if (!modelJson.format_version || !modelJson['minecraft:geometry']) {
                fs.unlinkSync(modelFile.path);
                fs.unlinkSync(textureFile.path);
                return res.status(400).json({ success: false, message: 'Неверный формат BlockBench модели.' });
            }
        } catch (err) {
            if (fs.existsSync(modelFile.path)) fs.unlinkSync(modelFile.path);
            if (fs.existsSync(textureFile.path)) fs.unlinkSync(textureFile.path);
            return res.status(400).json({ success: false, message: 'Ошибка парсинга JSON модели: ' + err.message });
        }
        
        const priceNum = parseInt(price) || 0;
        const rarityValue = rarity || 'common';
        
        db.run(`INSERT INTO cosmetics (name, description, pivot_point, price, rarity, model_file_path, texture_file_path) 
                VALUES (?, ?, ?, ?, ?, ?, ?)`, 
                [name, description || '', pivot_point, priceNum, rarityValue, modelFile.path, textureFile.path], 
                function(err) {
            if (err) {
                if (fs.existsSync(modelFile.path)) fs.unlinkSync(modelFile.path);
                if (fs.existsSync(textureFile.path)) fs.unlinkSync(textureFile.path);
                
                if (err.message.includes('UNIQUE')) {
                    return res.status(409).json({ success: false, message: 'Косметика с таким именем уже существует.' });
                }
                return res.status(500).json({ success: false, message: 'Ошибка сервера: ' + err.message });
            }
            
            res.json({ 
                success: true, 
                message: 'Косметика успешно добавлена!',
                cosmetic: {
                    id: this.lastID,
                    name: name,
                    pivot_point: pivot_point,
                    price: priceNum,
                    rarity: rarityValue
                }
            });
        });
    } catch (err) {
        return res.status(500).json({ success: false, message: 'Ошибка сервера: ' + err.message });
    }
});

// --- Получить список косметики (админ) ---
app.get('/api/admin/cosmetics', verifyAdminToken, (req, res) => {
    db.all(`SELECT id, name, description, pivot_point, price, rarity, is_active, created_at 
            FROM cosmetics 
            ORDER BY created_at DESC`, [], (err, cosmetics) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, cosmetics: cosmetics || [] });
    });
});

// --- Удалить косметику (админ) ---
app.delete('/api/admin/cosmetics/:id', verifyAdminToken, (req, res) => {
    const cosmeticId = Number.parseInt(req.params.id, 10);
    if (!Number.isInteger(cosmeticId) || cosmeticId <= 0) {
        return res.status(400).json({ success: false, message: 'Некорректный ID косметики.' });
    }

    db.get('SELECT model_file_path, texture_file_path FROM cosmetics WHERE id = ?', [cosmeticId], (selectErr, cosmetic) => {
        if (selectErr) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        if (!cosmetic) {
            return res.status(404).json({ success: false, message: 'Косметика не найдена.' });
        }

        db.run('DELETE FROM cosmetics WHERE id = ?', [cosmeticId], function(deleteErr) {
            if (deleteErr) {
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }

            const modelPath = cosmetic.model_file_path;
            const texturePath = cosmetic.texture_file_path;
            [modelPath, texturePath].forEach((filePath) => {
                if (filePath && fs.existsSync(filePath)) {
                    try {
                        fs.unlinkSync(filePath);
                    } catch (fileErr) {
                        logger.warn(`Не удалось удалить файл косметики: ${filePath}`);
                    }
                }
            });

            return res.json({ success: true, message: 'Косметика удалена.' });
        });
    });
});

// --- Создать новость (админ) ---
app.post('/api/admin/news', verifyAdminToken, (req, res) => {
    const { title, content, image_url, author } = req.body;
    
    if (!title || !content) {
        return res.status(400).json({ success: false, message: 'Заголовок и текст обязательны.' });
    }
    
    db.run(`INSERT INTO news (title, content, image_url, author) 
            VALUES (?, ?, ?, ?)`, 
            [title, content, image_url || null, author || 'Admin'], function(err) {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера: ' + err.message });
        }
        res.json({ success: true, message: 'Новость успешно создана!', news_id: this.lastID });
    });
});

// --- Получить список новостей (админ) ---
app.get('/api/admin/news', verifyAdminToken, (req, res) => {
    db.all(`SELECT * FROM news ORDER BY created_at DESC`, [], (err, news) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, news: news || [] });
    });
});

// --- Обновить новость (админ) ---
app.put('/api/admin/news/:id', verifyAdminToken, (req, res) => {
    const { id } = req.params;
    const { title, content, image_url } = req.body;
    
    if (!title || !content) {
        return res.status(400).json({ success: false, message: 'Заголовок и текст обязательны.' });
    }
    
    db.run(`UPDATE news SET title = ?, content = ?, image_url = ?, updated_at = CURRENT_TIMESTAMP 
            WHERE id = ?`, 
            [title, content, image_url || null, id], function(err) {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера: ' + err.message });
        }
        if (this.changes === 0) {
            return res.status(404).json({ success: false, message: 'Новость не найдена.' });
        }
        res.json({ success: true, message: 'Новость успешно обновлена!' });
    });
});

// --- Удалить новость (админ) ---
app.delete('/api/admin/news/:id', verifyAdminToken, (req, res) => {
    const { id } = req.params;
    
    db.run('DELETE FROM news WHERE id = ?', [id], function(err) {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера: ' + err.message });
        }
        if (this.changes === 0) {
            return res.status(404).json({ success: false, message: 'Новость не найдена.' });
        }
        res.json({ success: true, message: 'Новость успешно удалена!' });
    });
});

// ==================== ФОРУМ API ====================

app.get('/api/forum/categories', (req, res) => {
    db.all(`SELECT c.*, COUNT(t.id) as topics_count 
            FROM forum_categories c 
            LEFT JOIN forum_topics t ON c.id = t.category_id 
            GROUP BY c.id 
            ORDER BY c.order_index`, [], (err, categories) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, categories: categories || [] });
    });
});

app.get('/api/forum/topics', (req, res) => {
    const { category_id } = req.query;
    
    if (!category_id) {
        return res.status(400).json({ success: false, message: 'ID категории обязателен.' });
    }
    
    db.all(`SELECT t.*, COALESCE(u.role, 'player') as author_role
            FROM forum_topics t
            LEFT JOIN users u ON u.username = t.author_username
            WHERE t.category_id = ? 
            ORDER BY t.is_pinned DESC, t.last_reply_at DESC, t.created_at DESC`, 
            [category_id], (err, topics) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, topics: topics || [] });
    });
});

app.post('/api/forum/topics', (req, res) => {
    const { category_id, title, content, author_username } = req.body;
    
    if (!category_id || !title || !content || !author_username) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    db.run(`INSERT INTO forum_topics (category_id, author_username, title, content) 
            VALUES (?, ?, ?, ?)`, 
            [category_id, author_username, title, content], function(err) {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, topic_id: this.lastID, message: 'Тема создана!' });
    });
});

app.get('/api/forum/posts', (req, res) => {
    const { topic_id, username } = req.query;
    
    if (!topic_id) {
        return res.status(400).json({ success: false, message: 'ID темы обязателен.' });
    }
    
    db.run('UPDATE forum_topics SET views = views + 1 WHERE id = ?', [topic_id]);
    
    db.all(`SELECT p.*, COALESCE(u.role, 'player') as author_role,
            (SELECT COUNT(*) FROM forum_likes WHERE post_id = p.id) as likes_count,
            ${username ? `(SELECT COUNT(*) FROM forum_likes WHERE post_id = p.id AND username = ?) > 0 as is_liked` : '0 as is_liked'}
            FROM forum_posts p 
            LEFT JOIN users u ON u.username = p.author_username
            WHERE p.topic_id = ? 
            ORDER BY p.created_at ASC`, 
            username ? [username, topic_id] : [topic_id], (err, posts) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, posts: posts || [] });
    });
});

app.post('/api/forum/posts', (req, res) => {
    const { topic_id, content, author_username } = req.body;
    
    if (!topic_id || !content || !author_username) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    db.run(`INSERT INTO forum_posts (topic_id, author_username, content) 
            VALUES (?, ?, ?)`, 
            [topic_id, author_username, content], function(err) {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        db.run(`UPDATE forum_topics 
                SET replies_count = replies_count + 1, 
                    last_reply_at = CURRENT_TIMESTAMP 
                WHERE id = ?`, [topic_id]);
        
        res.json({ success: true, post_id: this.lastID, message: 'Сообщение добавлено!' });
    });
});

app.post('/api/forum/posts/like', (req, res) => {
    const { post_id, username } = req.body;
    
    if (!post_id || !username) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    db.get('SELECT id FROM forum_likes WHERE post_id = ? AND username = ?', 
            [post_id, username], (err, like) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (like) {
            db.run('DELETE FROM forum_likes WHERE post_id = ? AND username = ?', 
                    [post_id, username], (err) => {
                if (err) {
                    return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                }
                res.json({ success: true, liked: false, message: 'Лайк убран' });
            });
        } else {
            db.run('INSERT INTO forum_likes (post_id, username) VALUES (?, ?)', 
                    [post_id, username], (err) => {
                if (err) {
                    return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                }
                res.json({ success: true, liked: true, message: 'Лайк добавлен' });
            });
        }
    });
});

// --- Централизованная обработка ошибок ---
// --- Обработка ошибок ---
app.use(errorHandler);

// --- Обработка 404 ---
app.use((req, res) => {
    res.status(404).json({ 
        success: false, 
        message: 'Эндпоинт не найден.' 
    });
});

// --- Запуск сервера ---
app.listen(PORT, () => {
    logger.info(`API сервер запущен: http://localhost:${PORT}`);
    logger.info(`База данных: ${DB_PATH}`);
    logger.info(`Загрузки: ${UPLOADS_DIR}`);
    if (bot) {
        logger.info(`Telegram Bot: активен`);
    } else {
        if (!TELEGRAM_SILENT_MODE) {
            logger.warn(`Telegram Bot: не настроен (добавьте TELEGRAM_BOT_TOKEN в .env)`);
        }
    }
});
