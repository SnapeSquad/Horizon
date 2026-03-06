import { useState } from "react"
import { GlassPanel } from "@/components/ui/glass-panel"
import { Eye, ShoppingCart, Coins } from "lucide-react"

export function StorePage() {
  const [activeCategory, setActiveCategory] = useState('cosmetics')
  const [hoveredItem, setHoveredItem] = useState<number | null>(null)

  const categories = ['Cosmetics', 'Services', 'Currency', 'Bundles']
  
  const items = [
    { id: 1, name: 'Корона', price: 500, image: '👑', badge: 'NEW', badgeColor: 'green' },
    { id: 2, name: 'Крылья', price: 1200, image: '🪽', badge: 'SALE', badgeColor: 'red', discount: '-20%' },
    { id: 3, name: 'Меч', price: 800, image: '⚔️', badge: 'HIT', badgeColor: 'purple' },
    { id: 4, name: 'Щит', price: 600, image: '🛡️', badge: null },
    { id: 5, name: 'Плащ', price: 1500, image: '🧥', badge: 'NEW', badgeColor: 'green' },
    { id: 6, name: 'Шлем', price: 400, image: '⛑️', badge: null },
  ]

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-7xl mx-auto">
        {/* Top Bar - Balance */}
        <GlassPanel className="mb-6 p-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Coins className="text-accent" size={24} />
            <span className="text-xl font-bold">1,500</span>
            <span className="text-text-muted">Horikov</span>
          </div>
        </GlassPanel>

        <div className="flex gap-6">
          {/* Sidebar Categories */}
          <GlassPanel className="w-64 p-4">
            <h3 className="text-lg font-bold mb-4">Категории</h3>
            <div className="space-y-2">
              {categories.map((category) => (
                <button
                  key={category}
                  onClick={() => setActiveCategory(category.toLowerCase())}
                  className={`
                    w-full text-left px-4 py-2 rounded-lg transition-all
                    ${activeCategory === category.toLowerCase()
                      ? 'bg-accent/20 text-accent'
                      : 'text-text-muted hover:text-text-main hover:bg-surface'
                    }
                  `}
                >
                  {category}
                </button>
              ))}
            </div>
          </GlassPanel>

          {/* Items Grid */}
          <div className="flex-1">
            <div className="grid grid-cols-3 gap-6">
              {items.map((item) => (
                <GlassPanel
                  key={item.id}
                  className="relative overflow-hidden cursor-pointer transition-transform"
                  onMouseEnter={() => setHoveredItem(item.id)}
                  onMouseLeave={() => setHoveredItem(null)}
                  style={{
                    transform: hoveredItem === item.id ? 'scale(1.05)' : 'scale(1)',
                  }}
                >
                  {/* Badge */}
                  {item.badge && (
                    <div
                      className={`
                        absolute top-3 right-3 px-2 py-1 rounded text-xs font-bold z-10
                        ${item.badgeColor === 'red' && 'bg-red-500'}
                        ${item.badgeColor === 'green' && 'bg-green-500'}
                        ${item.badgeColor === 'purple' && 'bg-purple-500'}
                      `}
                    >
                      {item.badge} {item.discount || ''}
                    </div>
                  )}

                  {/* Item Image */}
                  <div className="h-48 flex items-center justify-center bg-gradient-to-br from-purple-500/20 to-blue-500/20">
                    <span className="text-8xl">{item.image}</span>
                  </div>

                  {/* Item Info */}
                  <div className="p-4">
                    <h3 className="text-lg font-bold mb-2">{item.name}</h3>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Coins className="text-accent" size={16} />
                        <span className="font-semibold">{item.price}</span>
                      </div>
                    </div>
                  </div>

                  {/* Hover Actions */}
                  {hoveredItem === item.id && (
                    <div className="absolute inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center gap-4">
                      <button className="px-4 py-2 bg-accent/20 text-accent rounded-lg hover:bg-accent/30 transition-colors flex items-center gap-2">
                        <Eye size={18} />
                        Попробовать
                      </button>
                      <button className="px-4 py-2 gradient-button rounded-lg text-white flex items-center gap-2">
                        <ShoppingCart size={18} />
                        Купить
                      </button>
                    </div>
                  )}
                </GlassPanel>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
