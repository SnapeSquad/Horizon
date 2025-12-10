document.addEventListener('DOMContentLoaded', function() {
    // --- ОБНОВЛЕННЫЕ ДАННЫЕ ГАРДЕРОБА С ИКОНКАМИ LUCIDE ---
    const wardrobeData = {
        head: [
            { name: "Корона", icon: "crown" },
            { name: "Шляпа", icon: "person-standing" },
            { name: "Очки", icon: "glasses" },
            { name: "Маска", icon: "theater" },
            { name: "Наушники", icon: "headphones" }
        ],
        body: [
            { name: "Толстовка", icon: "user-round" },
            { name: "Футболка", icon: "shirt" },
            { name: "Доспехи", icon: "shield" },
            { name: "Плащ", icon: "user-round-search" },
            { name: "Жилет", icon: "user-round-cog" }
        ],
        legs: [
            { name: "Джинсы", icon: "user-round" },
            { name: "Шорты", icon: "user-round" },
            { name: "Брюки", icon: "user-round" },
            { name: "Спорт. штаны", icon: "user-round" },
            { name: "Поножи", icon: "swords" }
        ],
        shoes: [
            { name: "Кроссовки", icon: "footprints" },
            { name: "Сапоги", icon: "footprints" },
            { name: "Сандалии", icon: "footprints" },
            { name: "Кеды", icon: "footprints" },
            { name: "Ботинки", icon: "footprints" }
        ],
        accessories: [
            { name: "Часы", icon: "clock" },
            { name: "Рюкзак", icon: "briefcase" },
            { name: "Крылья", icon: "feather" },
            { name: "Ожерелье", icon: "gem" },
            { name: "Пояс", icon: "gem" }
        ]
    };

    // Состояние приложения
    const state = {
        currentCategory: 'head',
        selectedItems: {},
        searchQuery: '',
        characterRotation: 0,
        activePage: 'main'
    };

    // Элементы DOM
    const elements = {
        mainBtn: document.getElementById('main-btn'),
        serversBtn: document.getElementById('servers-btn'),
        wardrobeBtn: document.getElementById('wardrobe-btn'),
        settingsBtn: document.getElementById('settings-btn-bottom'),
        forumBtn: document.getElementById('forum-btn'),
        aboutBtn: document.getElementById('about-btn'),
        shopBtn: document.getElementById('shop-btn'),
        playBtn: document.getElementById('play-btn'),
        closeBtn: document.querySelector('.close-btn'),
        modal: document.getElementById('wardrobe-modal'),
        pages: document.querySelectorAll('.page'),
        clothesList: document.getElementById('clothes-list'),
        searchInput: document.getElementById('search-clothes'),
        categoryTabs: document.querySelectorAll('.category-tab'),
        character: document.getElementById('character'),
        rotateLeft: document.getElementById('rotate-left'),
        rotateRight: document.getElementById('rotate-right'),
        leftButtons: document.querySelectorAll('.left-btn')
    };

    // Инициализация
    function init() {
        loadClothes('head');
        setupEventListeners();
        setActiveButton('main-btn');
        // Инициализируем вид персонажа при запуске
        updateCharacterAppearance();
    }

    // Настройка обработчиков событий
    function setupEventListeners() {
        // Переключение страниц
        elements.mainBtn.addEventListener('click', () => {
            showPage('main');
            setActiveButton('main-btn');
        });
        
        elements.serversBtn.addEventListener('click', () => {
            showPage('servers');
            setActiveButton('servers-btn');
        });
        
        // Гардероб открывает модальное окно сразу
        elements.wardrobeBtn.addEventListener('click', () => {
            showPage('wardrobe');
            setActiveButton('wardrobe-btn');
            openModal();
        });

        // Кнопка настроек
        elements.settingsBtn.addEventListener('click', () => {
            showMessage('Настройки', 'info');
        });

        // Кнопки в столбах
        elements.forumBtn.addEventListener('click', () => {
            showMessage('Форум', 'info');
        });
        
        elements.aboutBtn.addEventListener('click', () => {
            showMessage('О нас', 'info');
        });
        
        elements.shopBtn.addEventListener('click', () => {
            showMessage('Магазин', 'info');
        });

        // --- ОБНОВЛЕННАЯ ЛОГИКА КНОПКИ "ИГРАТЬ" ---
        let authData = {};
        window.electronAPI.onLoginSuccess((data) => {
            authData = data;
        });

        elements.playBtn.addEventListener('click', () => {
            const ram = document.getElementById('ram-slider').value;
            const version = document.getElementById('version-select').value;
            window.electronAPI.launchGame({ ram, version, ...authData });
        });

        // Закрытие модального окна гардероба
        elements.closeBtn.addEventListener('click', closeModal);
        
        elements.modal.addEventListener('click', (e) => {
            if (e.target === elements.modal) closeModal();
        });

        // Переключение категорий в гардеробе
        elements.categoryTabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const category = tab.dataset.category;
                switchCategory(category);
            });
        });

        // Поиск одежды
        const searchInput = document.getElementById('search-clothes');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                state.searchQuery = e.target.value.toLowerCase();
                filterClothes();
            });
        }

        // Вращение персонажа
        const rotateLeftBtn = document.getElementById('rotate-left');
        const rotateRightBtn = document.getElementById('rotate-right');
        if (rotateLeftBtn && rotateRightBtn) {
            rotateLeftBtn.addEventListener('click', () => rotateCharacter(-45));
            rotateRightBtn.addEventListener('click', () => rotateCharacter(45));
        }

        // --- УПРАВЛЕНИЕ ОКНОМ ---
        document.getElementById('minimize-btn')?.addEventListener('click', () => {
            window.electronAPI.minimizeWindow();
        });
        document.getElementById('maximize-btn')?.addEventListener('click', () => {
            window.electronAPI.maximizeWindow();
        });
        document.getElementById('close-btn-win')?.addEventListener('click', () => {
            window.electronAPI.closeWindow();
        });

        // Настройки
        const settingsModal = document.getElementById('settings-modal');
        const closeSettingsBtn = document.getElementById('close-settings-btn');
        const themeToggle = document.getElementById('theme-toggle');
        const ramSlider = document.getElementById('ram-slider');
        const ramValue = document.getElementById('ram-value');

        elements.settingsBtn.addEventListener('click', () => {
            settingsModal.classList.add('active');
        });

        closeSettingsBtn.addEventListener('click', () => {
            settingsModal.classList.remove('active');
        });

        themeToggle.addEventListener('change', () => {
            if (themeToggle.checked) {
                document.body.classList.add('dark-theme');
            } else {
                document.body.classList.remove('dark-theme');
            }
        });

        ramSlider.addEventListener('input', () => {
            ramValue.textContent = ramSlider.value;
        });
    }

    // Установить активную кнопку в левой панели
    function setActiveButton(buttonId) {
        elements.leftButtons.forEach(btn => {
            btn.classList.remove('active');
        });
        const button = document.getElementById(buttonId);
        if (button) {
            button.classList.add('active');
        }
    }

    // --- ОБНОВЛЕННАЯ ФУНКЦИЯ ОТОБРАЖЕНИЯ СТРАНИЦ С АНИМАЦИЕЙ ---
    function showPage(pageId) {
        if (state.activePage === pageId) return;

        const currentPage = document.getElementById(`${state.activePage}-page`);
        const nextPage = document.getElementById(`${pageId}-page`);

        if (currentPage) {
            currentPage.classList.remove('active');
        }

        state.activePage = pageId;

        setTimeout(() => {
            if (nextPage) {
                nextPage.classList.add('active');
            }
        }, 200);
    }

    // Открыть модальное окно гардероба
    function openModal() {
        elements.modal.classList.add('active');
        updateCharacterAppearance();
    }

    // Закрыть модальное окно гардероба
    function closeModal() {
        elements.modal.classList.remove('active');
    }

    // Загрузить одежду по категории
    function loadClothes(category) {
        state.currentCategory = category;
        const clothes = wardrobeData[category];
        
        elements.categoryTabs.forEach(tab => {
            tab.classList.toggle('active', tab.dataset.category === category);
        });
        
        elements.clothesList.innerHTML = '';
        
        clothes.forEach(item => {
            const isSelected = state.selectedItems[category]?.name === item.name;
            const clothingItem = document.createElement('div');
            clothingItem.className = `clothing-item glass-effect ${isSelected ? 'selected' : ''}`;
            clothingItem.dataset.name = item.name;
            clothingItem.dataset.category = category;
            
            clothingItem.innerHTML = `
                <i data-lucide="${item.icon}"></i>
                <span>${item.name}</span>
            `;
            
            clothingItem.addEventListener('click', () => selectClothing(item, category));
            elements.clothesList.appendChild(clothingItem);
        });

        lucide.createIcons();
    }

    // Переключить категорию
    function switchCategory(category) {
        loadClothes(category);
    }

    // Выбрать одежду
    function selectClothing(item, category) {
        if (state.selectedItems[category]?.name === item.name) {
            delete state.selectedItems[category];
            showMessage(`Снято: ${item.name}`);
        } else {
            state.selectedItems[category] = item;
            showMessage(`Выбрано: ${item.name}`);
        }
        
        updateClothingSelection(category);
        updateCharacterAppearance();
    }

    // Обновить выделение одежды в списке
    function updateClothingSelection(category) {
        const items = elements.clothesList.querySelectorAll('.clothing-item');
        items.forEach(item => {
            const isSelected = state.selectedItems[category]?.name === item.dataset.name;
            item.classList.toggle('selected', isSelected);
        });
    }

    // Фильтрация одежды по поиску
    function filterClothes() {
        const items = elements.clothesList.querySelectorAll('.clothing-item');
        let hasResults = false;
        
        items.forEach(item => {
            const matchesSearch = item.dataset.name.toLowerCase().includes(state.searchQuery);
            item.style.display = matchesSearch ? 'flex' : 'none';
            if (matchesSearch) hasResults = true;
        });
        
        if (!hasResults && state.searchQuery) {
            showError('Такого нету :(');
        }
    }

    // Вращение персонажа
    function rotateCharacter(angle) {
        state.characterRotation += angle;
        elements.character.style.transform = `rotateY(${state.characterRotation}deg)`;
    }

    // --- ВОССТАНОВЛЕННАЯ ФУНКЦИЯ ОБНОВЛЕНИЯ ПЕРСОНАЖА ---
    function updateCharacterAppearance() {
        const colors = {
            head: state.selectedItems.head ? '#FFD700' : '#8B4513',
            body: state.selectedItems.body ? '#1E90FF' : '#4682B4',
            legs: state.selectedItems.legs ? '#32CD32' : '#32CD32',
            shoes: state.selectedItems.shoes ? '#333' : '#333'
        };
        
        const head = document.querySelector('.character-head');
        const body = document.querySelector('.character-body');
        const legs = document.querySelector('.character-legs');
        const shoes = document.querySelector('.character-shoes');

        if (head) head.style.backgroundColor = colors.head;
        if (body) body.style.backgroundColor = colors.body;
        if (legs) legs.style.backgroundColor = colors.legs;
        if (shoes) shoes.style.backgroundColor = colors.shoes;
    }

    // Показать сообщение
    function showMessage(text, type = 'info') {
        const existingNotification = document.querySelector('.notification');
        if (existingNotification) existingNotification.remove();

        if (type === 'error') {
            showError(text);
            return;
        }
        
        const notification = document.createElement('div');
        notification.className = 'notification';
        notification.textContent = text;
        
        document.body.appendChild(notification);
        
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 3000);
    }

    // --- ОБНОВЛЕННАЯ ФУНКЦИЯ ОТОБРАЖЕНИЯ ОШИБОК С LUCIDE ---
    function showError(text) {
        const existingError = document.querySelector('.error-message');
        if (existingError) existingError.remove();
        
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.innerHTML = `
            <i data-lucide="alert-circle"></i>
            <span>${text}</span>
        `;
        
        document.body.appendChild(errorDiv);
        
        lucide.createIcons();
        
        setTimeout(() => {
            if (errorDiv.parentNode) {
                errorDiv.remove();
            }
        }, 3000);
    }

    // Запуск приложения
    init();
});
