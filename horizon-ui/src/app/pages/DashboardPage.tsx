import { useState } from 'react';
import WindowControls from '../components/WindowControls';
import ParticleBackground from '../components/ParticleBackground';
import { Bell, User, ChevronLeft, ChevronRight, Play, Users, Zap, Trophy, TrendingUp } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

const isElectron = typeof window !== 'undefined' && (window as any).electronAPI;

interface DashboardPageProps {
  user: any;
}

const newsItems = [
  {
    id: 1,
    title: 'Обновление 2.0: Новая эра начинается!',
    date: '5 января 2026',
    image: 'https://images.unsplash.com/photo-1631499792544-3c313e2a2511?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxtaW5lY3JhZnQlMjBnYW1pbmclMjBiYW5uZXJ8ZW58MXx8fHwxNzY3ODI0MjY3fDA&ixlib=rb-4.1.0&q=80&w=1080',
    description: 'Встречайте грандиозное обновление с новыми режимами игры, уникальными предметами и захватывающими квестами!',
    tag: 'Обновление',
  },
  {
    id: 2,
    title: 'Зимний турнир: Битва легенд',
    date: '3 января 2026',
    image: 'https://images.unsplash.com/photo-1676912002444-6a54ce34f5a3?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxnYW1pbmclMjBuZXdzJTIwYmFubmVyfGVufDF8fHx8MTc2NzgyNDI2N3ww&ixlib=rb-4.1.0&q=80&w=1080',
    description: 'Участвуйте в зимнем турнире PvP и получайте эксклюзивные награды! Призовой фонд 100,000 монет.',
    tag: 'Ивент',
  },
  {
    id: 3,
    title: 'Новая коллекция премиум скинов',
    date: '1 января 2026',
    image: 'https://images.unsplash.com/photo-1663010363660-d75c1c69958b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxnYW1lJTIwdXBkYXRlJTIwYW5ub3VuY2VtZW50fGVufDF8fHx8MTc2NzgyNDI2OHww&ixlib=rb-4.1.0&q=80&w=1080',
    description: 'Проверьте новую коллекцию премиум скинов в нашем магазине! Скидка 30% в первую неделю.',
    tag: 'Магазин',
  },
];

const servers = [
  { 
    name: 'Анархия', 
    online: 487, 
    max: 500, 
    mode: 'Выживание',
    description: 'Полная свобода действий',
    status: 'hot',
    region: 'RU',
  },
  { 
    name: 'SkyBlock', 
    online: 342, 
    max: 400, 
    mode: 'Скайблок',
    description: 'Развивайте свой остров',
    status: 'stable',
    region: 'EU',
  },
  { 
    name: 'MiniGames', 
    online: 256, 
    max: 500, 
    mode: 'Мини-игры',
    description: 'BedWars, SkyWars и другие',
    status: 'new',
    region: 'RU',
  },
  { 
    name: 'Creative', 
    online: 89, 
    max: 200, 
    mode: 'Креатив',
    description: 'Строй без ограничений',
    status: 'stable',
    region: 'RU',
  },
];

const stats = [
  { label: 'Игроков онлайн', value: '1,174', icon: Users, color: '#7C4DFF' },
  { label: 'Активных серверов', value: '4', icon: Zap, color: '#10B981' },
  { label: 'Ваш ранг', value: '#247', icon: Trophy, color: '#F59E0B' },
  { label: 'Уровень', value: '15', icon: TrendingUp, color: '#3B82F6' },
];

