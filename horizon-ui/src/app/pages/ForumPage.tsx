import { useState, useEffect } from 'react';
import WindowControls from '../components/WindowControls';
import ParticleBackground from '../components/ParticleBackground';
import { MessageSquare, TrendingUp, Users, Eye, MessageCircle, ThumbsUp, Pin, Lock, ChevronRight, Search } from 'lucide-react';
import { motion } from 'motion/react';
import { Avatar, AvatarImage, AvatarFallback } from '../components/ui/avatar';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';

interface ForumPageProps {
  user: any;
}

// XenForo-подобная структура как на Majestic RP
const forumCategories = [
  {
    id: 1,
    name: 'Новости проекта',
    description: 'Официальные новости и объявления',
    icon: '📰',
    color: '#7C4DFF',
    subforums: [
      { id: 101, name: 'Обновления', topics: 15, posts: 234, lastPost: { author: 'Admin', time: '5 минут назад', topic: 'Обновление 2.0' } },
      { id: 102, name: 'События', topics: 8, posts: 156, lastPost: { author: 'Curator', time: '1 час назад', topic: 'Зимний турнир' } }
    ]
  },
  {
    id: 2,
    name: 'Общение',
    description: 'Общайтесь с игроками',
    icon: '💬',
    color: '#00D4FF',
    subforums: [
      { id: 201, name: 'Общий чат', topics: 542, posts: 8934, lastPost: { author: 'Player123', time: '2 минуты назад', topic: 'Привет всем!' } },
      { id: 202, name: 'Поиск друзей', topics: 89, posts: 456, lastPost: { author: 'Player456', time: '10 минут назад', topic: 'Ищу тиммейтов' } }
    ]
  },
  {
    id: 3,
    name: 'Поддержка',
    description: 'Помощь и вопросы',
    icon: '🛠️',
    color: '#FFD93D',
    subforums: [
      { id: 301, name: 'Баг-репорты', topics: 78, posts: 345, lastPost: { author: 'Helper', time: '15 минут назад', topic: 'Исправлено: вылет игры' } },
      { id: 302, name: 'Вопросы', topics: 234, posts: 1234, lastPost: { author: 'Player789', time: '5 минут назад', topic: 'Как установить моды?' } }
    ]
  },
  {
    id: 4,
    name: 'Гайды и туториалы',
    description: 'Обучающие материалы',
    icon: '📚',
    color: '#6BCF7F',
    subforums: [
      { id: 401, name: 'Гайды для новичков', topics: 45, posts: 567, lastPost: { author: 'Admin', time: '2 часа назад', topic: 'Начало игры' } },
      { id: 402, name: 'Продвинутые гайды', topics: 23, posts: 289, lastPost: { author: 'ProPlayer', time: '1 день назад', topic: 'Эффективный фарм' } }
    ]
  }
];

const recentTopics = [
  { id: 1, title: 'Глобальное обновление 2.0!', author: 'Admin', replies: 156, views: 2456, lastPost: '5 минут назад', isPinned: true, category: 'Новости' },
  { id: 2, title: 'Зимний турнир - регистрация открыта', author: 'Curator', replies: 89, views: 1234, lastPost: '10 минут назад', isPinned: true, category: 'События' },
  { id: 3, title: 'Ищу тиммейтов для PvP', author: 'Player123', replies: 34, views: 567, lastPost: '15 минут назад', category: 'Поиск друзей' },
  { id: 4, title: 'Как получить донат предметы?', author: 'Newbie', replies: 12, views: 234, lastPost: '20 минут назад', category: 'Вопросы' },
  { id: 5, title: 'Гайд по крафту легендарных предметов', author: 'ProPlayer', replies: 67, views: 1890, lastPost: '30 минут назад', category: 'Гайды' }
];

const forumStats = {
  totalTopics: 1234,
  totalPosts: 23456,
  totalMembers: 5678,
  newestMember: 'NewPlayer123',
  onlineNow: 247
};

