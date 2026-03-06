DESIGN SPECIFICATION: HORIZON LAUNCHER & ECOSYSTEM

Visual Style: "Dark Liquid Glass" (Frosty UI / iOS 26 Concept). Target Audience: High-end Minecraft players (AAA-level experience).
1. ЦВЕТОВАЯ ПАЛИТРА И ГРАДИЕНТЫ (COLOR SYSTEM)

Нейросеть должна строго следовать этим кодам.
UI Base Colors (Интерфейс):

    Background: #14141e (Deep Navy) — Основной фон подложки.

    Surface (Glass Panels): rgba(30, 30, 45, 0.6) + Backdrop Blur 50px.

    Borders: rgba(255, 255, 255, 0.1) (Tonke, едва заметные линии).

    Primary Accent (Buttons/Links): Linear Gradient to right, #667eea -> #764ba2 (Violet Glow).

    Secondary Accent: #00f2fe (Cyber Cyan) — для иконок и активных состояний.

    Text Main: #FFFFFF (White).

    Text Muted: #A0A0B0 (Cool Grey).

Role Hierarchy Colors (Роли и Градиенты):

Эти цвета используются для Никнеймов, Рамок аватарок и Плашек на форуме.

    👑 Владелец (Owner):

        Gradient: Dark Red Flow (#8B0000 to #FF0000).

        Effect: Animated shimmering text, Bold weight.

    🛡 Куратор (Curator):

        Gradient: Light Red Flow (#FF4B4B to #FF9E9E).

        Effect: Shimmering text, Bold + Italic.

    ⚔️ Администратор (Admin):

        Color: Light Red (#FF6B6B). No animation. Bold + Italic.

    🟢 Модератор (Moderator):

        Color: Emerald Green (#2ecc71). Regular weight.

    🔵 Помощник (Helper):

        Color: Sky Blue (#3498db). Italic.

    💎 Ulta (Donator):

        Gradient: Lavender to Pink (#a18cd1 -> #fdc2ed). Bold, Italic, Underline.

    ⚡ Prime (Donator):

        Gradient: Cyan to Deep Blue (#54daf4 -> #545ed6). Bold, Italic.

    🔥 Boost (Donator):

        Gradient: Gold to Wine (#f6d14a -> #862f51). Italic.

    👤 Игрок (Player):

        Color: Grey (#B0B0B0). Grey border.

2. ТИПОГРАФИКА (TYPOGRAPHY)

    Headings / Roles / Buttons: Font Minecraft Unicode (Pixelated but smooth).

    Body Text / UI Elements: Font Inter or SF Pro Display (Clean sans-serif).

3. СТРУКТУРА ЭКРАНОВ (SCREENS BREAKDOWN)
A. Auth & Registration (Вход и Регистрация)

    Layout: Center floating modal on a blurred game background.

    Fields: Login, Password (with Eye icon).

    Effects:

        Input fields have no background, only a white bottom border that turns Cyan on focus.

        Button "Log In" pulses with a soft violet glow.

    2FA Mode:

        When 2FA is triggered, the form transforms into 6 distinct glass boxes for digits.

    Recovery:

        Button "Forgot Password?" leads to a "Telegram Reset" flow (Icon of Telegram + Text).

B. Main Dashboard (Главная)

    Left Sidebar: Vertical glass strip. Icons: Home, Store, Wardrobe, Forum, Settings. Active icon glows #00f2fe.

    Center Area:

        News Slider: 4 large rectangular cards with parallax images. Text overlay at the bottom.

        3D Character Preview: A designated zone where the player's skin (Steve/Alex) stands.

    Bottom Right: Huge, wide button "PLAY". Gradient background. Neon outer glow.

C. Store (Магазин)

    Tabs: Sidebar categories: Cosmetics, Services, Currency, Bundles.

    Top Bar: User balance display: [Coin Icon] 1,500 Horikov.

    Grid: Cards with items.

        Card Content: Image of item, Title, Price, Badges.

        Badges: Floating tags in corners: "SALE -20%" (Red), "NEW" (Green), "HIT" (Purple).

        Hover: Card scales up 5%, "Try On" (Eye Icon) and "Buy" (Cart Icon) buttons appear.

D. Forum (Форум)

    Style: Modernized XenForo list.

    Row Item:

        Left: 3D Head Render of the author (8x8 pixel style) inside a frame colored by their Role.

        Center: Topic Title (Bold), Author Nickname (Styled with Role Gradient).

        Right: Last reply info, Date.

    Header: Search bar and Notification Bell (Red dot if new alerts).

E. Admin Panel (Web Interface)

    Style: Shadcn UI adaptation. Dark Mode only.

    Specifics:

        Clean data tables.

        Status badges (Banned = Red pill, Active = Green pill).

        "Upload Cosmetic" area with dashed border for Drag & Drop.