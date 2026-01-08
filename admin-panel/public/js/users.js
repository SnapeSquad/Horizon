// Users Management
document.addEventListener('DOMContentLoaded', async () => {
    loadUsers();
    
    document.getElementById('searchUser')?.addEventListener('input', (e) => {
        filterUsers(e.target.value);
    });
    
    document.getElementById('addUserBtn')?.addEventListener('click', showAddUserModal);
});

async function loadUsers() {
    try {
        const response = await apiGet('/users/list');
        if (response.success) {
            renderUsers(response.users);
        }
    } catch (error) {
        console.error('Load users error:', error);
    }
}

function renderUsers(users) {
    const tbody = document.getElementById('usersBody');
    if (!tbody) return;
    
    tbody.innerHTML = users.map(user => `
        <tr>
            <td>${user.id}</td>
            <td>${user.username}</td>
            <td>
                <span class="badge badge-${getRoleBadgeClass(user.role)}">
                    ${user.role || 'player'}
                </span>
            </td>
            <td>${user.currency || 0} ₽</td>
            <td>${user.two_factor_enabled ? '✅' : '❌'}</td>
            <td>${formatDate(user.created_at)}</td>
            <td>
                <button onclick="editUser(${user.id})" class="btn btn-sm">Изменить</button>
                <button onclick="banUser(${user.id})" class="btn btn-sm btn-danger">Бан</button>
            </td>
        </tr>
    `).join('');
}

function getRoleBadgeClass(role) {
    const roleClasses = {
        'owner': 'danger',
        'curator': 'warning',
        'admin': 'success',
        'helper': 'info',
        'player': 'secondary'
    };
    return roleClasses[role] || 'secondary';
}

function filterUsers(query) {
    const rows = document.querySelectorAll('#usersBody tr');
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = text.includes(query.toLowerCase()) ? '' : 'none';
    });
}

async function editUser(userId) {
    // TODO: Implement edit modal
    alert(`Редактирование пользователя ID: ${userId}`);
}

async function banUser(userId) {
    if (!confirm('Забанить этого пользователя?')) return;
    
    try {
        const response = await apiPost('/admin/ban', { user_id: userId });
        if (response.success) {
            showToast('Пользователь заблокирован', 'success');
            loadUsers();
        }
    } catch (error) {
        showToast('Ошибка блокировки', 'error');
    }
}

function showAddUserModal() {
    // TODO: Implement add user modal
    alert('Функция добавления пользователя в разработке');
}

