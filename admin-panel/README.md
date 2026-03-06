# Horizon Admin Panel - Dark Liquid Glass UI

Полная реализация UI/UX дизайна в стиле "Dark Liquid Glass" согласно техническому заданию.

## 🎨 Особенности

- ✅ **Dark Liquid Glass** стиль с backdrop blur эффектами
- ✅ Точная цветовая палитра из tech.md
- ✅ Shimmering анимации для Owner/Curator ролей
- ✅ Glass panels с blur 50px
- ✅ Gradient кнопки с pulse эффектом
- ✅ Role badges с градиентами
- ✅ Все экраны из ТЗ (Auth, Dashboard, Store, Forum, Admin Panel)

## 🚀 Установка

```bash
npm install
```

## 🏃 Запуск

```bash
npm run dev
```

Откройте http://localhost:5173

## 📁 Структура

```
src/
├── components/
│   ├── ui/              # Базовые компоненты (Glass Panel, Gradient Button, Role Badge)
│   ├── auth/            # Форма авторизации с 2FA
│   ├── dashboard/       # Главная страница с новостями и 3D превью
│   ├── store/           # Магазин с карточками товаров
│   ├── forum/           # Форум с ролями и 3D heads
│   └── admin/           # Админ-панель на Shadcn UI
├── lib/
│   ├── colors.ts        # Цветовая палитра из tech.md
│   └── utils.ts         # Утилиты
└── App.tsx              # Главный компонент
```

## 🎯 Компоненты

### Glass Panel
Компонент с backdrop blur эффектом для всех панелей.

### Gradient Button
Кнопка с фиолетовым градиентом и pulse эффектом.

### Role Badge
Бейдж роли с градиентами и shimmering анимацией для Owner/Curator.

## 🎨 Цвета

Все цвета строго соответствуют tech.md:
- Background: `#14141e`
- Surface: `rgba(30, 30, 45, 0.6)` + blur 50px
- Primary Gradient: `#667eea` → `#764ba2`
- Accent: `#00f2fe`

## 📝 TODO

- [ ] Добавить реальную интеграцию с API
- [ ] Реализовать 3D превью персонажа
- [ ] Добавить parallax эффект для новостей
- [ ] Реализовать drag & drop для загрузки файлов
- [ ] Добавить больше анимаций
