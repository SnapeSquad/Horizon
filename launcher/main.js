const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const { Client, Authenticator } = require('minecraft-launcher-core');
const { ipcMain: ipcMainBetter } = require('electron-better-ipc');

const launcher = new Client();
let mainWindow;

function createWindow() {
    mainWindow = new BrowserWindow({
        // Убедимся, что размеры достаточно велики:
        width: 1250,      
        height: 920,      
        minWidth: 1250,   
        minHeight: 920,   
        frame: false, 
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true, 
            enableRemoteModule: false,
            // Сбрасываем масштаб на 100%, чтобы избежать проблем с DPI
            zoomFactor: 1.0 
        }
    });

    mainWindow.loadFile(path.join(__dirname, 'index.html'));

    // Для диагностики, если проблема осталась:
    // mainWindow.webContents.openDevTools();
}

app.whenReady().then(() => {
    createWindow();

    app.on('activate', function () {
        if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
});

app.on('window-all-closed', function () {
    if (process.platform !== 'darwin') app.quit();
});

// --- ЛОГИКА ЗАПУСКА ИГРЫ И ПРОГРЕССА ---
ipcMainBetter.answerRenderer('launch-game', async (options) => {
    // 1. Отправка начального статуса в UI
    ipcMainBetter.callRenderer(mainWindow, 'status-update', { status: 'Запуск Minecraft...', isDownloading: true });
    
    // Получаем выбранную версию и RAM из UI
    const { version, ram } = options;

    const launchOptions = {
        authorization: await Authenticator.getAuth("testUser"), // <-- Замените на вашу реальную логику авторизации!
        root: path.join(app.getPath('userData'), 'minecraft'), // Папка для файлов Minecraft
        version: version || '1.16.5', 
        memory: {
            max: ram || 4096, 
            min: 1024
        },
        // javaPath: 'C:\\Program Files\\Java\\jdk-17\\bin\\javaw.exe' // Опционально
    };

    // 2. Запуск и отслеживание событий
    launcher.launch(launchOptions).then(() => {
        // Успешный запуск
        ipcMainBetter.callRenderer(mainWindow, 'status-update', { status: 'Игра запущена! Закрытие лаунчера...', isDownloading: false });
        // Закрытие лаунчера после запуска
        setTimeout(() => app.quit(), 3000); 
    }).catch(err => {
        // Ошибка
        console.error("Ошибка запуска:", err);
        // Отправка ошибки в UI
        ipcMainBetter.callRenderer(mainWindow, 'error-alert', { message: `Ошибка запуска: ${err.message}` });
        ipcMainBetter.callRenderer(mainWindow, 'status-update', { status: `ОШИБКА`, isDownloading: false });
    });

    // 3. Отслеживание прогресса загрузки файлов (общий прогресс)
    launcher.on('progress', (e) => {
        const progressPercentage = Math.round((e.loaded / e.total) * 100);
        ipcMainBetter.callRenderer(mainWindow, 'download-progress', {
            task: `Обработка: ${e.task} (${e.loaded} из ${e.total})`,
            progress: progressPercentage
        });
    });
    
    // 4. Отслеживание прогресса скачивания (детальный прогресс)
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
// ----------------------------------------------------