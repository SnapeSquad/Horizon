import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import { Slider } from '../components/ui/slider';
import { Switch } from '../components/ui/switch';
import { motion } from 'motion/react';

export default function SettingsPage() {
  const [ram, setRam] = useState([4096]);
  const [javaVersion, setJavaVersion] = useState('Java 17');
  const [resolution, setResolution] = useState('fullhd');
  const [autoClose, setAutoClose] = useState(true);
  const [language, setLanguage] = useState('ru');

  return (
    <div className="flex h-screen">
      <Sidebar />
      
      <div className="flex-1 overflow-y-auto p-8">
        <h1 className="text-3xl font-bold mb-8">Настройки</h1>

        {/* Производительность */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8 p-6 rounded-xl"
          style={{
            backgroundColor: 'rgba(255, 255, 255, 0.05)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(124, 77, 255, 0.2)',
          }}
        >
          <h2 className="text-xl font-bold mb-6 flex items-center gap-3">
            <div className="w-1 h-6 rounded" style={{ backgroundColor: '#7C4DFF' }} />
            Производительность
          </h2>

          <div className="space-y-6">
            {/* RAM Slider */}
            <div>
              <div className="flex justify-between items-center mb-4">
                <label className="font-medium">Выделение ОЗУ</label>
                <span style={{ color: '#7C4DFF' }}>{ram[0]} MB</span>
              </div>
              <Slider
                value={ram}
                onValueChange={setRam}
                min={512}
                max={16384}
                step={512}
                className="w-full"
              />
              <div className="flex justify-between mt-2 text-xs" style={{ color: '#9CA3AF' }}>
                <span>512 MB</span>
                <span>16 GB</span>
              </div>
            </div>

            {/* Java Version */}
            <div>
              <label className="font-medium block mb-3">Версия Java</label>
              <select
                value={javaVersion}
                onChange={(e) => setJavaVersion(e.target.value)}
                className="w-full p-3 rounded-lg outline-none"
                style={{
                  backgroundColor: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid rgba(124, 77, 255, 0.2)',
                  color: '#E0E0E0',
                }}
              >
                <option value="Java 8">Java 8</option>
                <option value="Java 11">Java 11</option>
                <option value="Java 17">Java 17 (Рекомендуется)</option>
                <option value="Java 21">Java 21</option>
              </select>
            </div>
          </div>
        </motion.div>

        {/* Дисплей */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="mb-8 p-6 rounded-xl"
          style={{
            backgroundColor: 'rgba(255, 255, 255, 0.05)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(124, 77, 255, 0.2)',
          }}
        >
          <h2 className="text-xl font-bold mb-6 flex items-center gap-3">
            <div className="w-1 h-6 rounded" style={{ backgroundColor: '#7C4DFF' }} />
            Дисплей
          </h2>

          <div className="space-y-6">
            {/* Resolution */}
            <div>
              <label className="font-medium block mb-3">Разрешение</label>
              <div className="grid grid-cols-2 gap-3">
                {[
                  { value: 'fullhd', label: '1920x1080 (Full HD)' },
                  { value: '2k', label: '2560x1440 (2K)' },
                  { value: '4k', label: '3840x2160 (4K)' },
                  { value: 'windowed', label: 'Оконный режим' },
                ].map((option) => (
                  <button
                    key={option.value}
                    onClick={() => setResolution(option.value)}
                    className="p-3 rounded-lg transition-all"
                    style={{
                      backgroundColor: resolution === option.value ? 'rgba(124, 77, 255, 0.2)' : 'rgba(255, 255, 255, 0.05)',
                      border: `1px solid ${resolution === option.value ? '#7C4DFF' : 'rgba(124, 77, 255, 0.2)'}`,
                      color: resolution === option.value ? '#7C4DFF' : '#E0E0E0',
                    }}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Auto Close */}
            <div className="flex items-center justify-between">
              <div>
                <div className="font-medium mb-1">Автозакрытие лаунчера</div>
                <div className="text-sm" style={{ color: '#9CA3AF' }}>
                  Закрывать лаунчер после запуска игры
                </div>
              </div>
              <Switch checked={autoClose} onCheckedChange={setAutoClose} />
            </div>
          </div>
        </motion.div>

        {/* Локализация */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="p-6 rounded-xl"
          style={{
            backgroundColor: 'rgba(255, 255, 255, 0.05)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(124, 77, 255, 0.2)',
          }}
        >
          <h2 className="text-xl font-bold mb-6 flex items-center gap-3">
            <div className="w-1 h-6 rounded" style={{ backgroundColor: '#7C4DFF' }} />
            Локализация
          </h2>

          <div className="flex gap-4">
            {[
              { code: 'ru', flag: '🇷🇺', name: 'Русский' },
              { code: 'en', flag: '🇬🇧', name: 'English' },
              { code: 'tt', flag: '🇹🇷', name: 'Татарча' },
            ].map((lang) => (
              <motion.button
                key={lang.code}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setLanguage(lang.code)}
                className="flex flex-col items-center gap-2 p-4 rounded-xl flex-1 transition-all"
                style={{
                  backgroundColor: language === lang.code ? 'rgba(124, 77, 255, 0.2)' : 'rgba(255, 255, 255, 0.05)',
                  border: `1px solid ${language === lang.code ? '#7C4DFF' : 'rgba(124, 77, 255, 0.2)'}`,
                }}
              >
                <div className="text-4xl">{lang.flag}</div>
                <div 
                  className="font-medium"
                  style={{ color: language === lang.code ? '#7C4DFF' : '#E0E0E0' }}
                >
                  {lang.name}
                </div>
              </motion.button>
            ))}
          </div>
        </motion.div>
      </div>
    </div>
  );
}
