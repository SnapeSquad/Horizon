// api-server/server.js
require('dotenv').config(); 

const express = require('express');
const TelegramBot = require('node-telegram-bot-api');
const fs = require('fs');
const path = require('path');
const bcrypt = require('bcrypt');
const sqlite3 = require('sqlite3').verbose();
const speakeasy = require('speakeasy');
const QRCode = require('qrcode');
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
                google_secret TEXT,
                two_factor_enabled INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )`);
            
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

            // Верификация успешна (обычный вход)
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
    const { username, password, twoFactorCode } = req.body;

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
                        const loginCode = Math.floor(100000 + Math.random() * 900000).toString(); // 6-значный код
                        const expiresAt = Date.now() + 5 * 60 * 1000; // 5 минут
                        
                        // Сохраняем код для проверки
                        pendingTgLogins[`login_${user.username}`] = {
                            username: user.username,
                            code: loginCode,
                            timestamp: expiresAt,
                            type: 'login_2fa'
                        };
                        
                        // Отправляем код через Telegram
                        bot.sendMessage(user.telegram_chat_id,
                            `🔐 Код для входа в аккаунт *${user.username}*:\n\n` +
                            `*${loginCode}*\n\n` +
                            `Код действителен 5 минут.`,
                            { parse_mode: 'Markdown' }
                        ).catch(err => {
                            console.error('[LOGIN 2FA] Ошибка отправки кода в Telegram:', err);
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
                let codeValid = false;
                if (user.google_secret) {
                    // Проверка Google Authenticator
                    codeValid = speakeasy.totp.verify({
                        secret: user.google_secret,
                        encoding: 'base32',
                        token: twoFactorCode,
                        window: 2
                    });
                } else if (user.telegram_chat_id) {
                    // Проверка кода из Telegram
                    const loginData = pendingTgLogins[`login_${user.username}`];
                    if (loginData && loginData.type === 'login_2fa') {
                        if (Date.now() <= loginData.timestamp && loginData.code === twoFactorCode) {
                            codeValid = true;
                            delete pendingTgLogins[`login_${user.username}`];
                        }
                    }
                }

                if (!codeValid) {
                    return res.status(401).json({ success: false, message: 'Неверный код двухфакторной аутентификации.' });
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
    
    db.get('SELECT two_factor_enabled, telegram_chat_id, google_secret FROM users WHERE username = ?', [username], (err, user) => {
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
            hasTelegram: !!user.telegram_chat_id,
            hasGoogle: !!user.google_secret
        });
    });
});

// --- 6. Маршрут: Настроить Google Authenticator 2FA ---
app.post('/api/user/2fa/google/setup', async (req, res) => {
    const { username } = req.body;
    
    if (!username) {
        return res.status(400).json({ success: false, message: 'Имя пользователя обязательно.' });
    }
    
    // Генерируем секрет для Google Authenticator
    const secret = speakeasy.generateSecret({
        name: `Horizon (${username})`,
        issuer: 'Horizon Launcher'
    });
    
    // Генерируем QR-код
    try {
        const qrCodeUrl = await QRCode.toDataURL(secret.otpauth_url);
        
        // Сохраняем секрет в БД (временно, пока пользователь не подтвердит)
        db.run('UPDATE users SET google_secret = ? WHERE username = ?', [secret.base32, username], (err) => {
            if (err) {
                console.error('[2FA GOOGLE] Ошибка сохранения секрета:', err);
                return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
            }
            
            res.json({
                success: true,
                secret: secret.base32,
                qrCode: qrCodeUrl,
                manualEntryKey: secret.base32
            });
        });
    } catch (err) {
        console.error('[2FA GOOGLE] Ошибка генерации QR-кода:', err);
        res.status(500).json({ success: false, message: 'Ошибка генерации QR-кода.' });
    }
});

// --- 7. Маршрут: Подтвердить и включить Google Authenticator 2FA ---
app.post('/api/user/2fa/google/verify', (req, res) => {
    const { username, code } = req.body;
    
    if (!username || !code) {
        return res.status(400).json({ success: false, message: 'Имя пользователя и код обязательны.' });
    }
    
    db.get('SELECT google_secret FROM users WHERE username = ?', [username], (err, user) => {
        if (err) {
            console.error('[2FA GOOGLE VERIFY] Ошибка БД:', err);
            return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
        }
        
        if (!user || !user.google_secret) {
            return res.status(400).json({ success: false, message: '2FA не настроена для этого пользователя.' });
        }
        
        const verified = speakeasy.totp.verify({
            secret: user.google_secret,
            encoding: 'base32',
            token: code,
            window: 2
        });
        
        if (verified) {
            // Включаем 2FA
            db.run('UPDATE users SET two_factor_enabled = 1 WHERE username = ?', [username], (err) => {
                if (err) {
                    console.error('[2FA GOOGLE VERIFY] Ошибка обновления БД:', err);
                    return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                }
                
                res.json({ success: true, message: 'Google Authenticator 2FA успешно включена!' });
            });
        } else {
            res.status(400).json({ success: false, message: 'Неверный код. Попробуйте снова.' });
        }
    });
});

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
            db.run('UPDATE users SET two_factor_enabled = 0, google_secret = NULL, telegram_chat_id = NULL WHERE username = ?', [username], (err) => {
                if (err) {
                    console.error('[2FA DISABLE] Ошибка обновления БД:', err);
                    return res.status(500).json({ success: false, message: 'Ошибка сервера.' });
                }
                
                res.json({ success: true, message: '2FA успешно отключена.' });
            });
        });
    });
});

// Запуск сервера
app.listen(PORT, () => {
    console.log(`🚀 API запущен: http://localhost:${PORT}`);
});