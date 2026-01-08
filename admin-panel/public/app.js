// API URL
const API_URL = 'http://localhost:3000/api';
let authToken = localStorage.getItem('adminToken') || '';

// ============ АВТОРИЗАЦИЯ ============

document.getElementById('login-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorEl = document.getElementById('login-error');
    
    try {
        const response = await fetch(`${API_URL}/admin/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        
        const data = await response.json();
        
        if (data.success) {
            authToken = data.token;
            localStorage.setItem('adminToken', authToken);
            document.getElementById('login-screen').style.display = 'none';
            document.getElementById('admin-panel').style.display = 'flex';
            document.getElementById('admin-username').textContent = data.username;
            loadDashboard();
        } else {
            errorEl.textContent = data.error || 'Ошибка входа';
            errorEl.style.display = 'block';
        }
    } catch (error) {
        errorEl.textContent = 'Ошибка подключения к серверу';
        errorEl.style.display = 'block';
    }
});

// Проверка токена при загрузке
if (authToken) {
    document.getElementById('login-screen').style.display = 'none';
    document.getElementById('admin-panel').style.display = 'flex';
    loadDashboard();
}

// Выход
document.getElementById('logout-btn')?.addEventListener('click', () => {
    localStorage.removeItem('adminToken');
    authToken = '';
    location.reload();
});

// ============ НАВИГАЦИЯ ============

function switchPage(pageName) {
    // Убираем active у всех страниц и nav-item
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    
    // Активируем нужную страницу
    document.getElementById(`${pageName}-page`).classList.add('active');
    document.querySelector(`[data-page="${pageName}"]`).classList.add('active');
    
    // Загружаем данные
    if (pageName === 'shop') loadShopItems();
    else if (pageName === 'forum') loadForumData();
    else if (pageName === 'users') loadUsers();
    else if (pageName === 'dashboard') loadDashboard();
}

// Обработчики навигации
document.querySelectorAll('.nav-item[data-page]').forEach(item => {
    item.addEventListener('click', (e) => {
        e.preventDefault();
        switchPage(item.dataset.page);
    });
});

// ============ DASHBOARD ============

async function loadDashboard() {
    try {
        const response = await fetch(`${API_URL}/admin/stats`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        const data = await response.json();
        
        document.getElementById('stat-users').textContent = data.totalUsers;
        document.getElementById('stat-items').textContent = data.totalShopItems;
        document.getElementById('stat-topics').textContent = data.totalForumTopics;
        document.getElementById('stat-revenue').textContent = '💎 ' + data.totalRevenue;
    } catch (error) {
        console.error('Ошибка загрузки статистики:', error);
    }
}

// ============ МАГАЗИН ============

async function loadShopItems() {
    try {
        const response = await fetch(`${API_URL}/admin/shop/items`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        const data = await response.json();
        const tbody = document.getElementById('shop-items-table');
        
        tbody.innerHTML = data.items.map(item => `
            <tr>
                <td>${item.id}</td>
                <td style="font-size: 24px;">${item.icon}</td>
                <td>${item.name}</td>
                <td>💎 ${item.price}</td>
                <td>${item.category}</td>
                <td><span class="badge badge-${item.rarity.toLowerCase()}">${item.rarity}</span></td>
                <td>${item.featured ? '⭐' : '-'}</td>
                <td>
                    <button class="btn-delete" onclick="deleteShopItem(${item.id})">🗑️ Удалить</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Ошибка загрузки товаров:', error);
    }
}

function showAddItemModal() {
    document.getElementById('add-item-modal').style.display = 'flex';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

document.getElementById('add-item-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = {
        name: formData.get('name'),
        icon: formData.get('icon'),
        price: formData.get('price'),
        category: formData.get('category'),
        rarity: formData.get('rarity'),
        featured: formData.get('featured') === 'on'
    };
    
    try {
        const response = await fetch(`${API_URL}/admin/shop/items`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            closeModal('add-item-modal');
            e.target.reset();
            loadShopItems();
        }
    } catch (error) {
        console.error('Ошибка добавления товара:', error);
    }
});

