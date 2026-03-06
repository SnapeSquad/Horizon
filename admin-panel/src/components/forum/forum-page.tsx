import { GlassPanel } from "@/components/ui/glass-panel"
import { RoleBadge } from "@/components/ui/role-badge"
import { Search, Bell } from "lucide-react"

export function ForumPage() {
  const topics = [
    {
      id: 1,
      title: 'Обновление сервера до версии 1.20.1',
      author: { username: 'Admin', role: 'admin' as const },
      replies: 42,
      lastReply: '2 часа назад',
      avatar: '👑',
    },
    {
      id: 2,
      title: 'Новый ивент: Королевская битва',
      author: { username: 'Curator', role: 'curator' as const },
      replies: 128,
      lastReply: '5 часов назад',
      avatar: '🛡️',
    },
    {
      id: 3,
      title: 'Помощь с установкой модов',
      author: { username: 'Helper', role: 'helper' as const },
      replies: 15,
      lastReply: '1 день назад',
      avatar: '🔵',
    },
    {
      id: 4,
      title: 'Обсуждение новых косметических предметов',
      author: { username: 'UltraDonator', role: 'ulta' as const },
      replies: 67,
      lastReply: '3 дня назад',
      avatar: '💎',
    },
  ]

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <GlassPanel className="mb-6 p-4 flex items-center justify-between">
          <div className="flex-1 max-w-md relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" size={20} />
            <input
              type="text"
              placeholder="Поиск тем..."
              className="w-full pl-10 pr-4 py-2 glass-panel bg-surface/50 text-text-main placeholder-text-muted rounded-lg outline-none focus:border-accent border border-transparent transition-colors"
            />
          </div>
          <button className="relative p-2 text-text-muted hover:text-accent transition-colors">
            <Bell size={24} />
            <span className="absolute top-0 right-0 w-3 h-3 bg-red-500 rounded-full" />
          </button>
        </GlassPanel>

        {/* Topics List */}
        <div className="space-y-4">
          {topics.map((topic) => (
            <GlassPanel key={topic.id} className="p-6 hover:bg-surface/80 transition-colors cursor-pointer">
              <div className="flex items-center gap-6">
                {/* Avatar with Role-colored Frame */}
                <div
                  className={`
                    w-16 h-16 rounded-lg flex items-center justify-center text-4xl
                    ${topic.author.role === 'admin' && 'border-2 border-admin'}
                    ${topic.author.role === 'curator' && 'border-2 border-curator-start'}
                    ${topic.author.role === 'helper' && 'border-2 border-helper'}
                    ${topic.author.role === 'ulta' && 'border-2 border-ulta-start'}
                  `}
                  style={
                    topic.author.role === 'curator'
                      ? {
                          borderImage: 'linear-gradient(90deg, #FF4B4B, #FF9E9E) 1',
                        }
                      : topic.author.role === 'ulta'
                      ? {
                          borderImage: 'linear-gradient(90deg, #a18cd1, #fdc2ed) 1',
                        }
                      : {}
                  }
                >
                  {topic.avatar}
                </div>

                {/* Topic Info */}
                <div className="flex-1">
                  <h3 className="text-lg font-bold mb-2">{topic.title}</h3>
                  <div className="flex items-center gap-2">
                    <span className="text-text-muted text-sm">Автор:</span>
                    <RoleBadge role={topic.author.role} username={topic.author.username} />
                  </div>
                </div>

                {/* Stats */}
                <div className="text-right">
                  <div className="text-text-muted text-sm mb-1">
                    {topic.replies} ответов
                  </div>
                  <div className="text-text-muted text-xs">
                    {topic.lastReply}
                  </div>
                </div>
              </div>
            </GlassPanel>
          ))}
        </div>
      </div>
    </div>
  )
}
