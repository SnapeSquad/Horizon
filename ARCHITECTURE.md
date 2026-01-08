# 🏗️ АРХИТЕКТУРА HORIZON LAUNCHER

## 🎯 ВАЖНО: МЫ НЕ УШЛИ ПОЛНОСТЬЮ С JAVA!

### ❌ ЧТО УБРАЛИ:
- JavaFX WebView (лаги!)
- HybridLauncherWindow
- JCEF (Chromium для JavaFX)
- Microsoft OAuth в JavaFX

### ✅ ЧТО ДОБАВИЛИ:
- Electron launcher (UI без лагов)
- React роутинг с защитой
- Сохранение сессий в Electron

### ✅ ЧТО ОСТАЛОСЬ В JAVA:
- **MinecraftLauncher** - запуск игры
- **AuthService** - проверка HWID
- **VersionManager** - загрузка версий
- **AssetManager** - ассеты/библиотеки
- **Вся логика запуска Minecraft!**

---

## 📊 АРХИТЕКТУРА (3 СЛОЯ):

```
┌─────────────────────────────────────────────────┐
│           СЛОЙ 1: ELECTRON (UI)                 │
│                                                 │
│  Роль: Фронтенд интерфейс                      │
│  Технологии: Electron + React + Vite           │
│  Порт: -                                        │
│                                                 │
│  Компоненты:                                    │
│  • main.js - главный процесс Electron          │
│  • preload.js - IPC мост                       │
│  • React UI - все страницы (Auth/Dashboard/...) │
│  • Window controls - свернуть/развернуть       │
│  • Session storage - electron-store            │
│                                                 │
│  Задачи:                                        │
│  ✅ Отображение UI                             │
│  ✅ Обработка кликов                           │
│  ✅ Сохранение сессий                          │
│  ✅ IPC коммуникация                           │
│  ❌ НЕ запускает игру!                         │
└─────────────────────────────────────────────────┘
                    ↓
              HTTP REST API
                    ↓
┌─────────────────────────────────────────────────┐
│        СЛОЙ 2: JAVA (Game Launcher)             │
│                                                 │
│  Роль: Запуск Minecraft + Бэкенд логика        │
│  Технологии: Java 21 + Maven                   │
│  Порт: 8080                                     │
│                                                 │
│  Компоненты:                                    │
│  • MinecraftLauncher.java - запуск процесса    │
│  • AuthService.java - проверка HWID/токенов    │
│  • VersionManager.java - управление версиями   │
│  • AssetManager.java - загрузка ассетов        │
│  • ProfileManager.java - профили запуска       │
│                                                 │
│  API Эндпоинты (нужно добавить):               │
│  POST /api/game/launch                          │
│  GET  /api/game/versions                        │
│  POST /api/auth/validate                        │
│  GET  /api/profile/:username                    │
│                                                 │
│  Задачи:                                        │
│  ✅ Запуск Minecraft процесса                  │
│  ✅ Загрузка ассетов/библиотек                 │
│  ✅ Проверка HWID                              │
│  ✅ Валидация токенов                          │
│  ❌ НЕ отображает UI!                          │
└─────────────────────────────────────────────────┘
                    ↓
              HTTP REST API
                    ↓
┌─────────────────────────────────────────────────┐
│       СЛОЙ 3: NODE.JS (API Server)              │
│                                                 │
│  Роль: Форум + Магазин + База данных           │
│  Технологии: Express + MongoDB/PostgreSQL      │
│  Порт: 3000                                     │
│                                                 │
│  Компоненты:                                    │
│  • server.js - Express сервер                  │
│  • Forum API - создание/чтение постов          │
│  • Shop API - покупки товаров                  │
│  • User API - управление пользователями        │
│  • Admin API - админ-панель                    │
│                                                 │
│  API Эндпоинты:                                 │
│  POST /api/auth/login                           │
│  POST /api/auth/register                        │
│  GET  /api/forum/posts                          │
│  POST /api/forum/posts                          │
│  GET  /api/shop/items                           │
│  POST /api/shop/purchase                        │
│  GET  /api/admin/users                          │
│  POST /api/admin/ban                            │
│                                                 │
│  Задачи:                                        │
│  ✅ Хранение данных (БД)                       │
│  ✅ Форум (темы/посты)                         │
│  ✅ Магазин (товары/покупки)                   │
│  ✅ Админ-панель                               │
│  ❌ НЕ запускает игру!                         │
└─────────────────────────────────────────────────┘
```

---

## 🔄 ПОТОК ДАННЫХ:

### 1. Запуск игры:
```
Пользователь нажимает "Играть"
    ↓
Electron UI (кнопка)
    ↓
IPC: electronAPI.launchGame({ version: "1.20.1", username: "Player" })
    ↓
main.js: HTTP POST → http://localhost:8080/api/game/launch
    ↓
Java: MinecraftLauncher.launch(version, username)
    ↓
Java: Загружает ассеты/библиотеки
    ↓
Java: spawn("java", ["-jar", "minecraft.jar", ...])
    ↓
Minecraft запущен! ✅
```

