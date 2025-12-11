document.addEventListener('DOMContentLoaded', function() {
    const mainBtn = document.getElementById('main-btn');
    const serversBtn = document.getElementById('servers-btn');
    const wardrobeBtn = document.getElementById('wardrobe-btn');
    const notificationsBtn = document.getElementById('notifications-btn');
    const forumBtn = document.getElementById('forum-btn');
    const aboutBtn = document.getElementById('about-btn');
    const shopBtn = document.getElementById('shop-btn');
    const playBtn = document.getElementById('play-btn');
    const settingsBtn = document.getElementById('settings-btn-bottom');
    const pages = document.querySelectorAll('.page');
    const navItems = document.querySelectorAll('.nav-item');
    const alertBox = document.getElementById('alert-box');
    const alertMessage = document.getElementById('alert-message');

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

    shopBtn.addEventListener('click', () => {
        window.ipcRenderer.send('open-shop');
    });

    aboutBtn.addEventListener('click', () => {
        window.ipcRenderer.send('open-about');
    });

    forumBtn.addEventListener('click', () => {
        showAlert('Форум в разработке!', false);
    });

    playBtn.addEventListener('click', () => {
        // Логика запуска игры будет добавлена позже
        showAlert('Запуск игры...', false);
    });

    settingsBtn.addEventListener('click', () => {
        showAlert('Настройки в разработке!', false);
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
});
