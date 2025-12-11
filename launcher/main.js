const { app, BrowserWindow, ipcMain, shell, net } = require('electron');
const path = require('path');
const { Client, Authenticator } = require('minecraft-launcher-core');

const launcher = new Client();
let authWindow;
let mainWindow;
let aboutWindow;
let authenticatedUser = null;

// --- Утилита для выполнения сетевых запросов ---
function makeRequest(url, options, postData) {
    return new Promise((resolve, reject) => {
        const request = net.request(options);
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
        request.on('error', (error) => { reject(error); });
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
            enableRemoteModule: false,
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
            enableRemoteModule: false,
        }
    });
    mainWindow.loadFile(path.join(__dirname, 'index.html'));
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
    return makeRequest(options.path, options, postData);
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
    return makeRequest(options.path, options, postData);
});

// Открытие внешних ссылок
ipcMain.on('open-shop', () => shell.openExternal('https://hor1zon.fun'));
ipcMain.on('open-about', () => {
    if (!aboutWindow) {
        // Логика создания окна 'О нас'
    }
});
