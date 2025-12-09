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

ipcMain.on('launch-game', (event, { ram }) => {
    const { Client } = require('minecraft-launcher-core');
    const launcher = new Client();

    let opts = {
        clientPackage: null,
        authorization: event.sender.getPreloads()[0], // Just an example, this needs to be proper auth
        root: "./.minecraft",
        version: {
            number: "1.17.1",
            type: "release"
        },
        memory: {
            max: `${ram}G`,
            min: `1G`
        }
    }

    launcher.launch(opts);

    launcher.on('debug', (e) => console.log(e));
    launcher.on('data', (e) => console.log(e));
});
