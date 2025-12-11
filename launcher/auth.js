document.addEventListener('DOMContentLoaded', () => {
    const loginView = document.getElementById('login-view');
    const registerView = document.getElementById('register-view');
    const welcomeView = document.getElementById('welcome-view');
    const showRegisterLink = document.getElementById('show-register');
    const showLoginLink = document.getElementById('show-login');
    const loginBtn = document.getElementById('login-btn');
    const registerBtn = document.getElementById('register-btn');
    const logoutBtn = document.getElementById('logout-btn');
    const loginUsernameInput = document.getElementById('login-username');
    const loginPasswordInput = document.getElementById('login-password');
    const registerUsernameInput = document.getElementById('register-username');
    const registerPasswordInput = document.getElementById('register-password');
    const welcomeUsernameSpan = document.getElementById('welcome-username');
    const alertBox = document.getElementById('alert-box');
    const alertMessage = document.getElementById('alert-message');

    const API_URL = 'http://localhost:3000/api/auth';

    // Показать/скрыть формы
    function showLogin() {
        loginView.style.display = 'block';
        registerView.style.display = 'none';
    }

    function showRegister() {
        loginView.style.display = 'none';
        registerView.style.display = 'block';
    }

    showRegisterLink.addEventListener('click', showRegister);
    showLoginLink.addEventListener('click', showLogin);

    // Показать алерт
    function showAlert(message, isError = false) {
        alertMessage.textContent = message;
        alertBox.className = 'alert-box';
        alertBox.classList.add(isError ? 'error' : 'success');
        alertBox.style.display = 'block';
        setTimeout(() => {
            alertBox.style.display = 'none';
        }, 3000);
    }

    // Обработчик входа
    loginBtn.addEventListener('click', async () => {
        const username = loginUsernameInput.value;
        const password = loginPasswordInput.value;

        try {
            const response = await fetch(`${API_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
            });
            const data = await response.json();
            if (response.ok) {
                loginSuccess(data.username);
            } else {
                showAlert(data.message, true);
            }
        } catch (error) {
            showAlert('Ошибка подключения к серверу.', true);
        }
    });

    // Обработчик регистрации
    registerBtn.addEventListener('click', async () => {
        const username = registerUsernameInput.value;
        const password = registerPasswordInput.value;

        try {
            const response = await fetch(`${API_URL}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
            });
            const data = await response.json();
            if (response.ok) {
                showAlert('Регистрация прошла успешно! Теперь вы можете войти.');
                registerView.classList.remove('active');
                loginView.classList.add('active');
            } else {
                showAlert(data.message, true);
            }
        } catch (error) {
            showAlert('Ошибка подключения к серверу.', true);
        }
    });

    // Обработчик выхода
    logoutBtn.addEventListener('click', () => {
        sessionStorage.removeItem('username');
        updateUI();
    });

    // Успешный вход
    function loginSuccess(username) {
        sessionStorage.setItem('username', username);
        updateUI();
        // Отправляем имя пользователя в основной процесс
        if (window.ipcRenderer) {
            window.ipcRenderer.send('login-success', username);
        }
    }

    // Обновление UI
    function updateUI() {
        const username = sessionStorage.getItem('username');
        const mainPage = document.getElementById('main-page');
        const authContainer = document.getElementById('auth-container');

        if (username) {
            authContainer.style.display = 'none';
            mainPage.style.display = 'block';
            welcomeView.style.display = 'flex';
            welcomeUsernameSpan.textContent = username;
        } else {
            authContainer.style.display = 'block';
            mainPage.style.display = 'none';
            welcomeView.style.display = 'none';
            showLogin();
        }
    }

    // Инициализация
    updateUI();
});
