import { useNavigate, useLocation } from 'react-router-dom';
import { Home, ShoppingBag, Shirt, MessageSquare, Settings } from 'lucide-react';
import { motion } from 'motion/react';

const menuItems = [
  { icon: Home, label: 'Главная', path: '/dashboard' },
  { icon: ShoppingBag, label: 'Магазин', path: '/shop' },
  { icon: Shirt, label: 'Гардероб', path: '/wardrobe' },
  { icon: MessageSquare, label: 'Форум', path: '/forum' },
  { icon: Settings, label: 'Настройки', path: '/settings' },
];

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <div 
      className="w-20 h-screen flex flex-col items-center py-8 gap-6"
      style={{
        backgroundColor: 'rgba(0, 0, 0, 0.3)',
        borderRight: '1px solid rgba(124, 77, 255, 0.2)',
      }}
    >
      {menuItems.map((item) => {
        const Icon = item.icon;
        const isActive = location.pathname === item.path;

        return (
          <motion.button
            key={item.path}
            onClick={() => navigate(item.path)}
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.95 }}
            className="relative p-4 rounded-xl transition-all duration-300 group"
            style={{
              backgroundColor: isActive ? 'rgba(124, 77, 255, 0.2)' : 'transparent',
            }}
          >
            <Icon 
              className="w-6 h-6" 
              style={{ 
                color: isActive ? '#7C4DFF' : '#9CA3AF',
                filter: isActive ? 'drop-shadow(0 0 10px rgba(124, 77, 255, 0.8))' : 'none',
              }} 
            />
            
            {/* Tooltip */}
            <div 
              className="absolute left-full ml-2 px-3 py-2 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity duration-300 whitespace-nowrap"
              style={{
                backgroundColor: 'rgba(0, 0, 0, 0.9)',
                border: '1px solid rgba(124, 77, 255, 0.3)',
                top: '50%',
                transform: 'translateY(-50%)',
                pointerEvents: 'none',
              }}
            >
              <span style={{ color: '#E0E0E0', fontSize: '14px' }}>{item.label}</span>
            </div>

            {isActive && (
              <motion.div
                layoutId="activeIndicator"
                className="absolute left-0 top-0 bottom-0 w-1 rounded-r"
                style={{ backgroundColor: '#7C4DFF' }}
                transition={{ type: 'spring', stiffness: 300, damping: 30 }}
              />
            )}
          </motion.button>
        );
      })}
    </div>
  );
}
