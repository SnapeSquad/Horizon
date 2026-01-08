// api-server/server.js
require('dotenv').config(); 

const express = require('express');
const TelegramBot = require('node-telegram-bot-api');
const fs = require('fs');
const path = require('path');
const bcrypt = require('bcrypt');
const sqlite3 = require('sqlite3').verbose();
// Google Authenticator удален - используется только Telegram 2FA
// --- ИМПОРТ БИБЛИОТЕКИ MINECRAFT ---
const mcu = require('minecraft-server-util');
const cors = require('cors');
// -----------------------------------
const app = express();
const PORT = 3000; 
const DB_PATH = path.join(__dirname, 'users.db');

// --- Инициализация SQLite БД ---
const db = new sqlite3.Database(DB_PATH, (err) => {
    if (err) {
        console.error('❌ Ошибка подключения к БД:', err.message);
    } else {
        console.log('✅ Подключено к SQLite БД');
        // Создаем таблицы
        db.serialize(() => {
            db.run(`CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                telegram_chat_id TEXT,
                two_factor_enabled INTEGER DEFAULT 0,
                cosmetics TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            // Добавляем колонку cosmetics, если её нет
            db.run(`ALTER TABLE users ADD COLUMN cosmetics TEXT`, (err) => {
                if (err && !err.message.includes('duplicate column')) {
                    console.warn('⚠️ Не удалось добавить колонку cosmetics:', err.message);
                }
            });
            
            // Добавляем колонку skin, если её нет
            db.run(`ALTER TABLE users ADD COLUMN skin TEXT`, (err) => {
                if (err && !err.message.includes('duplicate column')) {
                    console.warn('⚠️ Не удалось добавить колонку skin:', err.message);
                }
            });
            
            // Добавляем колонку currency (донат-валюта), если её нет
            db.run(`ALTER TABLE users ADD COLUMN currency INTEGER DEFAULT 0`, (err) => {
                if (err && !err.message.includes('duplicate column')) {
                    console.warn('⚠️ Не удалось добавить колонку currency:', err.message);
                }
            });
            
            // Добавляем колонку skin_model (модель скина: classic/slim), если её нет
            db.run(`ALTER TABLE users ADD COLUMN skin_model TEXT DEFAULT 'classic'`, (err) => {
                if (err && !err.message.includes('duplicate column')) {
                    console.warn('⚠️ Не удалось добавить колонку skin_model:', err.message);
                }
            });
            
            // Добавляем колонку cape (плащ), если её нет
            db.run(`ALTER TABLE users ADD COLUMN cape TEXT`, (err) => {
                if (err && !err.message.includes('duplicate column')) {
                    console.warn('⚠️ Не удалось добавить колонку cape:', err.message);
                }
            });
            
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
            
            // Таблица новостей
            db.run(`CREATE TABLE IF NOT EXISTS news (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                content TEXT NOT NULL,
                image_url TEXT,
                author_username TEXT NOT NULL,
                category TEXT DEFAULT 'general',
                is_published INTEGER DEFAULT 1,
                views INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            // Таблица товаров магазина
            db.run(`CREATE TABLE IF NOT EXISTS shop_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                price INTEGER NOT NULL,
                discount INTEGER DEFAULT 0,
                category TEXT DEFAULT 'cosmetics',
                rarity TEXT DEFAULT 'common',
                image_url TEXT,
                is_available INTEGER DEFAULT 1,
                stock INTEGER DEFAULT -1,
                sales_count INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            // Таблица покупок
            db.run(`CREATE TABLE IF NOT EXISTS purchases (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                item_id INTEGER NOT NULL,
                price_paid INTEGER NOT NULL,
                payment_method TEXT DEFAULT 'currency',
                status TEXT DEFAULT 'completed',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            // Таблица транзакций валюты
            db.run(`CREATE TABLE IF NOT EXISTS currency_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                amount INTEGER NOT NULL,
                transaction_type TEXT NOT NULL,
                description TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
            // Инициализируем категории форума
            db.get('SELECT COUNT(*) as count FROM forum_categories', (err, row) => {
                if (!err && row.count === 0) {
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
            
            // Инициализируем новости
            db.get('SELECT COUNT(*) as count FROM news', (err, row) => {
                if (!err && row.count === 0) {
                    const newsItems = [
                        [
                            'Глобальное обновление 2.0!',
                            'Встречайте грандиозное обновление с новыми режимами игры, уникальными предметами и захватывающими квестами!',
                            'Приветствуем всех игроков! Мы рады объявить о выходе глобального обновления 2.0, которое принесет множество изменений и улучшений в игру. В этом обновлении мы сосредоточились на оптимизации производительности, добавлении нового контента и улучшении пользовательского опыта.',
                            'https://images.unsplash.com/photo-1631499792544-3c313e2a2511?w=1080',
                            'Admin',
                            'update'
                        ],
                        [
                            'Зимний турнир: Битва легенд',
                            'Участвуйте в зимнем турнире PvP и получайте эксклюзивные награды! Призовой фонд 100,000 монет.',
                            'Уважаемые игроки! Рады сообщить о начале зимнего турнира PvP. Соревнуйтесь с другими игроками за звание чемпиона и получайте ценные награды!',
                            'https://images.unsplash.com/photo-1676912002444-6a54ce34f5a3?w=1080',
                            'Admin',
                            'event'
                        ],
                        [
                            'Новая коллекция премиум скинов',
                            'Проверьте новую коллекцию премиум скинов в нашем магазине! Скидка 30% в первую неделю.',
                            'Встречайте новую коллекцию эксклюзивных скинов! Более 50 уникальных дизайнов уже доступны в магазине.',
                            'https://images.unsplash.com/photo-1663010363660-d75c1c69958b?w=1080',
                            'Admin',
                            'shop'
                        ]
                    ];
                    
                    const stmt = db.prepare('INSERT INTO news (title, description, content, image_url, author_username, category) VALUES (?, ?, ?, ?, ?, ?)');
                    newsItems.forEach(news => stmt.run(...news));
                    stmt.finalize();
                    console.log('✅ Инициализировано 3 новости');
                }
            });
            
            // Инициализируем товары магазина
            db.get('SELECT COUNT(*) as count FROM shop_items', (err, row) => {
                if (!err && row.count === 0) {
                    const shopItems = [
                        ['Элитный скин "Небесный воин"', 'Эксклюзивный скин с анимированными частицами', 2500, 20, 'skins', 'legendary', null, 1, 10],
                        ['Крылья дракона', 'Огненные крылья с уникальной анимацией', 3000, 0, 'cosmetics', 'legendary', null, 1, 5],
                        ['Светящаяся корона', 'Корона с эффектом свечения', 1500, 10, 'cosmetics', 'epic', null, 1, 20],
                        ['Питомец: Мини-дракон', 'Верный спутник в приключениях', 2000, 0, 'pets', 'epic', null, 1, 15],
                        ['Набор "Легенда"', 'Полный набор легендарной брони', 5000, 30, 'bundles', 'legendary', null, 1, 3],
                        ['Эффект частиц "Звезды"', 'Окружает персонажа звездами', 1000, 0, 'effects', 'rare', null, 1, 50],
                        ['Плащ "Ночной страж"', 'Темный плащ с эффектом тени', 1800, 15, 'cosmetics', 'epic', null, 1, 12],
                        ['Скин "Киберпанк 2077"', 'Футуристический скин', 2200, 0, 'skins', 'epic', null, 1, 8]
                    ];
                    
                    const stmt = db.prepare('INSERT INTO shop_items (name, description, price, discount, category, rarity, image_url, is_available, stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)');
                    shopItems.forEach(item => stmt.run(...item));
                    stmt.finalize();
                    console.log('✅ Инициализировано 8 товаров магазина');
                }
            });
            
            // Миграция данных из JSON, если файл существует
            const USERS_JSON_PATH = path.join(__dirname, 'users.json');
            if (fs.existsSync(USERS_JSON_PATH)) {
                try {
                    const jsonData = fs.readFileSync(USERS_JSON_PATH, 'utf8');
                    const users = JSON.parse(jsonData);
                    const stmt = db.prepare('INSERT OR IGNORE INTO users (username, password) VALUES (?, ?)');
                    users.forEach(user => {
                        stmt.run(user.username, user.password);
                    });
                    stmt.finalize();
                    console.log(`✅ Мигрировано ${users.length} пользователей из JSON в SQLite`);
                } catch (err) {
                    console.warn('⚠️ Не удалось мигрировать данные из JSON:', err.message);
                }
            }
        });
    }
});

// --- Инициализация Telegram ---
const BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;

if (!BOT_TOKEN || BOT_TOKEN === 'ВАШ_СЕКРЕТНЫЙ_ТОКЕН_БОТА') {
    console.error("❌ ОШИБКА: Замените 'ВАШ_СЕКРЕТНЫЙ_ТОКЕН_БОТА' на реальный токен в файле .env!");
}

const bot = BOT_TOKEN && BOT_TOKEN !== 'ВАШ_СЕКРЕТНЫЙ_ТОКЕН_БОТА' 
    ? new TelegramBot(BOT_TOKEN, { polling: true })
    : null;

// --- Временное хранилище для ожидания входа ---
// Ключ: Сгенерированный код (например, 't4y8z')
// Значение: { username: Никнейм, timestamp: время истечения }
const pendingTgLogins = {};
// Ключ: Chat ID пользователя
// Значение: { username: Никнейм, chatId: Chat ID, tgUsername: @ник } (после верификации ботом)
const pendingTgLoginsByChatId = {};


// --- ЛОГИКА ОБРАБОТКИ СООБЩЕНИЙ ОТ TELEGRAM ---
if (bot) {
    bot.on('message', (msg) => {
        const chatId = msg.chat.id;
        const text = msg.text ? msg.text.trim() : '';
        const textLower = text.toLowerCase();

        console.log(`[TG MSG] Received from ${chatId}: ${text}`);

        // 1. Проверяем, является ли сообщение кодом для входа или привязки 2FA
        // Проверяем как в нижнем, так и в верхнем регистре
        const codeKey = textLower;
        const codeKeyUpper = text.toUpperCase();
        const codeToCheck = pendingTgLogins[codeKey] || pendingTgLogins[codeKeyUpper];
        
        if (codeToCheck) {
            const loginData = codeToCheck;
            const actualCodeKey = pendingTgLogins[codeKey] ? codeKey : codeKeyUpper;
            
            // Проверка на срок действия кода
            if (Date.now() > loginData.timestamp) {
                delete pendingTgLogins[codeKey];
                delete pendingTgLogins[codeKeyUpper];
                bot.sendMessage(chatId, `❌ Срок действия кода истек. Попробуйте снова.`, { parse_mode: 'Markdown' });
                return;
            }

            // Если это привязка 2FA
            if (loginData.type === '2fa_setup') {
                // Сохраняем chat_id в БД
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
                        `✅ Telegram 2FA успешно привязана для аккаунта *${loginData.username}*!\n\n` +
                        `Теперь при каждом входе вы будете получать код подтверждения через этот бот.`,
                        { parse_mode: 'Markdown' }
                    );
                    console.log(`[TG 2FA] Telegram привязан для ${loginData.username}, chat_id: ${chatId}`);
                });
                return;
            }

            // Если это код для входа (login_2fa)
            if (loginData.type === 'login_2fa') {
                const textTrimmed = String(text).trim().replace(/\s/g, '');
                const codeMatch = String(loginData.code).trim() === textTrimmed;
                const notExpired = Date.now() <= loginData.timestamp;
                
                console.log(`[TG LOGIN] Проверка кода через бота: введен="${textTrimmed}", ожидается="${loginData.code}", match=${codeMatch}, expired=${!notExpired}`);
                
                // Проверяем, что код соответствует и не истек
                if (notExpired && codeMatch) {
                    // Код верный, но вход происходит через API, не через бота
                    // Просто подтверждаем пользователю
                    bot.sendMessage(chatId,
                        `✅ Код подтвержден! Вы можете войти в лаунчер.`,
                        { parse_mode: 'Markdown' }
                    );
                    console.log(`[TG LOGIN] Код подтвержден для ${loginData.username} через бота`);
                    
                    // Удаляем код
                    delete pendingTgLogins[textTrimmed];
                    if (pendingTgLoginsByChatId[chatId]) {
                        delete pendingTgLoginsByChatId[chatId][textTrimmed];
                    }
                } else {
                    bot.sendMessage(chatId, `❌ Код неверный или истек.`, { parse_mode: 'Markdown' });
                }
                return;
            }

            // Верификация успешна (обычный вход - старый формат)
            loginData.chatId = chatId;
            loginData.tgUsername = msg.from.username; 
            
            // Удаляем старую запись и добавляем в обратный индекс
            delete pendingTgLogins[codeKey];
            delete pendingTgLogins[codeKeyUpper];
            pendingTgLoginsByChatId[chatId] = loginData;

            bot.sendMessage(chatId, 
                `✅ Аккаунт *${loginData.username}* верифицирован! Вы можете вернуться в лаунчер.`,
                { parse_mode: 'Markdown' }
            );
            
            console.log(`[TG AUTH] Код ${text} успешно верифицирован для ${loginData.username}`);
            return;
        }

        // 2. Команды бота
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
            bot.sendMessage(chatId, 
                `❓ Неизвестная команда.\n\n` +
                `Отправьте код из лаунчера или используйте /help для справки.`,
                { parse_mode: 'Markdown' }
            );
        }
    });

    // Обработка ошибок Polling
    bot.on('polling_error', (error) => {
        console.error('[TG BOT] Polling error:', error);
    });

    console.log(`🤖 Telegram Bot запущен и слушает входящие сообщения (Polling).`);
} else {
    console.warn(`⚠️ Telegram Bot не запущен, так как не найден токен в .env.`);
}


app.use(express.json());
app.use(cors());

app.get('/', (req, res) => {
    res.send('API Server запущен и ждет запросов от лаунчера!');
});


// --- 1. Маршрут: Статус сервера Minecraft ---
app.get('/api/server/status', async (req, res) => {
    // !!! ИЗМЕНИТЕ ЭТИ ПАРАМЕТРЫ НА ВАШИ !!!
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
            players: {
                online: 0,
                max: 0
            }
        });
    }
});

// --- 2. Маршрут: Регистрация нового пользователя ---
app.post('/api/auth/register', (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({ success: false, message: 'Логин и пароль обязательны.' });
    }

    // Проверяем, существует ли пользователь
    db.get('SELECT id FROM users WHERE username = ?', [username], (err, row) => {
        if (err) {
            console.error('[REGISTER] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера при проверке пользователя.' });
        }

        if (row) {
            return res.status(409).json({ success: false, message: 'Пользователь с таким именем уже существует.' });
        }

        // Хэшируем пароль и сохраняем в БД
        bcrypt.hash(password, 10, (err, hashedPassword) => {
            if (err) {
                console.error('[REGISTER] Ошибка хэширования пароля:', err);
                return res.status(500).json({ success: false, message: 'Ошибка сервера при регистрации.' });
            }

            db.run('INSERT INTO users (username, password) VALUES (?, ?)', [username, hashedPassword], function(err) {
                if (err) {
                    console.error('[REGISTER] Ошибка сохранения в БД:', err);
                    return res.status(500).json({ success: false, message: 'Ошибка сервера при сохранении пользователя.' });
                }
                console.log(`[REGISTER] Пользователь ${username} успешно зарегистрирован.`);
                res.json({ success: true, message: 'Регистрация прошла успешно!' });
            });
        });
    });
});


// --- 3. Маршрут: Вход по логину/паролю ---
app.post('/api/auth/login', (req, res) => {
    let { username, password, twoFactorCode } = req.body;
    
    // Нормализуем код 2FA: убираем все пробелы и не-цифры, оставляем только цифры
    if (twoFactorCode) {
        twoFactorCode = String(twoFactorCode).trim().replace(/\D/g, '');
        console.log(`[LOGIN] Получен код 2FA после нормализации: "${twoFactorCode}" (длина: ${twoFactorCode.length})`);
    }

    if (!username || !password) {
        return res.status(400).json({ success: false, message: 'Логин и пароль обязательны.' });
    }

    db.get('SELECT * FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[LOGIN] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }

        if (!user) {
            return res.status(401).json({ success: false, message: 'Неверный логин или пароль.' });
        }

        bcrypt.compare(password, user.password, (err, isMatch) => {
            if (err) {
                console.error('[LOGIN] Ошибка сравнения пароля:', err);
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }

            if (!isMatch) {
                return res.status(401).json({ success: false, message: 'Неверный логин или пароль.' });
            }

            // Проверяем, включена ли 2FA
            if (user.two_factor_enabled) {
                // Если 2FA включена, проверяем код
                if (!twoFactorCode) {
                    // Если это Telegram 2FA, отправляем код через бота
                    if (user.telegram_chat_id && bot) {
                        // Удаляем старые коды для этого пользователя (если есть)
                        const chatIdStr = String(user.telegram_chat_id);
                        const oldCodes = Object.keys(pendingTgLogins).filter(k => {
                            const data = pendingTgLogins[k];
                            return data?.type === 'login_2fa' && data?.username === user.username;
                        });
                        oldCodes.forEach(key => {
                            delete pendingTgLogins[key];
                            if (pendingTgLoginsByChatId[chatIdStr]) {
                                delete pendingTgLoginsByChatId[chatIdStr][key];
                            }
                        });
                        if (oldCodes.length > 0) {
                            console.log(`[LOGIN 2FA] Удалены старые коды для ${user.username}:`, oldCodes);
                        }
                        
                        // Генерируем 6-значный код
                        const loginCode = String(Math.floor(100000 + Math.random() * 900000));
                        const expiresAt = Date.now() + 5 * 60 * 1000; // 5 минут
                        
                        // Убеждаемся, что код - это строка из 6 цифр без пробелов
                        const loginCodeClean = loginCode.trim().replace(/\D/g, ''); // Убираем все не-цифры
                        
                        if (loginCodeClean.length !== 6) {
                            console.error(`[LOGIN 2FA] ОШИБКА: Сгенерирован некорректный код "${loginCodeClean}" для ${user.username}`);
                            return res.status(500).json({ success: false, message: 'Ошибка генерации кода. Попробуйте снова.' });
                        }
                        
                        const loginData = {
                            username: user.username,
                            code: loginCodeClean,
                            timestamp: expiresAt,
                            type: 'login_2fa',
                            chatId: chatIdStr
                        };
                        
                        // Сохраняем код с ключом, который является самим кодом (строго строка)
                        pendingTgLogins[loginCodeClean] = loginData;
                        
                        // Также сохраняем в обратном индексе по chatId для быстрого поиска
                        if (!pendingTgLoginsByChatId[chatIdStr]) {
                            pendingTgLoginsByChatId[chatIdStr] = {};
                        }
                        pendingTgLoginsByChatId[chatIdStr][loginCodeClean] = loginData;
                        
                        console.log(`[LOGIN 2FA] ✅ Сгенерирован код для ${user.username}: "${loginCodeClean}"`);
                        console.log(`[LOGIN 2FA] Сохранен в pendingTgLogins["${loginCodeClean}"] =`, JSON.stringify(loginData));
                        console.log(`[LOGIN 2FA] Сохранен в обратном индексе: pendingTgLoginsByChatId["${chatIdStr}"]["${loginCodeClean}"]`);
                        console.log(`[LOGIN 2FA] Все активные коды login_2fa:`, Object.keys(pendingTgLogins).filter(k => pendingTgLogins[k]?.type === 'login_2fa'));
                        
                        // Отправляем код через Telegram - используем тот же код, что и сохранили
                        bot.sendMessage(user.telegram_chat_id,
                            `🔐 Код для входа в аккаунт *${user.username}*:\n\n` +
                            `*${loginCodeClean}*\n\n` +
                            `Код действителен 5 минут.\n\n` +
                            `Введите этот код в лаунчере для завершения входа.`,
                            { parse_mode: 'Markdown' }
                        ).then(() => {
                            console.log(`[LOGIN 2FA] ✅ Код "${loginCodeClean}" успешно отправлен в Telegram для ${user.username} (chat_id: ${user.telegram_chat_id})`);
                        }).catch(err => {
                            console.error('[LOGIN 2FA] ❌ Ошибка отправки кода в Telegram:', err);
                        });
                    }
                    
                    return res.status(200).json({ 
                        success: false, 
                        requires2FA: true,
                        message: user.telegram_chat_id 
                            ? 'Код отправлен в Telegram. Введите его для входа.' 
                            : 'Требуется код двухфакторной аутентификации.' 
                    });
                }

                // Проверяем код в зависимости от типа 2FA
                // ПРИОРИТЕТ: Telegram 2FA (если есть telegram_chat_id, используем ТОЛЬКО его, игнорируя Google)
                let codeValid = false;
                console.log(`[LOGIN 2FA CHECK] Начало проверки Telegram 2FA для ${user.username}: telegram_chat_id=${user.telegram_chat_id}, twoFactorCode=${twoFactorCode ? `"${twoFactorCode}"` : 'null'}`);
                
                if (user.telegram_chat_id) {
                    // Проверка Telegram 2FA
                    console.log(`[LOGIN 2FA CHECK] ✅ Используем Telegram 2FA для ${user.username}`);
                    // Проверка кода из Telegram - ищем по самому коду
                    // Нормализуем код: убираем все пробелы и не-цифры, оставляем только цифры
                    const codeKey = String(twoFactorCode || '').trim().replace(/\D/g, '');
                    
                    if (!codeKey || codeKey.length === 0) {
                        console.log(`[LOGIN 2FA VERIFY] ❌ Пустой или некорректный код для ${user.username}`);
                        return res.status(401).json({ success: false, message: 'Код двухфакторной аутентификации обязателен.' });
                    }
                    
                    if (codeKey.length !== 6) {
                        console.log(`[LOGIN 2FA VERIFY] ❌ Неверная длина кода для ${user.username}: "${codeKey}" (длина: ${codeKey.length}, ожидается: 6)`);
                        return res.status(401).json({ success: false, message: 'Код должен состоять из 6 цифр.' });
                    }
                    
                    console.log(`[LOGIN 2FA VERIFY] 🔍 Проверка кода для ${user.username}: введен "${codeKey}" (тип: ${typeof codeKey}, длина: ${codeKey.length})`);
                    
                    // Получаем все активные коды для отладки
                    const allActiveCodes = Object.keys(pendingTgLogins)
                        .filter(k => pendingTgLogins[k]?.type === 'login_2fa')
                        .map(k => ({ 
                            key: k, 
                            keyType: typeof k,
                            keyLength: k.length,
                            username: pendingTgLogins[k].username, 
                            code: pendingTgLogins[k].code,
                            codeType: typeof pendingTgLogins[k].code,
                            codeLength: String(pendingTgLogins[k].code).length
                        }));
                    console.log(`[LOGIN 2FA VERIFY] Все активные коды login_2fa (${allActiveCodes.length} шт.):`, JSON.stringify(allActiveCodes, null, 2));
                    
                    // Ищем код напрямую в основном индексе
                    console.log(`[LOGIN 2FA VERIFY] 🔎 Поиск кода "${codeKey}" в pendingTgLogins...`);
                    console.log(`[LOGIN 2FA VERIFY] Все ключи в pendingTgLogins (${Object.keys(pendingTgLogins).length} шт.):`, Object.keys(pendingTgLogins));
                    
                    // Пробуем найти код разными способами
                    let loginData = null;
                    
                    // Способ 1: Прямой поиск по ключу
                    if (pendingTgLogins[codeKey]) {
                        loginData = pendingTgLogins[codeKey];
                        console.log(`[LOGIN 2FA VERIFY] ✅ Код найден прямым поиском: pendingTgLogins["${codeKey}"]`);
                    }
                    
                    // Способ 2: Поиск в обратном индексе
                    if (!loginData) {
                        const chatIdStr = String(user.telegram_chat_id);
                        if (pendingTgLoginsByChatId[chatIdStr] && pendingTgLoginsByChatId[chatIdStr][codeKey]) {
                            loginData = pendingTgLoginsByChatId[chatIdStr][codeKey];
                            console.log(`[LOGIN 2FA VERIFY] ✅ Код найден в обратном индексе: pendingTgLoginsByChatId["${chatIdStr}"]["${codeKey}"]`);
                        }
                    }
                    
                    // Способ 3: Поиск по username среди всех кодов login_2fa
                    if (!loginData) {
                        console.log(`[LOGIN 2FA VERIFY] 🔎 Поиск по username среди всех кодов...`);
                        for (const key in pendingTgLogins) {
                            const data = pendingTgLogins[key];
                            if (data && data.type === 'login_2fa' && data.username === user.username) {
                                console.log(`[LOGIN 2FA VERIFY] Найден код для пользователя: ключ="${key}", код="${data.code}", введен="${codeKey}"`);
                                if (String(data.code).trim() === codeKey) {
                                    loginData = data;
                                    console.log(`[LOGIN 2FA VERIFY] ✅ Код найден по username: ключ="${key}", код="${data.code}"`);
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (!loginData) {
                        console.log(`[LOGIN 2FA VERIFY] ❌ Код "${codeKey}" не найден ни одним способом`);
                        console.log(`[LOGIN 2FA VERIFY] Все коды login_2fa:`, Object.keys(pendingTgLogins)
                            .filter(k => pendingTgLogins[k]?.type === 'login_2fa')
                            .map(k => ({ key: k, username: pendingTgLogins[k].username, code: pendingTgLogins[k].code })));
                    }
                    
                    if (loginData && loginData.type === 'login_2fa') {
                        console.log(`[LOGIN 2FA VERIFY] 📋 Найдена запись: username="${loginData.username}", code="${loginData.code}" (тип: ${typeof loginData.code}), timestamp=${new Date(loginData.timestamp).toISOString()}, now=${new Date().toISOString()}`);
                        
                        // Проверяем, что код соответствует пользователю и не истек
                        const usernameMatch = loginData.username === user.username;
                        const notExpired = Date.now() <= loginData.timestamp;
                        const codeMatch = String(loginData.code).trim() === codeKey;
                        
                        console.log(`[LOGIN 2FA VERIFY] 🔐 Проверки: usernameMatch=${usernameMatch}, notExpired=${notExpired}, codeMatch=${codeMatch}`);
                        console.log(`[LOGIN 2FA VERIFY] 📊 Детали сравнения: loginData.code="${loginData.code}" (${typeof loginData.code}, длина: ${String(loginData.code).length}), codeKey="${codeKey}" (${typeof codeKey}, длина: ${codeKey.length})`);
                        
                        if (usernameMatch && notExpired && codeMatch) {
                            codeValid = true;
                            // Удаляем код из всех индексов ПОСЛЕ успешной проверки
                            // Но НЕ удаляем сразу, чтобы избежать проблем с повторными запросами
                            // Удалим код только после успешного входа
                            console.log(`[LOGIN 2FA VERIFY] ✅✅✅ КОД ПОДТВЕРЖДЕН для ${user.username}`);
                            console.log(`[LOGIN 2FA VERIFY] Код будет удален после успешного входа`);
                        } else {
                            console.log(`[LOGIN 2FA VERIFY] ❌ Код не прошел проверку: usernameMatch=${usernameMatch}, notExpired=${notExpired} (${Date.now()} <= ${loginData.timestamp}), codeMatch=${codeMatch}`);
                        }
                    } else {
                        console.log(`[LOGIN 2FA VERIFY] ❌ Запись не найдена. Искали ключ: "${codeKey}" (тип: ${typeof codeKey}, длина: ${codeKey.length})`);
                        console.log(`[LOGIN 2FA VERIFY] Доступные ключи в pendingTgLogins:`, Object.keys(pendingTgLogins).slice(0, 10));
                    }
                } else {
                    // Если нет Telegram 2FA, но есть 2FA включена - ошибка
                    console.log(`[LOGIN 2FA CHECK] ❌ У пользователя ${user.username} включена 2FA, но нет настроенного Telegram`);
                    return res.status(401).json({ success: false, message: '2FA настроена некорректно. Обратитесь к администратору.' });
                }

                if (!codeValid) {
                    console.log(`[LOGIN 2FA CHECK] ❌ Код не прошел проверку для ${user.username}`);
                    return res.status(401).json({ success: false, message: 'Неверный код двухфакторной аутентификации.' });
                }
                
                console.log(`[LOGIN 2FA CHECK] ✅ Код подтвержден для ${user.username}`);
                
                // Удаляем код ТОЛЬКО после успешной проверки и перед отправкой ответа
                if (codeValid && twoFactorCode) {
                    const codeKey = String(twoFactorCode).trim().replace(/\D/g, '');
                    const chatIdStr = String(user.telegram_chat_id);
                    delete pendingTgLogins[codeKey];
                    if (pendingTgLoginsByChatId[chatIdStr]) {
                        delete pendingTgLoginsByChatId[chatIdStr][codeKey];
                    }
                    console.log(`[LOGIN 2FA] Код "${codeKey}" удален после успешной проверки`);
                }
            }

            console.log(`[LOGIN] Пользователь ${username} успешно вошел в систему.`);
            res.json({ 
                success: true, 
                token: `fake-auth-token-${Date.now()}`, 
                username: user.username,
                has2FA: user.two_factor_enabled === 1
            });
        });
    });
});


// --- 3. Маршрут: Запрос на начало входа (Генерация кода для Telegram) ---
app.post('/api/auth/tg_init', (req, res) => {
    const { username } = req.body; 

    if (!bot) {
         return res.status(503).json({ success: false, message: "Сервер Telegram API не запущен." });
    }
    
    const authCode = Math.random().toString(36).substring(2, 7); 
    
    pendingTgLogins[authCode] = {
        username: username,
        timestamp: Date.now() + 5 * 60 * 1000 // Код истекает через 5 минут
    };
    
    return res.json({ 
        success: true, 
        authCode: authCode,
        message: `Отправьте код "${authCode.toUpperCase()}" нашему Telegram боту.` 
    });
});


// --- 4. Маршрут: Опрос статуса входа (Polling) ---
app.post('/api/auth/poll_login', (req, res) => {
    const { username } = req.body;

    const verifiedEntry = Object.values(pendingTgLoginsByChatId).find(entry => 
        entry.username === username 
    );
    
    if (verifiedEntry) {
        const chatId = verifiedEntry.chatId;
        
        // Удаляем временную запись, чтобы код нельзя было использовать повторно
        delete pendingTgLoginsByChatId[chatId]; 

        const authToken = `REAL-TOKEN-TG-${chatId}-${Date.now()}`;
        
        return res.json({
            success: true,
            token: authToken,
            username: username
        });
    }

    return res.status(202).json({ 
        success: false, 
        message: 'Ожидание подтверждения от Telegram...',
        status: 'pending' 
    });
});


// --- 5. Маршрут: Получить статус 2FA пользователя ---
app.get('/api/user/2fa/status', (req, res) => {
    const { username } = req.query;
    
    if (!username) {
        return res.status(400).json({ success: false, message: 'Имя пользователя обязательно.' });
    }
    
    db.get('SELECT two_factor_enabled, telegram_chat_id FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[2FA STATUS] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        res.json({
            success: true,
            enabled: user.two_factor_enabled === 1,
            hasTelegram: !!user.telegram_chat_id
        });
    });
});

// Google Authenticator endpoints удалены - используется только Telegram 2FA

// --- 8. Маршрут: Настроить Telegram 2FA ---
app.post('/api/user/2fa/telegram/setup', (req, res) => {
    const { username } = req.body;
    
    if (!username) {
        return res.status(400).json({ success: false, message: 'Имя пользователя обязательно.' });
    }
    
    if (!bot) {
        return res.status(503).json({ success: false, message: 'Telegram бот не настроен.' });
    }
    
    // Генерируем код для привязки
    const linkCode = Math.random().toString(36).substring(2, 10).toUpperCase();
    const expiresAt = Date.now() + 10 * 60 * 1000; // 10 минут
    
    // Сохраняем код для привязки (в обоих регистрах для удобства)
    pendingTgLogins[linkCode.toLowerCase()] = {
        username: username,
        timestamp: expiresAt,
        type: '2fa_setup'
    };
    pendingTgLogins[linkCode] = {
        username: username,
        timestamp: expiresAt,
        type: '2fa_setup'
    };
    
    // Получаем имя бота асинхронно
    bot.getMe().then((botInfo) => {
        res.json({
            success: true,
            linkCode: linkCode,
            botUsername: botInfo.username,
            message: `Отправьте код "${linkCode}" боту @${botInfo.username} для привязки Telegram 2FA. Код действителен 10 минут.`
        });
    }).catch((err) => {
        console.error('[TELEGRAM 2FA SETUP] Ошибка получения информации о боте:', err);
        res.json({
            success: true,
            linkCode: linkCode,
            message: `Отправьте код "${linkCode}" боту для привязки Telegram 2FA. Код действителен 10 минут.`
        });
    });
});

// --- 9. Маршрут: Отключить 2FA ---
app.post('/api/user/2fa/disable', (req, res) => {
    const { username, password } = req.body;
    
    if (!username || !password) {
        return res.status(400).json({ success: false, message: 'Имя пользователя и пароль обязательны.' });
    }
    
    db.get('SELECT password FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[2FA DISABLE] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        bcrypt.compare(password, user.password, (err, isMatch) => {
            if (err || !isMatch) {
                return res.status(401).json({ success: false, message: 'Неверный пароль.' });
            }
            
            // Отключаем 2FA и очищаем секреты
            db.run('UPDATE users SET two_factor_enabled = 0, telegram_chat_id = NULL WHERE username = ?', [username], (err) => {
                if (err) {
                    console.error('[2FA DISABLE] Ошибка обновления БД:', err);
                    return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                }
                
                res.json({ success: true, message: '2FA успешно отключена.' });
            });
        });
    });
});

// --- 10. Маршрут: Получить косметику пользователя ---
app.get('/api/user/cosmetics', (req, res) => {
    const { username } = req.query;
    
    if (!username) {
        return res.status(400).json({ success: false, message: 'Имя пользователя обязательно.' });
    }
    
    db.get('SELECT cosmetics FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[COSMETICS] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
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
        
        // Если косметики нет, добавляем базовую косметику
        if (cosmetics.length === 0) {
            cosmetics = getDefaultCosmetics();
            // Сохраняем базовую косметику в БД
            db.run('UPDATE users SET cosmetics = ? WHERE username = ?', 
                [JSON.stringify(cosmetics), username], (err) => {
                    if (err) {
                        console.error('[COSMETICS] Ошибка сохранения базовой косметики:', err);
                    }
                });
        }
        
        res.json({ success: true, cosmetics: cosmetics });
    });
});

// Функция для получения базовой косметики
function getDefaultCosmetics() {
    return [
        {
            id: 'cap_blue',
            name: 'Синяя кепка',
            type: 'hat',
            icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect fill="%230000FF" width="64" height="20"/><rect y="20" fill="%23FFFFFF" width="64" height="44"/></svg>',
            description: 'Стильная синяя кепка'
        },
        {
            id: 'cape_red',
            name: 'Красный плащ',
            type: 'cape',
            icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect fill="%23FF0000" width="64" height="64"/></svg>',
            description: 'Элегантный красный плащ'
        },
        {
            id: 'glasses_sunglasses',
            name: 'Солнечные очки',
            type: 'accessory',
            icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect fill="%23000000" width="64" height="20" y="22"/><rect fill="%23FFFFFF" width="20" height="20" x="10" y="22"/><rect fill="%23FFFFFF" width="20" height="20" x="34" y="22"/></svg>',
            description: 'Крутые солнечные очки'
        },
        {
            id: 'badge_vip',
            name: 'VIP Значок',
            type: 'badge',
            icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><circle fill="%23FFD700" cx="32" cy="32" r="30"/><text x="32" y="40" font-size="24" fill="%23000000" text-anchor="middle" font-weight="bold">VIP</text></svg>',
            description: 'Эксклюзивный VIP значок',
            price: 500
        },
        {
            id: 'golden_crown',
            name: 'Золотая Корона',
            type: 'hat',
            icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><path fill="%23FFD700" d="M32 10 L40 30 L32 25 L24 30 Z"/></svg>',
            description: 'Корона из чистого золота',
            price: 1000
        },
        {
            id: 'rainbow_trail',
            name: 'Радужный След',
            type: 'trail',
            icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect fill="%23FF0000" width="12" height="64"/><rect fill="%23FF7F00" width="12" height="64" x="12"/><rect fill="%23FFFF00" width="12" height="64" x="24"/><rect fill="%2300FF00" width="12" height="64" x="36"/><rect fill="%230000FF" width="12" height="64" x="48"/></svg>',
            description: 'Оставляет радужный след за вами',
            price: 750
        },
        {
            id: 'diamond_sword',
            name: 'Алмазный Меч',
            type: 'weapon',
            icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect fill="%2300FFFF" width="8" height="40" x="28" y="10"/><rect fill="%23FFFFFF" width="20" height="8" x="22" y="2"/></svg>',
            description: 'Косметический алмазный меч',
            price: 1200
        }
    ];
}

// --- 11. Маршрут: Добавить косметику пользователю ---
app.post('/api/user/cosmetics/add', (req, res) => {
    const { username, cosmetic } = req.body;
    
    if (!username || !cosmetic) {
        return res.status(400).json({ success: false, message: 'Имя пользователя и косметика обязательны.' });
    }
    
    db.get('SELECT cosmetics FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[COSMETICS ADD] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
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
        
        // Проверяем, нет ли уже такой косметики
        if (!cosmetics.find(c => c.id === cosmetic.id)) {
            cosmetics.push(cosmetic);
            
            db.run('UPDATE users SET cosmetics = ? WHERE username = ?', 
                [JSON.stringify(cosmetics), username], (err) => {
                if (err) {
                    console.error('[COSMETICS ADD] Ошибка обновления БД:', err);
                    return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                }
                
                res.json({ success: true, message: 'Косметика добавлена!' });
            });
        } else {
            res.status(409).json({ success: false, message: 'Косметика уже добавлена.' });
        }
    });
});

// --- 12. Маршрут: Загрузить скин пользователя ---
app.post('/api/user/skin', (req, res) => {
    const { username, skin } = req.body;
    
    if (!username || !skin) {
        return res.status(400).json({ success: false, message: 'Имя пользователя и скин обязательны.' });
    }
    
    // Проверяем, существует ли пользователь
    db.get('SELECT id FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[SKIN UPLOAD] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        // Сохраняем скин в базе данных (можно также сохранить в файл)
        // В данном случае сохраняем base64 в БД, но лучше сохранять в файлы
        db.run('UPDATE users SET skin = ? WHERE username = ?', [skin, username], (err) => {
            if (err) {
                console.error('[SKIN UPLOAD] Ошибка обновления БД:', err);
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }
            
            res.json({ success: true, message: 'Скин успешно загружен!' });
        });
    });
});

// --- 13. Маршрут: Получить скин пользователя ---
app.get('/api/user/skin', (req, res) => {
    const { username } = req.query;
    
    if (!username) {
        return res.status(400).json({ success: false, message: 'Имя пользователя обязательно.' });
    }
    
    db.get('SELECT skin FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[SKIN GET] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        if (user.skin) {
            res.json({ success: true, skin: user.skin });
        } else {
            res.json({ success: false, message: 'Скин не найден.' });
        }
    });
});

// --- 13.2. Маршрут: Получить модель скина пользователя ---
app.get('/api/user/skin/model', (req, res) => {
    const { username } = req.query;
    
    if (!username) {
        return res.status(400).json({ success: false, message: 'Имя пользователя обязательно.' });
    }
    
    db.get('SELECT skin_model FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[SKIN MODEL GET] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        res.json({ success: true, model: user.skin_model || 'classic' });
    });
});

// --- 14. Маршрут: Получить баланс донат-валюты ---
app.get('/api/user/currency', (req, res) => {
    const { username } = req.query;
    
    if (!username) {
        return res.status(400).json({ success: false, message: 'Имя пользователя обязательно.' });
    }
    
    db.get('SELECT currency FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[CURRENCY GET] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        res.json({ success: true, balance: user.currency || 0 });
    });
});

// --- 15. Маршрут: Выдать донат-валюту (админ команда) ---
app.post('/api/admin/currency/give', (req, res) => {
    const { username, amount, adminToken } = req.body;
    
    // Простая проверка админ токена (в продакшене использовать более безопасный метод)
    const ADMIN_TOKEN = process.env.ADMIN_TOKEN || 'horizon_admin_2024';
    
    if (!adminToken || adminToken !== ADMIN_TOKEN) {
        return res.status(403).json({ success: false, message: 'Неверный админ токен.' });
    }
    
    if (!username || !amount || amount <= 0) {
        return res.status(400).json({ success: false, message: 'Имя пользователя и сумма обязательны.' });
    }
    
    db.get('SELECT currency FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[CURRENCY GIVE] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        const newBalance = (user.currency || 0) + amount;
        
        db.run('UPDATE users SET currency = ? WHERE username = ?', [newBalance, username], (err) => {
            if (err) {
                console.error('[CURRENCY GIVE] Ошибка обновления БД:', err);
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }
            
            console.log(`[CURRENCY GIVE] Выдано ${amount} валюты пользователю ${username}, новый баланс: ${newBalance}`);
            res.json({ success: true, message: `Выдано ${amount} валюты. Новый баланс: ${newBalance}`, balance: newBalance });
        });
    });
});

// --- 16. Маршрут: Покупка косметики за донат-валюту ---
app.post('/api/user/currency/purchase', (req, res) => {
    const { username, cosmeticId, price } = req.body;
    
    if (!username || !cosmeticId || !price || price <= 0) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    db.get('SELECT currency, cosmetics FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[CURRENCY PURCHASE] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user) {
            return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
        }
        
        const balance = user.currency || 0;
        if (balance < price) {
            return res.status(400).json({ success: false, message: 'Недостаточно средств.' });
        }
        
        // Получаем косметику из списка доступной
        const availableCosmetics = getAvailableCosmetics();
        const cosmetic = availableCosmetics.find(c => c.id === cosmeticId);
        
        if (!cosmetic) {
            return res.status(404).json({ success: false, message: 'Косметика не найдена.' });
        }
        
        // Проверяем, нет ли уже такой косметики
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
        
        // Вычитаем валюту и добавляем косметику
        const newBalance = balance - price;
        cosmetics.push(cosmetic);
        
        db.run('UPDATE users SET currency = ?, cosmetics = ? WHERE username = ?', 
            [newBalance, JSON.stringify(cosmetics), username], (err) => {
            if (err) {
                console.error('[CURRENCY PURCHASE] Ошибка обновления БД:', err);
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }
            
            console.log(`[CURRENCY PURCHASE] Пользователь ${username} купил косметику ${cosmeticId} за ${price}, новый баланс: ${newBalance}`);
            res.json({ success: true, message: 'Косметика успешно куплена!', balance: newBalance });
        });
    });
});

// ==================== ФОРУМ API ====================

// Получить категории форума
app.get('/api/forum/categories', (req, res) => {
    db.all(`SELECT c.*, COUNT(t.id) as topics_count 
            FROM forum_categories c 
            LEFT JOIN forum_topics t ON c.id = t.category_id 
            GROUP BY c.id 
            ORDER BY c.order_index`, [], (err, categories) => {
        if (err) {
            console.error('[FORUM CATEGORIES] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, categories: categories || [] });
    });
});

// Получить темы категории
app.get('/api/forum/topics', (req, res) => {
    const { category_id } = req.query;
    
    if (!category_id) {
        return res.status(400).json({ success: false, message: 'ID категории обязателен.' });
    }
    
    db.all(`SELECT * FROM forum_topics 
            WHERE category_id = ? 
            ORDER BY is_pinned DESC, last_reply_at DESC, created_at DESC`, 
            [category_id], (err, topics) => {
        if (err) {
            console.error('[FORUM TOPICS] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, topics: topics || [] });
    });
});

// Создать тему
app.post('/api/forum/topics', (req, res) => {
    const { category_id, title, content, author_username } = req.body;
    
    if (!category_id || !title || !content || !author_username) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    db.run(`INSERT INTO forum_topics (category_id, author_username, title, content) 
            VALUES (?, ?, ?, ?)`, 
            [category_id, author_username, title, content], function(err) {
        if (err) {
            console.error('[FORUM TOPIC CREATE] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, topic_id: this.lastID, message: 'Тема создана!' });
    });
});

// Получить сообщения темы
app.get('/api/forum/posts', (req, res) => {
    const { topic_id, username } = req.query;
    
    if (!topic_id) {
        return res.status(400).json({ success: false, message: 'ID темы обязателен.' });
    }
    
    // Увеличиваем просмотры
    db.run('UPDATE forum_topics SET views = views + 1 WHERE id = ?', [topic_id]);
    
    db.all(`SELECT p.*, 
            (SELECT COUNT(*) FROM forum_likes WHERE post_id = p.id) as likes_count,
            ${username ? `(SELECT COUNT(*) FROM forum_likes WHERE post_id = p.id AND username = ?) > 0 as is_liked` : '0 as is_liked'}
            FROM forum_posts p 
            WHERE p.topic_id = ? 
            ORDER BY p.created_at ASC`, 
            username ? [username, topic_id] : [topic_id], (err, posts) => {
        if (err) {
            console.error('[FORUM POSTS] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, posts: posts || [] });
    });
});

// Создать сообщение
app.post('/api/forum/posts', (req, res) => {
    const { topic_id, content, author_username } = req.body;
    
    if (!topic_id || !content || !author_username) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    db.run(`INSERT INTO forum_posts (topic_id, author_username, content) 
            VALUES (?, ?, ?)`, 
            [topic_id, author_username, content], function(err) {
        if (err) {
            console.error('[FORUM POST CREATE] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        // Обновляем счетчик ответов и время последнего ответа
        db.run(`UPDATE forum_topics 
                SET replies_count = replies_count + 1, 
                    last_reply_at = CURRENT_TIMESTAMP 
                WHERE id = ?`, [topic_id]);
        
        res.json({ success: true, post_id: this.lastID, message: 'Сообщение добавлено!' });
    });
});

// Лайкнуть сообщение
app.post('/api/forum/posts/like', (req, res) => {
    const { post_id, username } = req.body;
    
    if (!post_id || !username) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    // Проверяем, есть ли уже лайк
    db.get('SELECT id FROM forum_likes WHERE post_id = ? AND username = ?', 
            [post_id, username], (err, like) => {
        if (err) {
            console.error('[FORUM LIKE] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (like) {
            // Удаляем лайк
            db.run('DELETE FROM forum_likes WHERE post_id = ? AND username = ?', 
                    [post_id, username], (err) => {
                if (err) {
                    return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                }
                res.json({ success: true, liked: false, message: 'Лайк убран' });
            });
        } else {
            // Добавляем лайк
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

// Модерация темы (pin/lock)
app.post('/api/forum/topics/moderate', (req, res) => {
    const { topic_id, action, moderator_username } = req.body;
    
    if (!topic_id || !action || !moderator_username) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    // Проверка прав модератора
    db.get('SELECT * FROM users WHERE username = ?', [moderator_username], (err, user) => {
        if (err || !user) {
            return res.status(403).json({ success: false, message: 'Доступ запрещен.' });
        }
        
        // Роли с правами модерации (owner, curator, admin, helper)
        const hasModRights = ['owner', 'curator', 'admin', 'helper'].includes(user.role || 'player');
        if (!hasModRights) {
            return res.status(403).json({ success: false, message: 'Нет прав для модерации.' });
        }
        
        let query = '';
        if (action === 'pin') {
            query = 'UPDATE forum_topics SET is_pinned = NOT is_pinned WHERE id = ?';
        } else if (action === 'lock') {
            query = 'UPDATE forum_topics SET is_locked = NOT is_locked WHERE id = ?';
        } else {
            return res.status(400).json({ success: false, message: 'Неверное действие.' });
        }
        
        db.run(query, [topic_id], (err) => {
            if (err) {
                console.error('[FORUM MODERATE] Ошибка БД:', err);
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }
            res.json({ success: true, message: `Действие ${action} выполнено!` });
        });
    });
});

// Удаление темы
app.delete('/api/forum/topics/:id', (req, res) => {
    const { id } = req.params;
    const { moderator_username } = req.body;
    
    if (!moderator_username) {
        return res.status(400).json({ success: false, message: 'Модератор не указан.' });
    }
    
    // Проверка прав
    db.get('SELECT * FROM users WHERE username = ?', [moderator_username], (err, user) => {
        if (err || !user) {
            return res.status(403).json({ success: false, message: 'Доступ запрещен.' });
        }
        
        const hasModRights = ['owner', 'curator', 'admin'].includes(user.role || 'player');
        if (!hasModRights) {
            return res.status(403).json({ success: false, message: 'Нет прав для удаления.' });
        }
        
        // Удаляем тему и все ее сообщения
        db.run('DELETE FROM forum_posts WHERE topic_id = ?', [id], (err) => {
            if (err) {
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }
            
            db.run('DELETE FROM forum_topics WHERE id = ?', [id], (err) => {
                if (err) {
                    return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                }
                res.json({ success: true, message: 'Тема удалена!' });
            });
        });
    });
});

// Редактирование сообщения
app.put('/api/forum/posts/:id', (req, res) => {
    const { id } = req.params;
    const { content, author_username } = req.body;
    
    if (!content || !author_username) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    // Проверяем, что пользователь - автор сообщения
    db.get('SELECT * FROM forum_posts WHERE id = ? AND author_username = ?', 
            [id, author_username], (err, post) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!post) {
            return res.status(403).json({ success: false, message: 'Вы не можете редактировать это сообщение.' });
        }
        
        db.run(`UPDATE forum_posts SET content = ?, is_edited = 1, edited_at = CURRENT_TIMESTAMP WHERE id = ?`, 
                [content, id], (err) => {
            if (err) {
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }
            res.json({ success: true, message: 'Сообщение отредактировано!' });
        });
    });
});

// Удаление сообщения
app.delete('/api/forum/posts/:id', (req, res) => {
    const { id } = req.params;
    const { username } = req.body;
    
    if (!username) {
        return res.status(400).json({ success: false, message: 'Пользователь не указан.' });
    }
    
    // Проверяем, что пользователь - автор или модератор
    db.get('SELECT * FROM forum_posts WHERE id = ?', [id], (err, post) => {
        if (err || !post) {
            return res.status(404).json({ success: false, message: 'Сообщение не найдено.' });
        }
        
        db.get('SELECT * FROM users WHERE username = ?', [username], (err, user) => {
            if (err || !user) {
                return res.status(403).json({ success: false, message: 'Доступ запрещен.' });
            }
            
            const isAuthor = post.author_username === username;
            const isModerator = ['owner', 'curator', 'admin'].includes(user.role || 'player');
            
            if (!isAuthor && !isModerator) {
                return res.status(403).json({ success: false, message: 'Нет прав для удаления.' });
            }
            
            // Удаляем сообщение и его лайки
            db.run('DELETE FROM forum_likes WHERE post_id = ?', [id], (err) => {
                if (err) {
                    return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                }
                
                db.run('DELETE FROM forum_posts WHERE id = ?', [id], (err) => {
                    if (err) {
                        return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                    }
                    
                    // Уменьшаем счетчик ответов
                    db.run('UPDATE forum_topics SET replies_count = replies_count - 1 WHERE id = ?', 
                            [post.topic_id]);
                    
                    res.json({ success: true, message: 'Сообщение удалено!' });
                });
            });
        });
    });
});

// ==================== КОСМЕТИКА И МОДЫ API ====================

// Получить список модов
app.get('/api/cosmetics/mods', (req, res) => {
    db.all('SELECT * FROM cosmetic_mods WHERE is_active = 1 ORDER BY created_at DESC', [], (err, mods) => {
        if (err) {
            console.error('[COSMETIC MODS] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, mods: mods || [] });
    });
});

// Получить анимации косметики
app.get('/api/cosmetics/animations', (req, res) => {
    const { cosmetic_id } = req.query;
    
    const query = cosmetic_id 
        ? 'SELECT * FROM cosmetic_animations WHERE cosmetic_id = ?'
        : 'SELECT * FROM cosmetic_animations';
    const params = cosmetic_id ? [cosmetic_id] : [];
    
    db.all(query, params, (err, animations) => {
        if (err) {
            console.error('[COSMETIC ANIMATIONS] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, animations: animations || [] });
    });
});

// Сохранить анимацию косметики
app.post('/api/cosmetics/animations', (req, res) => {
    const { cosmetic_id, animation_type, frames, duration, loop } = req.body;
    
    if (!cosmetic_id || !animation_type || !frames) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    db.run(`INSERT INTO cosmetic_animations (cosmetic_id, animation_type, frames, duration, loop) 
            VALUES (?, ?, ?, ?, ?)`, 
            [cosmetic_id, animation_type, frames, duration || 1000, loop !== undefined ? loop : 1], 
            function(err) {
        if (err) {
            console.error('[COSMETIC ANIMATION SAVE] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, animation_id: this.lastID, message: 'Анимация сохранена!' });
    });
});

// Запуск сервера
// ==================== TELEGRAM НОВОСТИ ====================

const TELEGRAM_CHANNEL = 'Hor1zonNews';
const TELEGRAM_RSS_URL = `https://rsshub.app/telegram/channel/${TELEGRAM_CHANNEL}`;

// Получить новости из Telegram
async function fetchTelegramNews() {
    try {
        const Parser = require('rss-parser');
        const parser = new Parser();
        const feed = await parser.parseURL(TELEGRAM_RSS_URL);
        
        return feed.items.slice(0, 10).map((item, index) => ({
            id: `tg_${index}`,
            title: item.title || 'Новость Horizon',
            description: item.contentSnippet || item.content?.substring(0, 200) || '',
            content: item.content || item.contentSnippet || '',
            image_url: extractImageFromContent(item.content) || 'https://images.unsplash.com/photo-1614732414444-096e5f1122d5?w=1080',
            author_username: 'Horizon News',
            category: 'telegram',
            views: 0,
            created_at: item.pubDate || new Date().toISOString()
        }));
    } catch (error) {
        console.error('[TELEGRAM NEWS] Ошибка:', error);
        return [];
    }
}

function extractImageFromContent(content) {
    if (!content) return null;
    const imgMatch = content.match(/<img[^>]+src="([^">]+)"/);
    return imgMatch ? imgMatch[1] : null;
}

// ==================== НОВОСТИ API ====================

// Получить список новостей
app.get('/api/news', async (req, res) => {
    const { category, limit = 10, source = 'all' } = req.query;
    
    try {
        let dbNews = [];
        let telegramNews = [];
        
        // Получаем новости из БД
        await new Promise((resolve) => {
            let query = 'SELECT * FROM news WHERE is_published = 1';
            const params = [];
            
            if (category && category !== 'telegram') {
                query += ' AND category = ?';
                params.push(category);
            }
            
            query += ' ORDER BY created_at DESC';
            
            db.all(query, params, (err, news) => {
                if (!err) dbNews = news || [];
                resolve();
            });
        });
        
        // Получаем новости из Telegram
        if (source === 'all' || source === 'telegram') {
            telegramNews = await fetchTelegramNews();
        }
        
        // Объединяем и сортируем
        const allNews = [...dbNews, ...telegramNews]
            .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
            .slice(0, parseInt(limit));
        
        res.json({ success: true, news: allNews });
    } catch (error) {
        console.error('[NEWS] Ошибка:', error);
        res.status(500).json({ success: false, message: 'Ошибка сервера.' });
    }
});

// Получить одну новость
app.get('/api/news/:id', (req, res) => {
    const { id } = req.params;
    
    // Увеличиваем просмотры
    db.run('UPDATE news SET views = views + 1 WHERE id = ?', [id]);
    
    db.get('SELECT * FROM news WHERE id = ?', [id], (err, news) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        if (!news) {
            return res.status(404).json({ success: false, message: 'Новость не найдена.' });
        }
        res.json({ success: true, news });
    });
});

// Создать новость (админ)
app.post('/api/news', (req, res) => {
    const { title, description, content, image_url, author_username, category = 'general' } = req.body;
    
    if (!title || !description || !content || !author_username) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    // Проверка прав администратора
    db.get('SELECT * FROM users WHERE username = ?', [author_username], (err, user) => {
        if (err || !user) {
            return res.status(403).json({ success: false, message: 'Доступ запрещен.' });
        }
        
        const hasAdminRights = ['owner', 'curator', 'admin'].includes(user.role || 'player');
        if (!hasAdminRights) {
            return res.status(403).json({ success: false, message: 'Нет прав для создания новостей.' });
        }
        
        db.run(`INSERT INTO news (title, description, content, image_url, author_username, category) 
                VALUES (?, ?, ?, ?, ?, ?)`,
                [title, description, content, image_url, author_username, category], function(err) {
            if (err) {
                console.error('[NEWS CREATE] Ошибка БД:', err);
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }
            res.json({ success: true, news_id: this.lastID, message: 'Новость создана!' });
        });
    });
});

// ==================== МАГАЗИН API ====================

// Получить товары магазина
app.get('/api/shop/items', (req, res) => {
    const { category, rarity, limit = 50 } = req.query;
    
    let query = 'SELECT * FROM shop_items WHERE is_available = 1';
    const params = [];
    
    if (category) {
        query += ' AND category = ?';
        params.push(category);
    }
    
    if (rarity) {
        query += ' AND rarity = ?';
        params.push(rarity);
    }
    
    query += ' ORDER BY created_at DESC LIMIT ?';
    params.push(parseInt(limit));
    
    db.all(query, params, (err, items) => {
        if (err) {
            console.error('[SHOP ITEMS] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, items: items || [] });
    });
});

// Купить товар
app.post('/api/shop/purchase', (req, res) => {
    const { username, item_id } = req.body;
    
    if (!username || !item_id) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    // Получаем информацию о товаре
    db.get('SELECT * FROM shop_items WHERE id = ?', [item_id], (err, item) => {
        if (err || !item) {
            return res.status(404).json({ success: false, message: 'Товар не найден.' });
        }
        
        if (!item.is_available || (item.stock !== -1 && item.stock <= 0)) {
            return res.status(400).json({ success: false, message: 'Товар недоступен.' });
        }
        
        // Получаем баланс пользователя
        db.get('SELECT currency FROM users WHERE username = ?', [username], (err, user) => {
            if (err || !user) {
                return res.status(404).json({ success: false, message: 'Пользователь не найден.' });
            }
            
            const finalPrice = Math.floor(item.price * (1 - item.discount / 100));
            
            if (user.currency < finalPrice) {
                return res.status(400).json({ success: false, message: 'Недостаточно средств.' });
            }
            
            // Списываем средства
            db.run('UPDATE users SET currency = currency - ? WHERE username = ?', 
                    [finalPrice, username], (err) => {
                if (err) {
                    return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                }
                
                // Создаем запись о покупке
                db.run(`INSERT INTO purchases (username, item_id, price_paid) VALUES (?, ?, ?)`,
                        [username, item_id, finalPrice], (err) => {
                    if (err) {
                        // Откатываем списание средств
                        db.run('UPDATE users SET currency = currency + ? WHERE username = ?', [finalPrice, username]);
                        return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                    }
                    
                    // Обновляем статистику товара
                    db.run(`UPDATE shop_items 
                            SET sales_count = sales_count + 1, 
                                stock = CASE WHEN stock = -1 THEN -1 ELSE stock - 1 END 
                            WHERE id = ?`, [item_id]);
                    
                    // Записываем транзакцию
                    db.run(`INSERT INTO currency_transactions (username, amount, transaction_type, description) 
                            VALUES (?, ?, 'purchase', ?)`,
                            [username, -finalPrice, `Покупка: ${item.name}`]);
                    
                    res.json({ 
                        success: true, 
                        message: 'Покупка успешна!',
                        new_balance: user.currency - finalPrice
                    });
                });
            });
        });
    });
});

// Получить историю покупок пользователя
app.get('/api/shop/purchases/:username', (req, res) => {
    const { username } = req.params;
    
    db.all(`SELECT p.*, si.name as item_name, si.rarity, si.category 
            FROM purchases p 
            LEFT JOIN shop_items si ON p.item_id = si.id 
            WHERE p.username = ? 
            ORDER BY p.created_at DESC`,
            [username], (err, purchases) => {
        if (err) {
            console.error('[SHOP PURCHASES] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, purchases: purchases || [] });
    });
});

// Получить транзакции валюты
app.get('/api/currency/transactions/:username', (req, res) => {
    const { username } = req.params;
    const { limit = 20 } = req.query;
    
    db.all(`SELECT * FROM currency_transactions 
            WHERE username = ? 
            ORDER BY created_at DESC 
            LIMIT ?`,
            [username, parseInt(limit)], (err, transactions) => {
        if (err) {
            console.error('[CURRENCY TRANSACTIONS] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        res.json({ success: true, transactions: transactions || [] });
    });
});

// ==================== ЮKASSA ПЛАТЕЖИ ====================

const yukassa = require('./yukassa');

// Создать платеж
app.post('/api/payments/create', async (req, res) => {
    const { amount, description, username } = req.body;
    
    if (!amount || !username) {
        return res.status(400).json({ success: false, message: 'Все поля обязательны.' });
    }
    
    const returnUrl = `http://localhost:5173/#/shop?payment=success`;
    const result = await yukassa.createPayment(amount, description || 'Пополнение баланса', username, returnUrl);
    
    if (result.success) {
        // Сохраняем информацию о платеже в БД
        db.run(`INSERT INTO payments (username, amount, payment_id, status) VALUES (?, ?, ?, ?)`,
            [username, amount, result.paymentId, 'pending']);
        
        res.json({
            success: true,
            paymentId: result.paymentId,
            confirmationUrl: result.confirmationUrl
        });
    } else {
        res.status(500).json(result);
    }
});

// Проверить статус платежа
app.get('/api/payments/status/:paymentId', async (req, res) => {
    const { paymentId } = req.params;
    
    const result = await yukassa.getPaymentStatus(paymentId);
    res.json(result);
});

// Webhook от ЮKassa
app.post('/api/payments/webhook', async (req, res) => {
    const result = await yukassa.handleWebhook(req.body);
    
    if (result.success && result.status === 'succeeded') {
        // Начисляем валюту пользователю
        db.run('UPDATE users SET currency = currency + ? WHERE username = ?',
            [result.amount, result.username], (err) => {
            if (!err) {
                // Обновляем статус платежа
                db.run('UPDATE payments SET status = ? WHERE payment_id = ?',
                    ['succeeded', result.paymentId]);
                
                // Записываем транзакцию
                db.run(`INSERT INTO currency_transactions (username, amount, transaction_type, description) 
                        VALUES (?, ?, 'payment', ?)`,
                        [result.username, result.amount, `Пополнение через ЮKassa: ${result.orderId}`]);
            }
        });
    }
    
    res.json({ success: true });
});

// Создать таблицу платежей
db.run(`CREATE TABLE IF NOT EXISTS payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    amount REAL NOT NULL,
    payment_id TEXT NOT NULL,
    status TEXT DEFAULT 'pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
)`);

// ==================== СЕРВЕРЫ ====================

// Получить список серверов
app.get('/api/servers', (req, res) => {
    const servers = [
        {
            id: 'survival',
            name: 'Survival',
            ip: 'survival.horizon-rp.ru',
            port: 25565,
            version: '1.20.1',
            online: 247,
            maxPlayers: 500,
            status: 'online',
            description: 'Классический сервер выживания'
        },
        {
            id: 'creative',
            name: 'Creative',
            ip: 'creative.horizon-rp.ru',
            port: 25565,
            version: '1.20.1',
            online: 89,
            maxPlayers: 200,
            status: 'online',
            description: 'Творческий режим для строителей'
        }
    ];
    
    res.json({ success: true, servers });
});

// ==================== ЗАПУСК СЕРВЕРА ====================
app.listen(PORT, () => {
    console.log(`🚀 API запущен: http://localhost:${PORT}`);
    console.log(`💳 ЮKassa: ${process.env.YUKASSA_SHOP_ID ? 'Настроено' : 'Требуется настройка в .env'}`);
});