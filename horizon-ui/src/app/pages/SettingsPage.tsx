import { useState } from 'react';
import WindowControls from '../components/WindowControls';
import ParticleBackground from '../components/ParticleBackground';
import { Slider } from '../components/ui/slider';
import { Switch } from '../components/ui/switch';
import { motion } from 'motion/react';
import { Cpu, Monitor, Globe, Save, RotateCcw } from 'lucide-react';

const isElectron = typeof window !== 'undefined' && (window as any).electronAPI;

interface SettingsPageProps {
  user: any;
}

export default function SettingsPage({ user }: SettingsPageProps) {
  const [ram, setRam] = useState([4096]);
  const [selectedServer, setSelectedServer] = useState('survival');
  const [resolution, setResolution] = useState('fullhd');
  const [autoClose, setAutoClose] = useState(true);
  const [language, setLanguage] = useState('ru');
  const [hasChanges, setHasChanges] = useState(false);

  const handleChange = () => setHasChanges(true);
  
  const servers = [
    { id: 'survival', name: 'Survival', online: 247, maxPlayers: 500, ip: 'survival.horizon-rp.ru' },
    { id: 'creative', name: 'Creative', online: 89, maxPlayers: 200, ip: 'creative.horizon-rp.ru' }
  ];

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
            <h1 className="text-2xl font-bold">Настройки</h1>
            <p className="text-sm" style={{ color: '#9CA3AF' }}>
              Оптимизируйте производительность и персонализацию
            </p>
          </div>

          <div className="flex items-center gap-4">
            {hasChanges && (
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                className="flex gap-3"
              >
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setHasChanges(false)}
                  className="px-6 py-3 rounded-xl font-medium flex items-center gap-2"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.1)',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                  }}
                >
                  <RotateCcw className="w-4 h-4" />
                  Отменить
                </motion.button>
                
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => {
                    const settings = {
                      ram: ram[0],
                      javaVersion,
                      resolution,
                      autoClose,
                      language,
                    };
                    if (window.javaBridge && window.javaBridge.saveSettings) {
                      window.javaBridge.saveSettings(JSON.stringify(settings));
                    } else {
                      console.log('Dev: save settings', settings);
                    }
                    setHasChanges(false);
                  }}
                  className="px-6 py-3 rounded-xl font-medium flex items-center gap-2"
                  style={{
                    background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                    boxShadow: '0 4px 20px rgba(124, 77, 255, 0.4)',
                  }}
                >
                  <Save className="w-4 h-4" />
                  Сохранить
                </motion.button>
              </motion.div>
            )}
            
            <WindowControls />
          </div>
        </div>

        {/* Settings Content */}
        <div className="flex-1 overflow-y-auto p-8">
          <div className="max-w-4xl mx-auto space-y-6">
            {/* Performance Section */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="p-8 rounded-2xl relative overflow-hidden"
              style={{
                background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%)',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(124, 77, 255, 0.2)',
              }}
            >
              <div className="flex items-center gap-3 mb-6">
                <div 
                  className="p-3 rounded-xl"
                  style={{
                    background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                    boxShadow: '0 4px 20px rgba(124, 77, 255, 0.3)',
                  }}
                >
                  <Cpu className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h2 className="text-2xl font-bold">Производительность</h2>
                  <p className="text-sm" style={{ color: '#9CA3AF' }}>
                    Настройте параметры для оптимальной работы
                  </p>
                </div>
              </div>

              <div className="space-y-8">
                {/* RAM Allocation */}
                <div>
                  <div className="flex justify-between items-center mb-4">
                    <div>
                      <label className="font-medium text-lg">Выделение ОЗУ</label>
                      <p className="text-sm mt-1" style={{ color: '#9CA3AF' }}>
                        Больше памяти = лучшая производительность
                      </p>
                    </div>
                    <div 
                      className="px-6 py-3 rounded-xl font-bold text-xl"
                      style={{
                        background: 'rgba(124, 77, 255, 0.2)',
                        border: '1px solid rgba(124, 77, 255, 0.3)',
                        color: '#7C4DFF',
                      }}
                    >
                      {(ram[0] / 1024).toFixed(1)} GB
                    </div>
                  </div>
                  <Slider
                    value={ram}
                    onValueChange={(val) => { setRam(val); handleChange(); }}
                    min={512}
                    max={16384}
                    step={512}
                    className="w-full"
                  />
                  <div className="flex justify-between mt-3 text-sm" style={{ color: '#9CA3AF' }}>
                    <span>512 MB</span>
                    <span>Рекомендуемое: 4 GB</span>
                    <span>16 GB</span>
                  </div>
                </div>

                {/* Java Version */}
                <div>
                  <label className="font-medium text-lg block mb-2">Версия Java</label>
                  <p className="text-sm mb-4" style={{ color: '#9CA3AF' }}>
                    Выберите версию Java для запуска игры
                  </p>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { value: 'Java 8', recommended: false },
                      { value: 'Java 11', recommended: false },
                      { value: 'Java 17', recommended: true },
                      { value: 'Java 21', recommended: false },
                    ].map((java) => (
                      <motion.button
                        key={java.value}
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        onClick={() => { setJavaVersion(java.value); handleChange(); }}
                        className="p-4 rounded-xl transition-all relative overflow-hidden"
                        style={{
                          background: javaVersion === java.value
                            ? 'linear-gradient(135deg, rgba(124, 77, 255, 0.3) 0%, rgba(124, 77, 255, 0.1) 100%)'
                            : 'rgba(255, 255, 255, 0.05)',
                          border: `2px solid ${javaVersion === java.value ? '#7C4DFF' : 'rgba(124, 77, 255, 0.2)'}`,
                        }}
                      >
                        <div className="text-left">
                          <div className="font-medium">{java.value}</div>
                          {java.recommended && (
                            <div className="text-xs mt-1" style={{ color: '#10B981' }}>
                              ✓ Рекомендуется
                            </div>
                          )}
                        </div>
                      </motion.button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Background Gradient */}
              <div 
                className="absolute -bottom-20 -right-20 w-60 h-60 rounded-full opacity-20"
                style={{
                  background: 'radial-gradient(circle, #7C4DFF 0%, transparent 70%)',
                  filter: 'blur(60px)',
                }}
              />
            </motion.div>

            {/* Display Section */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="p-8 rounded-2xl relative overflow-hidden"
              style={{
                background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%)',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(124, 77, 255, 0.2)',
              }}
            >
              <div className="flex items-center gap-3 mb-6">
                <div 
                  className="p-3 rounded-xl"
                  style={{
                    background: 'linear-gradient(135deg, #10B981 0%, #059669 100%)',
                    boxShadow: '0 4px 20px rgba(16, 185, 129, 0.3)',
                  }}
                >
                  <Monitor className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h2 className="text-2xl font-bold">Дисплей</h2>
                  <p className="text-sm" style={{ color: '#9CA3AF' }}>
                    Настройки отображения и интерфейса
                  </p>
                </div>
              </div>

              <div className="space-y-8">
                {/* Resolution */}
                <div>
                  <label className="font-medium text-lg block mb-4">Разрешение экрана</label>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { value: 'fullhd', label: '1920×1080', subtitle: 'Full HD' },
                      { value: '2k', label: '2560×1440', subtitle: '2K QHD' },
                      { value: '4k', label: '3840×2160', subtitle: '4K UHD' },
                      { value: 'windowed', label: 'Оконный', subtitle: 'Режим окна' },
                    ].map((res) => (
                      <motion.button
                        key={res.value}
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        onClick={() => { setResolution(res.value); handleChange(); }}
                        className="p-4 rounded-xl transition-all text-left"
                        style={{
                          background: resolution === res.value
                            ? 'linear-gradient(135deg, rgba(16, 185, 129, 0.3) 0%, rgba(16, 185, 129, 0.1) 100%)'
                            : 'rgba(255, 255, 255, 0.05)',
                          border: `2px solid ${resolution === res.value ? '#10B981' : 'rgba(124, 77, 255, 0.2)'}`,
                        }}
                      >
                        <div className="font-medium">{res.label}</div>
                        <div className="text-xs mt-1" style={{ color: '#9CA3AF' }}>
                          {res.subtitle}
                        </div>
                      </motion.button>
                    ))}
                  </div>
                </div>

                {/* Auto Close Launcher */}
                <div 
                  className="flex items-center justify-between p-6 rounded-xl"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.03)',
                    border: '1px solid rgba(124, 77, 255, 0.2)',
                  }}
                >
                  <div>
                    <div className="font-medium text-lg mb-1">Автозакрытие лаунчера</div>
                    <div className="text-sm" style={{ color: '#9CA3AF' }}>
                      Автоматически закрывать лаунчер после запуска игры
                    </div>
                  </div>
                  <Switch 
                    checked={autoClose} 
                    onCheckedChange={(checked) => { setAutoClose(checked); handleChange(); }} 
                  />
                </div>
              </div>

              {/* Background Gradient */}
              <div 
                className="absolute -top-20 -left-20 w-60 h-60 rounded-full opacity-20"
                style={{
                  background: 'radial-gradient(circle, #10B981 0%, transparent 70%)',
                  filter: 'blur(60px)',
                }}
              />
            </motion.div>

            {/* Localization Section */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="p-8 rounded-2xl relative overflow-hidden"
              style={{
                background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%)',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(124, 77, 255, 0.2)',
              }}
            >
              <div className="flex items-center gap-3 mb-6">
                <div 
                  className="p-3 rounded-xl"
                  style={{
                    background: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)',
                    boxShadow: '0 4px 20px rgba(245, 158, 11, 0.3)',
                  }}
                >
                  <Globe className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h2 className="text-2xl font-bold">Локализация</h2>
                  <p className="text-sm" style={{ color: '#9CA3AF' }}>
                    Выберите язык интерфейса
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-4">
                {[
                  { code: 'ru', flag: '🇷🇺', name: 'Русский', native: 'Russian' },
                  { code: 'en', flag: '🇬🇧', name: 'English', native: 'English' },
                  { code: 'tt', flag: '🇹🇷', name: 'Татарча', native: 'Tatar' },
                ].map((lang) => (
                  <motion.button
                    key={lang.code}
                    whileHover={{ scale: 1.05, y: -4 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => { setLanguage(lang.code); handleChange(); }}
                    className="flex flex-col items-center gap-3 p-6 rounded-xl transition-all relative overflow-hidden"
                    style={{
                      background: language === lang.code
                        ? 'linear-gradient(135deg, rgba(245, 158, 11, 0.3) 0%, rgba(245, 158, 11, 0.1) 100%)'
                        : 'rgba(255, 255, 255, 0.05)',
                      border: `2px solid ${language === lang.code ? '#F59E0B' : 'rgba(124, 77, 255, 0.2)'}`,
                    }}
                  >
                    <div className="text-5xl">{lang.flag}</div>
                    <div className="text-center">
                      <div className="font-bold" style={{ color: language === lang.code ? '#F59E0B' : '#E0E0E0' }}>
                        {lang.name}
                      </div>
                      <div className="text-xs mt-1" style={{ color: '#9CA3AF' }}>
                        {lang.native}
                      </div>
                    </div>
                    
                    {language === lang.code && (
                      <motion.div
                        layoutId="selectedLanguage"
                        className="absolute inset-0 rounded-xl -z-10"
                        style={{
                          background: 'radial-gradient(circle at center, rgba(245, 158, 11, 0.2) 0%, transparent 70%)',
                        }}
                      />
                    )}
                  </motion.button>
                ))}
              </div>

              {/* Background Gradient */}
              <div 
                className="absolute -bottom-20 -right-20 w-60 h-60 rounded-full opacity-20"
                style={{
                  background: 'radial-gradient(circle, #F59E0B 0%, transparent 70%)',
                  filter: 'blur(60px)',
                }}
              />
            </motion.div>
          </div>
        </div>
      </div>
    </div>
  );
}
