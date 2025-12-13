const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('ipc', {
    // Односторонний канал (от рендерера к главному)
    send: (channel, data) => {
        ipcRenderer.send(channel, data);
    },
    // Двусторонний канал (рендер -> главный -> рендер)
    invoke: (channel, data) => {
        return ipcRenderer.invoke(channel, data);
    },
    // Односторонний канал (от главного к рендереру)
    on: (channel, func) => {
        ipcRenderer.on(channel, (event, ...args) => func(...args));
    }
});
