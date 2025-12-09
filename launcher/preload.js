const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  login: (data) => ipcRenderer.send('login', data),
  register: (data) => ipcRenderer.send('register', data),
  minimizeWindow: () => ipcRenderer.send('minimize-window'),
  maximizeWindow: () => ipcRenderer.send('maximize-window'),
  closeWindow: () => ipcRenderer.send('close-window'),
  launchGame: (data) => ipcRenderer.send('launch-game', data)
});

ipcRenderer.on('login-failed', (event, message) => {
    alert(`Ошибка входа: ${message}`);
});

ipcRenderer.on('register-success', (event, message) => {
    alert(message);
});

ipcRenderer.on('register-failed', (event, message) => {
    alert(`Ошибка регистрации: ${message}`);
});
