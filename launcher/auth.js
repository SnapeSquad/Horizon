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
        const username = loginUsernameInput.value;
        const password = loginPasswordInput.value;
        try {
            const response = await window.ipc.invoke('login-request', { username, password });
            if (response.statusCode === 200) {
                window.ipc.send('login-success', response.body.username);
            } else {
                showAlert(response.body.message, true);
            }
        } catch (error) {
            showAlert(`Критическая ошибка: ${error.message}`, true);
        }
    });

    registerBtn.addEventListener('click', async () => {
        const username = registerUsernameInput.value;
        const password = registerPasswordInput.value;
        try {
            const response = await window.ipc.invoke('register-request', { username, password });
            if (response.statusCode === 200) {
                showAlert('Регистрация прошла успешно! Теперь вы можете войти.', false);
                toggleForms();
            } else {
                showAlert(response.body.message, true);
            }
        } catch (error) {
            showAlert(`Критическая ошибка: ${error.message}`, true);
        }
    });
});
