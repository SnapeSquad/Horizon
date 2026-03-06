document.addEventListener('DOMContentLoaded', function() {
    // Данные гардероба
    const wardrobeData = {
        head: [
            { name: "Шлем", icon: "fas fa-helmet-battle" },
            { name: "Шляпа", icon: "fas fa-hat-cowboy" },
            { name: "Кепка", icon: "fas fa-baseball-ball" },
            { name: "Очки", icon: "fas fa-glasses" },
            { name: "Маска", icon: "fas fa-mask" }
        ],
        body: [
            { name: "Куртка", icon: "fas fa-jacket" },
            { name: "Футболка", icon: "fas fa-tshirt" },
            { name: "Доспехи", icon: "fas fa-shield-alt" },
            { name: "Плащ", icon: "fas fa-vest" },
            { name: "Жилет", icon: "fas fa-vest-patches" }
        ],
        legs: [
            { name: "Джинсы", icon: "fas fa-jeans" },
            { name: "Шорты", icon: "fas fa-swimming-pool" },
            { name: "Юбка", icon: "fas fa-female" },
            { name: "Штаны", icon: "fas fa-hat-cowboy-side" },
            { name: "Поножи", icon: "fas fa-shield-virus" }
        ],
        shoes: [
            { name: "Кроссовки", icon: "fas fa-walking" },
            { name: "Сапоги", icon: "fas fa-boot" },
            { name: "Сандалии", icon: "fas fa-shoe-prints" },
            { name: "Тапочки", icon: "fas fa-socks" },
            { name: "Лапти", icon: "fas fa-haykal" }
        ],
        accessories: [
            { name: "Часы", icon: "fas fa-clock" },
            { name: "Рюкзак", icon: "fas fa-backpack" },
            { name: "Крылья", icon: "fas fa-feather-alt" },
            { name: "Хвост", icon: "fas fa-paw" },
            { name: "Пояс", icon: "fas fa-grip-lines" }
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

        // Кнопка ИГРАТЬ
        elements.playBtn.addEventListener('click', () => {
            showMessage('Запуск игры...', 'success');
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
        elements.searchInput.addEventListener('input', (e) => {
            state.searchQuery = e.target.value.toLowerCase();
            filterClothes();
        });

        // Вращение персонажа
        elements.rotateLeft.addEventListener('click', () => rotateCharacter(-45));
        elements.rotateRight.addEventListener('click', () => rotateCharacter(45));
    }

    // Установить активную кнопку в левой панели
    function setActiveButton(buttonId) {
        elements.leftButtons.forEach(btn => {
            btn.classList.remove('active');
        });
        document.getElementById(buttonId).classList.add('active');
    }

    // Показать страницу
    function showPage(pageId) {
        state.activePage = pageId;
        elements.pages.forEach(page => page.classList.remove('active'));
        document.getElementById(`${pageId}-page`).classList.add('active');
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
        
        // Обновить активную вкладку
        elements.categoryTabs.forEach(tab => {
            tab.classList.toggle('active', tab.dataset.category === category);
        });
        
        // Очистить список
        elements.clothesList.innerHTML = '';
        
        // Добавить элементы одежды
        clothes.forEach(item => {
            const isSelected = state.selectedItems[category]?.name === item.name;
            const clothingItem = document.createElement('div');
            clothingItem.className = `clothing-item ${isSelected ? 'selected' : ''}`;
            clothingItem.dataset.name = item.name;
            clothingItem.dataset.category = category;
            
            clothingItem.innerHTML = `
                <i class="${item.icon}"></i>
                <span>${item.name}</span>
            `;
            
            clothingItem.addEventListener('click', () => selectClothing(item, category));
            elements.clothesList.appendChild(clothingItem);
        });
    }

    // Переключить категорию
    function switchCategory(category) {
        loadClothes(category);
    }

    // Выбрать одежду
    function selectClothing(item, category) {
        // Если уже выбрана - снять выбор
        if (state.selectedItems[category]?.name === item.name) {
            delete state.selectedItems[category];
            showMessage(`Снято: ${item.name}`);
        } else {
            state.selectedItems[category] = item;
            showMessage(`Выбрано: ${item.name}`);
        }
        
        // Обновить UI
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
        
        // Показать ошибку если ничего не найдено
        if (!hasResults && state.searchQuery) {
            showError('Такого нету :(');
        }
    }

    // Вращение персонажа
    function rotateCharacter(angle) {
        state.characterRotation += angle;
        elements.character.style.transform = `rotateY(${state.characterRotation}deg)`;
    }

    // Обновить внешний вид персонажа
    function updateCharacterAppearance() {
        const colors = {
            head: state.selectedItems.head ? '#FFD700' : '#8B4513',
            body: state.selectedItems.body ? '#1E90FF' : '#4682B4',
            legs: state.selectedItems.legs ? '#32CD32' : '#32CD32',
            shoes: state.selectedItems.shoes ? '#333' : '#333'
        };
        
        document.querySelector('.character-head').style.backgroundColor = colors.head;
        document.querySelector('.character-body').style.backgroundColor = colors.body;
        document.querySelector('.character-legs').style.backgroundColor = colors.legs;
        document.querySelector('.character-shoes').style.backgroundColor = colors.shoes;
    }

    // Показать сообщение
    function showMessage(text, type = 'info') {
        console.log(`${type}: ${text}`);
        
        if (type === 'error') {
            showError(text);
            return;
        }
        
        const notification = document.createElement('div');
        notification.className = 'notification';
        notification.textContent = text;
        notification.style.cssText = `
            position: fixed;
            bottom: 120px;
            left: 50%;
            transform: translateX(-50%);
            background: ${type === 'success' ? '#4CAF50' : '#2196F3'};
            color: white;
            padding: 15px 30px;
            border-radius: 50px;
            z-index: 1001;
            font-weight: 600;
            box-shadow: 0 10px 25px rgba(0,0,0,0.2);
            animation: fadeInUp 0.3s ease;
            text-align: center;
            max-width: 80%;
        `;
        
        document.body.appendChild(notification);
        
        // Автоматически удалить через 3 секунды
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 3000);
    }

    // Показать ошибку
    function showError(text) {
        // Удалить старые сообщения об ошибке
        const existingError = document.querySelector('.error-message');
        if (existingError) existingError.remove();
        
        // Создать новое сообщение
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.innerHTML = `
            <i class="fas fa-exclamation-circle"></i>
            <span>${text}</span>
        `;
        
        document.body.appendChild(errorDiv);
        
        // Автоматически удалить через 3 секунды
        setTimeout(() => {
            if (errorDiv.parentNode) {
                errorDiv.remove();
            }
        }, 3000);
    }

    // Запуск приложения
    init();
});