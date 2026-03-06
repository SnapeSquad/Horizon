# UI/UX Технический стек - Рекомендации для дизайнера

## 🎨 ВИЗУАЛЬНЫЙ СТИЛЬ

**Стиль:** "Dark Liquid Glass" (Frosty UI / iOS 26 Concept)  
**Целевая аудитория:** High-end Minecraft players (AAA-level experience)

---

## 1. ЦВЕТОВАЯ ПАЛИТРА И ГРАДИЕНТЫ

### UI Base Colors (Интерфейс)

**⚠️ Нейросеть должна строго следовать этим кодам:**

- **Background:** `#14141e` (Deep Navy) — Основной фон подложки
- **Surface (Glass Panels):** `rgba(30, 30, 45, 0.6)` + Backdrop Blur 50px
- **Borders:** `rgba(255, 255, 255, 0.1)` (Тонкие, едва заметные линии)
- **Primary Accent (Buttons/Links):** Linear Gradient to right: `#667eea` → `#764ba2` (Violet Glow)
- **Secondary Accent:** `#00f2fe` (Cyber Cyan) — для иконок и активных состояний
- **Text Main:** `#FFFFFF` (White)
- **Text Muted:** `#A0A0B0` (Cool Grey)

### Role Hierarchy Colors (Роли и Градиенты)

Эти цвета используются для **Никнеймов, Рамок аватарок и Плашек на форуме:**

| Роль | Цвет/Градиент | Эффект |
|------|---------------|--------|
| 👑 **Владелец (Owner)** | Dark Red Flow: `#8B0000` → `#FF0000` | Animated shimmering text, Bold |
| 🛡 **Куратор (Curator)** | Light Red Flow: `#FF4B4B` → `#FF9E9E` | Shimmering text, Bold + Italic |
| ⚔️ **Администратор (Admin)** | Light Red: `#FF6B6B` | No animation, Bold + Italic |
| 🟢 **Модератор (Moderator)** | Emerald Green: `#2ecc71` | Regular weight |
| 🔵 **Помощник (Helper)** | Sky Blue: `#3498db` | Italic |
| 💎 **Ulta (Donator)** | Lavender to Pink: `#a18cd1` → `#fdc2ed` | Bold, Italic, Underline |
| ⚡ **Prime (Donator)** | Cyan to Deep Blue: `#54daf4` → `#545ed6` | Bold, Italic |
| 🔥 **Boost (Donator)** | Gold to Wine: `#f6d14a` → `#862f51` | Italic |
| 👤 **Игрок (Player)** | Grey: `#B0B0B0` | Grey border |

---

## 2. ТИПОГРАФИКА

- **Headings / Roles / Buttons:** Font **Minecraft Unicode** (Pixelated but smooth)
- **Body Text / UI Elements:** Font **Inter** or **SF Pro Display** (Clean sans-serif)

---

## 3. СТРУКТУРА ЭКРАНОВ

### A. Auth & Registration (Вход и Регистрация)

**Layout:** Center floating modal on a blurred game background

**Fields:**
- Login
- Password (with Eye icon toggle)

**Effects:**
- Input fields: **no background**, only white bottom border that turns **Cyan (`#00f2fe`)** on focus
- Button "Log In": pulses with soft violet glow (gradient `#667eea` → `#764ba2`)

**2FA Mode:**
- When 2FA is triggered, form transforms into **6 distinct glass boxes** for digits
- Each box: glass panel with backdrop blur

**Recovery:**
- Button "Forgot Password?" leads to "Telegram Reset" flow
- Icon: Telegram + Text

---

### B. Main Dashboard (Главная)

**Left Sidebar:**
- Vertical glass strip
- Icons: Home, Store, Wardrobe, Forum, Settings
- Active icon glows `#00f2fe` (Cyber Cyan)

**Center Area:**
- **News Slider:** 4 large rectangular cards with parallax images
  - Text overlay at the bottom
  - Glass effect on cards
- **3D Character Preview:** Designated zone where player's skin (Steve/Alex) stands
  - 3D render with rotation controls

**Bottom Right:**
- Huge, wide button **"PLAY"**
- Gradient background (`#667eea` → `#764ba2`)
- Neon outer glow effect

---

### C. Store (Магазин)

**Tabs:** Sidebar categories: Cosmetics, Services, Currency, Bundles

**Top Bar:**
- User balance display: `[Coin Icon] 1,500 Horikov`
- Glass panel with backdrop blur

