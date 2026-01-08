import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'motion/react';
import { 
  Home, 
  ShoppingBag, 
  Shirt, 
  MessageSquare, 
  Settings,
  LogOut,
  User,
  Crown,
  Star,
  Shield
} from 'lucide-react';
import { Avatar, AvatarImage, AvatarFallback } from './ui/avatar';

interface SidebarProps {
  user: any;
  onLogout: () => void;
}

const menuItems = [
  { id: 'dashboard', icon: Home, label: 'Главная', path: '/dashboard' },
  { id: 'shop', icon: ShoppingBag, label: 'Магазин', path: '/shop' },
  { id: 'wardrobe', icon: Shirt, label: 'Гардероб', path: '/wardrobe' },
  { id: 'forum', icon: MessageSquare, label: 'Форум', path: '/forum' },
  { id: 'settings', icon: Settings, label: 'Настройки', path: '/settings' },
];

const roleIcons: Record<string, { icon: any; color: string; gradient: string }> = {
  'owner': { 
    icon: Crown, 
    color: '#FF6B6B',
    gradient: 'linear-gradient(135deg, #FF6B6B 0%, #FF8E8E 100%)'
  },
  'curator': { 
    icon: Star, 
    color: '#FFD93D',
    gradient: 'linear-gradient(135deg, #FFD93D 0%, #FFE66D 100%)'
  },
  'admin': { 
    icon: Shield, 
    color: '#6BCF7F',
    gradient: 'linear-gradient(135deg, #6BCF7F 0%, #8FE9A2 100%)'
  },
  'helper': { 
    icon: User, 
    color: '#4ECDC4',
    gradient: 'linear-gradient(135deg, #4ECDC4 0%, #71E0D8 100%)'
  },
  'player': { 
    icon: User, 
    color: '#95A5A6',
    gradient: 'linear-gradient(135deg, #95A5A6 0%, #BDC3C7 100%)'
  }
};

export default function Sidebar({ user, onLogout }: SidebarProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const [hoveredItem, setHoveredItem] = useState<string | null>(null);

  const isActive = (path: string) => location.pathname === path;

  const roleData = roleIcons[user?.role || 'player'];
  const RoleIcon = roleData.icon;

  const getSkinHeadUrl = (username: string) => {
    return `https://crafatar.com/avatars/${username}?size=128&overlay`;
  };

  return (
    <motion.div
      initial={{ x: -100, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      transition={{ duration: 0.5 }}
      className="w-80 h-screen flex flex-col p-6"
      style={{
        backgroundColor: 'rgba(15, 15, 19, 0.95)',
        backdropFilter: 'blur(20px)',
        borderRight: '1px solid rgba(124, 77, 255, 0.2)',
      }}
    >
      {/* Logo */}
      <div className="mb-8 text-center">
        <h1 className="text-3xl font-bold mb-1" style={{
          background: 'linear-gradient(135deg, #7C4DFF 0%, #00D4FF 100%)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
        }}>
          HORIZON
        </h1>
        <p className="text-xs text-gray-500">Launcher v2.0</p>
      </div>

      {/* User Profile */}
      <motion.div
        whileHover={{ scale: 1.02 }}
        className="mb-8 p-4 rounded-2xl cursor-pointer transition-all"
        style={{
          background: roleData.gradient,
          boxShadow: `0 4px 20px ${roleData.color}40`
        }}
      >
        <div className="flex items-center gap-4">
          <Avatar className="w-16 h-16 border-2 border-white/20">
            <AvatarImage src={getSkinHeadUrl(user?.username || 'Steve')} />
            <AvatarFallback>{(user?.username || 'U')[0]}</AvatarFallback>
          </Avatar>
          <div className="flex-1">
            <div className="font-bold text-white text-lg">{user?.username || 'Guest'}</div>
            <div className="flex items-center gap-1 text-white/80 text-sm">
              <RoleIcon size={14} />
              <span>{roleData.color === '#95A5A6' ? 'Игрок' : user?.role}</span>
            </div>
          </div>
        </div>
      </motion.div>

      {/* Navigation */}
      <nav className="flex-1 space-y-2">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const active = isActive(item.path);
          const hovered = hoveredItem === item.id;

          return (
            <motion.button
              key={item.id}
              onClick={() => navigate(item.path)}
              onMouseEnter={() => setHoveredItem(item.id)}
              onMouseLeave={() => setHoveredItem(null)}
              whileHover={{ x: 5 }}
              whileTap={{ scale: 0.98 }}
              className="w-full flex items-center gap-4 px-4 py-3 rounded-xl transition-all"
              style={{
                backgroundColor: active 
                  ? 'rgba(124, 77, 255, 0.2)' 
                  : hovered 
                  ? 'rgba(255, 255, 255, 0.05)' 
                  : 'transparent',
                border: active 
                  ? '1px solid rgba(124, 77, 255, 0.4)' 
                  : '1px solid transparent',
                color: active ? '#7C4DFF' : '#E0E0E0',
              }}
            >
              <Icon size={22} />
              <span className="font-medium">{item.label}</span>
              {active && (
                <motion.div
                  layoutId="activeIndicator"
                  className="ml-auto w-2 h-2 rounded-full"
                  style={{ backgroundColor: '#7C4DFF' }}
                />
              )}
            </motion.button>
          );
        })}
      </nav>

      {/* Logout Button */}
      <motion.button
        onClick={onLogout}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        className="w-full flex items-center gap-4 px-4 py-3 rounded-xl transition-all"
        style={{
          backgroundColor: 'rgba(239, 68, 68, 0.1)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          color: '#EF4444',
        }}
      >
        <LogOut size={22} />
        <span className="font-medium">Выйти</span>
      </motion.button>

      {/* Version Info */}
      <div className="mt-4 text-center text-xs text-gray-600">
        <p>© 2024 Horizon Team</p>
      </div>
    </motion.div>
  );
}
