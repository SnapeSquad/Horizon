import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import { Bell, User, ChevronLeft, ChevronRight, Play } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { ImageWithFallback } from '../components/figma/ImageWithFallback';

const newsItems = [
  {
    id: 1,
    title: 'Обновление 2.0: Новые возможности!',
    date: '5 января 2026',
    image: 'https://images.unsplash.com/photo-1631499792544-3c313e2a2511?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxtaW5lY3JhZnQlMjBnYW1pbmclMjBiYW5uZXJ8ZW58MXx8fHwxNzY3ODI0MjY3fDA&ixlib=rb-4.1.0&q=80&w=1080',
    description: 'Встречайте грандиозное обновление с новыми режимами игры!',
  },
  {
    id: 2,
    title: 'Новый ивент: Зимняя битва',
    date: '3 января 2026',
    image: 'https://images.unsplash.com/photo-1676912002444-6a54ce34f5a3?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxnYW1pbmclMjBuZXdzJTIwYmFubmVyfGVufDF8fHx8MTc2NzgyNDI2N3ww&ixlib=rb-4.1.0&q=80&w=1080',
    description: 'Участвуйте в зимнем ивенте и получайте эксклюзивные награды!',
  },
  {
    id: 3,
    title: 'Новые скины в магазине',
    date: '1 января 2026',
    image: 'https://images.unsplash.com/photo-1663010363660-d75c1c69958b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxnYW1lJTIwdXBkYXRlJTIwYW5ub3VuY2VtZW50fGVufDF8fHx8MTc2NzgyNDI2OHww&ixlib=rb-4.1.0&q=80&w=1080',
    description: 'Проверьте новую коллекцию скинов в нашем магазине!',
  },
];

const servers = [
  { name: 'Анархия', online: 124, max: 500, mode: 'Выживание' },
  { name: 'SkyBlock', online: 89, max: 300, mode: 'Скайблок' },
  { name: 'MiniGames', online: 256, max: 400, mode: 'Мини-игры' },
  { name: 'Creative', online: 45, max: 200, mode: 'Креатив' },
];

