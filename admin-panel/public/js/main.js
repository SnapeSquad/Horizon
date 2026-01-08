// Horizon Admin Panel - Main JS

// Toast notifications
function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);
    
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// API helpers
async function apiGet(endpoint) {
    try {
        const response = await fetch(`/api${endpoint}`);
        return await response.json();
    } catch (error) {
        console.error('API Error:', error);
        showToast('Ошибка загрузки данных', 'error');
        return { success: false };
    }
}

async function apiPost(endpoint, data) {
    try {
        const response = await fetch(`/api${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await response.json();
    } catch (error) {
        console.error('API Error:', error);
        showToast('Ошибка отправки данных', 'error');
        return { success: false };
    }
}

async function apiDelete(endpoint, data = {}) {
    try {
        const response = await fetch(`/api${endpoint}`, {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await response.json();
    } catch (error) {
        console.error('API Error:', error);
        showToast('Ошибка удаления', 'error');
        return { success: false };
    }
}

// Confirm dialog
function confirm(message) {
    return window.confirm(message);
}

// Format date
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU') + ' ' + date.toLocaleTimeString('ru-RU');
}

// Format currency
function formatCurrency(amount) {
    return amount.toLocaleString('ru-RU') + ' ₽';
}

console.log('Horizon Admin Panel loaded');

