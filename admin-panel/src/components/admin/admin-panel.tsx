import { useState } from "react"
import { GlassPanel } from "@/components/ui/glass-panel"
import { Upload, X } from "lucide-react"

export function AdminPanel() {
  const [activeTab, setActiveTab] = useState<'cosmetics' | 'moderation' | 'news'>('cosmetics')
  const [isDragging, setIsDragging] = useState(false)

  const users = [
    { id: 1, username: 'Player1', hwid: 'ABC123', balance: 1500, status: 'active' },
    { id: 2, username: 'Player2', hwid: 'DEF456', balance: 800, status: 'banned' },
    { id: 3, username: 'Player3', hwid: 'GHI789', balance: 2000, status: 'active' },
  ]

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-4xl font-bold mb-8 font-minecraft">Admin Panel</h1>

        {/* Tabs */}
        <div className="flex gap-2 mb-6 border-b border-border">
          {(['cosmetics', 'moderation', 'news'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`
                px-6 py-3 font-semibold transition-colors
                ${activeTab === tab
                  ? 'text-accent border-b-2 border-accent'
                  : 'text-text-muted hover:text-text-main'
                }
              `}
            >
              {tab === 'cosmetics' && 'Косметика'}
              {tab === 'moderation' && 'Модерация'}
              {tab === 'news' && 'Новости'}
            </button>
          ))}
        </div>

        {/* Cosmetics Tab */}
        {activeTab === 'cosmetics' && (
          <div className="space-y-6">
            {/* Upload Form */}
            <GlassPanel className="p-6">
              <h2 className="text-2xl font-bold mb-6">Загрузка косметики</h2>
              
              <div className="grid grid-cols-2 gap-4 mb-6">
                <div>
                  <label className="block text-sm font-medium mb-2">Название *</label>
                  <input
                    type="text"
                    className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">Описание</label>
                  <input
                    type="text"
                    className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                  />
                </div>
              </div>

              {/* Drag & Drop Area */}
              <div
                className={`
                  border-2 border-dashed rounded-lg p-12 text-center transition-colors
                  ${isDragging
                    ? 'border-accent bg-accent/10'
                    : 'border-border hover:border-accent/50'
                  }
                `}
                onDragOver={(e) => {
                  e.preventDefault()
                  setIsDragging(true)
                }}
                onDragLeave={() => setIsDragging(false)}
                onDrop={(e) => {
                  e.preventDefault()
                  setIsDragging(false)
                  // Handle file drop
                }}
              >
                <Upload className="mx-auto mb-4 text-text-muted" size={48} />
                <p className="text-text-muted mb-2">
                  Перетащите файлы сюда или нажмите для выбора
                </p>
                <p className="text-text-muted text-sm">
                  Модель JSON и текстура PNG
                </p>
              </div>
            </GlassPanel>

            {/* Cosmetics List */}
            <GlassPanel className="p-6">
              <h3 className="text-xl font-bold mb-4">Список косметики</h3>
              <div className="space-y-2">
                {[1, 2, 3].map((item) => (
                  <div
                    key={item}
                    className="flex items-center justify-between p-4 glass-panel bg-surface/50 rounded-lg"
                  >
                    <div>
                      <div className="font-semibold">Косметика {item}</div>
                      <div className="text-text-muted text-sm">Описание косметики</div>
                    </div>
                    <button className="text-red-500 hover:text-red-400">
                      <X size={20} />
                    </button>
                  </div>
                ))}
              </div>
            </GlassPanel>
          </div>
        )}

        {/* Moderation Tab */}
        {activeTab === 'moderation' && (
          <div className="space-y-6">
            {/* Users Table */}
            <GlassPanel className="p-6">
              <h2 className="text-2xl font-bold mb-6">Пользователи</h2>
              
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="text-left py-3 px-4 text-text-muted">ID</th>
                      <th className="text-left py-3 px-4 text-text-muted">Ник</th>
                      <th className="text-left py-3 px-4 text-text-muted">HWID</th>
                      <th className="text-left py-3 px-4 text-text-muted">Баланс</th>
                      <th className="text-left py-3 px-4 text-text-muted">Статус</th>
                      <th className="text-left py-3 px-4 text-text-muted">Действия</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((user) => (
                      <tr key={user.id} className="border-b border-border/50 hover:bg-surface/50">
                        <td className="py-3 px-4">{user.id}</td>
                        <td className="py-3 px-4 font-semibold">{user.username}</td>
                        <td className="py-3 px-4 text-text-muted font-mono text-sm">{user.hwid}</td>
                        <td className="py-3 px-4">{user.balance}</td>
                        <td className="py-3 px-4">
                          <span
                            className={`
                              px-3 py-1 rounded-full text-xs font-semibold
                              ${user.status === 'active'
                                ? 'bg-green-500/20 text-green-400'
                                : 'bg-red-500/20 text-red-400'
                              }
                            `}
                          >
                            {user.status === 'active' ? 'Active' : 'Banned'}
                          </span>
                        </td>
                        <td className="py-3 px-4">
                          <button className="text-accent hover:text-primary-start transition-colors text-sm">
                            Действия
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </GlassPanel>

            {/* Bans List */}
            <GlassPanel className="p-6">
              <h3 className="text-xl font-bold mb-4">Черный список HWID</h3>
              <div className="space-y-2">
                <div className="p-4 glass-panel bg-surface/50 rounded-lg">
                  <div className="font-mono text-sm text-text-muted">ABC123</div>
                </div>
              </div>
            </GlassPanel>
          </div>
        )}

        {/* News Tab */}
        {activeTab === 'news' && (
          <GlassPanel className="p-6">
            <h2 className="text-2xl font-bold mb-6">Редактор новостей</h2>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-2">Заголовок *</label>
                <input
                  type="text"
                  className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-2">Текст (Markdown) *</label>
                <textarea
                  rows={10}
                  className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent font-mono text-sm"
                  placeholder="Вы можете использовать Markdown разметку..."
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-2">URL картинки</label>
                <input
                  type="url"
                  className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                  placeholder="https://example.com/image.png"
                />
              </div>
              
              <button className="px-6 py-3 gradient-button rounded-lg text-white">
                Сохранить новость
              </button>
            </div>
          </GlassPanel>
        )}
      </div>
    </div>
  )
}
