const { app, BrowserWindow, ipcMain, shell, net } = require('electron');
const path = require('path');
const { Client, Authenticator } = require('minecraft-launcher-core');

const launcher = new Client();
let authWindow;
let mainWindow;
let aboutWindow;
let authenticatedUser = null;

// --- Утилита для выполнения сетевых запросов ---
<<<<<<< HEAD
function makeRequest(options, postData) {
=======
function makeRequest(urlString, options, postData) {
>>>>>>> 0e800cdc67d6b53feacf26dc79738c3e9fc8d628
    return new Promise((resolve, reject) => {
        const parsedUrl = new URL(urlString);
        const requestOptions = {
            protocol: parsedUrl.protocol,
            hostname: parsedUrl.hostname,
            port: parsedUrl.port,
            path: parsedUrl.pathname,
            ...options
        };

        const request = net.request(requestOptions);

        request.on('response', (response) => {
            let body = '';
            response.on('data', (chunk) => { body += chunk.toString(); });
            response.on('end', () => {
                try {
                    resolve({ statusCode: response.statusCode, body: JSON.parse(body) });
                } catch (e) {
                    reject(new Error(`Invalid JSON response: ${body}`));
                }
            });
        });
        request.on('error', (error) => {
            console.error(`[makeRequest] Network Error: ${error.message}`);
            reject(error);
        });
        if (postData) {
            request.write(postData);
        }
        request.end();
    });
}

function createAuthWindow() {
    authWindow = new BrowserWindow({
        width: 400,
        height: 600,
        frame: false,
        resizable: false,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
        }
    });
    authWindow.loadFile(path.join(__dirname, 'auth.html'));
}

function createMainWindow() {
    mainWindow = new BrowserWindow({
        width: 1250,
        height: 920,
        minWidth: 1250,
        minHeight: 920,
        frame: false,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
        }
    });
    mainWindow.loadFile(path.join(__dirname, 'index.html'));
}

function createAboutWindow() {
    if (aboutWindow) {
        aboutWindow.focus();
        return;
    }
    aboutWindow = new BrowserWindow({
        width: 600,
        height: 400,
        frame: false,
        resizable: false,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            enableRemoteModule: false,
        }
    });
    aboutWindow.loadFile(path.join(__dirname, 'about.html'));
    aboutWindow.on('closed', () => {
        aboutWindow = null;
    });
}

app.whenReady().then(() => {
    createAuthWindow();
    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) createAuthWindow();
    });
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
});

// --- ОБРАБОТЧИКИ IPC ---
<<<<<<< HEAD

// Вход успешен -> Закрыть окно входа, открыть главное
ipcMain.on('login-success', (event, username) => {
    authenticatedUser = username;
    if (authWindow) authWindow.close();
    createMainWindow();
    mainWindow.webContents.on('did-finish-load', () => {
        mainWindow.webContents.send('user-login', authenticatedUser);
    });
});

// Запрос на вход
ipcMain.handle('login-request', async (event, credentials) => {
    const postData = JSON.stringify(credentials);
    const options = {
        method: 'POST',
        protocol: 'http:',
        hostname: 'localhost',
        port: 3000,
        path: '/api/auth/login',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(postData)
        }
    };
    return makeRequest(options, postData);
});

// Запрос на регистрацию
ipcMain.handle('register-request', async (event, credentials) => {
    const postData = JSON.stringify(credentials);
    const options = {
        method: 'POST',
        protocol: 'http:',
        hostname: 'localhost',
        port: 3000,
        path: '/api/auth/register',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(postData)
        }
    };
    return makeRequest(options, postData);
});

// Открытие внешних ссылок
ipcMain.on('open-shop', () => shell.openExternal('https://hor1zon.fun'));
ipcMain.on('open-about', () => {
    // Логика создания окна 'О нас'
});

// Выход из системы
ipcMain.on('logout', () => {
    authenticatedUser = null;
    if (mainWindow) {
        mainWindow.close();
    }
    createAuthWindow();
});

// --- ЛОГИКА ЗАПУСКА ИГРЫ ---
=======
>>>>>>> 0e800cdc67d6b53feacf26dc79738c3e9fc8d628
ipcMain.handle('launch-game', async (event, options) => {
    if (!authenticatedUser) {
        return { success: false, message: 'Пользователь не аутентифицирован.' };
    }

    const { version, ram } = options;

    const launchOptions = {
        authorization: Authenticator.getAuth(authenticatedUser, ''), // Пароль не требуется для оффлайн-режима
        root: path.join(app.getPath('userData'), 'minecraft'),
        version: {
            number: version,
            type: 'release'
        },
        memory: {
            max: `${ram}M`,
            min: '1024M'
        },
    };

    // Удаляем старые обработчики событий перед добавлением новых, чтобы избежать накопления
    launcher.removeAllListeners('debug');
    launcher.removeAllListeners('data');
    launcher.removeAllListeners('progress');
    launcher.removeAllListeners('close');

    launcher.on('debug', (e) => console.log('[DEBUG]', e));
    launcher.on('data', (e) => console.log('[DATA]', e));
    launcher.on('progress', (e) => {
        if (mainWindow && !mainWindow.isDestroyed()) {
            mainWindow.webContents.send('launch-progress', {
                type: e.type,
                task: e.task,
                total: e.total,
                loaded: e.loaded
            });
        }
    });

    launcher.on('close', (e) => {
        if (mainWindow && !mainWindow.isDestroyed()) {
            if (e === 0) {
                mainWindow.webContents.send('launch-success');
            } else {
                mainWindow.webContents.send('launch-error', `Игра закрылась с кодом ошибки: ${e}`);
            }
        }
    });

    launcher.launch(launchOptions);

    return { success: true };
<<<<<<< HEAD
});
=======
});

// Вход успешен -> Закрыть окно входа, открыть главное
ipcMain.on('login-success', (event, username) => {
    authenticatedUser = username;
    if (authWindow) authWindow.close();
    createMainWindow();
    mainWindow.webContents.on('did-finish-load', () => {
        mainWindow.webContents.send('user-login', authenticatedUser);
    });
});

// Запрос на вход
ipcMain.handle('login-request', async (event, credentials) => {
    const postData = JSON.stringify(credentials);
    const url = 'http://localhost:3000/api/auth/login';
    const options = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(postData)
        }
    };
    return makeRequest(url, options, postData);
});

// Запрос на регистрацию
ipcMain.handle('register-request', async (event, credentials) => {
    const postData = JSON.stringify(credentials);
    const url = 'http://localhost:3000/api/auth/register';
    const options = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(postData)
        }
    };
    return makeRequest(url, options, postData);
});

// Открытие внешних ссылок
ipcMain.on('open-shop', () => shell.openExternal('https://hor1zon.fun'));
ipcMain.on('open-about', createAboutWindow);

// Выход из системы
ipcMain.on('logout', () => {
    authenticatedUser = null;
    if (mainWindow) {
        mainWindow.close();
    }
    createAuthWindow();
});
>>>>>>> 0e800cdc67d6b53feacf26dc79738c3e9fc8d628