export default function DashboardPage() {
  const [currentNews, setCurrentNews] = useState(0);
  const [expandedNews, setExpandedNews] = useState<number | null>(null);

  const nextNews = () => {
    setCurrentNews((prev) => (prev + 1) % newsItems.length);
  };

  const prevNews = () => {
    setCurrentNews((prev) => (prev - 1 + newsItems.length) % newsItems.length);
  };

  return (
    <div className="flex h-screen">
      <Sidebar />
      
      <div className="flex-1 flex flex-col">
        {/* Хедер с профилем */}
        <div 
          className="h-20 flex items-center justify-end px-8 gap-6"
          style={{
            backgroundColor: 'rgba(0, 0, 0, 0.3)',
            borderBottom: '1px solid rgba(124, 77, 255, 0.2)',
          }}
        >
          {/* Баланс */}
          <div className="flex items-center gap-2 px-4 py-2 rounded-lg" style={{ backgroundColor: 'rgba(124, 77, 255, 0.1)' }}>
            <div className="w-6 h-6 rounded-full" style={{ background: 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)' }} />
            <span className="font-medium">1,250 монет</span>
          </div>

          {/* Уведомления */}
          <motion.button
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.95 }}
            className="relative p-3 rounded-lg"
            style={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
          >
            <Bell className="w-5 h-5" style={{ color: '#E0E0E0' }} />
            <div 
              className="absolute top-2 right-2 w-2 h-2 rounded-full"
              style={{ backgroundColor: '#EF4444' }}
            />
          </motion.button>

          {/* Профиль */}
          <div className="flex items-center gap-3">
            <div 
              className="w-10 h-10 rounded-lg flex items-center justify-center"
              style={{ 
                background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
              }}
            >
              <User className="w-6 h-6 text-white" />
            </div>
            <div>
              <div className="font-medium">Player123</div>
              <div className="text-xs" style={{ color: '#9CA3AF' }}>Уровень 15</div>
            </div>
          </div>
        </div>

        {/* Основной контент */}
        <div className="flex-1 overflow-y-auto p-8">
          {/* Слайдер новостей */}
          <div className="mb-8">
            <h2 className="text-2xl font-bold mb-4">Новости</h2>
            
            <div className="relative">
              <AnimatePresence mode="wait">
                <motion.div
                  key={currentNews}
                  initial={{ opacity: 0, x: 100 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -100 }}
                  transition={{ duration: 0.3 }}
                  className="relative rounded-xl overflow-hidden cursor-pointer"
                  style={{
                    height: '400px',
                    backgroundColor: 'rgba(0, 0, 0, 0.3)',
                    border: '1px solid rgba(124, 77, 255, 0.2)',
                  }}
                  onClick={() => setExpandedNews(expandedNews === newsItems[currentNews].id ? null : newsItems[currentNews].id)}
                >
                  <img 
                    src={newsItems[currentNews].image}
                    alt={newsItems[currentNews].title}
                    className="w-full h-full object-cover opacity-60"
                  />
                  
                  <div 
                    className="absolute inset-0"
                    style={{
                      background: 'linear-gradient(to top, rgba(15, 15, 19, 0.95) 0%, transparent 50%)',
                    }}
                  />

                  <div className="absolute bottom-0 left-0 right-0 p-8">
                    <div className="text-sm mb-2" style={{ color: '#9CA3AF' }}>
                      {newsItems[currentNews].date}
                    </div>
                    <h3 className="text-3xl font-bold mb-3">{newsItems[currentNews].title}</h3>
                    <p style={{ color: '#9CA3AF' }}>{newsItems[currentNews].description}</p>
                    
                    {expandedNews === newsItems[currentNews].id && (
                      <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="mt-4 pt-4"
                        style={{ borderTop: '1px solid rgba(124, 77, 255, 0.2)' }}
                      >
                        <p style={{ color: '#E0E0E0' }}>
                          Полный текст новости с подробным описанием всех обновлений и изменений...
                        </p>
                      </motion.div>
                    )}
                  </div>
                </motion.div>
              </AnimatePresence>

              {/* Кнопки навигации */}
              <button
                onClick={prevNews}
                className="absolute left-4 top-1/2 -translate-y-1/2 p-3 rounded-full transition-all"
                style={{
                  backgroundColor: 'rgba(0, 0, 0, 0.5)',
                  backdropFilter: 'blur(10px)',
                }}
              >
                <ChevronLeft className="w-6 h-6" />
              </button>
              <button
                onClick={nextNews}
                className="absolute right-4 top-1/2 -translate-y-1/2 p-3 rounded-full transition-all"
                style={{
                  backgroundColor: 'rgba(0, 0, 0, 0.5)',
                  backdropFilter: 'blur(10px)',
                }}
              >
                <ChevronRight className="w-6 h-6" />
              </button>

              {/* Индикаторы */}
              <div className="flex justify-center gap-2 mt-4">
                {newsItems.map((_, index) => (
                  <button
                    key={index}
                    onClick={() => setCurrentNews(index)}
                    className="h-2 rounded-full transition-all"
                    style={{
                      width: currentNews === index ? '32px' : '8px',
                      backgroundColor: currentNews === index ? '#7C4DFF' : 'rgba(124, 77, 255, 0.3)',
                    }}
                  />
                ))}
              </div>
            </div>
          </div>

          {/* Выбор сервера */}
          <div>
            <h2 className="text-2xl font-bold mb-4">Выберите сервер</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {servers.map((server) => (
                <motion.div
                  key={server.name}
                  whileHover={{ scale: 1.02, y: -4 }}
                  className="p-6 rounded-xl"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.05)',
                    backdropFilter: 'blur(20px)',
                    border: '1px solid rgba(124, 77, 255, 0.2)',
                  }}
                >
                  <h3 className="text-xl font-bold mb-2">{server.name}</h3>
                  <div className="text-sm mb-1" style={{ color: '#9CA3AF' }}>{server.mode}</div>
                  
                  <div className="flex items-center gap-2 mb-4">
                    <div className="w-2 h-2 rounded-full" style={{ backgroundColor: '#10B981' }} />
                    <span className="text-sm" style={{ color: '#10B981' }}>
                      Online: {server.online}/{server.max}
                    </span>
                  </div>

                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    className="w-full py-3 px-4 rounded-lg flex items-center justify-center gap-2 font-medium transition-all"
                    style={{
                      background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                      boxShadow: '0 4px 15px rgba(124, 77, 255, 0.3)',
                    }}
                  >
                    <Play className="w-4 h-4" />
                    Играть
                  </motion.button>
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
