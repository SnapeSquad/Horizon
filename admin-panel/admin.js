// API базовый URL
const API_BASE = 'http://localhost:3000';

// Получить токен из localStorage
function getAdminToken() {
    return localStorage.getItem('adminToken') || '';
}

// Сохранить токен
function saveToken() {
    const token = document.getElementById('adminTokenInput').value.trim();
    if (token) {
        localStorage.setItem('adminToken', token);
        alert('Токен сохранен!');
        // Загружаем данные для текущей вкладки
        const activeTab = document.querySelector('.tab-section:not(.hidden)').id.replace('section-', '');
        loadTabData(activeTab);
    } else {
        alert('Введите токен!');
    }
}

// Загрузить токен при загрузке страницы
window.addEventListener('DOMContentLoaded', () => {
    const savedToken = getAdminToken();
    if (savedToken) {
        document.getElementById('adminTokenInput').value = savedToken;
    }
});

// Переключение табов
function showTab(tabName) {
    // Скрываем все секции
    document.querySelectorAll('.tab-section').forEach(section => {
        section.classList.add('hidden');
    });
    
    // Сбрасываем стили всех табов
    document.querySelectorAll('[id^="tab-"]').forEach(tab => {
        tab.classList.remove('border-blue-600', 'text-blue-600');
        tab.classList.add('text-gray-600', 'border-transparent');
    });
    
    // Показываем выбранную секцию
    document.getElementById(`section-${tabName}`).classList.remove('hidden');
    
    // Подсвечиваем выбранный таб
    const activeTab = document.getElementById(`tab-${tabName}`);
    activeTab.classList.remove('text-gray-600', 'border-transparent');
    activeTab.classList.add('border-blue-600', 'text-blue-600');
    
    // Загружаем данные
    loadTabData(tabName);
}

// Загрузка данных для таба
function loadTabData(tabName) {
    const token = getAdminToken();
    if (!token) {
        console.warn('Токен не установлен');
        return;
    }
    
    switch(tabName) {
        case 'cosmetics':
            loadCosmetics();
            break;
        case 'moderation':
            loadUsers();
            loadBans();
            break;
        case 'news':
            loadNews();
            break;
    }
}

// API запрос с токеном
async function apiRequest(endpoint, options = {}) {
    const token = getAdminToken();
    if (!token) {
        throw new Error('Токен не установлен');
    }
    
    const headers = {
        'x-admin-token': token,
        ...options.headers
    };
    
    // Удаляем Content-Type для FormData (будет установлен автоматически)
    if (options.body instanceof FormData) {
        delete headers['Content-Type'];
    } else if (!options.headers?.['Content-Type']) {
        headers['Content-Type'] = 'application/json';
    }
    
    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: 'Ошибка сервера' }));
        throw new Error(error.message || `HTTP ${response.status}`);
    }
    
    return response.json();
}

// ==================== КОСМЕТИКА ====================

// Загрузка списка косметики
async function loadCosmetics() {
    const listEl = document.getElementById('cosmeticsList');
    try {
        const data = await apiRequest('/api/admin/cosmetics');
        if (data.success && data.cosmetics.length > 0) {
            listEl.innerHTML = data.cosmetics.map(cosmetic => `
                <div class="border border-gray-200 rounded-lg p-4 flex justify-between items-center">
                    <div>
                        <h4 class="font-semibold">${escapeHtml(cosmetic.name)}</h4>
                        <p class="text-sm text-gray-600">Кость: ${escapeHtml(cosmetic.pivot_point)} | Цена: ${cosmetic.price} | Редкость: ${escapeHtml(cosmetic.rarity)}</p>
                        ${cosmetic.description ? `<p class="text-sm text-gray-500 mt-1">${escapeHtml(cosmetic.description)}</p>` : ''}
                    </div>
                    <span class="px-3 py-1 rounded-full text-xs font-semibold ${cosmetic.is_active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
                        ${cosmetic.is_active ? 'Активна' : 'Неактивна'}
                    </span>
                </div>
            `).join('');
        } else {
            listEl.innerHTML = '<p class="text-gray-500">Косметика не найдена</p>';
        }
    } catch (error) {
        listEl.innerHTML = `<p class="text-red-500">Ошибка загрузки: ${error.message}</p>`;
    }
}

