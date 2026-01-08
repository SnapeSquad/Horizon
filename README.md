# 🚀 HORIZON LAUNCHER 2.0

Minecraft лаунчер с современным UI на Electron + React.

---

## ⚡ БЫСТРЫЙ СТАРТ

### Windows (ОДИН КЛИК!):

```powershell
start.bat
```

**ВСЁ!** Скрипт автоматически:
- Установит зависимости (если нужно)
- Запустит API Server (порт 3000)
- Запустит React UI (порт 5173/5174)
- Откроет Electron Launcher

---

### Linux/Mac:

```bash
npm run start
```

Или вручную:
```bash
# Установка зависимостей
npm run install:all

# Запуск всего сразу
npm run dev
```

---

## 📁 СТРУКТУРА ПРОЕКТА

```
Horizon/
├── api-server/              # Node.js API (порт 3000)
│   ├── server.js           - Express сервер
│   └── package.json
│
├── horizon-ui/              # React UI (порт 5173/5174)
│   ├── src/app/            - Компоненты React
│   └── package.json
│
├── electron-launcher/       # Electron App
│   ├── main.js             - Главный процесс
│   ├── preload.js          - IPC мост
│   └── package.json
│
├── start.bat                # Автозапуск для Windows
├── start.js                 # Автозапуск для Node.js
└── package.json             # Корневой package.json
```

---

## 🎯 АРХИТЕКТУРА

```
Electron UI (фронтенд)
      ↓ HTTP
Node.js API (бэкенд)
      ↓ DB
База данных
```

**Примечание:** Java launcher оставлен для запуска Minecraft, но не используется для UI!

---

## 🛠️ КОМАНДЫ

| Команда | Описание |
|---------|----------|
| `npm run start` | Запустить всё |
| `npm run dev` | Запустить в dev режиме |
| `npm run install:all` | Установить все зависимости |
| `npm run dev:api` | Только API Server |
| `npm run dev:ui` | Только React UI |
| `npm run dev:electron` | Только Electron |

---

## ✅ СИСТЕМНЫЕ ТРЕБОВАНИЯ

- **Node.js** 18+ ([скачать](https://nodejs.org/))
- **npm** 9+ (идет с Node.js)
- **Windows** 10/11 или Linux/Mac

---

## 🎮 ИСПОЛЬЗОВАНИЕ

1. Запусти `start.bat`
2. Дождись открытия окна Electron
3. Зарегистрируйся (вкладка "Регистрация")
4. Войди (вкладка "Вход")
5. Готово! Используй лаунчер!

---

## 📚 ДОКУМЕНТАЦИЯ

- `ARCHITECTURE.md` - Полная архитектура проекта
- `QUICK_START_GUIDE.md` - Подробная инструкция
- `FINAL_REPORT.md` - Финальный отчет разработки

---

## 🐛 ПРОБЛЕМЫ?

### "npm not found"
Установи Node.js: https://nodejs.org/

### "Port already in use"
Закрой другие приложения на портах 3000, 5173, 5174.

### "React UI не загружается"
Подожди 10 секунд после запуска `start.bat`.

---

## 👥 КОМАНДА

Разработка: Horizon Team  
Версия: 2.0.0  
Дата: 2026-01-08

---

## 📄 ЛИЦЕНЗИЯ

MIT License - свободное использование

---

**ВАЖНО:** Этот лаунчер использует Electron для UI и Node.js для API. Java launcher оставлен только для запуска самой игры Minecraft!