### 2. Авторизация:
```
Пользователь вводит логин/пароль
    ↓
React: AuthPage → кнопка "Войти"
    ↓
IPC: electronAPI.login({ username, password })
    ↓
main.js: HTTP POST → http://localhost:3000/api/auth/login
    ↓
Node.js: Проверка в БД → генерация JWT токена
    ↓
Node.js: HTTP POST → http://localhost:8080/api/auth/validate
    ↓
Java: Проверка HWID
    ↓
Ответ: { success: true, token, role }
    ↓
main.js: Сохраняет в electron-store
    ↓
React: Переход на Dashboard ✅
```

### 3. Создание поста на форуме:
```
Пользователь создает тему
    ↓
React: ForumPage → кнопка "Опубликовать"
    ↓
IPC: electronAPI.createForumPost({ category, title, content })
    ↓
main.js: HTTP POST → http://localhost:3000/api/forum/posts
    ↓
Node.js: Сохраняет в БД
    ↓
Ответ: { success: true, postId }
    ↓
React: Обновляет список тем ✅
```

---

## 🚀 ЗАПУСК ВСЕХ 3 СЛОЕВ:

### Терминал 1: React Dev Server
```powershell
cd C:\Users\skviz\Desktop\Horizon\horizon-ui
npm run dev
# Порт: 5173 или 5174 (если 5173 занят)
```

### Терминал 2: Node.js API Server
```powershell
cd C:\Users\skviz\Desktop\Horizon\api-server
npm start
# Порт: 3000
```

### Терминал 3: Java API Server
```powershell
cd C:\Users\skviz\Desktop\Horizon\launcher-java
mvn exec:java -Dexec.mainClass="com.horizon.launcher.Main"
# Порт: 8080
```

### Терминал 4: Electron Launcher
```powershell
cd C:\Users\skviz\Desktop\Horizon\electron-launcher
npm run dev
# Откроется окно UI
```

---

## 📝 ЧТО НУЖНО ДОДЕЛАТЬ:

### 1. Java API Controller
Создать: `launcher-java/src/main/java/com/horizon/launcher/api/ElectronApiController.java`

```java
package com.horizon.launcher.api;

import spark.Request;
import spark.Response;
import static spark.Spark.*;

public class ElectronApiController {
    public void setupRoutes() {
        // Запуск игры
        post("/api/game/launch", this::launchGame);
        
        // Список версий
        get("/api/game/versions", this::getVersions);
        
        // Валидация токена
        get("/api/auth/validate", this::validateToken);
    }
    
    private String launchGame(Request req, Response res) {
        // TODO: Вызвать MinecraftLauncher
        return "{ \"success\": true }";
    }
}
```

### 2. Node.js Forum API
Добавить в `api-server/server.js`:

```javascript
// Форум
app.get('/api/forum/posts', (req, res) => {
    // TODO: Получить из БД
});

app.post('/api/forum/posts', (req, res) => {
    // TODO: Сохранить в БД
});
```

### 3. Подключить Базу Данных
- PostgreSQL или MongoDB
- Таблицы: users, forum_posts, shop_items, purchases

---

## ✅ ЧТО УЖЕ РАБОТАЕТ:

| Компонент | Статус | Описание |
|-----------|--------|----------|
| Electron UI | ✅ | Отображается без лагов |
| React Router | ✅ | Навигация работает |
| Window Controls | ✅ | Свернуть/развернуть/закрыть |
| Session Storage | ✅ | Автологин после перезапуска |
| Sidebar | ✅ | Профиль, роли, навигация |
| Forum UI | ✅ | Категории, темы, посты (UI) |
| Shop UI | ✅ | Карточки товаров (UI) |
| Wardrobe UI | ✅ | Список предметов (UI) |
| Java MinecraftLauncher | ✅ | Код готов (нужен API) |
| Node.js API | ⚠️ | Структура есть, нужна реализация |

---

## 🎯 ИТОГ:

### Electron = ТОЛЬКО UI!
- Рендерит React компоненты
- Обрабатывает клики
- Сохраняет сессии
- **НЕ запускает игру!**

### Java = ТОЛЬКО Minecraft!
- Запускает игру
- Загружает ассеты
- Проверяет HWID
- **НЕ отображает UI!**

### Node.js = ТОЛЬКО API!
- Форум
- Магазин
- База данных
- **НЕ запускает игру и НЕ отображает UI!**

---

**ВЫВОД:** Мы НЕ УШЛИ с Java! Просто разделили ответственность:
- **UI** → Electron (без лагов!)
- **Game Launch** → Java (как и было!)
- **API** → Node.js (для форума/магазина)

Это **ПРАВИЛЬНАЯ АРХИТЕКТУРА** для лаунчера! 🎯

---

**ВЕРСИЯ:** 2.0.0  
**ДАТА:** 2026-01-08  
**СТАТУС:** 🟢 АРХИТЕКТУРА УТВЕРЖДЕНА

