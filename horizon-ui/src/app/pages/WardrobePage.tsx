import { useState } from 'react';
import WindowControls from '../components/WindowControls';
import ParticleBackground from '../components/ParticleBackground';
import SkinViewer3D from '../components/SkinViewer3D';
import { Lock, Check, RotateCcw, Sparkles } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface WardrobePageProps {
  user: any;
}

type ItemRarity = 'common' | 'rare' | 'epic' | 'legendary';

interface WardrobeItem {
  id: number;
  name: string;
  rarity: ItemRarity;
  unlocked: boolean;
  equipped: boolean;
  category: 'clothes' | 'accessories' | 'pets';
}

const rarityColors = {
  common: { color: '#9CA3AF', glow: 'rgba(156, 163, 175, 0.3)' },
  rare: { color: '#3B82F6', glow: 'rgba(59, 130, 246, 0.3)' },
  epic: { color: '#A855F7', glow: 'rgba(168, 85, 247, 0.3)' },
  legendary: { color: '#F59E0B', glow: 'rgba(245, 158, 11, 0.3)' },
};

const items: WardrobeItem[] = [
  { id: 1, name: 'Кожаная броня', rarity: 'common', unlocked: true, equipped: true, category: 'clothes' },
  { id: 2, name: 'Железная броня', rarity: 'rare', unlocked: true, equipped: false, category: 'clothes' },
  { id: 3, name: 'Алмазная броня', rarity: 'epic', unlocked: true, equipped: false, category: 'clothes' },
  { id: 4, name: 'Незеритовая броня', rarity: 'legendary', unlocked: false, equipped: false, category: 'clothes' },
  { id: 5, name: 'Элитная броня', rarity: 'legendary', unlocked: true, equipped: false, category: 'clothes' },
  { id: 6, name: 'Крылья ангела', rarity: 'epic', unlocked: true, equipped: false, category: 'accessories' },
  { id: 7, name: 'Корона короля', rarity: 'legendary', unlocked: false, equipped: false, category: 'accessories' },
  { id: 8, name: 'Светящийся шлем', rarity: 'rare', unlocked: true, equipped: false, category: 'accessories' },
  { id: 9, name: 'Волк', rarity: 'common', unlocked: true, equipped: false, category: 'pets' },
  { id: 10, name: 'Дракон', rarity: 'legendary', unlocked: false, equipped: false, category: 'pets' },
  { id: 11, name: 'Феникс', rarity: 'epic', unlocked: true, equipped: false, category: 'pets' },
];

