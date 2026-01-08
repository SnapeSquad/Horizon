const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const axios = require('axios');
const Store = require('electron-store');
const { spawn } = require('child_process');

// Хранилище для сессий
const store = new Store({
  name: 'horizon-session',
  encryptionKey: 'horizon-secret-key-2024'
});

let mainWindow = null;
let apiUrl = 'http://localhost:3000'; // Node.js API server (Java не нужен для UI!)

/**
 * Создание главного окна
 */
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1200,
    minHeight: 800,
    frame: false, // Frameless для custom window controls
    transparent: true,
    backgroundColor: '#00000000',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  });

  // Dev mode - загружаем из Vite dev server
  if (process.env.NODE_ENV === 'development' || !app.isPackaged) {
    // Проверяем несколько портов (Vite может использовать 5173, 5174, 5175)
    const tryPorts = [5173, 5174, 5175];
    let loaded = false;
    
    const checkPort = async (port) => {
      try {
        await axios.get(`http://localhost:${port}`, { timeout: 500 });
        return true;
      } catch {
        return false;
      }
    };
    
    const loadDevServer = async () => {
      for (const port of tryPorts) {
        console.log(`Checking port ${port}...`);
        const isAvailable = await checkPort(port);
        if (isAvailable) {
          const url = `http://localhost:${port}`;
          console.log(`✅ Found Vite dev server at ${url}`);
          mainWindow.loadURL(url);
          loaded = true;
          break;
        }
      }
      
      if (!loaded) {
        console.error('❌ Could not find Vite dev server on any port!');
        console.error('Please start: cd horizon-ui && npm run dev');
      }
    };
    
    loadDevServer();
    mainWindow.webContents.openDevTools();
  } else {
    // Production - загружаем из собранных файлов
    mainWindow.loadFile(path.join(__dirname, '../horizon-ui/dist/index.html'));
  }

  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  // Проверяем сохраненную сессию при запуске
  checkSavedSession();
}

/**
 * Проверка сохраненной сессии
 */
async function checkSavedSession() {
  const session = store.get('session');
  if (session && session.token) {
    try {
      // Проверяем, что токен еще не истек
      if (session.expiresAt && session.expiresAt > Date.now()) {
        console.log('✅ Сессия найдена и валидна, автоматический вход...');
        mainWindow.webContents.send('auto-login', session);
      } else {
        console.log('⏰ Сессия истекла, удаляем...');
        store.delete('session');
      }
    } catch (error) {
      console.error('❌ Ошибка проверки сессии:', error.message);
      store.delete('session');
    }
  }
}

/**
 * IPC Handlers - связь между Renderer и Main процессом
 */

// Авторизация
ipcMain.handle('auth:login', async (event, { username, password, twoFactorCode }) => {
  try {
    const payload = {
      username,
      password,
      hwid: await getHWID()
    };
    
    // Добавляем 2FA код, если он есть
    if (twoFactorCode) {
      payload.twoFactorCode = twoFactorCode;
    }
    
    const response = await axios.post(`${apiUrl}/api/auth/login`, payload);

    if (response.data.success) {
      // Сохраняем сессию
      const session = {
        token: response.data.token,
        username: response.data.username,
        role: response.data.role || 'player',
        expiresAt: Date.now() + (7 * 24 * 60 * 60 * 1000) // 7 дней
      };
      store.set('session', session);
      return { success: true, session };
    }
    
    // Если требуется 2FA, возвращаем это в UI
    if (response.data.requires2FA) {
      return { 
        success: false, 
        requires2FA: true,
        message: response.data.message || 'Код отправлен в Telegram. Введите его для входа.'
      };
    }

    return { success: false, message: response.data.message };
  } catch (error) {
    console.error('Login error:', error);
    return { success: false, message: error.message };
  }
});

// Регистрация
ipcMain.handle('auth:register', async (event, { username, password, email }) => {
  try {
    const response = await axios.post(`${apiUrl}/api/auth/register`, {
      username,
      password,
      email,
      hwid: await getHWID()
    });

    return {
      success: response.data.success,
      message: response.data.message
    };
  } catch (error) {
    console.error('Register error:', error);
    return { success: false, message: error.message };
  }
});

// Выход
ipcMain.handle('auth:logout', async () => {
  store.delete('session');
  return { success: true };
});

// Получение текущей сессии
ipcMain.handle('auth:getSession', async () => {
  return store.get('session', null);
});

// Window controls
ipcMain.handle('window:minimize', () => {
  if (mainWindow) mainWindow.minimize();
});