export default function ForumPage({ user }: ForumPageProps) {
  const [searchQuery, setSearchQuery] = useState('');

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
            <h1 className="text-3xl font-bold flex items-center gap-3">
              <MessageSquare className="w-8 h-8" style={{ color: '#7C4DFF' }} />
              Форум Horizon
            </h1>
            <p className="text-sm" style={{ color: '#9CA3AF' }}>
              {forumStats.onlineNow} игроков онлайн • {forumStats.totalMembers} участников
            </p>
          </div>
          <WindowControls />
        </div>

        {/* Main Content */}
        <div className="flex-1 overflow-auto custom-scrollbar p-8">
          {/* Search Bar */}
          <div className="mb-6">
            <div className="relative max-w-2xl">
              <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
              <Input
                type="text"
                placeholder="Поиск по форуму..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-12 pr-4 py-3 rounded-xl"
                style={{
                  backgroundColor: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid rgba(124, 77, 255, 0.3)',
                  color: '#E0E0E0'
                }}
              />
            </div>
          </div>

          {/* Stats Bar */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
            <motion.div
              whileHover={{ scale: 1.02 }}
              className="p-4 rounded-xl"
              style={{
                background: 'linear-gradient(135deg, rgba(124, 77, 255, 0.2) 0%, rgba(124, 77, 255, 0.1) 100%)',
                border: '1px solid rgba(124, 77, 255, 0.3)',
              }}
            >
              <div className="flex items-center gap-3">
                <MessageSquare className="w-8 h-8 text-purple-400" />
                <div>
                  <p className="text-2xl font-bold">{forumStats.totalTopics.toLocaleString()}</p>
                  <p className="text-xs text-gray-400">Тем</p>
                </div>
              </div>
            </motion.div>

            <motion.div
              whileHover={{ scale: 1.02 }}
              className="p-4 rounded-xl"
              style={{
                background: 'linear-gradient(135deg, rgba(0, 212, 255, 0.2) 0%, rgba(0, 212, 255, 0.1) 100%)',
                border: '1px solid rgba(0, 212, 255, 0.3)',
              }}
            >
              <div className="flex items-center gap-3">
                <MessageCircle className="w-8 h-8 text-cyan-400" />
                <div>
                  <p className="text-2xl font-bold">{forumStats.totalPosts.toLocaleString()}</p>
                  <p className="text-xs text-gray-400">Сообщений</p>
                </div>
              </div>
            </motion.div>

            <motion.div
              whileHover={{ scale: 1.02 }}
              className="p-4 rounded-xl"
              style={{
                background: 'linear-gradient(135deg, rgba(107, 207, 127, 0.2) 0%, rgba(107, 207, 127, 0.1) 100%)',
                border: '1px solid rgba(107, 207, 127, 0.3)',
              }}
            >
              <div className="flex items-center gap-3">
                <Users className="w-8 h-8 text-green-400" />
                <div>
                  <p className="text-2xl font-bold">{forumStats.totalMembers.toLocaleString()}</p>
                  <p className="text-xs text-gray-400">Участников</p>
                </div>
              </div>
            </motion.div>

            <motion.div
              whileHover={{ scale: 1.02 }}
              className="p-4 rounded-xl"
              style={{
                background: 'linear-gradient(135deg, rgba(255, 217, 61, 0.2) 0%, rgba(255, 217, 61, 0.1) 100%)',
                border: '1px solid rgba(255, 217, 61, 0.3)',
              }}
            >
              <div className="flex items-center gap-3">
                <TrendingUp className="w-8 h-8 text-yellow-400" />
                <div>
                  <p className="text-2xl font-bold">{forumStats.onlineNow}</p>
                  <p className="text-xs text-gray-400">Онлайн</p>
                </div>
              </div>
            </motion.div>
          </div>

          {/* Categories */}
          <div className="space-y-6">
            {forumCategories.map((category) => (
              <motion.div
                key={category.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="rounded-xl overflow-hidden"
                style={{
                  backgroundColor: 'rgba(0, 0, 0, 0.3)',
                  backdropFilter: 'blur(20px)',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                }}
              >
                {/* Category Header */}
                <div 
                  className="px-6 py-4 flex items-center gap-3"
                  style={{
                    background: `linear-gradient(135deg, ${category.color}40 0%, ${category.color}20 100%)`,
                    borderBottom: `1px solid ${category.color}60`
                  }}
                >
                  <span className="text-3xl">{category.icon}</span>
                  <div>
                    <h2 className="text-xl font-bold text-white">{category.name}</h2>
                    <p className="text-sm text-gray-300">{category.description}</p>
                  </div>
                </div>

                {/* Subforums */}
                <div className="divide-y divide-white/10">
                  {category.subforums.map((subforum) => (
                    <motion.div
                      key={subforum.id}
                      whileHover={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
                      className="px-6 py-4 flex items-center gap-6 cursor-pointer"
                    >
                      <div 
                        className="w-12 h-12 rounded-xl flex items-center justify-center text-2xl"
                        style={{
                          background: `linear-gradient(135deg, ${category.color} 0%, ${category.color}80 100%)`,
                        }}
                      >
                        {category.icon}
                      </div>

                      <div className="flex-1">
                        <h3 className="text-lg font-semibold text-white mb-1 flex items-center gap-2">
                          {subforum.name}
                          <ChevronRight size={16} className="text-gray-400" />
                        </h3>
                        <div className="flex items-center gap-4 text-sm text-gray-400">
                          <span className="flex items-center gap-1">
                            <MessageSquare size={14} />
                            {subforum.topics} тем
                          </span>
                          <span className="flex items-center gap-1">
                            <MessageCircle size={14} />
                            {subforum.posts} постов
                          </span>
                        </div>
                      </div>

                      <div className="text-right min-w-[200px]">
                        <p className="text-sm text-white mb-1">{subforum.lastPost.topic}</p>
                        <p className="text-xs text-gray-400">
                          {subforum.lastPost.author} • {subforum.lastPost.time}
                        </p>
                      </div>
                    </motion.div>
                  ))}
                </div>
              </motion.div>
            ))}
          </div>

          {/* Recent Activity */}
          <div className="mt-8">
            <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-2">
              <TrendingUp className="w-6 h-6 text-purple-400" />
              Последняя активность
            </h2>

            <div 
              className="rounded-xl overflow-hidden"
              style={{
                backgroundColor: 'rgba(0, 0, 0, 0.3)',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
              }}
            >
              <div className="divide-y divide-white/10">
                {recentTopics.map((topic) => (
                  <motion.div
                    key={topic.id}
                    whileHover={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
                    className="px-6 py-4 flex items-center gap-4 cursor-pointer"
                  >
                    {topic.isPinned && <Pin className="w-5 h-5 text-yellow-400" />}
                    
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-white mb-1">{topic.title}</h3>
                      <div className="flex items-center gap-4 text-sm text-gray-400">
                        <span>{topic.author}</span>
                        <span className="flex items-center gap-1">
                          <Eye size={14} />
                          {topic.views}
                        </span>
                        <span className="flex items-center gap-1">
                          <MessageCircle size={14} />
                          {topic.replies}
                        </span>
                        <Badge variant="secondary">{topic.category}</Badge>
                      </div>
                    </div>

                    <div className="text-right text-sm text-gray-400">
                      {topic.lastPost}
                    </div>
                  </motion.div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