export default function WardrobePage({ user }: WardrobePageProps) {
  const [activeTab, setActiveTab] = useState<'clothes' | 'accessories' | 'pets'>('clothes');
  const [rotation, setRotation] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [startX, setStartX] = useState(0);
  const [selectedItems, setSelectedItems] = useState<number[]>([1]);
  const [hasChanges, setHasChanges] = useState(false);

  const handleMouseDown = (e: React.MouseEvent) => {
    setIsDragging(true);
    setStartX(e.clientX);
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (isDragging) {
      const delta = e.clientX - startX;
      setRotation((prev) => prev + delta * 0.5);
      setStartX(e.clientX);
    }
  };

  const handleMouseUp = () => setIsDragging(false);

  const toggleItem = (itemId: number) => {
    const item = items.find(i => i.id === itemId);
    if (!item?.unlocked) return;
    
    setSelectedItems((prev) =>
      prev.includes(itemId)
        ? prev.filter((id) => id !== itemId)
        : [...prev, itemId]
    );
    setHasChanges(true);
  };

  const filteredItems = items.filter((item) => item.category === activeTab);

  const tabs = [
    { key: 'clothes' as const, label: 'Одежда', icon: '👕' },
    { key: 'accessories' as const, label: 'Аксессуары', icon: '👑' },
    { key: 'pets' as const, label: 'Питомцы', icon: '🐺' },
  ];

  return (
    <div className="flex h-screen relative">
      <ParticleBackground />
      
      <div className="flex-1 flex">
        {/* 3D Character Preview */}
        <div className="flex-1 flex flex-col relative">
          {/* Header */}
          <div 
            className="h-20 flex items-center justify-between px-8 relative z-10"
            style={{
              background: 'linear-gradient(180deg, rgba(0, 0, 0, 0.5) 0%, rgba(0, 0, 0, 0.2) 100%)',
              backdropFilter: 'blur(20px)',
              borderBottom: '1px solid rgba(124, 77, 255, 0.2)',
            }}
          >
            <div>
              <h1 className="text-2xl font-bold flex items-center gap-3">
                <Sparkles className="w-7 h-7" style={{ color: '#7C4DFF' }} />
                Гардероб
              </h1>
              <p className="text-sm" style={{ color: '#9CA3AF' }}>
                Настройте внешний вид персонажа
              </p>
            </div>

            <WindowControls />
          </div>

          {/* Character Display */}
          <div className="flex-1 flex items-center justify-center p-8">
            <div className="relative">
              {/* 3D Skin Viewer */}
              <div
                className="relative"
                onMouseDown={handleMouseDown}
                onMouseMove={handleMouseMove}
                onMouseUp={handleMouseUp}
                onMouseLeave={handleMouseUp}
                style={{
                  cursor: isDragging ? 'grabbing' : 'grab',
                }}
              >
                <SkinViewer3D 
                  username={user?.username || 'Steve'}
                  width={400}
                  height={550}
                  rotation={rotation}
                />

                {/* Floating Particles */}
                {[...Array(12)].map((_, i) => (
                  <motion.div
                    key={i}
                    className="absolute w-2 h-2 rounded-full pointer-events-none"
                    style={{
                      backgroundColor: '#7C4DFF',
                      top: `${Math.random() * 100}%`,
                      left: `${Math.random() * 100}%`,
                      filter: 'blur(1px)',
                    }}
                    animate={{
                      y: [0, -30, 0],
                      opacity: [0.2, 1, 0.2],
                      scale: [0.5, 1.2, 0.5],
                    }}
                    transition={{
                      duration: 2 + Math.random() * 2,
                      repeat: Infinity,
                      delay: i * 0.15,
                    }}
                  />
                ))}
              </div>

              {/* Controls Hint */}
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="absolute bottom-0 left-1/2 -translate-x-1/2 px-6 py-3 rounded-xl text-sm flex items-center gap-2"
                style={{
                  backgroundColor: 'rgba(0, 0, 0, 0.8)',
                  backdropFilter: 'blur(10px)',
                  border: '1px solid rgba(124, 77, 255, 0.3)',
                  color: '#9CA3AF',
                }}
              >
                <RotateCcw className="w-4 h-4" />
                Перетащите для поворота
              </motion.div>
            </div>
          </div>
        </div>

        {/* Wardrobe Panel */}
        <div 
          className="w-[500px] flex flex-col relative"
          style={{
            background: 'linear-gradient(180deg, rgba(0, 0, 0, 0.6) 0%, rgba(0, 0, 0, 0.4) 100%)',
            backdropFilter: 'blur(30px)',
            borderLeft: '1px solid rgba(124, 77, 255, 0.3)',
          }}
        >
          {/* Tabs */}
          <div className="flex p-2 gap-2" style={{ borderBottom: '1px solid rgba(124, 77, 255, 0.2)' }}>
            {tabs.map((tab) => (
              <motion.button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="flex-1 py-4 px-4 rounded-xl font-medium transition-all flex items-center justify-center gap-2 relative overflow-hidden"
                style={{
                  background: activeTab === tab.key
                    ? 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)'
                    : 'rgba(255, 255, 255, 0.05)',
                  color: activeTab === tab.key ? '#fff' : '#9CA3AF',
                  boxShadow: activeTab === tab.key ? '0 4px 20px rgba(124, 77, 255, 0.4)' : 'none',
                }}
              >
                <span className="text-xl">{tab.icon}</span>
                <span>{tab.label}</span>
                
                {activeTab === tab.key && (
                  <motion.div
                    layoutId="activeTabIndicator"
                    className="absolute bottom-0 left-0 right-0 h-1 rounded-t-full"
                    style={{ backgroundColor: '#fff' }}
                  />
                )}
              </motion.button>
            ))}
          </div>

          {/* Items Grid */}
          <div className="flex-1 overflow-y-auto p-6">
            <div className="grid grid-cols-3 gap-4">
              <AnimatePresence mode="popLayout">
                {filteredItems.map((item, index) => (
                  <motion.div
                    key={item.id}
                    layout
                    initial={{ opacity: 0, scale: 0.8 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.8 }}
                    transition={{ delay: index * 0.05 }}
                    whileHover={{ scale: item.unlocked ? 1.05 : 1, y: item.unlocked ? -4 : 0 }}
                    onClick={() => toggleItem(item.id)}
                    className="relative aspect-square rounded-xl flex flex-col items-center justify-center cursor-pointer group"
                    style={{
                      background: item.unlocked 
                        ? 'linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%)'
                        : 'rgba(255, 255, 255, 0.03)',
                      border: `2px solid ${rarityColors[item.rarity].color}`,
                      boxShadow: selectedItems.includes(item.id) 
                        ? `0 8px 30px ${rarityColors[item.rarity].glow}` 
                        : 'none',
                      opacity: item.unlocked ? 1 : 0.5,
                    }}
                  >
                    {/* Rarity Glow */}
                    {item.unlocked && (
                      <div 
                        className="absolute inset-0 rounded-xl opacity-0 group-hover:opacity-30 transition-opacity"
                        style={{
                          background: `radial-gradient(circle at center, ${rarityColors[item.rarity].color} 0%, transparent 70%)`,
                          filter: 'blur(20px)',
                        }}
                      />
                    )}

                    {/* Item Icon */}
                    <div 
                      className="text-5xl mb-2 relative z-10"
                      style={{
                        filter: item.unlocked ? 'none' : 'grayscale(1) brightness(0.5)',
                      }}
                    >
                      {item.category === 'clothes' && '👕'}
                      {item.category === 'accessories' && '👑'}
                      {item.category === 'pets' && '🐺'}
                    </div>

                    {/* Lock Icon */}
                    {!item.unlocked && (
                      <div className="absolute inset-0 flex items-center justify-center bg-black/60 rounded-xl backdrop-blur-sm">
                        <Lock className="w-8 h-8" style={{ color: '#9CA3AF' }} />
                      </div>
                    )}

                    {/* Selected Check */}
                    {selectedItems.includes(item.id) && item.unlocked && (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        className="absolute top-2 right-2 w-7 h-7 rounded-full flex items-center justify-center z-20"
                        style={{
                          background: 'linear-gradient(135deg, #10B981 0%, #059669 100%)',
                          boxShadow: '0 4px 15px rgba(16, 185, 129, 0.4)',
                        }}
                      >
                        <Check className="w-4 h-4 text-white" />
                      </motion.div>
                    )}

                    {/* Item Name */}
                    <div 
                      className="absolute -bottom-8 left-0 right-0 text-center text-xs font-medium px-1 truncate"
                      style={{ color: rarityColors[item.rarity].color }}
                    >
                      {item.name}
                    </div>

                    {/* Rarity Badge */}
                    <div 
                      className="absolute top-2 left-2 px-2 py-0.5 rounded text-[10px] font-bold uppercase z-10"
                      style={{
                        backgroundColor: rarityColors[item.rarity].color,
                        color: '#000',
                      }}
                    >
                      {item.rarity}
                    </div>
                  </motion.div>
                ))}
              </AnimatePresence>
            </div>
          </div>

          {/* Apply Button */}
          <div className="p-6">
            <AnimatePresence>
              {hasChanges && (
                <motion.button
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: 20 }}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => setHasChanges(false)}
                  className="w-full py-4 rounded-xl font-bold relative overflow-hidden"
                  style={{
                    background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                    boxShadow: '0 10px 40px rgba(124, 77, 255, 0.4)',
                  }}
                >
                  <span className="relative z-10">Применить изменения</span>
                  
                  {/* Animated shine */}
                  <motion.div
                    className="absolute inset-0"
                    style={{
                      background: 'linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent)',
                    }}
                    animate={{
                      x: ['-100%', '100%'],
                    }}
                    transition={{
                      duration: 1.5,
                      repeat: Infinity,
                      repeatDelay: 0.5,
                    }}
                  />
                </motion.button>
              )}
            </AnimatePresence>
          </div>
        </div>
      </div>
    </div>
  );
}
