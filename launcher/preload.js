const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  login: (data) => ipcRenderer.send('login', data),
  register: (data) => ipcRenderer.send('register', data),
  minimizeWindow: () => ipcRenderer.send('minimize-window'),
  maximizeWindow: () => ipcRenderer.send('maximize-window'),
  closeWindow: () => ipcRenderer.send('close-window'),
  launchGame: (data) => ipcRenderer.send('launch-game', data),
  onLoginSuccess: (callback) => ipcRenderer.on('login-success', (event, data) => callback(data))
});

function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `<i data-lucide="${type === 'success' ? 'check-circle' : 'alert-circle'}"></i><span>${message}</span>`;
    document.body.appendChild(notification);
    lucide.createIcons();
    setTimeout(() => notification.remove(), 4000);
}

ipcRenderer.on('login-failed', (event, message) => {
    showNotification(message, 'error');
});

ipcRenderer.on('register-success', (event, message) => {
    showNotification(message, 'success');
});

ipcRenderer.on('register-failed', (event, message) => {
    showNotification(message, 'error');
});
