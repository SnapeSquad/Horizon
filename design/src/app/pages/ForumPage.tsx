import Sidebar from '../components/Sidebar';
import { ArrowLeft, ArrowRight, RotateCw } from 'lucide-react';

export default function ForumPage() {
  return (
    <div className="flex h-screen">
      <Sidebar />
      
      <div className="flex-1 flex flex-col">
        {/* Browser Header */}
        <div 
          className="h-16 flex items-center gap-4 px-6"
          style={{
            backgroundColor: 'rgba(0, 0, 0, 0.3)',
            borderBottom: '1px solid rgba(124, 77, 255, 0.2)',
          }}
        >
          <div className="flex gap-2">
            <button 
              className="p-2 rounded-lg transition-all"
              style={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
            >
              <ArrowLeft className="w-5 h-5" style={{ color: '#9CA3AF' }} />
            </button>
            <button 
              className="p-2 rounded-lg transition-all"
              style={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
            >
              <ArrowRight className="w-5 h-5" style={{ color: '#9CA3AF' }} />
            </button>
            <button 
              className="p-2 rounded-lg transition-all"
              style={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
            >
              <RotateCw className="w-5 h-5" style={{ color: '#9CA3AF' }} />
            </button>
          </div>

          <div 
            className="flex-1 px-4 py-2 rounded-lg"
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(124, 77, 255, 0.2)',
              color: '#9CA3AF',
            }}
          >
            https://forum.server.ru/
          </div>
        </div>

        {/* Forum Content (Simulated) */}
        <div className="flex-1 overflow-auto p-8">
          <div className="max-w-5xl mx-auto space-y-6">
            {/* Forum Header */}
            <div 
              className="p-6 rounded-xl"
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.05)',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(124, 77, 255, 0.2)',
              }}
            >
              <h1 className="text-3xl font-bold mb-2">Форум сервера</h1>
              <p style={{ color: '#9CA3AF' }}>Обсуждайте игру, делитесь опытом и находите друзей</p>
            </div>

            {/* Forum Categories */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {[
                { title: 'Общее обсуждение', posts: 1234, threads: 156 },
                { title: 'Новости и обновления', posts: 567, threads: 45 },
                { title: 'Технические вопросы', posts: 890, threads: 89 },
                { title: 'Предложения', posts: 345, threads: 67 },
              ].map((category, index) => (
                <div
                  key={index}
                  className="p-6 rounded-xl cursor-pointer transition-all hover:scale-102"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.05)',
                    backdropFilter: 'blur(20px)',
                    border: '1px solid rgba(124, 77, 255, 0.2)',
                  }}
                >
                  <h3 className="text-xl font-bold mb-2">{category.title}</h3>
                  <div className="flex gap-4 text-sm" style={{ color: '#9CA3AF' }}>
                    <span>{category.threads} тем</span>
                    <span>•</span>
                    <span>{category.posts} сообщений</span>
                  </div>
                </div>
              ))}
            </div>

            {/* Recent Threads */}
            <div 
              className="p-6 rounded-xl"
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.05)',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(124, 77, 255, 0.2)',
              }}
            >
              <h2 className="text-2xl font-bold mb-4">Последние темы</h2>
              <div className="space-y-3">
                {[
                  { title: 'Когда будет следующее обновление?', author: 'Player123', replies: 24, time: '5 мин назад' },
                  { title: 'Помогите с багом', author: 'NoobMaster', replies: 12, time: '15 мин назад' },
                  { title: 'Ищу команду для SkyBlock', author: 'ProGamer', replies: 45, time: '1 час назад' },
                ].map((thread, index) => (
                  <div
                    key={index}
                    className="p-4 rounded-lg flex items-center justify-between cursor-pointer transition-all"
                    style={{
                      backgroundColor: 'rgba(255, 255, 255, 0.03)',
                      border: '1px solid rgba(124, 77, 255, 0.1)',
                    }}
                  >
                    <div className="flex-1">
                      <h4 className="font-medium mb-1">{thread.title}</h4>
                      <div className="text-sm" style={{ color: '#9CA3AF' }}>
                        от {thread.author} • {thread.time}
                      </div>
                    </div>
                    <div 
                      className="px-3 py-1 rounded-full text-sm"
                      style={{
                        backgroundColor: 'rgba(124, 77, 255, 0.2)',
                        color: '#7C4DFF',
                      }}
                    >
                      {thread.replies} ответов
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
