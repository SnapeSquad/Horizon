import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import { User, Lock, Loader } from 'lucide-react';

export default function AuthPage() {
  const [accountType, setAccountType] = useState<'license' | 'cracked'>('license');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    
    // Имитация входа
    setTimeout(() => {
      setIsLoading(false);
      navigate('/dashboard');
    }, 1500);
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-md"
      >
        {/* Логотип */}
        <motion.div
          initial={{ scale: 0.8 }}
          animate={{ scale: 1 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="text-center mb-12"
        >
          <div 
            className="text-6xl font-bold mb-2"
            style={{
              background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text'
            }}
          >
            СЕРВЕР
          </div>
          <div className="text-gray-500 text-sm">Добро пожаловать</div>
        </motion.div>

        {/* Форма входа */}
        <div 
          className="p-8 rounded-xl"
          style={{
            backgroundColor: 'rgba(255, 255, 255, 0.05)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(124, 77, 255, 0.2)',
          }}
        >
          {/* Переключатель типа аккаунта */}
          <div 
            className="flex gap-2 p-1 mb-6 rounded-lg"
            style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}
          >
            <button
              onClick={() => setAccountType('license')}
              className="flex-1 py-3 px-4 rounded-lg transition-all duration-300 flex items-center justify-center gap-2"
              style={{
                backgroundColor: accountType === 'license' ? '#7C4DFF' : 'transparent',
                color: accountType === 'license' ? '#fff' : '#9CA3AF',
              }}
            >
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M11.5 2C6.81 2 3 5.81 3 10.5S6.81 19 11.5 19c2.15 0 4.11-.78 5.63-2.06l4.29 4.29 1.41-1.41-4.29-4.29A9.456 9.456 0 0021 10.5C21 5.81 17.19 2 11.5 2zm0 2C16.11 4 19 6.89 19 11.5S16.11 19 11.5 19 4 16.11 4 11.5 6.89 4 11.5 4z"/>
              </svg>
              Лицензия
            </button>
            <button
              onClick={() => setAccountType('cracked')}
              className="flex-1 py-3 px-4 rounded-lg transition-all duration-300 flex items-center justify-center gap-2"
              style={{
                backgroundColor: accountType === 'cracked' ? '#7C4DFF' : 'transparent',
                color: accountType === 'cracked' ? '#fff' : '#9CA3AF',
              }}
            >
              <User className="w-5 h-5" />
              Пиратка
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Поле логина */}
            <div>
              <div 
                className="flex items-center gap-3 p-4 rounded-lg transition-all duration-300"
                style={{
                  backgroundColor: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid rgba(124, 77, 255, 0.2)',
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = '#7C4DFF';
                  e.currentTarget.style.boxShadow = '0 0 20px rgba(124, 77, 255, 0.3)';
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = 'rgba(124, 77, 255, 0.2)';
                  e.currentTarget.style.boxShadow = 'none';
                }}
              >
                <User className="w-5 h-5" style={{ color: '#7C4DFF' }} />
                <input
                  type="text"
                  placeholder={accountType === 'license' ? 'Email или логин' : 'Никнейм'}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="flex-1 bg-transparent outline-none"
                  style={{ color: '#E0E0E0' }}
                  required
                />
              </div>
            </div>

            {/* Поле пароля (только для лицензии) */}
            {accountType === 'license' && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                transition={{ duration: 0.3 }}
              >
                <div 
                  className="flex items-center gap-3 p-4 rounded-lg transition-all duration-300"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(124, 77, 255, 0.2)',
                  }}
                  onFocus={(e) => {
                    e.currentTarget.style.borderColor = '#7C4DFF';
                    e.currentTarget.style.boxShadow = '0 0 20px rgba(124, 77, 255, 0.3)';
                  }}
                  onBlur={(e) => {
                    e.currentTarget.style.borderColor = 'rgba(124, 77, 255, 0.2)';
                    e.currentTarget.style.boxShadow = 'none';
                  }}
                >
                  <Lock className="w-5 h-5" style={{ color: '#7C4DFF' }} />
                  <input
                    type="password"
                    placeholder="Пароль"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="flex-1 bg-transparent outline-none"
                    style={{ color: '#E0E0E0' }}
                    required
                  />
                </div>
              </motion.div>
            )}

            {/* Кнопка входа */}
            <motion.button
              type="submit"
              disabled={isLoading}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="w-full py-4 rounded-lg font-medium transition-all duration-300"
              style={{
                background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                boxShadow: '0 0 30px rgba(124, 77, 255, 0.5)',
                color: '#fff',
              }}
            >
              {isLoading ? (
                <Loader className="w-5 h-5 mx-auto animate-spin" />
              ) : (
                'Войти'
              )}
            </motion.button>
          </form>

          {/* Дополнительные ссылки */}
          {accountType === 'license' && (
            <div className="mt-6 flex justify-between text-sm">
              <button className="hover:text-[#7C4DFF] transition-colors" style={{ color: '#9CA3AF' }}>
                Забыли пароль?
              </button>
              <button className="hover:text-[#7C4DFF] transition-colors" style={{ color: '#9CA3AF' }}>
                Регистрация
              </button>
            </div>
          )}
        </div>
      </motion.div>
    </div>
  );
}