async function deleteShopItem(id) {
    if (!confirm('Удалить этот товар?')) return;
    
    try {
        const response = await fetch(`${API_URL}/admin/shop/items/${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        if (response.ok) {
            loadShopItems();
        }
    } catch (error) {
        console.error('Ошибка удаления товара:', error);
    }
}

// ============ ФОРУМ ============

async function loadForumData() {
    await loadForumCategories();
    await loadForumTopics();
}

async function loadForumCategories() {
    try {
        const response = await fetch(`${API_URL}/admin/forum/categories`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        const data = await response.json();
        const container = document.getElementById('forum-categories');
        
        container.innerHTML = data.categories.map(cat => `
            <div class="forum-category">
                <div class="category-info">
                    <div class="category-icon">${cat.icon}</div>
                    <div>
                        <div class="category-name">${cat.name}</div>
                        <div class="category-count">${cat.topics} тем</div>
                    </div>
                </div>
                <button class="btn-delete" onclick="deleteForumCategory(${cat.id})">🗑️</button>
            </div>
        `).join('');
    } catch (error) {
        console.error('Ошибка загрузки категорий:', error);
    }
}

async function loadForumTopics() {
    try {
        const response = await fetch(`${API_URL}/admin/forum/topics`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        const data = await response.json();
        const tbody = document.getElementById('forum-topics-table');
        
        tbody.innerHTML = data.topics.map(topic => `
            <tr>
                <td>${topic.id}</td>
                <td>${topic.title}</td>
                <td>${topic.author}</td>
                <td>${topic.views}</td>
                <td>${topic.replies}</td>
                <td>
                    ${topic.pinned ? '<span class="badge badge-rare">📌 Закреплено</span>' : ''}
                    ${topic.locked ? '<span class="badge badge-common">🔒 Закрыто</span>' : ''}
                </td>
                <td>
                    <button class="btn-edit" onclick="toggleTopicPin(${topic.id})">📌</button>
                    <button class="btn-edit" onclick="toggleTopicLock(${topic.id})">🔒</button>
                    <button class="btn-delete" onclick="deleteForumTopic(${topic.id})">🗑️</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Ошибка загрузки тем:', error);
    }
}

function showAddCategoryModal() {
    document.getElementById('add-category-modal').style.display = 'flex';
}

document.getElementById('add-category-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = {
        name: formData.get('name'),
        icon: formData.get('icon'),
        color: formData.get('color')
    };
    
    try {
        const response = await fetch(`${API_URL}/admin/forum/categories`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            closeModal('add-category-modal');
            e.target.reset();
            loadForumCategories();
        }
    } catch (error) {
        console.error('Ошибка добавления категории:', error);
    }
});

async function deleteForumCategory(id) {
    if (!confirm('Удалить эту категорию?')) return;
    
    try {
        const response = await fetch(`${API_URL}/admin/forum/categories/${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        if (response.ok) {
            loadForumCategories();
        }
    } catch (error) {
        console.error('Ошибка удаления категории:', error);
    }
}

async function toggleTopicPin(id) {
    try {
        const response = await fetch(`${API_URL}/admin/forum/topics/${id}/pin`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        if (response.ok) {
            loadForumTopics();
        }
    } catch (error) {
        console.error('Ошибка закрепления темы:', error);
    }
}

async function toggleTopicLock(id) {
    try {
        const response = await fetch(`${API_URL}/admin/forum/topics/${id}/lock`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        if (response.ok) {
            loadForumTopics();
        }
    } catch (error) {
        console.error('Ошибка блокировки темы:', error);
    }
}

async function deleteForumTopic(id) {
    if (!confirm('Удалить эту тему?')) return;
    
    try {
        const response = await fetch(`${API_URL}/admin/forum/topics/${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        if (response.ok) {
            loadForumTopics();
        }
    } catch (error) {
        console.error('Ошибка удаления темы:', error);
    }
}

// ============ ПОЛЬЗОВАТЕЛИ ============

async function loadUsers() {
    try {
        const response = await fetch(`${API_URL}/admin/users`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        const data = await response.json();
        const tbody = document.getElementById('users-table');
        
        tbody.innerHTML = data.users.map(user => `
            <tr>
                <td>${user.username}</td>
                <td>
                    <input type="number" value="${user.balance}" id="balance-${user.username}" 
                           style="width: 80px; background: rgba(255, 255, 255, 0.08); border: 1px solid var(--border-color); 
                           border-radius: 8px; padding: 5px; color: white;">
                    <button class="btn-edit" onclick="updateUserBalance('${user.username}')">💾</button>
                </td>
                <td><span class="badge badge-rare">${user.rank}</span></td>
                <td>${user.banned ? '🚫 Забанен' : '✅ Активен'}</td>
                <td>
                    <button class="btn-${user.banned ? 'edit' : 'delete'}" onclick="toggleUserBan('${user.username}')">
                        ${user.banned ? '✅ Разбанить' : '🚫 Забанить'}
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Ошибка загрузки пользователей:', error);
    }
}

async function updateUserBalance(username) {
    const amount = document.getElementById(`balance-${username}`).value;
    
    try {
        const response = await fetch(`${API_URL}/admin/users/${username}/balance`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ amount })
        });
        
        if (response.ok) {
            alert('Баланс обновлен!');
        }
    } catch (error) {
        console.error('Ошибка обновления баланса:', error);
    }
}

async function toggleUserBan(username) {
    try {
        const response = await fetch(`${API_URL}/admin/users/${username}/ban`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        
        if (response.ok) {
            loadUsers();
        }
    } catch (error) {
        console.error('Ошибка бана пользователя:', error);
    }
}