ipcMain.handle('window:maximize', () => {
  if (mainWindow) {
    if (mainWindow.isMaximized()) {
      mainWindow.unmaximize();
    } else {
      mainWindow.maximize();
    }
  }
});

ipcMain.handle('window:close', () => {
  if (mainWindow) mainWindow.close();
});

// Запуск Minecraft
ipcMain.handle('game:launch', async (event, { version, username }) => {
  try {
    // Вызываем Node.js API (он перенаправит на Java если нужно)
    const response = await axios.post(`${apiUrl}/api/game/launch`, {
      version,
      username
    });

    if (response.data.success) {
      return { success: true, message: 'Игра запущена!' };
    }

    return { success: false, message: response.data.message };
  } catch (error) {
    console.error('Launch error:', error);
    return { success: false, message: error.message };
  }
});

// Forum API
ipcMain.handle('forum:getPosts', async (event, { category, page = 1 }) => {
  try {
    const session = store.get('session');
    const response = await axios.get(`${apiUrl}/api/forum/posts`, {
      params: { category, page, limit: 20 },
      headers: session ? { 'Authorization': `Bearer ${session.token}` } : {}
    });
    return response.data;
  } catch (error) {
    console.error('Forum get posts error:', error);
    return { success: false, message: error.message };
  }
});

ipcMain.handle('forum:createPost', async (event, { category, title, content }) => {
  try {
    const session = store.get('session');
    if (!session) return { success: false, message: 'Не авторизован' };

    const response = await axios.post(`${apiUrl}/api/forum/posts`, {
      category,
      title,
      content
    }, {
      headers: { 'Authorization': `Bearer ${session.token}` }
    });
    return response.data;
  } catch (error) {
    console.error('Forum create post error:', error);
    return { success: false, message: error.message };
  }
});

// Shop API
ipcMain.handle('shop:getItems', async () => {
  try {
    const response = await axios.get(`${apiUrl}/api/shop/items`);
    return response.data;
  } catch (error) {
    console.error('Shop get items error:', error);
    return { success: false, message: error.message };
  }
});

ipcMain.handle('shop:purchase', async (event, { itemId }) => {
  try {
    const session = store.get('session');
    if (!session) return { success: false, message: 'Не авторизован' };

    const response = await axios.post(`${apiUrl}/api/shop/purchase`, {
      itemId
    }, {
      headers: { 'Authorization': `Bearer ${session.token}` }
    });
    return response.data;
  } catch (error) {
    console.error('Shop purchase error:', error);
    return { success: false, message: error.message };
  }
});

// Admin API
ipcMain.handle('admin:getUsers', async (event, { page = 1, search = '' }) => {
  try {
    const session = store.get('session');
    if (!session || !['admin', 'curator', 'owner'].includes(session.role)) {
      return { success: false, message: 'Нет прав доступа' };
    }

    const response = await axios.get(`${apiUrl}/api/admin/users`, {
      params: { page, search, limit: 50 },
      headers: { 'Authorization': `Bearer ${session.token}` }
    });
    return response.data;
  } catch (error) {
    console.error('Admin get users error:', error);
    return { success: false, message: error.message };
  }
});

ipcMain.handle('admin:banUser', async (event, { username, reason, duration }) => {
  try {
    const session = store.get('session');
    if (!session || !['admin', 'curator', 'owner'].includes(session.role)) {
      return { success: false, message: 'Нет прав доступа' };
    }

    const response = await axios.post(`${apiUrl}/api/admin/ban`, {
      username,
      reason,
      duration
    }, {
      headers: { 'Authorization': `Bearer ${session.token}` }
    });
    return response.data;
  } catch (error) {
    console.error('Admin ban user error:', error);
    return { success: false, message: error.message };
  }
});

/**
 * Получение HWID
 */
async function getHWID() {
  const os = require('os');
  const crypto = require('crypto');
  
  const cpus = os.cpus();
  const networkInterfaces = os.networkInterfaces();
  
  const cpuInfo = cpus[0].model + cpus[0].speed;
  const networkInfo = JSON.stringify(networkInterfaces);
  
  const hwid = crypto.createHash('sha256')
    .update(cpuInfo + networkInfo + os.hostname())
    .digest('hex');
  
  return hwid;
}

/**
 * App lifecycle
 */
app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (mainWindow === null) {
    createWindow();
  }
});

// Обработка некорректного завершения
process.on('uncaughtException', (error) => {
  console.error('Uncaught exception:', error);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled rejection at:', promise, 'reason:', reason);
});