**Grid:** Cards with items

**Card Content:**
- Image of item
- Title
- Price
- Badges (floating tags in corners):
  - "SALE -20%" (Red)
  - "NEW" (Green)
  - "HIT" (Purple)

**Hover Effects:**
- Card scales up 5%
- "Try On" (Eye Icon) and "Buy" (Cart Icon) buttons appear
- Smooth transition

---

### D. Forum (Форум)

**Style:** Modernized XenForo list

**Row Item:**
- **Left:** 3D Head Render of the author (8x8 pixel style) inside a frame colored by their Role
- **Center:** 
  - Topic Title (Bold)
  - Author Nickname (Styled with Role Gradient)
- **Right:** Last reply info, Date

**Header:**
- Search bar (glass input)
- Notification Bell (Red dot if new alerts)

---

### E. Admin Panel (Web Interface)

**Style:** Shadcn UI adaptation. **Dark Mode only.**

**Specifics:**
- Clean data tables (glass panels)
- Status badges:
  - Banned = Red pill
  - Active = Green pill
- "Upload Cosmetic" area with dashed border for Drag & Drop
- Glass panels with backdrop blur
- Use color palette from section 1

---

## 4. ТЕХНИЧЕСКИЙ СТЕК

### Admin Panel: React + Shadcn UI + TypeScript

**Почему React + Shadcn UI:**
1. ✅ Shadcn UI построен на Tailwind CSS — легко кастомизировать под нашу палитру
2. ✅ Готовые компоненты — таблицы, формы, модальные окна
3. ✅ Компоненты копируются в проект — полный контроль над стилями
4. ✅ TypeScript — типобезопасность
5. ✅ Accessibility — компоненты на Radix UI (a11y из коробки)

**Зависимости:**
```json
{
  "framework": "React 18+",
  "ui-library": "Shadcn UI",
  "styling": "Tailwind CSS",
  "language": "TypeScript",
  "build-tool": "Vite или Next.js",
  "animations": "Framer Motion (для shimmering эффектов)"
}
```

### Launcher (Electron): React + Shadcn UI

**Рекомендация:** React + Shadcn UI для единообразия дизайна

---

## 5. КОМПОНЕНТЫ, КОТОРЫЕ НУЖНЫ

### Общие компоненты:
- 🎨 **Glass Panels** — с backdrop blur эффектом
- 📊 **Data Table** — для списка пользователей, косметики, новостей
- 📝 **Forms** — с bottom border эффектом (как в Auth)
- 🎨 **File Upload** — с dashed border для Drag & Drop
- 🔔 **Toast Notifications** — glass panels
- 🎭 **Modal/Dialog** — floating glass modals
- 🎯 **Tabs** — для навигации
- 🔍 **Search/Filter** — glass input fields
- 🏷️ **Badges** — для статусов и ролей (с градиентами)
- 🎨 **Gradient Buttons** — с violet glow эффектом
- 💫 **Shimmering Text** — для ролей (Owner, Curator)
- 🎭 **3D Character Preview** — для гардероба и главной

### Специфичные компоненты:
- **Role Badge** — с градиентами и эффектами (shimmering для Owner/Curator)
- **News Card** — с parallax эффектом
- **Store Item Card** — с hover эффектами
- **Forum Row** — с 3D head render и role-colored frame

---

## 6. АНИМАЦИИ И ЭФФЕКТЫ

### Обязательные эффекты:
1. **Backdrop Blur** — на всех glass panels (50px)
2. **Shimmering Animation** — для Owner и Curator ролей
3. **Pulse Glow** — для кнопки "Log In" и "PLAY"
4. **Hover Scale** — для карточек магазина (scale 1.05)
5. **Parallax** — для новостных карточек
6. **Smooth Transitions** — для всех интерактивных элементов (0.3s ease)

### Градиенты:
- Все градиенты должны быть **smooth** (не резкие переходы)
- Использовать CSS `linear-gradient` или `radial-gradient`

---

## 7. АДАПТИВНОСТЬ

- **Desktop** (основной) — 1920x1080 и выше
- **Tablet** (опционально) — 1024x768
- **Mobile** (опционально) — 375x667

---

## 8. СТРУКТУРА ПРОЕКТА (предложение)

