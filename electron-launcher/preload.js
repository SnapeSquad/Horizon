const { contextBridge, ipcRenderer } = require('electron');

/**
 * Preload script - безопасный мост между Renderer и Main процессом
 * Expose только необходимые API в window.electronAPI
 */

contextBridge.exposeInMainWorld('electronAPI', {
  // Авторизация
  login: (credentials) => ipcRenderer.invoke('auth:login', credentials),
  register: (data) => ipcRenderer.invoke('auth:register', data),
  logout: () => ipcRenderer.invoke('auth:logout'),
  getSession: () => ipcRenderer.invoke('auth:getSession'),
  
  // Автологин listener
  onAutoLogin: (callback) => {
    ipcRenderer.on('auto-login', (event, session) => callback(session));
  },
  
  // Window controls
  minimizeWindow: () => ipcRenderer.invoke('window:minimize'),
  maximizeWindow: () => ipcRenderer.invoke('window:maximize'),
  closeWindow: () => ipcRenderer.invoke('window:close'),
  
  // Game
  launchGame: (data) => ipcRenderer.invoke('game:launch', data),
  
  // Forum
  getForumPosts: (params) => ipcRenderer.invoke('forum:getPosts', params),
  createForumPost: (data) => ipcRenderer.invoke('forum:createPost', data),
  
  // Shop
  getShopItems: () => ipcRenderer.invoke('shop:getItems'),
  purchaseItem: (data) => ipcRenderer.invoke('shop:purchase', data),
  
  // Admin
  getUsers: (params) => ipcRenderer.invoke('admin:getUsers', params),
  banUser: (data) => ipcRenderer.invoke('admin:banUser', data),
  
  // Platform info
  platform: process.platform,
  isElectron: true
});

console.log('Preload script loaded successfully');

