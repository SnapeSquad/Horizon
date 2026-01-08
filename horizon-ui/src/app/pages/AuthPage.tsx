import { useState } from 'react';
import { motion } from 'motion/react';
import { User, Lock, Mail, Loader2, Sparkles, Key } from 'lucide-react';
import WindowControls from '../components/WindowControls';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';

// Проверка Electron API
const isElectron = typeof window !== 'undefined' && (window as any).electronAPI;

interface AuthPageProps {
  onLogin: (session: any) => void;
}

export default function AuthPage({ onLogin }: AuthPageProps) {
  const [activeTab, setActiveTab] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [twoFactorCode, setTwoFactorCode] = useState('');
  const [needsTwoFactor, setNeedsTwoFactor] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      if (isElectron) {
        const result = await (window as any).electronAPI.login({ 
          username, 
          password,
          twoFactorCode: twoFactorCode || undefined
        });
        
        if (result.success) {
          onLogin(result.session);
        } else if (result.requires2FA || result.needsTwoFactor) {
          // Сервер требует 2FA код
          setNeedsTwoFactor(true);
          setError(result.message || 'Код отправлен в Telegram. Введите его для входа.');
        } else {
          setError(result.message || 'Неверный логин или пароль');
        }
      } else {
        // Fallback для браузера (тестирование)
        await new Promise(resolve => setTimeout(resolve, 1000));
        onLogin({ username, role: 'player', token: 'test-token' });
      }
    } catch (err: any) {
      setError(err.message || 'Ошибка подключения к серверу');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (password.length < 6) {
      setError('Пароль должен быть минимум 6 символов');
      return;
    }

    setIsLoading(true);

    try {
      if (isElectron) {
        const result = await (window as any).electronAPI.register({ 
          username, 
          password, 
          email 
        });
        if (result.success) {
          setActiveTab('login');
          setError('');
          setPassword('');
          alert('✅ Регистрация успешна! Теперь войдите в систему.');
        } else {
          setError(result.message || 'Ошибка регистрации');
        }
      } else {
        await new Promise(resolve => setTimeout(resolve, 1000));
        setActiveTab('login');
        alert('✅ Регистрация успешна! Теперь войдите.');
      }
    } catch (err: any) {
      setError(err.message || 'Ошибка подключения к серверу');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
      {/* Window Controls */}
      <div className="absolute top-4 right-4 z-50">
        <WindowControls />
      </div>

      {/* Auth Container */}
      <motion.div
        initial={{ opacity: 0, scale: 0.9, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        transition={{ duration: 0.6, type: 'spring' }}
        className="relative w-full max-w-md z-10"
      >
        {/* Glow effect */}
        <div 
          className="absolute inset-0 rounded-3xl"
          style={{
            background: 'linear-gradient(135deg, rgba(124, 77, 255, 0.4) 0%, rgba(0, 212, 255, 0.3) 100%)',
            filter: 'blur(60px)',
            zIndex: -1,
          }}
        />

        <div 
          className="p-8 rounded-3xl shadow-2xl"
          style={{
            backgroundColor: 'rgba(15, 15, 19, 0.9)',
            backdropFilter: 'blur(40px)',
            border: '1px solid rgba(124, 77, 255, 0.3)',
          }}
        >
          {/* Logo */}
          <motion.div
            initial={{ scale: 0.8 }}
            animate={{ scale: 1 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="text-center mb-8"
          >
            <h1 className="text-5xl font-bold mb-2" style={{
              background: 'linear-gradient(135deg, #7C4DFF 0%, #00D4FF 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              textShadow: '0 0 40px rgba(124, 77, 255, 0.5)'
            }}>
              HORIZON
            </h1>
            <p className="text-sm opacity-70">Minecraft Launcher</p>
          </motion.div>

          {/* Tabs */}
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="grid w-full grid-cols-2 mb-6" style={{
              backgroundColor: 'rgba(255, 255, 255, 0.05)',
              padding: '4px',
              borderRadius: '12px'
            }}>
              <TabsTrigger value="login" style={{
                borderRadius: '8px',
                padding: '10px',
                transition: 'all 0.3s'
              }}>
                Вход
              </TabsTrigger>
              <TabsTrigger value="register" style={{
                borderRadius: '8px',
                padding: '10px',
                transition: 'all 0.3s'
              }}>
                Регистрация
              </TabsTrigger>
            </TabsList>

            {/* Login Tab */}
            <TabsContent value="login">
              <form onSubmit={handleLogin} className="space-y-5">
                <div className="space-y-2">
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                    <Input
                      type="text"
                      placeholder="Никнейм"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      required
                      disabled={isLoading}
                      className="w-full pl-11 pr-4 py-3 rounded-xl transition-all"
                      style={{
                        backgroundColor: 'rgba(255, 255, 255, 0.05)',
                        border: '1px solid rgba(124, 77, 255, 0.3)',
                        color: '#E0E0E0'
                      }}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                    <Input
                      type="password"
                      placeholder="Пароль"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      disabled={isLoading || needsTwoFactor}
                      className="w-full pl-11 pr-4 py-3 rounded-xl transition-all"
                      style={{
                        backgroundColor: 'rgba(255, 255, 255, 0.05)',
                        border: '1px solid rgba(124, 77, 255, 0.3)',
                        color: '#E0E0E0'
                      }}
                    />
                  </div>
                </div>

                {/* 2FA Code Input - показывается только когда нужен код */}
                {needsTwoFactor && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    transition={{ duration: 0.3 }}
                    className="space-y-2"
                  >
                    <div className="relative">
                      <Key className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                      <Input
                        type="text"
                        placeholder="Код из Telegram"
                        value={twoFactorCode}
                        onChange={(e) => setTwoFactorCode(e.target.value)}
                        required
                        disabled={isLoading}
                        maxLength={6}
                        className="w-full pl-11 pr-4 py-3 rounded-xl transition-all"
                        style={{
                          backgroundColor: 'rgba(255, 255, 255, 0.05)',
                          border: '1px solid rgba(255, 193, 7, 0.5)',
                          color: '#E0E0E0',
                          boxShadow: '0 0 20px rgba(255, 193, 7, 0.3)'
                        }}
                        autoFocus
                      />
                    </div>
                    <p className="text-xs text-yellow-400 ml-1">📱 Проверьте Telegram и введите 6-значный код</p>
                  </motion.div>
                )}

                {error && (
                  <motion.div
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="text-red-400 text-sm text-center p-2 rounded-lg"
                    style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)' }}
                  >
                    {error}
                  </motion.div>
                )}

                <Button
                  type="submit"
                  disabled={isLoading}
                  className="w-full py-3 rounded-xl font-semibold text-lg flex items-center justify-center gap-2 transition-all"
                  style={{
                    background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                    boxShadow: '0 6px 30px rgba(124, 77, 255, 0.5)',
                  }}
                >
                  {isLoading ? (
                    <>
                      <Loader2 className="animate-spin" size={20} />
                      Вход...
                    </>
                  ) : (
                    <>
                      <Sparkles size={20} />
                      Войти
                    </>
                  )}
                </Button>
              </form>
            </TabsContent>

            {/* Register Tab */}
            <TabsContent value="register">
              <form onSubmit={handleRegister} className="space-y-5">
                <div className="space-y-2">
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                    <Input
                      type="text"
                      placeholder="Никнейм"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      required
                      disabled={isLoading}
                      minLength={3}
                      maxLength={16}
                      className="w-full pl-11 pr-4 py-3 rounded-xl transition-all"
                      style={{
                        backgroundColor: 'rgba(255, 255, 255, 0.05)',
                        border: '1px solid rgba(124, 77, 255, 0.3)',
                        color: '#E0E0E0'
                      }}
                    />
                  </div>
                  <p className="text-xs text-gray-500 ml-1">От 3 до 16 символов</p>
                </div>

                <div className="space-y-2">
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                    <Input
                      type="email"
                      placeholder="Email (опционально)"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      disabled={isLoading}
                      className="w-full pl-11 pr-4 py-3 rounded-xl transition-all"
                      style={{
                        backgroundColor: 'rgba(255, 255, 255, 0.05)',
                        border: '1px solid rgba(124, 77, 255, 0.3)',
                        color: '#E0E0E0'
                      }}
                    />
                  </div>
                  <p className="text-xs text-gray-500 ml-1">Для восстановления пароля</p>
                </div>

                <div className="space-y-2">
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                    <Input
                      type="password"
                      placeholder="Пароль"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      disabled={isLoading}
                      minLength={6}
                      className="w-full pl-11 pr-4 py-3 rounded-xl transition-all"
                      style={{
                        backgroundColor: 'rgba(255, 255, 255, 0.05)',
                        border: '1px solid rgba(124, 77, 255, 0.3)',
                        color: '#E0E0E0'
                      }}
                    />
                  </div>
                  <p className="text-xs text-gray-500 ml-1">Минимум 6 символов</p>
                </div>

                {error && (
                  <motion.div
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="text-red-400 text-sm text-center p-2 rounded-lg"
                    style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)' }}
                  >
                    {error}
                  </motion.div>
                )}

                <Button
                  type="submit"
                  disabled={isLoading}
                  className="w-full py-3 rounded-xl font-semibold text-lg flex items-center justify-center gap-2 transition-all"
                  style={{
                    background: 'linear-gradient(135deg, #00D4FF 0%, #66E0FF 100%)',
                    boxShadow: '0 6px 30px rgba(0, 212, 255, 0.5)',
                  }}
                >
                  {isLoading ? (
                    <>
                      <Loader2 className="animate-spin" size={20} />
                      Регистрация...
                    </>
                  ) : (
                    <>
                      <Sparkles size={20} />
                      Создать аккаунт
                    </>
                  )}
                </Button>
              </form>
            </TabsContent>
          </Tabs>

          {/* Footer */}
          <div className="mt-6 text-center text-xs text-gray-500">
            <p>Создавая аккаунт, вы соглашаетесь с правилами сервера</p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