```
admin-panel/
├── package.json
├── vite.config.ts
├── tailwind.config.js          # Кастомизация цветов из tech.md
├── components.json              # Конфигурация Shadcn UI
├── src/
│   ├── components/
│   │   ├── ui/                 # Shadcn UI компоненты (копируются)
│   │   ├── glass/             # Glass panels с backdrop blur
│   │   ├── roles/              # Role badges с градиентами
│   │   └── animations/         # Shimmering, pulse эффекты
│   ├── pages/
│   ├── lib/
│   │   ├── colors.ts          # Цветовая палитра из tech.md
│   │   └── gradients.ts       # Градиенты для ролей
│   └── App.tsx
└── public/
```

---

## 9. КОНФИГУРАЦИЯ TAILWIND CSS

```javascript
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      colors: {
        background: '#14141e',
        surface: 'rgba(30, 30, 45, 0.6)',
        border: 'rgba(255, 255, 255, 0.1)',
        primary: {
          start: '#667eea',
          end: '#764ba2',
        },
        accent: '#00f2fe',
        text: {
          main: '#FFFFFF',
          muted: '#A0A0B0',
        },
        // Role colors
        owner: { start: '#8B0000', end: '#FF0000' },
        curator: { start: '#FF4B4B', end: '#FF9E9E' },
        admin: '#FF6B6B',
        moderator: '#2ecc71',
        helper: '#3498db',
        // ... остальные роли
      },
      backdropBlur: {
        glass: '50px',
      },
      fontFamily: {
        minecraft: ['Minecraft Unicode', 'monospace'],
        sans: ['Inter', 'SF Pro Display', 'sans-serif'],
      },
    },
  },
}
```

---

## 10. ЧТО НУЖНО ОТ ДИЗАЙНЕРА

### Обязательно:
1. ✅ **Дизайн-система** в Figma/Sketch с точными цветами из tech.md
2. ✅ **Макеты всех экранов** (Auth, Dashboard, Store, Forum, Admin Panel)
3. ✅ **Компоненты** с указанием эффектов (shimmering, pulse, blur)
4. ✅ **Анимации** (описание или видео)
5. ✅ **Типографика** — примеры использования Minecraft Unicode и Inter

### Желательно:
- Интерактивный прототип (Figma)
- Примеры hover состояний
- Примеры активных/disabled состояний

---

## 11. БЫСТРЫЙ СТАРТ ДЛЯ РАЗРАБОТЧИКА

### 1. Установка Shadcn UI:
```bash
npx shadcn-ui@latest init
```

### 2. Добавление компонентов:
```bash
npx shadcn-ui@latest add button
npx shadcn-ui@latest add table
npx shadcn-ui@latest add dialog
npx shadcn-ui@latest add input
npx shadcn-ui@latest add badge
# и т.д.
```

### 3. Кастомизация:
- Обновить `tailwind.config.js` с цветами из tech.md
- Создать компоненты для glass panels
- Добавить анимации для shimmering и pulse

---

## 12. ПРИМЕРЫ КОДА

### Glass Panel:
```css
.glass-panel {
  background: rgba(30, 30, 45, 0.6);
  backdrop-filter: blur(50px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}
```

### Shimmering Text (Owner/Curator):
```css
@keyframes shimmer {
  0% { background-position: -200% center; }
  100% { background-position: 200% center; }
}

.shimmer-text {
  background: linear-gradient(90deg, #8B0000, #FF0000, #8B0000);
  background-size: 200% auto;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: shimmer 3s linear infinite;
}
```

### Gradient Button:
```css
.gradient-button {
  background: linear-gradient(to right, #667eea, #764ba2);
  box-shadow: 0 0 20px rgba(102, 126, 234, 0.5);
  transition: all 0.3s ease;
}

.gradient-button:hover {
  box-shadow: 0 0 30px rgba(102, 126, 234, 0.8);
  transform: scale(1.05);
}
```

---

## 13. ИТОГОВАЯ РЕКОМЕНДАЦИЯ

✅ **Admin Panel:** React + Shadcn UI + TypeScript + Tailwind CSS  
✅ **Launcher:** React + Shadcn UI (для единообразия)  
✅ **Стиль:** Dark Liquid Glass с точными цветами из tech.md  
✅ **Анимации:** Shimmering, Pulse, Backdrop Blur, Parallax  

---

## 14. КОНТАКТЫ

Если у дизайнера есть вопросы по техническим ограничениям или возможностям - можно обсудить перед началом работы.

**Важно:** Все цвета должны точно соответствовать кодам из tech.md. Нейросеть должна строго следовать этим кодам.