export default function DashboardPage({ user }: DashboardPageProps) {
  const [currentNews, setCurrentNews] = useState(0);
  const [expandedNews, setExpandedNews] = useState<number | null>(null);
  const [notifications, setNotifications] = useState(3);

  const nextNews = () => setCurrentNews((prev) => (prev + 1) % newsItems.length);
  const prevNews = () => setCurrentNews((prev) => (prev - 1 + newsItems.length) % newsItems.length);

  return (
    <div className="flex h-screen relative">
      <ParticleBackground />
      
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
            <h1 className="text-2xl font-bold">Добро пожаловать, Player123!</h1>
            <p className="text-sm" style={{ color: '#9CA3AF' }}>
              Готов к новым приключениям?
            </p>
          </div>

          <div className="flex items-center gap-4">
            {/* Balance */}
            <motion.div 
              whileHover={{ scale: 1.05 }}
              className="flex items-center gap-3 px-6 py-3 rounded-xl cursor-pointer"
              style={{ 
                background: 'linear-gradient(135deg, rgba(255, 215, 0, 0.2) 0%, rgba(255, 165, 0, 0.1) 100%)',
                border: '1px solid rgba(255, 215, 0, 0.3)',
              }}
            >
              <div 
                className="w-8 h-8 rounded-full flex items-center justify-center"
                style={{ background: 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)' }}
              >
                <span className="text-lg">💎</span>
              </div>
              <div>
                <div className="text-sm" style={{ color: '#9CA3AF' }}>Баланс</div>
                <div className="font-bold text-lg" style={{ color: '#FFD700' }}>1,250</div>
              </div>
            </motion.div>

            {/* Notifications */}
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.95 }}
              className="relative p-3 rounded-xl"
              style={{ 
                backgroundColor: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid rgba(124, 77, 255, 0.2)',
              }}
            >
              <Bell className="w-6 h-6" style={{ color: '#E0E0E0' }} />
              {notifications > 0 && (
                <motion.div 
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  className="absolute -top-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold"
                  style={{ backgroundColor: '#EF4444', color: '#fff' }}
                >
                  {notifications}
                </motion.div>
              )}
            </motion.button>

            {/* Profile */}
            <motion.div 
              whileHover={{ scale: 1.05 }}
              className="flex items-center gap-3 px-4 py-2 rounded-xl cursor-pointer"
              style={{ 
                backgroundColor: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid rgba(124, 77, 255, 0.2)',
              }}
            >
              <div 
                className="w-10 h-10 rounded-xl flex items-center justify-center relative overflow-hidden"
                style={{ background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)' }}
              >
                <User className="w-6 h-6 text-white relative z-10" />
                <motion.div
                  className="absolute inset-0"
                  animate={{
                    background: [
                      'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                      'linear-gradient(135deg, #B794F6 0%, #7C4DFF 100%)',
                    ],
                  }}
                  transition={{ duration: 2, repeat: Infinity, repeatType: 'reverse' }}
                />
              </div>
              <div>
                <div className="font-medium">Player123</div>
                <div className="text-xs flex items-center gap-1" style={{ color: '#7C4DFF' }}>
                  <span>Уровень 15</span>
                  <div className="w-1 h-1 rounded-full bg-[#7C4DFF]" />
                  <span>VIP</span>
                </div>
              </div>
            </motion.div>

            <WindowControls />
          </div>
        </div>

        {/* Main Content */}
        <div className="flex-1 overflow-y-auto p-8 relative">
          {/* Stats Cards */}
          <div className="grid grid-cols-4 gap-4 mb-8">
            {stats.map((stat, index) => {
              const Icon = stat.icon;
              return (
                <motion.div
                  key={stat.label}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.1 }}
                  whileHover={{ y: -4, scale: 1.02 }}
                  className="p-6 rounded-xl relative overflow-hidden"
                  style={{
                    background: 'rgba(255, 255, 255, 0.03)',
                    backdropFilter: 'blur(20px)',
                    border: '1px solid rgba(124, 77, 255, 0.2)',
                  }}
                >
                  <div className="flex items-start justify-between mb-4">
                    <div 
                      className="p-3 rounded-lg"
                      style={{ 
                        backgroundColor: `${stat.color}20`,
                        border: `1px solid ${stat.color}40`,
                      }}
                    >
                      <Icon className="w-5 h-5" style={{ color: stat.color }} />
                    </div>
                  </div>
                  <div className="text-3xl font-bold mb-1">{stat.value}</div>
                  <div className="text-sm" style={{ color: '#9CA3AF' }}>{stat.label}</div>
                  
                  {/* Subtle gradient overlay */}
                  <div 
                    className="absolute top-0 right-0 w-32 h-32 rounded-full opacity-20"
                    style={{
                      background: `radial-gradient(circle, ${stat.color} 0%, transparent 70%)`,
                      filter: 'blur(30px)',
                    }}
                  />
                </motion.div>
              );
            })}
          </div>

          {/* News Slider */}
          <div className="mb-8">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold">Последние новости</h2>
              <div className="flex gap-2">
                {newsItems.map((_, index) => (
                  <motion.button
                    key={index}
                    onClick={() => setCurrentNews(index)}
                    whileHover={{ scale: 1.2 }}
                    className="rounded-full transition-all"
                    style={{
                      width: currentNews === index ? '32px' : '8px',
                      height: '8px',
                      backgroundColor: currentNews === index ? '#7C4DFF' : 'rgba(124, 77, 255, 0.3)',
                    }}
                  />
                ))}
              </div>
            </div>
            
            <div className="relative">
              <AnimatePresence mode="wait">
                <motion.div
                  key={currentNews}
                  initial={{ opacity: 0, x: 100 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -100 }}
                  transition={{ duration: 0.5, type: 'spring' }}
                  className="relative rounded-2xl overflow-hidden group cursor-pointer"
                  style={{ height: '500px' }}
                  onClick={() => setExpandedNews(expandedNews === newsItems[currentNews].id ? null : newsItems[currentNews].id)}
                >
                  {/* Background Image */}
                  <img 
                    src={newsItems[currentNews].image}
                    alt={newsItems[currentNews].title}
                    className="absolute inset-0 w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                  />
                  
                  {/* Gradient Overlay */}
                  <div 
                    className="absolute inset-0"
                    style={{
                      background: 'linear-gradient(to top, rgba(15, 15, 19, 0.98) 0%, rgba(15, 15, 19, 0.7) 40%, transparent 70%)',
                    }}
                  />

                  {/* Content */}
                  <div className="absolute bottom-0 left-0 right-0 p-10">
                    <motion.div
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.2 }}
                    >
                      <div className="flex items-center gap-4 mb-4">
                        <span 
                          className="px-4 py-1.5 rounded-full text-sm font-medium"
                          style={{
                            background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                            color: '#fff',
                          }}
                        >
                          {newsItems[currentNews].tag}
                        </span>
                        <span className="text-sm" style={{ color: '#9CA3AF' }}>
                          {newsItems[currentNews].date}
                        </span>
                      </div>
                      <h3 className="text-4xl font-bold mb-4">{newsItems[currentNews].title}</h3>
                      <p className="text-lg mb-6" style={{ color: '#D1D5DB' }}>
                        {newsItems[currentNews].description}
                      </p>
                      
                      <motion.button
                        whileHover={{ scale: 1.05, x: 5 }}
                        className="px-6 py-3 rounded-xl font-medium inline-flex items-center gap-2"
                        style={{
                          background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                          color: '#fff',
                        }}
                      >
                        Подробнее
                        <ChevronRight className="w-5 h-5" />
                      </motion.button>
                    </motion.div>
                  </div>

                  {/* Navigation Arrows */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      prevNews();
                    }}
                    className="absolute left-6 top-1/2 -translate-y-1/2 p-4 rounded-full opacity-0 group-hover:opacity-100 transition-all"
                    style={{
                      backgroundColor: 'rgba(0, 0, 0, 0.7)',
                      backdropFilter: 'blur(10px)',
                    }}
                  >
                    <ChevronLeft className="w-6 h-6" />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      nextNews();
                    }}
                    className="absolute right-6 top-1/2 -translate-y-1/2 p-4 rounded-full opacity-0 group-hover:opacity-100 transition-all"
                    style={{
                      backgroundColor: 'rgba(0, 0, 0, 0.7)',
                      backdropFilter: 'blur(10px)',
                    }}
                  >
                    <ChevronRight className="w-6 h-6" />
                  </button>
                </motion.div>
              </AnimatePresence>
            </div>
          </div>

          {/* Server Grid */}
          <div>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold">Доступные серверы</h2>
              <div className="text-sm" style={{ color: '#9CA3AF' }}>
                Онлайн: <span style={{ color: '#10B981' }}>1,174</span> игроков
              </div>
            </div>
            
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-6">
              {servers.map((server, index) => (
                <motion.div
                  key={server.name}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.1 }}
                  whileHover={{ y: -8, scale: 1.02 }}
                  className="relative p-6 rounded-2xl overflow-hidden group"
                  style={{
                    background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%)',
                    backdropFilter: 'blur(20px)',
                    border: '1px solid rgba(124, 77, 255, 0.2)',
                  }}
                >
                  {/* Status Badge */}
                  <div className="absolute top-4 right-4">
                    {server.status === 'hot' && (
                      <motion.div
                        animate={{ scale: [1, 1.1, 1] }}
                        transition={{ duration: 2, repeat: Infinity }}
                        className="px-3 py-1 rounded-full text-xs font-bold"
                        style={{ background: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)', color: '#fff' }}
                      >
                        🔥 HOT
                      </motion.div>
                    )}
                    {server.status === 'new' && (
                      <div className="px-3 py-1 rounded-full text-xs font-bold" style={{ background: 'linear-gradient(135deg, #10B981 0%, #059669 100%)', color: '#fff' }}>
                        ✨ NEW
                      </div>
                    )}
                  </div>

                  <div className="mb-6">
                    <h3 className="text-2xl font-bold mb-2">{server.name}</h3>
                    <div className="text-sm mb-1" style={{ color: '#9CA3AF' }}>{server.mode}</div>
                    <div className="text-xs" style={{ color: '#6B7280' }}>{server.description}</div>
                  </div>

                  {/* Server Info */}
                  <div className="space-y-3 mb-6">
                    <div className="flex items-center justify-between text-sm">
                      <span style={{ color: '#9CA3AF' }}>Онлайн</span>
                      <span className="font-medium">
                        <span style={{ color: '#10B981' }}>{server.online}</span>
                        <span style={{ color: '#6B7280' }}>/{server.max}</span>
                      </span>
                    </div>
                    
                    {/* Progress Bar */}
                    <div className="h-1.5 rounded-full overflow-hidden" style={{ backgroundColor: 'rgba(255, 255, 255, 0.1)' }}>
                      <motion.div
                        initial={{ width: 0 }}
                        animate={{ width: `${(server.online / server.max) * 100}%` }}
                        transition={{ duration: 1, delay: index * 0.1 }}
                        className="h-full rounded-full"
                        style={{
                          background: server.online / server.max > 0.8 
                            ? 'linear-gradient(90deg, #EF4444 0%, #DC2626 100%)'
                            : 'linear-gradient(90deg, #10B981 0%, #059669 100%)',
                        }}
                      />
                    </div>

                    <div className="flex items-center gap-2 text-xs" style={{ color: '#9CA3AF' }}>
                      <span>🌍 {server.region}</span>
                      <div className="w-1 h-1 rounded-full bg-gray-600" />
                      <span>Пинг: {Math.floor(Math.random() * 30 + 10)}ms</span>
                    </div>
                  </div>

                  {/* Play Button */}
                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => {
                      if (window.javaBridge && window.javaBridge.launchGame) {
                        window.javaBridge.launchGame(server.name);
                      } else {
                        console.log(`Dev: launch ${server.name}`);
                      }
                    }}
                    className="w-full py-3 px-4 rounded-xl flex items-center justify-center gap-2 font-bold relative overflow-hidden group/btn"
                    style={{
                      background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                      boxShadow: '0 4px 20px rgba(124, 77, 255, 0.4)',
                    }}
                  >
                    <Play className="w-5 h-5" />
                    <span>Играть</span>
                    
                    {/* Hover effect */}
                    <motion.div
                      className="absolute inset-0 opacity-0 group-hover/btn:opacity-100"
                      style={{
                        background: 'linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent)',
                      }}
                      animate={{ x: ['-100%', '100%'] }}
                      transition={{ duration: 1.5, repeat: Infinity }}
                    />
                  </motion.button>

                  {/* Background Effect */}
                  <div 
                    className="absolute -bottom-10 -right-10 w-40 h-40 rounded-full opacity-0 group-hover:opacity-20 transition-opacity duration-500"
                    style={{
                      background: 'radial-gradient(circle, #7C4DFF 0%, transparent 70%)',
                      filter: 'blur(40px)',
                    }}
                  />
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
