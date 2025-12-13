document.addEventListener('DOMContentLoaded', function() {
    const mainBtn = document.getElementById('main-btn');
    const serversBtn = document.getElementById('servers-btn');
    const wardrobeBtn = document.getElementById('wardrobe-btn');
    const notificationsBtn = document.getElementById('notifications-btn');
    const forumBtn = document.getElementById('forum-btn');
    const aboutBtn = document.getElementById('about-btn');
    const shopBtn = document.getElementById('shop-btn');
    const logoutBtn = document.getElementById('logout-btn');
    const playBtn = document.getElementById('play-btn');
    const settingsBtn = document.getElementById('settings-btn-bottom');
    const pages = document.querySelectorAll('.page');
    const navItems = document.querySelectorAll('.nav-item');
    const alertBox = document.getElementById('alert-box');
    const alertMessage = document.getElementById('alert-message');

    // --- НАСТРОЙКИ ЗАПУСКА ---
    let gameVersion = '1.20.1';
    let ramInGB = 4;

    function showPage(pageId) {
        pages.forEach(page => {
            page.style.display = 'none';
        });
        document.getElementById(pageId).style.display = 'block';
    }

    function setActiveNav(navId) {
        navItems.forEach(item => {
            item.classList.remove('active');
        });
        document.getElementById(navId).classList.add('active');
    }

    function showAlert(message, isError = false) {
        alertMessage.textContent = message;
        alertBox.className = 'alert-box';
        alertBox.classList.add(isError ? 'error' : 'success');
        alertBox.style.display = 'block';
        setTimeout(() => {
            alertBox.style.display = 'none';
        }, 3000);
    }

    mainBtn.addEventListener('click', () => {
        showPage('main-page');
        setActiveNav('main-btn');
    });

    serversBtn.addEventListener('click', () => {
        showPage('servers-page');
        setActiveNav('servers-btn');
    });

    wardrobeBtn.addEventListener('click', () => {
        showPage('wardrobe-page');
        setActiveNav('wardrobe-btn');
    });

    notificationsBtn.addEventListener('click', () => {
        showPage('notifications-page');
        setActiveNav('notifications-btn');
    });

    logoutBtn.addEventListener('click', () => {
        window.ipc.send('logout');
    });

    shopBtn.addEventListener('click', () => {
        window.ipcRenderer.send('open-shop');
    });

    aboutBtn.addEventListener('click', () => {
        window.ipcRenderer.send('open-about');
    });

    forumBtn.addEventListener('click', () => {
        showAlert('Форум в разработке!', false);
    });

    const launchProgressContainer = document.getElementById('launch-progress-container');
    const launchStatusText = document.getElementById('launch-status-text');
    const progressBarInner = document.getElementById('progress-bar-inner');

    playBtn.addEventListener('click', () => {
        // Показываем прогресс-бар и скрываем кнопку
        playBtn.style.display = 'none';
        launchProgressContainer.style.display = 'flex';

        launchStatusText.textContent = 'Подготовка к запуску...';
        progressBarInner.style.width = '0%';

        window.ipc.invoke('launch-game', { version: gameVersion, ram: ramInGB * 1024 });
    });

    // Обработка прогресса запуска
    window.ipc.on('launch-progress', (data) => {
        launchStatusText.textContent = `${data.type}: ${data.task} (${Math.round((data.loaded / data.total) * 100)}%)`;
        progressBarInner.style.width = `${(data.loaded / data.total) * 100}%`;
    });

    window.ipc.on('launch-success', () => {
        launchStatusText.textContent = 'Игра запущена! Лаунчер скоро закроется.';
        progressBarInner.style.width = '100%';
    });

    window.ipc.on('launch-error', (errorMessage) => {
        showAlert(errorMessage, true);
        // Возвращаем кнопку
        playBtn.style.display = 'block';
        launchProgressContainer.style.display = 'none';
    });

    // --- ЛОГИКА МОДАЛЬНОГО ОКНА НАСТРОЕК ---
    const settingsModal = document.getElementById('settings-modal');
    const closeSettingsModalBtn = document.getElementById('close-settings-modal');
    const saveSettingsBtn = document.getElementById('save-settings-btn');
    const versionSelect = document.getElementById('version-select');
    const ramSlider = document.getElementById('ram-slider');
    const ramValue = document.getElementById('ram-value');

    function openSettingsModal() {
        // Устанавливаем текущие значения в модальном окне
        versionSelect.value = gameVersion;
        ramSlider.value = ramInGB;
        ramValue.textContent = `${ramInGB} ГБ`;
        settingsModal.style.display = 'block';
    }

    function closeSettingsModal() {
        settingsModal.style.display = 'none';
    }

    settingsBtn.addEventListener('click', openSettingsModal);
    closeSettingsModalBtn.addEventListener('click', closeSettingsModal);

    saveSettingsBtn.addEventListener('click', () => {
        gameVersion = versionSelect.value;
        ramInGB = ramSlider.value;
        showAlert('Настройки сохранены!', false);
        closeSettingsModal();
    });

    ramSlider.addEventListener('input', () => {
        ramValue.textContent = `${ramSlider.value} ГБ`;
    });

    // Handle user-login event from main process
    window.ipcRenderer.on('user-login', (username) => {
        const welcomeView = document.getElementById('welcome-view');
        const welcomeUsernameSpan = document.getElementById('welcome-username');
        const mainPage = document.getElementById('main-page');

        console.log(`Received user-login event for user: ${username}`);

        // Hide auth container and show the main page
        showPage('main-page');
        
        // Display welcome message
        if (welcomeView) welcomeView.style.display = 'flex';
        if (welcomeUsernameSpan) welcomeUsernameSpan.textContent = username;
    });

    // Initial page setup
    showPage('main-page');
    setActiveNav('main-btn');

    // --- ЛОГИКА СТАТУСА СЕРВЕРА ---
    async function updateServerStatus() {
        const motdEl = document.getElementById('server-motd');
        const versionEl = document.getElementById('server-version');
        const playersEl = document.getElementById('server-players');

        try {
            const response = await fetch('http://localhost:3000/api/server/status');
            const data = await response.json();

            if (data.online) {
                motdEl.textContent = data.motd;
                versionEl.textContent = data.version;
                playersEl.textContent = `${data.players.online}/${data.players.max}`;
            } else {
                motdEl.textContent = 'Сервер оффлайн';
                versionEl.textContent = 'N/A';
                playersEl.textContent = '0/0';
            }
        } catch (error) {
            console.error('Failed to fetch server status:', error);
            motdEl.textContent = 'Ошибка загрузки';
            versionEl.textContent = 'N/A';
            playersEl.textContent = 'N/A';
        }
    }

    // Обновляем статус при загрузке и при входе пользователя
    updateServerStatus();
    window.ipc.on('user-login', () => {
        updateServerStatus();
    });
});
