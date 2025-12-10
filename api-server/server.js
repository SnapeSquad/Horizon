// api-server/server.js
require('dotenv').config(); 

const express = require('express');
const TelegramBot = require('node-telegram-bot-api');
// --- ИМПОРТ БИБЛИОТЕКИ MINECRAFT ---
const mcu = require('minecraft-server-util');
// -----------------------------------
const app = express();
const PORT = 3000; 

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

// --- НАСТРОЙКА БАЗЫ ДАННЫХ SQLITE ---
const sqlite3 = require('sqlite3').verbose();
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const db = new sqlite3.Database('./users.db', (err) => {
    if (err) {
        console.error(err.message);
    }
    console.log('Подключено к базе данных пользователей.');
});

// Создание таблицы пользователей, если она не существует
db.run('CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT)');


// --- ЛОГИКА ОБРАБОТКИ СООБЩЕНИЙ ОТ TELEGRAM ---
if (bot) {
    bot.on('message', (msg) => {
        const chatId = msg.chat.id;
        const text = msg.text ? msg.text.trim().toLowerCase() : '';

        console.log(`[TG MSG] Received from ${chatId}: ${text}`);

        // 1. Проверяем, является ли сообщение кодом, который мы ждем
        if (pendingTgLogins.hasOwnProperty(text)) {
            const loginData = pendingTgLogins[text];
            
            // Проверка на срок действия кода
            if (Date.now() > loginData.timestamp) {
                delete pendingTgLogins[text];
                bot.sendMessage(chatId, `❌ Срок действия кода истек. Попробуйте снова зайти через лаунчер.`, { parse_mode: 'Markdown' });
                return;
            }

            // Верификация успешна
            loginData.chatId = chatId;
            loginData.tgUsername = msg.from.username; 
            
            // Удаляем старую запись и добавляем в обратный индекс
            delete pendingTgLogins[text];
            pendingTgLoginsByChatId[chatId] = loginData;

            bot.sendMessage(chatId, 
                `✅ Аккаунт *${loginData.username}* верифицирован! Вы можете вернуться в лаунчер.`,
                { parse_mode: 'Markdown' }
            );
            
            console.log(`[TG AUTH] Код ${text} успешно верифицирован для ${loginData.username}`);
            return;
        }

        // 2. Стандартный ответ
        if (text === '/start') {
            bot.sendMessage(chatId, 
                `Привет! Чтобы войти в лаунчер, введите в нем свой никнейм и следуйте инструкциям. ` +
                `Лаунчер пришлет вам одноразовый код, который нужно отправить мне.`,
                { parse_mode: 'Markdown' }
            );
        } else {
             bot.sendMessage(chatId, "Неизвестная команда. Чтобы войти в лаунчер, отправьте мне код, который он сгенерирует.");
        }
    });

    // ... (Обработка ошибок Polling)

    console.log(`🤖 Telegram Bot запущен и слушает входящие сообщения (Polling).`);
} else {
    console.warn(`⚠️ Telegram Bot не запущен, так как не найден токен в .env.`);
}


app.use(express.json());
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*'); 
    res.header('Access-Control-Allow-Headers', 'Content-Type');
    next();
});

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


// --- 2. Маршрут: Вход по логину/паролю ---
app.post('/api/auth/login', (req, res) => {
    const { username, password } = req.body;
    if (username === 'test' && password === '123') {
        return res.json({ success: true, token: 'fake-auth-token-12345', username: 'TestPlayer' });
    } else {
        return res.status(401).json({ success: false, message: 'Неверный логин или пароль.' });
    }
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

// --- 5. Маршрут: Регистрация нового пользователя ---
app.post('/api/auth/register', async (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) {
        return res.status(400).json({ success: false, message: 'Имя пользователя и пароль обязательны.' });
    }
    const hashedPassword = await bcrypt.hash(password, 10);
    db.run('INSERT INTO users (username, password) VALUES (?, ?)', [username, hashedPassword], function(err) {
        if (err) {
            return res.status(409).json({ success: false, message: 'Пользователь с таким именем уже существует.' });
        }
        console.log(`[AUTH] Пользователь зарегистрирован: ${username}`);
        return res.json({ success: true, message: 'Регистрация успешна.' });
    });
});

// --- 6. Маршрут: Вход существующего пользователя ---
app.post('/api/auth/login', (req, res) => {
    const { username, password } = req.body;
    db.get('SELECT * FROM users WHERE username = ?', [username], async (err, user) => {
        if (err || !user) {
            return res.status(401).json({ success: false, message: 'Неверное имя пользователя или пароль.' });
        }
        const match = await bcrypt.compare(password, user.password);
        if (match) {
            const token = jwt.sign({ username: user.username }, process.env.JWT_SECRET || 'your-secret-key', { expiresIn: '1h' });
            console.log(`[AUTH] Пользователь вошел в систему: ${username}`);
            return res.json({ success: true, token: token, username: user.username });
        } else {
            return res.status(401).json({ success: false, message: 'Неверное имя пользователя или пароль.' });
        }
    });
});


// Запуск сервера
app.listen(PORT, () => {
    console.log(`🚀 API запущен: http://localhost:${PORT}`);
});