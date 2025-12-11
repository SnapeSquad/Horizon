const { app, BrowserWindow, ipcMain, shell } = require('electron');
const path = require('path');
const { Client, Authenticator } = require('minecraft-launcher-core');
const { ipcMain: ipcMainBetter } = require('electron-better-ipc');

const launcher = new Client();
let authWindow;
let mainWindow;
let aboutWindow;
let authenticatedUser = null;

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

function createAboutWindow() {
    aboutWindow = new BrowserWindow({
        width: 400,
        height: 300,
        frame: true,
        title: "О нас",
        resizable: false,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false
        }
    });

    aboutWindow.loadFile(path.join(__dirname, 'about.html'));

    aboutWindow.on('closed', () => {
        aboutWindow = null;
    });
}

app.whenReady().then(() => {
    createAuthWindow();

    app.on('activate', function () {
        if (BrowserWindow.getAllWindows().length === 0) createAuthWindow();
    });
});

app.on('window-all-closed', function () {
    if (process.platform !== 'darwin') app.quit();
});

ipcMain.on('login-success', (event, username) => {
    authenticatedUser = username;
    if (authWindow) {
        authWindow.close();
    }
    createMainWindow();
    // Передаем имя пользователя в главное окно после его загрузки
    mainWindow.webContents.on('did-finish-load', () => {
        mainWindow.webContents.send('user-login', authenticatedUser);
    });
});

ipcMain.on('open-shop', () => {
    shell.openExternal('https://hor1zon.fun');
});

ipcMain.on('open-about', () => {
    if (!aboutWindow) {
        createAboutWindow();
    }
});

// --- ЛОГИКА ЗАПУСКА ИГРЫ И ПРОГРЕССА ---
ipcMainBetter.answerRenderer('launch-game', async (options) => {
    if (!authenticatedUser) {
        ipcMainBetter.callRenderer(mainWindow, 'error-alert', { message: 'Пожалуйста, войдите в свой аккаунт.' });
        return;
    }

    // 1. Отправка начального статуса в UI
    ipcMainBetter.callRenderer(mainWindow, 'status-update', { status: 'Запуск Minecraft...', isDownloading: true });
    
    // Получаем выбранную версию и RAM из UI
    const { version, ram } = options;

    const launchOptions = {
        authorization: await Authenticator.getAuth(authenticatedUser),
        root: path.join(app.getPath('userData'), 'minecraft'),
        version: version || '1.16.5', 
        memory: {
            max: ram || 4096, 
            min: 1024
        },
    };

    // 2. Запуск и отслеживание событий
    launcher.launch(launchOptions).then(() => {
        ipcMainBetter.callRenderer(mainWindow, 'status-update', { status: 'Игра запущена! Закрытие лаунчера...', isDownloading: false });
        setTimeout(() => app.quit(), 3000); 
    }).catch(err => {
        console.error("Ошибка запуска:", err);
        ipcMainBetter.callRenderer(mainWindow, 'error-alert', { message: `Ошибка запуска: ${err.message}` });
        ipcMainBetter.callRenderer(mainWindow, 'status-update', { status: `ОШИБКА`, isDownloading: false });
    });

    // 3. Отслеживание прогресса загрузки файлов
    launcher.on('progress', (e) => {
        const progressPercentage = Math.round((e.loaded / e.total) * 100);
        ipcMainBetter.callRenderer(mainWindow, 'download-progress', {
            task: `Обработка: ${e.task} (${e.loaded} из ${e.total})`,
            progress: progressPercentage
        });
    });
    
    // 4. Отслеживание прогресса скачивания
    launcher.on('download-progress', (e) => {
        const progressPercentage = Math.round((e.loaded / e.total) * 100);
        const loadedMB = (e.loaded / 1024 / 1024).toFixed(2);
        const totalMB = (e.total / 1024 / 1024).toFixed(2);

        ipcMainBetter.callRenderer(mainWindow, 'download-progress', {
            task: `Скачивание: ${e.type} (${loadedMB} MB / ${totalMB} MB)`,
            progress: progressPercentage
        });
    });
});
