// Servers Management
document.addEventListener('DOMContentLoaded', async () => {
    loadServers();
    
    document.getElementById('addServerBtn')?.addEventListener('click', showAddServerModal);
});

async function loadServers() {
    try {
        const response = await apiGet('/servers');
        if (response.success) {
            renderServers(response.servers);
            updateStats(response.servers);
        }
    } catch (error) {
        console.error('Load servers error:', error);
    }
}

function renderServers(servers) {
    const container = document.getElementById('serversList');
    if (!container) return;
    
    container.innerHTML = servers.map(server => `
        <div class="server-card">
            <div class="server-header">
                <div class="flex-between">
                    <div>
                        <h3>${server.name}</h3>
                        <p class="text-gray-400">${server.ip}:${server.port}</p>
                    </div>
                    <div class="server-status ${server.status}">
                        ${server.status === 'online' ? '🟢 Онлайн' : '🔴 Оффлайн'}
                    </div>
                </div>
            </div>
            
            <div class="server-body">
                <div class="server-stat">
                    <span>Игроков:</span>
                    <strong>${server.online} / ${server.maxPlayers}</strong>
                </div>
                <div class="server-stat">
                    <span>Версия:</span>
                    <strong>${server.version}</strong>
                </div>
                <div class="server-stat">
                    <span>Описание:</span>
                    <strong>${server.description}</strong>
                </div>
            </div>
            
            <div class="server-actions">
                <button onclick="editServer('${server.id}')" class="btn btn-sm">Изменить</button>
                <button onclick="restartServer('${server.id}')" class="btn btn-sm btn-warning">Перезапуск</button>
                <button onclick="deleteServer('${server.id}')" class="btn btn-sm btn-danger">Удалить</button>
            </div>
        </div>
    `).join('');
}

function updateStats(servers) {
    const totalPlayers = servers.reduce((sum, s) => sum + s.online, 0);
    const onlineServers = servers.filter(s => s.status === 'online').length;
    
    document.getElementById('serversOnline').textContent = onlineServers;
    document.getElementById('totalPlayers').textContent = totalPlayers;
}

async function editServer(serverId) {
    alert(`Редактирование сервера: ${serverId}`);
}

async function restartServer(serverId) {
    if (!confirm('Перезапустить сервер?')) return;
    showToast('Команда перезапуска отправлена', 'info');
}

async function deleteServer(serverId) {
    if (!confirm('Удалить сервер?')) return;
    showToast('Функция удаления в разработке', 'warning');
}

function showAddServerModal() {
    alert('Добавление сервера в разработке');
}

