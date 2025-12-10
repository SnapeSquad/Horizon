const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const axios = require('axios');

let mainWindow;
let loginWindow;

function createLoginWindow() {
    loginWindow = new BrowserWindow({
        width: 500,
        height: 700,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
        }
    });
    loginWindow.loadFile(path.join(__dirname, 'login.html'));
}

function createMainWindow() {
    mainWindow = new BrowserWindow({
        width: 1200,
        height: 800,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
        }
    });
    mainWindow.loadFile(path.join(__dirname, 'index.html'));
}

app.whenReady().then(() => {
    createLoginWindow();

    app.on('activate', function () {
        if (BrowserWindow.getAllWindows().length === 0) createLoginWindow();
    });
});

app.on('window-all-closed', function () {
    if (process.platform !== 'darwin') app.quit();
});

ipcMain.on('login', async (event, { username, password }) => {
    try {
        const response = await axios.post('http://localhost:3000/api/auth/login', { username, password });
        if (response.data.success) {
            createMainWindow();
            mainWindow.webContents.on('did-finish-load', () => {
                mainWindow.webContents.send('login-success', response.data);
            });
            loginWindow.close();
        } else {
            loginWindow.webContents.send('login-failed', response.data.message);
        }
    } catch (error) {
        loginWindow.webContents.send('login-failed', 'Ошибка при подключении к серверу.');
    }
});

ipcMain.on('register', async (event, { username, password }) => {
    try {
        const response = await axios.post('http://localhost:3000/api/auth/register', { username, password });
        if (response.data.success) {
            loginWindow.webContents.send('register-success', 'Регистрация прошла успешно! Теперь вы можете войти.');
        } else {
            loginWindow.webContents.send('register-failed', response.data.message);
        }
    } catch (error) {
        loginWindow.webContents.send('register-failed', 'Ошибка при подключении к серверу.');
    }
});

// --- УПРАВЛЕНИЕ ОКНОМ ---
ipcMain.on('minimize-window', () => {
    mainWindow?.minimize();
});

ipcMain.on('maximize-window', () => {
    if (mainWindow?.isMaximized()) {
        mainWindow.unmaximize();
    } else {
        mainWindow?.maximize();
    }
});

ipcMain.on('close-window', () => {
    app.quit();
});

ipcMain.on('launch-game', (event, { ram, username, token }) => {
    const { Client } = require('minecraft-launcher-core');
    const launcher = new Client();

    let opts = {
        clientPackage: null,
        authorization: {
            access_token: token,
            name: username,
            uuid: "", // This should be fetched from a proper auth server
            user_properties: '{}'
        },
        root: path.join(app.getPath('userData'), '.minecraft'),
        version: {
            number: version,
            type: "release"
        },
        memory: {
            max: `${ram}G`,
            min: `1G`
        }
    }

    launcher.launch(opts);

    launcher.on('debug', (e) => mainWindow.webContents.send('launch-debug', e));
    launcher.on('data', (e) => mainWindow.webContents.send('launch-data', e));
    launcher.on('progress', (e) => {
        mainWindow.webContents.send('launch-progress', {
            type: e.type,
            progress: e.progress,
            total: e.total
        });
    });
});