// Обработка формы косметики
document.getElementById('cosmeticForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const formData = new FormData();
    formData.append('name', document.getElementById('cosmeticName').value);
    formData.append('description', document.getElementById('cosmeticDescription').value || '');
    formData.append('pivot_point', document.getElementById('cosmeticPivot').value);
    formData.append('price', document.getElementById('cosmeticPrice').value);
    formData.append('rarity', document.getElementById('cosmeticRarity').value);
    
    const modelFile = document.getElementById('cosmeticModel').files[0];
    const textureFile = document.getElementById('cosmeticTexture').files[0];
    
    if (!modelFile || !textureFile) {
        alert('Выберите файлы модели и текстуры!');
        return;
    }
    
    formData.append('model', modelFile);
    formData.append('texture', textureFile);
    
    const submitBtn = e.target.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Загрузка...';
    
    try {
        const data = await apiRequest('/api/admin/cosmetics', {
            method: 'POST',
            body: formData
        });
        
        if (data.success) {
            alert('Косметика успешно добавлена!');
            e.target.reset();
            loadCosmetics();
        }
    } catch (error) {
        alert('Ошибка: ' + error.message);
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Загрузить косметику';
    }
});

// ==================== МОДЕРАЦИЯ ====================

// Загрузка списка пользователей
async function loadUsers() {
    const tbody = document.getElementById('usersTableBody');
    try {
        const data = await apiRequest('/api/admin/users');
        if (data.success && data.users.length > 0) {
            tbody.innerHTML = data.users.map(user => `
                <tr class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${user.id}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${escapeHtml(user.username)}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        ${user.hwid ? `<code class="bg-gray-100 px-2 py-1 rounded">${escapeHtml(user.hwid)}</code>` : '<span class="text-gray-400">Не указан</span>'}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${user.currency || 0}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${formatDate(user.created_at)}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm">
                        ${user.hwid ? `
                            <button onclick="banUser('${escapeHtml(user.hwid)}', '${escapeHtml(user.username)}')" 
                                    class="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded transition-all">
                                Забанить
                            </button>
                        ` : '<span class="text-gray-400">Нет HWID</span>'}
                    </td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="6" class="px-6 py-4 text-center text-gray-500">Пользователи не найдены</td></tr>';
        }
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="6" class="px-6 py-4 text-center text-red-500">Ошибка загрузки: ${error.message}</td></tr>`;
    }
}

// Загрузка списка банов
async function loadBans() {
    const listEl = document.getElementById('bansList');
    try {
        const data = await apiRequest('/api/admin/bans');
        if (data.success && data.bans.length > 0) {
            listEl.innerHTML = data.bans.map(ban => `
                <div class="border border-gray-200 rounded-lg p-4 flex justify-between items-center">
                    <div>
                        <h4 class="font-semibold"><code class="bg-gray-100 px-2 py-1 rounded">${escapeHtml(ban.hwid)}</code></h4>
                        ${ban.reason ? `<p class="text-sm text-gray-600 mt-1">Причина: ${escapeHtml(ban.reason)}</p>` : ''}
                        <p class="text-xs text-gray-500 mt-1">Забанен: ${ban.banned_by} | ${formatDate(ban.created_at)}</p>
                    </div>
                    <button onclick="unbanUser('${escapeHtml(ban.hwid)}')" 
                            class="px-3 py-1 bg-green-600 hover:bg-green-700 text-white rounded transition-all text-sm">
                        Разбанить
                    </button>
                </div>
            `).join('');
        } else {
            listEl.innerHTML = '<p class="text-gray-500">Черный список пуст</p>';
        }
    } catch (error) {
        listEl.innerHTML = `<p class="text-red-500">Ошибка загрузки: ${error.message}</p>`;
    }
}

// Забанить пользователя
async function banUser(hwid, username) {
    const reason = prompt(`Забанить пользователя ${username} (HWID: ${hwid})\nПричина (необязательно):`);
    if (reason === null) return; // Отмена
    
    try {
        const data = await apiRequest('/api/admin/users/ban', {
            method: 'POST',
            body: JSON.stringify({
                hwid: hwid,
                reason: reason || 'Не указана',
                username: username
            })
        });
        
        if (data.success) {
            alert('Пользователь забанен!');
            loadUsers();
            loadBans();
        }
    } catch (error) {
        alert('Ошибка: ' + error.message);
    }
}

// Разбанить пользователя
async function unbanUser(hwid) {
    if (!confirm(`Разбанить HWID: ${hwid}?`)) return;
    
    try {
        const data = await apiRequest(`/api/admin/users/unban/${encodeURIComponent(hwid)}`, {
            method: 'DELETE'
        });
        
        if (data.success) {
            alert('Пользователь разбанен!');
            loadBans();
        }
    } catch (error) {
        alert('Ошибка: ' + error.message);
    }
}

// ==================== НОВОСТИ ====================

// Загрузка списка новостей
async function loadNews() {
    const listEl = document.getElementById('newsList');
    try {
        const data = await apiRequest('/api/admin/news');
        if (data.success && data.news.length > 0) {
            listEl.innerHTML = data.news.map(news => `
                <div class="border border-gray-200 rounded-lg p-4">
                    <div class="flex justify-between items-start mb-2">
                        <div class="flex-1">
                            <h4 class="font-semibold text-lg">${escapeHtml(news.title)}</h4>
                            <p class="text-sm text-gray-500 mt-1">Автор: ${escapeHtml(news.author || 'Admin')} | ${formatDate(news.created_at)}</p>
                            ${news.image_url ? `<img src="${escapeHtml(news.image_url)}" alt="${escapeHtml(news.title)}" class="mt-3 max-w-md rounded-lg">` : ''}
                        </div>
                        <div class="flex space-x-2 ml-4">
                            <button onclick="editNews(${news.id})" 
                                    class="px-3 py-1 bg-blue-600 hover:bg-blue-700 text-white rounded transition-all text-sm">
                                Редактировать
                            </button>
                            <button onclick="deleteNews(${news.id})" 
                                    class="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded transition-all text-sm">
                                Удалить
                            </button>
                        </div>
                    </div>
                    <div class="mt-3 text-sm text-gray-700 border-t pt-3">
                        <pre class="whitespace-pre-wrap font-sans">${escapeHtml(news.content)}</pre>
                    </div>
                </div>
            `).join('');
        } else {
            listEl.innerHTML = '<p class="text-gray-500">Новости не найдены</p>';
        }
    } catch (error) {
        listEl.innerHTML = `<p class="text-red-500">Ошибка загрузки: ${error.message}</p>`;
    }
}

// Редактировать новость
async function editNews(id) {
    try {
        const data = await apiRequest('/api/admin/news');
        const news = data.news.find(n => n.id === id);
        if (news) {
            document.getElementById('newsId').value = news.id;
            document.getElementById('newsTitle').value = news.title;
            document.getElementById('newsContent').value = news.content;
            document.getElementById('newsImageUrl').value = news.image_url || '';
            document.getElementById('newsForm').scrollIntoView({ behavior: 'smooth' });
        }
    } catch (error) {
        alert('Ошибка загрузки новости: ' + error.message);
    }
}

// Удалить новость
async function deleteNews(id) {
    if (!confirm('Удалить эту новость?')) return;
    
    try {
        const data = await apiRequest(`/api/admin/news/${id}`, {
            method: 'DELETE'
        });
        
        if (data.success) {
            alert('Новость удалена!');
            loadNews();
        }
    } catch (error) {
        alert('Ошибка: ' + error.message);
    }
}

// Обработка формы новостей
document.getElementById('newsForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const newsId = document.getElementById('newsId').value;
    const newsData = {
        title: document.getElementById('newsTitle').value,
        content: document.getElementById('newsContent').value,
        image_url: document.getElementById('newsImageUrl').value || null
    };
    
    const submitBtn = e.target.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Сохранение...';
    
    try {
        let data;
        if (newsId) {
            // Обновление
            data = await apiRequest(`/api/admin/news/${newsId}`, {
                method: 'PUT',
                body: JSON.stringify(newsData)
            });
        } else {
            // Создание
            data = await apiRequest('/api/admin/news', {
                method: 'POST',
                body: JSON.stringify(newsData)
            });
        }
        
        if (data.success) {
            alert(newsId ? 'Новость обновлена!' : 'Новость создана!');
            resetNewsForm();
            loadNews();
        }
    } catch (error) {
        alert('Ошибка: ' + error.message);
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Сохранить новость';
    }
});

// Сброс формы новостей
function resetNewsForm() {
    document.getElementById('newsForm').reset();
    document.getElementById('newsId').value = '';
}

// ==================== УТИЛИТЫ ====================

// Экранирование HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Форматирование даты
function formatDate(dateString) {
    if (!dateString) return 'Не указана';
    const date = new Date(dateString);
    return date.toLocaleString('ru-RU');
}
