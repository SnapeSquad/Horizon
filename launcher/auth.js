document.addEventListener('DOMContentLoaded', () => {
    const loginView = document.getElementById('login-view');
    const registerView = document.getElementById('register-view');
    const showRegisterLink = document.getElementById('show-register');
    const showLoginLink = document.getElementById('show-login');
    const loginBtn = document.getElementById('login-btn');
    const registerBtn = document.getElementById('register-btn');
    const loginUsernameInput = document.getElementById('login-username');
    const loginPasswordInput = document.getElementById('login-password');
    const registerUsernameInput = document.getElementById('register-username');
    const registerPasswordInput = document.getElementById('register-password');
    const login2FACodeInput = document.getElementById('login-2fa-code');
    const login2FAContainer = document.getElementById('2fa-code-container');
    const alertBox = document.getElementById('alert-box');
    const alertMessage = document.getElementById('alert-message');
    let requires2FA = false;

    function showAlert(message, isError = false) {
        alertMessage.textContent = message;
        alertBox.className = 'alert-box';
        alertBox.classList.add(isError ? 'error' : 'success');
        alertBox.style.display = 'block';
        setTimeout(() => { alertBox.style.display = 'none'; }, 4000);
    }

    function toggleForms() {
        loginView.classList.toggle('active');
        registerView.classList.toggle('active');
    }

    showRegisterLink.addEventListener('click', toggleForms);
    showLoginLink.addEventListener('click', toggleForms);

    loginBtn.addEventListener('click', async () => {
        const username = loginUsernameInput.value.trim();
        const password = loginPasswordInput.value;
        const twoFactorCode = login2FACodeInput ? login2FACodeInput.value : null;
        
        if (!username || !password) {
            showAlert('Пожалуйста, введите имя пользователя и пароль', true);
            return;
        }
        
        if (requires2FA && (!twoFactorCode || twoFactorCode.length !== 6)) {
            showAlert('Пожалуйста, введите 6-значный код 2FA', true);
            return;
        }
        
        try {
            const response = await window.ipc.invoke('login-request', { username, password, twoFactorCode });
            if (response && response.statusCode === 200) {
                if (response.body && response.body.requires2FA) {
                    // Требуется 2FA код
                    requires2FA = true;
                    if (login2FAContainer) login2FAContainer.style.display = 'block';
                    showAlert('Введите код двухфакторной аутентификации', false);
                    return;
                } else if (response.body && response.body.success) {
                    window.ipc.send('login-success', response.body.username);
                } else {
                    showAlert(response?.body?.message || 'Ошибка входа. Проверьте правильность данных.', true);
                }
            } else {
                showAlert(response?.body?.message || 'Ошибка входа. Проверьте правильность данных.', true);
            }
        } catch (error) {
            let errorMessage = 'Ошибка подключения к серверу';
            if (error.message.includes('Network error')) {
                errorMessage = 'Не удалось подключиться к серверу. Убедитесь, что API-сервер запущен.';
            } else if (error.message.includes('Invalid JSON')) {
                errorMessage = 'Сервер вернул неверный ответ. Попробуйте позже.';
            } else {
                errorMessage = `Ошибка: ${error.message}`;
            }
            showAlert(errorMessage, true);
        }
    });

    registerBtn.addEventListener('click', async () => {
        const username = registerUsernameInput.value.trim();
        const password = registerPasswordInput.value;
        
        if (!username || !password) {
            showAlert('Пожалуйста, введите имя пользователя и пароль', true);
            return;
        }
        
        if (username.length < 3) {
            showAlert('Имя пользователя должно содержать минимум 3 символа', true);
            return;
        }
        
        if (password.length < 4) {
            showAlert('Пароль должен содержать минимум 4 символа', true);
            return;
        }
        
        try {
            const response = await window.ipc.invoke('register-request', { username, password });
            if (response && response.statusCode === 200 && response.body && response.body.success) {
                showAlert('Регистрация прошла успешно! Теперь вы можете войти.', false);
                toggleForms();
            } else {
                showAlert(response?.body?.message || 'Ошибка регистрации. Попробуйте другое имя пользователя.', true);
            }
        } catch (error) {
            let errorMessage = 'Ошибка подключения к серверу';
            if (error.message.includes('Network error')) {
                errorMessage = 'Не удалось подключиться к серверу. Убедитесь, что API-сервер запущен.';
            } else if (error.message.includes('Invalid JSON')) {
                errorMessage = 'Сервер вернул неверный ответ. Попробуйте позже.';
            } else {
                errorMessage = `Ошибка: ${error.message}`;
            }
            showAlert(errorMessage, true);
        }
    });
});
