import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import { Lock, Check } from 'lucide-react';
import { motion } from 'motion/react';

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
  common: '#9CA3AF',
  rare: '#3B82F6',
  epic: '#A855F7',
  legendary: '#F59E0B',
};

const items: WardrobeItem[] = [
  { id: 1, name: 'Кожаная броня', rarity: 'common', unlocked: true, equipped: true, category: 'clothes' },
  { id: 2, name: 'Железная броня', rarity: 'rare', unlocked: true, equipped: false, category: 'clothes' },
  { id: 3, name: 'Алмазная броня', rarity: 'epic', unlocked: true, equipped: false, category: 'clothes' },
  { id: 4, name: 'Незеритовая броня', rarity: 'legendary', unlocked: false, equipped: false, category: 'clothes' },
  { id: 5, name: 'Крылья', rarity: 'rare', unlocked: true, equipped: false, category: 'accessories' },
  { id: 6, name: 'Корона', rarity: 'epic', unlocked: false, equipped: false, category: 'accessories' },
  { id: 7, name: 'Волк', rarity: 'common', unlocked: true, equipped: false, category: 'pets' },
  { id: 8, name: 'Дракон', rarity: 'legendary', unlocked: false, equipped: false, category: 'pets' },
];

export default function WardrobePage() {
  const [activeTab, setActiveTab] = useState<'clothes' | 'accessories' | 'pets'>('clothes');
  const [rotation, setRotation] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [startX, setStartX] = useState(0);
  const [selectedItems, setSelectedItems] = useState<number[]>([1]);

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

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const toggleItem = (itemId: number) => {
    setSelectedItems((prev) =>
      prev.includes(itemId)
        ? prev.filter((id) => id !== itemId)
        : [...prev, itemId]
    );
  };

  const filteredItems = items.filter((item) => item.category === activeTab);

  return (
    <div className="flex h-screen">
      <Sidebar />
      
      <div className="flex-1 flex">
        {/* 3D Просмотр персонажа */}
        <div 
          className="flex-1 flex items-center justify-center p-8"
          style={{
            background: 'radial-gradient(circle at center, rgba(124, 77, 255, 0.1) 0%, transparent 70%)',
          }}
        >
          <div className="relative">
            {/* 3D модель (имитация) */}
            <motion.div
              className="relative"
              onMouseDown={handleMouseDown}
              onMouseMove={handleMouseMove}
              onMouseUp={handleMouseUp}
              onMouseLeave={handleMouseUp}
              style={{
                cursor: isDragging ? 'grabbing' : 'grab',
                width: '400px',
                height: '500px',
              }}
            >
              <motion.div
                animate={{ rotateY: rotation }}
                transition={{ type: 'spring', stiffness: 50, damping: 20 }}
                className="w-full h-full flex items-center justify-center"
                style={{
                  transformStyle: 'preserve-3d',
                }}
              >
                {/* Тело персонажа (упрощенная версия) */}
                <div 
                  className="relative"
                  style={{
                    width: '120px',
                    height: '320px',
                    background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                    borderRadius: '12px',
                    boxShadow: '0 20px 60px rgba(124, 77, 255, 0.4)',
                  }}
                >
                  {/* Голова */}
                  <div 
                    className="absolute -top-16 left-1/2 -translate-x-1/2"
                    style={{
                      width: '80px',
                      height: '80px',
                      background: 'linear-gradient(135deg, #9D7FF9 0%, #C8ADFF 100%)',
                      borderRadius: '12px',
                      boxShadow: '0 10px 30px rgba(124, 77, 255, 0.3)',
                    }}
                  >
                    {/* Глаза */}
                    <div className="absolute top-1/3 left-1/4 w-3 h-3 bg-white rounded-sm" />
                    <div className="absolute top-1/3 right-1/4 w-3 h-3 bg-white rounded-sm" />
                  </div>

                  {/* Руки */}
                  <div 
                    className="absolute top-8 -left-10"
                    style={{
                      width: '30px',
                      height: '100px',
                      background: 'linear-gradient(135deg, #9D7FF9 0%, #C8ADFF 100%)',
                      borderRadius: '8px',
                    }}
                  />
                  <div 
                    className="absolute top-8 -right-10"
                    style={{
                      width: '30px',
                      height: '100px',
                      background: 'linear-gradient(135deg, #9D7FF9 0%, #C8ADFF 100%)',
                      borderRadius: '8px',
                    }}
                  />

                  {/* Ноги */}
                  <div 
                    className="absolute -bottom-28 left-4"
                    style={{
                      width: '30px',
                      height: '110px',
                      background: 'linear-gradient(135deg, #6A3FD9 0%, #9D7FF9 100%)',
                      borderRadius: '8px',
                    }}
                  />
                  <div 
                    className="absolute -bottom-28 right-4"
                    style={{
                      width: '30px',
                      height: '110px',
                      background: 'linear-gradient(135deg, #6A3FD9 0%, #9D7FF9 100%)',
                      borderRadius: '8px',
                    }}
                  />
                </div>
              </motion.div>

              {/* Частицы вокруг персонажа */}
              {[...Array(8)].map((_, i) => (
                <motion.div
                  key={i}
                  className="absolute w-2 h-2 rounded-full"
                  style={{
                    backgroundColor: '#7C4DFF',
                    top: `${Math.random() * 100}%`,
                    left: `${Math.random() * 100}%`,
                    filter: 'blur(2px)',
                  }}
                  animate={{
                    y: [0, -20, 0],
                    opacity: [0.3, 0.8, 0.3],
                  }}
                  transition={{
                    duration: 2 + Math.random() * 2,
                    repeat: Infinity,
                    delay: i * 0.2,
                  }}
                />
              ))}

              {/* Тень */}
              <div 
                className="absolute -bottom-16 left-1/2 -translate-x-1/2"
                style={{
                  width: '200px',
                  height: '20px',
                  background: 'radial-gradient(ellipse at center, rgba(0, 0, 0, 0.3) 0%, transparent 70%)',
                  filter: 'blur(10px)',
                }}
              />
            </motion.div>

            {/* Подсказка */}
            <div 
              className="absolute bottom-0 left-1/2 -translate-x-1/2 px-4 py-2 rounded-lg text-sm"
              style={{
                backgroundColor: 'rgba(0, 0, 0, 0.7)',
                backdropFilter: 'blur(10px)',
                color: '#9CA3AF',
              }}
            >
              Перетащите, чтобы повернуть
            </div>
          </div>
        </div>

        {/* Инвентарь */}
        <div 
          className="w-[450px] flex flex-col"
          style={{
            backgroundColor: 'rgba(0, 0, 0, 0.3)',
            borderLeft: '1px solid rgba(124, 77, 255, 0.2)',
          }}
        >
          {/* Вкладки */}
          <div 
            className="flex border-b"
            style={{ borderColor: 'rgba(124, 77, 255, 0.2)' }}
          >
            {[
              { key: 'clothes' as const, label: 'Одежда' },
              { key: 'accessories' as const, label: 'Аксессуары' },
              { key: 'pets' as const, label: 'Питомцы' },
            ].map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className="flex-1 py-4 px-6 font-medium transition-all"
                style={{
                  color: activeTab === tab.key ? '#7C4DFF' : '#9CA3AF',
                  borderBottom: activeTab === tab.key ? '2px solid #7C4DFF' : '2px solid transparent',
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Сетка предметов */}
          <div className="flex-1 overflow-y-auto p-6">
            <div className="grid grid-cols-3 gap-4">
              {filteredItems.map((item) => (
                <motion.div
                  key={item.id}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => item.unlocked && toggleItem(item.id)}
                  className="relative aspect-square rounded-xl flex items-center justify-center cursor-pointer"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.05)',
                    border: `2px solid ${rarityColors[item.rarity]}`,
                    opacity: item.unlocked ? 1 : 0.5,
                  }}
                >
                  {/* Иконка предмета */}
                  <div 
                    className="text-4xl"
                    style={{
                      filter: item.unlocked ? 'none' : 'grayscale(1)',
                    }}
                  >
                    {item.category === 'clothes' && '👕'}
                    {item.category === 'accessories' && '👑'}
                    {item.category === 'pets' && '🐺'}
                  </div>

                  {/* Замок для закрытых предметов */}
                  {!item.unlocked && (
                    <div className="absolute inset-0 flex items-center justify-center bg-black/50 rounded-xl">
                      <Lock className="w-6 h-6" style={{ color: '#9CA3AF' }} />
                    </div>
                  )}

                  {/* Галочка для выбранных */}
                  {selectedItems.includes(item.id) && item.unlocked && (
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      className="absolute top-2 right-2 w-6 h-6 rounded-full flex items-center justify-center"
                      style={{
                        backgroundColor: '#10B981',
                      }}
                    >
                      <Check className="w-4 h-4 text-white" />
                    </motion.div>
                  )}

                  {/* Название предмета */}
                  <div 
                    className="absolute -bottom-6 left-0 right-0 text-center text-xs"
                    style={{ color: rarityColors[item.rarity] }}
                  >
                    {item.name}
                  </div>
                </motion.div>
              ))}
            </div>
          </div>

          {/* Кнопка применить */}
          <div className="p-6">
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="w-full py-4 rounded-lg font-medium"
              style={{
                background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                boxShadow: '0 4px 20px rgba(124, 77, 255, 0.4)',
              }}
            >
              Применить изменения
            </motion.button>
          </div>
        </div>
      </div>
    </div>
  );
}
