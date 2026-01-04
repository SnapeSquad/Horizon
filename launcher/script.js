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
        window.ipc.send('open-shop');
    });

    aboutBtn.addEventListener('click', () => {
        window.ipc.send('open-about');
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
    window.ipc.on('user-login', (username) => {
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
    
    let currentUsername = null;
    window.ipc.on('user-login', (username) => {
        currentUsername = username;
        updateServerStatus();
        loadNotifications();
        check2FAStatus();
    });

    // --- ЛОГИКА УВЕДОМЛЕНИЙ И 2FA ---
    async function loadNotifications() {
        const container = document.getElementById('notifications-container');
        if (!container) return;

        container.innerHTML = '';
        
        // Первое уведомление о необходимости настроить 2FA
        const notification = document.createElement('div');
        notification.className = 'notification-item';
        notification.innerHTML = `
            <div class="notification-icon">🔒</div>
            <div class="notification-content">
                <h3>Защитите свой аккаунт</h3>
                <p>Рекомендуем настроить двухфакторную аутентификацию для дополнительной защиты вашего аккаунта.</p>
            </div>
            <button class="notification-action" onclick="showPage('notifications-page'); setActiveNav('notifications-btn');">Настроить</button>
        `;
        container.appendChild(notification);
    }

    async function check2FAStatus() {
        if (!currentUsername) return;
        
        try {
            const response = await window.ipc.invoke('get-2fa-status', currentUsername);
            if (response && response.body && response.body.success) {
                update2FAStatus(response.body);
            }
        } catch (error) {
            console.error('Failed to get 2FA status:', error);
        }
    }

    function update2FAStatus(status) {
        const statusEl = document.getElementById('2fa-status');
        if (!statusEl) return;

        if (status.enabled) {
            statusEl.innerHTML = `
                <div class="tfa-status-enabled">
                    <i class="fas fa-shield-alt"></i>
                    <span>2FA включена</span>
                    ${status.hasTelegram ? '<span class="tfa-type">Telegram</span>' : ''}
                    ${status.hasGoogle ? '<span class="tfa-type">Google Authenticator</span>' : ''}
                </div>
            `;
        } else {
            statusEl.innerHTML = `
                <div class="tfa-status-disabled">
                    <i class="fas fa-shield-alt"></i>
                    <span>2FA не настроена</span>
                </div>
            `;
        }
    }

    // Обработчики кнопок 2FA
    document.getElementById('telegram-2fa-btn')?.addEventListener('click', async () => {
        if (!currentUsername) return;
        
        const btn = document.getElementById('telegram-2fa-btn');
        const originalText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Настройка...';
        
        try {
            const response = await window.ipc.invoke('setup-telegram-2fa', currentUsername);
            if (response && response.body && response.body.success) {
                const botName = response.body.botUsername ? `@${response.body.botUsername}` : 'боту';
                showAlert(`Отправьте код "${response.body.linkCode}" ${botName} в Telegram для привязки 2FA. Код действителен 10 минут.`, false);
                
                // Периодически проверяем статус 2FA после привязки
                const checkInterval = setInterval(async () => {
                    await check2FAStatus();
                    const statusEl = document.getElementById('2fa-status');
                    if (statusEl && statusEl.querySelector('.tfa-status-enabled')) {
                        clearInterval(checkInterval);
                        showAlert('Telegram 2FA успешно привязана!', false);
                    }
                }, 2000);
                
                // Останавливаем проверку через 2 минуты
                setTimeout(() => clearInterval(checkInterval), 120000);
            } else {
                showAlert(response?.body?.message || 'Ошибка настройки Telegram 2FA', true);
            }
        } catch (error) {
            console.error('Telegram 2FA setup error:', error);
            showAlert('Ошибка подключения к серверу. Убедитесь, что API-сервер запущен.', true);
        } finally {
            btn.disabled = false;
            btn.innerHTML = originalText;
        }
    });

    document.getElementById('google-2fa-btn')?.addEventListener('click', async () => {
        if (!currentUsername) return;
        
        const btn = document.getElementById('google-2fa-btn');
        const setupContainer = document.getElementById('2fa-setup-container');
        if (!setupContainer) return;

        const originalText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Настройка...';

        try {
            const response = await window.ipc.invoke('setup-google-2fa', currentUsername);
            if (response && response.body && response.body.success) {
                setupContainer.innerHTML = `
                    <div class="tfa-setup">
                        <h3>Настройка Google Authenticator</h3>
                        <p>Отсканируйте QR-код в приложении Google Authenticator:</p>
                        <img src="${response.body.qrCode}" alt="QR Code" style="max-width: 300px; margin: 20px 0; border: 2px solid rgba(255,255,255,0.1); border-radius: 8px;">
                        <p>Или введите ключ вручную:</p>
                        <code style="background: rgba(0,0,0,0.3); padding: 10px; display: block; margin: 10px 0; word-break: break-all; font-family: monospace;">${response.body.manualEntryKey}</code>
                        <input type="text" id="google-2fa-code" placeholder="Введите 6-значный код для подтверждения" maxlength="6" style="width: 100%; padding: 10px; margin: 10px 0; text-align: center; letter-spacing: 4px; font-size: 18px; font-weight: 600;">
                        <button id="verify-google-2fa-btn" style="width: 100%; padding: 10px; margin-top: 10px; background: #e94560; border: none; border-radius: 8px; color: white; font-weight: 600; cursor: pointer;">Подтвердить</button>
                        <button id="cancel-google-2fa-btn" style="width: 100%; padding: 10px; margin-top: 10px; background: rgba(255,255,255,0.1); border: none; border-radius: 8px; color: white; cursor: pointer;">Отмена</button>
                    </div>
                `;
                setupContainer.style.display = 'block';

                // Обработчик подтверждения
                document.getElementById('verify-google-2fa-btn')?.addEventListener('click', async () => {
                    const code = document.getElementById('google-2fa-code')?.value;
                    if (!code || code.length !== 6) {
                        showAlert('Введите 6-значный код', true);
                        return;
                    }

                    const verifyBtn = document.getElementById('verify-google-2fa-btn');
                    verifyBtn.disabled = true;
                    verifyBtn.textContent = 'Проверка...';

                    try {
                        const verifyResponse = await window.ipc.invoke('verify-google-2fa', currentUsername, code);
                        if (verifyResponse && verifyResponse.body && verifyResponse.body.success) {
                            showAlert('Google Authenticator 2FA успешно включена!', false);
                            setupContainer.style.display = 'none';
                            await check2FAStatus();
                        } else {
                            showAlert(verifyResponse?.body?.message || 'Неверный код. Попробуйте снова.', true);
                            verifyBtn.disabled = false;
                            verifyBtn.textContent = 'Подтвердить';
                        }
                    } catch (error) {
                        console.error('Google 2FA verify error:', error);
                        showAlert('Ошибка проверки кода. Убедитесь, что API-сервер запущен.', true);
                        verifyBtn.disabled = false;
                        verifyBtn.textContent = 'Подтвердить';
                    }
                });

                // Обработчик отмены
                document.getElementById('cancel-google-2fa-btn')?.addEventListener('click', () => {
                    setupContainer.style.display = 'none';
                });
            } else {
                showAlert(response?.body?.message || 'Ошибка настройки Google Authenticator', true);
            }
        } catch (error) {
            console.error('Google 2FA setup error:', error);
            showAlert('Ошибка подключения к серверу. Убедитесь, что API-сервер запущен.', true);
        } finally {
            btn.disabled = false;
            btn.innerHTML = originalText;
        }
    });
});
