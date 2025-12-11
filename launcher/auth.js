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
    const alertBox = document.getElementById('alert-box');
    const alertMessage = document.getElementById('alert-message');

    // Показать/скрыть формы
    function showLogin() {
        loginView.classList.add('active');
        registerView.classList.remove('active');
    }

    function showRegister() {
        loginView.classList.remove('active');
        registerView.classList.add('active');
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

    // Обработчик входа через IPC
    loginBtn.addEventListener('click', async () => {
        const username = loginUsernameInput.value;
        const password = loginPasswordInput.value;

        try {
            const response = await window.ipcRendererBetter.callMain('login-request', { username, password });
            if (response.statusCode === 200) {
                // Успешный вход, отправляем имя пользователя в основной процесс
                if (window.ipcRenderer) {
                    window.ipcRenderer.send('login-success', response.body.username);
                }
            } else {
                showAlert(response.body.message, true);
            }
        } catch (error) {
            console.error('IPC Login Error:', error);
            showAlert('Ошибка связи с основным процессом.', true);
        }
    });

    // Обработчик регистрации через IPC
    registerBtn.addEventListener('click', async () => {
        const username = registerUsernameInput.value;
        const password = registerPasswordInput.value;

        try {
            const response = await window.ipcRendererBetter.callMain('register-request', { username, password });
            if (response.statusCode === 200) {
                showAlert('Регистрация прошла успешно! Теперь вы можете войти.');
                showLogin();
            } else {
                showAlert(response.body.message, true);
            }
        } catch (error) {
            console.error('IPC Register Error:', error);
            showAlert('Ошибка связи с основным процессом.', true);
        }
    });

    // Инициализация
    showLogin();
});
