const { contextBridge, ipcRenderer } = require('electron');
const { ipcRenderer: ipcRendererBetter } = require('electron-better-ipc');

// Раскрытие стандартного IPC (для базовых команд, таких как закрытие окна)
contextBridge.exposeInMainWorld('ipcRenderer', {
    send: (channel, data) => ipcRenderer.send(channel, data),
    on: (channel, func) => ipcRenderer.on(channel, (event, ...args) => func(...args)),
});

// Раскрытие electron-better-ipc (для обмена сложными данными, таких как прогресс)
contextBridge.exposeInMainWorld('ipcRendererBetter', ipcRendererBetter);