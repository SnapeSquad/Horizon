import { useState } from "react"
import { GlassPanel } from "@/components/ui/glass-panel"
import { GradientButton } from "@/components/ui/gradient-button"
import { Home, Store, Shirt, MessageSquare, Settings } from "lucide-react"

export function MainDashboard() {
  const [activeTab, setActiveTab] = useState<'home' | 'store' | 'wardrobe' | 'forum' | 'settings'>('home')

  const tabs = [
    { id: 'home' as const, icon: Home, label: 'Главная' },
    { id: 'store' as const, icon: Store, label: 'Магазин' },
    { id: 'wardrobe' as const, icon: Shirt, label: 'Гардероб' },
    { id: 'forum' as const, icon: MessageSquare, label: 'Форум' },
    { id: 'settings' as const, icon: Settings, label: 'Настройки' },
  ]

  return (
    <div className="min-h-screen bg-background flex">
      {/* Left Sidebar */}
      <GlassPanel className="w-20 p-4 flex flex-col items-center gap-4">
        {tabs.map((tab) => {
          const Icon = tab.icon
          const isActive = activeTab === tab.id
          
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`
                w-14 h-14 rounded-lg flex items-center justify-center transition-all
                ${isActive 
                  ? 'bg-accent/20 text-accent' 
                  : 'text-text-muted hover:text-text-main hover:bg-surface'
                }
              `}
              style={isActive ? {
                boxShadow: '0 0 20px rgba(0, 242, 254, 0.3)',
              } : {}}
              title={tab.label}
            >
              <Icon size={24} />
            </button>
          )
        })}
      </GlassPanel>

      {/* Main Content */}
      <div className="flex-1 p-8">
        {activeTab === 'home' && (
          <div className="space-y-8">
            {/* News Slider */}
            <div className="grid grid-cols-2 gap-6">
              {[1, 2, 3, 4].map((item) => (
                <GlassPanel
                  key={item}
                  className="h-64 relative overflow-hidden group cursor-pointer"
                >
                  <div
                    className="absolute inset-0 bg-gradient-to-br from-purple-500/30 to-blue-500/30"
                    style={{
                      backgroundImage: `url(https://picsum.photos/800/600?random=${item})`,
                      backgroundSize: 'cover',
                      backgroundPosition: 'center',
                      transform: 'scale(1.1)',
                      transition: 'transform 0.3s ease',
                    }}
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent" />
                  <div className="absolute bottom-0 left-0 right-0 p-6 z-10">
                    <h3 className="text-xl font-bold mb-2">Новость {item}</h3>
                    <p className="text-text-muted text-sm">
                      Описание новости с интересным контентом...
                    </p>
                  </div>
                </GlassPanel>
              ))}
            </div>

            {/* 3D Character Preview */}
            <GlassPanel className="h-96 flex items-center justify-center">
              <div className="text-center">
                <div className="w-32 h-32 mx-auto mb-4 bg-gradient-to-br from-purple-500/20 to-blue-500/20 rounded-lg flex items-center justify-center">
                  <span className="text-6xl">🧍</span>
                </div>
                <p className="text-text-muted">3D Превью персонажа</p>
                <p className="text-text-muted text-sm mt-2">
                  Здесь будет 3D модель скина игрока
                </p>
              </div>
            </GlassPanel>
          </div>
        )}

        {/* Other tabs content */}
        {activeTab !== 'home' && (
          <GlassPanel className="p-8">
            <h2 className="text-2xl font-bold mb-4">
              {tabs.find(t => t.id === activeTab)?.label}
            </h2>
            <p className="text-text-muted">Контент в разработке...</p>
          </GlassPanel>
        )}
      </div>

      {/* Play Button */}
      <div className="fixed bottom-8 right-8">
        <GradientButton
          variant="pulse"
          className="px-12 py-6 text-xl font-minecraft"
          style={{
            fontSize: '1.5rem',
            boxShadow: '0 0 40px rgba(102, 126, 234, 0.6)',
          }}
        >
          ИГРАТЬ
        </GradientButton>
      </div>
    </div>
  )
}
