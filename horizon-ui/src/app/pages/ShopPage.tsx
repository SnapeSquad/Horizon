import { useState } from 'react';
import WindowControls from '../components/WindowControls';
import ParticleBackground from '../components/ParticleBackground';
import { ShoppingCart, Star, Zap, Crown, Sparkles, Check, X } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

const isElectron = typeof window !== 'undefined' && (window as any).electronAPI;

interface ShopPageProps {
  user: any;
}

interface ShopItem {
  id: number;
  name: string;
  price: number;
  originalPrice?: number;
  period: string;
  popular: boolean;
  featured: boolean;
  icon: string;
  gradient: string;
  benefits: string[];
  category: 'rank' | 'currency' | 'cosmetic' | 'other';
}

const shopItems: ShopItem[] = [
  {
    id: 1,
    name: 'VIP Статус',
    price: 299,
    originalPrice: 399,
    period: 'месяц',
    popular: true,
    featured: false,
    icon: '👑',
    gradient: 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)',
    benefits: ['Приоритетный вход', 'x2 опыта', 'Цветной ник', 'Доступ к VIP лобби'],
    category: 'rank',
  },
  {
    id: 2,
    name: 'Premium Статус',
    price: 499,
    period: 'месяц',
    popular: false,
    featured: true,
    icon: '💎',
    gradient: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
    benefits: ['Все VIP привилегии', 'x3 опыта', 'Эффекты полета', 'Уникальный префикс', 'Premium лобби'],
    category: 'rank',
  },
  {
    id: 3,
    name: 'Набор скинов "Легенда"',
    price: 399,
    originalPrice: 599,
    period: 'навсегда',
    popular: true,
    featured: false,
    icon: '👕',
    gradient: 'linear-gradient(135deg, #EC4899 0%, #DB2777 100%)',
    benefits: ['10 эксклюзивных скинов', 'Анимированные текстуры', 'Уникальные эффекты'],
    category: 'cosmetic',
  },
  {
    id: 4,
    name: '5000 Монет',
    price: 199,
    period: 'единоразово',
    popular: false,
    featured: false,
    icon: '💰',
    gradient: 'linear-gradient(135deg, #10B981 0%, #059669 100%)',
    benefits: ['+500 бонусных монет', 'Мгновенное зачисление'],
    category: 'currency',
  },
  {
    id: 5,
    name: 'Легендарный кейс',
    price: 399,
    period: 'единоразово',
    popular: true,
    featured: false,
    icon: '📦',
    gradient: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)',
    benefits: ['Гарантированный легендарный предмет', 'Шанс на мифический дроп', '10 редких предметов'],
    category: 'other',
  },
  {
    id: 6,
    name: 'Питомец "Дракон"',
    price: 799,
    period: 'навсегда',
    popular: false,
    featured: true,
    icon: '🐉',
    gradient: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)',
    benefits: ['Легендарный питомец', 'Уникальные способности', 'Анимированные эффекты', 'Бонус к опыту'],
    category: 'cosmetic',
  },
];

export default function ShopPage({ user }: ShopPageProps) {
  const [cart, setCart] = useState<number[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [hoveredItem, setHoveredItem] = useState<number | null>(null);

  const purchaseItem = (itemId: number, price: number, name: string) => {
    if (window.javaBridge && window.javaBridge.purchaseItem) {
      window.javaBridge.purchaseItem(itemId, price);
    } else {
      console.log(`Dev: purchase ${name} for ${price}`);
    }
  };

  const addToCart = (itemId: number) => {
    if (!cart.includes(itemId)) {
      setCart([...cart, itemId]);
    }
  };

  const removeFromCart = (itemId: number) => {
    setCart(cart.filter(id => id !== itemId));
  };

  const totalPrice = cart.reduce((sum, id) => {
    const item = shopItems.find(i => i.id === id);
    return sum + (item?.price || 0);
  }, 0);

  const filteredItems = selectedCategory === 'all' 
    ? shopItems 
    : shopItems.filter(item => item.category === selectedCategory);

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
            <h1 className="text-2xl font-bold flex items-center gap-3">
              <ShoppingCart className="w-7 h-7" style={{ color: '#7C4DFF' }} />
              Премиум магазин
            </h1>
            <p className="text-sm" style={{ color: '#9CA3AF' }}>
              Улучшите свой игровой опыт
            </p>
          </div>

          <div className="flex items-center gap-4">
            {/* Cart */}
            <motion.div 
              whileHover={{ scale: 1.05 }}
              className="relative px-6 py-3 rounded-xl cursor-pointer"
              style={{ 
                background: 'linear-gradient(135deg, rgba(124, 77, 255, 0.2) 0%, rgba(124, 77, 255, 0.1) 100%)',
                border: '1px solid rgba(124, 77, 255, 0.3)',
              }}
            >
              <div className="flex items-center gap-3">
                <ShoppingCart className="w-5 h-5" style={{ color: '#7C4DFF' }} />
                <div>
                  <div className="text-xs" style={{ color: '#9CA3AF' }}>Корзина</div>
                  <div className="font-bold">{totalPrice} ₽</div>
                </div>
              </div>
              {cart.length > 0 && (
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  className="absolute -top-2 -right-2 w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold"
                  style={{ background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)' }}
                >
                  {cart.length}
                </motion.div>
              )}
            </motion.div>

            <WindowControls />
          </div>
        </div>

        {/* Category Filter */}
        <div className="px-8 py-6 flex gap-3">
          {[
            { key: 'all', label: 'Все товары', icon: '🛍️' },
            { key: 'rank', label: 'Статусы', icon: '👑' },
            { key: 'currency', label: 'Валюта', icon: '💰' },
            { key: 'cosmetic', label: 'Косметика', icon: '✨' },
            { key: 'other', label: 'Прочее', icon: '📦' },
          ].map((category) => (
            <motion.button
              key={category.key}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setSelectedCategory(category.key)}
              className="px-6 py-3 rounded-xl font-medium flex items-center gap-2 transition-all"
              style={{
                background: selectedCategory === category.key
                  ? 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)'
                  : 'rgba(255, 255, 255, 0.05)',
                border: `1px solid ${selectedCategory === category.key ? '#7C4DFF' : 'rgba(124, 77, 255, 0.2)'}`,
                color: selectedCategory === category.key ? '#fff' : '#9CA3AF',
              }}
            >
              <span>{category.icon}</span>
              {category.label}
            </motion.button>
          ))}
        </div>

        {/* Products Grid */}
        <div className="flex-1 overflow-y-auto px-8 pb-8">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <AnimatePresence mode="popLayout">
              {filteredItems.map((item, index) => (
                <motion.div
                  key={item.id}
                  layout
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.9 }}
                  transition={{ delay: index * 0.05 }}
                  onMouseEnter={() => setHoveredItem(item.id)}
                  onMouseLeave={() => setHoveredItem(null)}
                  className="relative group"
                >
                  {/* Main Card */}
                  <motion.div
                    whileHover={{ y: -8 }}
                    className="relative p-6 rounded-2xl overflow-hidden h-full flex flex-col"
                    style={{
                      background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%)',
                      backdropFilter: 'blur(20px)',
                      border: '1px solid rgba(124, 77, 255, 0.2)',
                      boxShadow: hoveredItem === item.id ? '0 20px 60px rgba(124, 77, 255, 0.3)' : 'none',
                    }}
                  >
                    {/* Badges */}
                    <div className="absolute top-4 right-4 flex flex-col gap-2 z-10">
                      {item.featured && (
                        <motion.div
                          animate={{ rotate: [0, 5, -5, 0] }}
                          transition={{ duration: 2, repeat: Infinity }}
                          className="px-3 py-1 rounded-full text-xs font-bold flex items-center gap-1"
                          style={{
                            background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                            boxShadow: '0 4px 15px rgba(124, 77, 255, 0.4)',
                          }}
                        >
                          <Zap className="w-3 h-3" />
                          FEATURED
                        </motion.div>
                      )}
                      {item.popular && (
                        <motion.div
                          animate={{ scale: [1, 1.05, 1] }}
                          transition={{ duration: 2, repeat: Infinity }}
                          className="px-3 py-1 rounded-full text-xs font-bold flex items-center gap-1"
                          style={{
                            background: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)',
                          }}
                        >
                          <Star className="w-3 h-3" />
                          ПОПУЛЯРНО
                        </motion.div>
                      )}
                      {item.originalPrice && (
                        <div 
                          className="px-3 py-1 rounded-full text-xs font-bold"
                          style={{
                            background: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)',
                          }}
                        >
                          -{Math.round((1 - item.price / item.originalPrice) * 100)}%
                        </div>
                      )}
                    </div>

                    {/* Icon */}
                    <motion.div
                      animate={{ 
                        rotateY: hoveredItem === item.id ? 360 : 0,
                        scale: hoveredItem === item.id ? 1.1 : 1,
                      }}
                      transition={{ duration: 0.6 }}
                      className="w-24 h-24 rounded-2xl mx-auto mb-6 flex items-center justify-center text-5xl relative"
                      style={{
                        background: item.gradient,
                        boxShadow: '0 10px 40px rgba(0, 0, 0, 0.3)',
                      }}
                    >
                      {item.icon}
                      
                      {/* Sparkle effect */}
                      {hoveredItem === item.id && (
                        <motion.div
                          className="absolute inset-0"
                          animate={{
                            opacity: [0, 1, 0],
                          }}
                          transition={{ duration: 1.5, repeat: Infinity }}
                        >
                          <Sparkles className="absolute top-2 right-2 w-4 h-4 text-white" />
                          <Sparkles className="absolute bottom-2 left-2 w-3 h-3 text-white" />
                        </motion.div>
                      )}
                    </motion.div>

                    {/* Title & Period */}
                    <div className="text-center mb-4">
                      <h3 className="text-xl font-bold mb-1">{item.name}</h3>
                      <div className="text-sm" style={{ color: '#9CA3AF' }}>
                        {item.period}
                      </div>
                    </div>

                    {/* Benefits */}
                    <div className="flex-1 space-y-2 mb-6">
                      {item.benefits.map((benefit, idx) => (
                        <motion.div
                          key={idx}
                          initial={{ opacity: 0, x: -10 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: 0.1 * idx }}
                          className="flex items-center gap-2 text-sm"
                        >
                          <Check className="w-4 h-4 flex-shrink-0" style={{ color: '#10B981' }} />
                          <span style={{ color: '#D1D5DB' }}>{benefit}</span>
                        </motion.div>
                      ))}
                    </div>

                    {/* Price */}
                    <div className="text-center mb-6">
                      {item.originalPrice && (
                        <div className="text-sm line-through mb-1" style={{ color: '#6B7280' }}>
                          {item.originalPrice} ₽
                        </div>
                      )}
                      <div 
                        className="text-4xl font-black"
                        style={{
                          background: item.gradient,
                          WebkitBackgroundClip: 'text',
                          WebkitTextFillColor: 'transparent',
                          backgroundClip: 'text',
                        }}
                      >
                        {item.price} ₽
                      </div>
                    </div>

                    {/* Buy Button */}
                    <motion.button
                      whileHover={{ scale: 1.05 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => {
                        if (cart.includes(item.id)) {
                          removeFromCart(item.id);
                        } else {
                          addToCart(item.id);
                          purchaseItem(item.id, item.price, item.name);
                        }
                      }}
                      className="w-full py-4 px-6 rounded-xl font-bold flex items-center justify-center gap-2 relative overflow-hidden"
                      style={{
                        background: cart.includes(item.id)
                          ? 'linear-gradient(135deg, #10B981 0%, #059669 100%)'
                          : item.gradient,
                        boxShadow: '0 8px 30px rgba(0, 0, 0, 0.3)',
                      }}
                    >
                      {cart.includes(item.id) ? (
                        <>
                          <Check className="w-5 h-5" />
                          В корзине
                        </>
                      ) : (
                        <>
                          <ShoppingCart className="w-5 h-5" />
                          Купить
                        </>
                      )}

                      {/* Shine effect */}
                      <motion.div
                        className="absolute inset-0 opacity-0 group-hover:opacity-100"
                        style={{
                          background: 'linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent)',
                        }}
                        animate={{
                          x: ['-100%', '100%'],
                        }}
                        transition={{
                          duration: 1.5,
                          repeat: Infinity,
                          repeatDelay: 0.5,
                        }}
                      />
                    </motion.button>

                    {/* Background gradient effect */}
                    <div 
                      className="absolute -bottom-20 -right-20 w-60 h-60 rounded-full opacity-0 group-hover:opacity-30 transition-opacity duration-500"
                      style={{
                        background: item.gradient,
                        filter: 'blur(60px)',
                      }}
                    />
                  </motion.div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>

          {/* Payment Methods */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-12 p-8 rounded-2xl"
            style={{
              background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(124, 77, 255, 0.2)',
            }}
          >
            <h2 className="text-xl font-bold mb-6 flex items-center gap-2">
              <Crown className="w-6 h-6" style={{ color: '#7C4DFF' }} />
              Способы оплаты
            </h2>
            <div className="flex gap-4 flex-wrap">
              {[
                { name: 'Банковская карта', icon: '💳' },
                { name: 'QIWI', icon: '🥝' },
                { name: 'WebMoney', icon: '💼' },
                { name: 'Яндекс.Деньги', icon: '🟡' },
                { name: 'Криптовалюта', icon: '₿' },
                { name: 'СБП', icon: '⚡' },
              ].map((method) => (
                <motion.div
                  key={method.name}
                  whileHover={{ scale: 1.05, y: -2 }}
                  className="px-6 py-3 rounded-xl flex items-center gap-2"
                  style={{
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(124, 77, 255, 0.2)',
                  }}
                >
                  <span className="text-xl">{method.icon}</span>
                  <span style={{ color: '#D1D5DB' }}>{method.name}</span>
                </motion.div>
              ))}
            </div>
          </motion.div>
        </div>

        {/* Checkout Bar */}
        <AnimatePresence>
          {cart.length > 0 && (
            <motion.div
              initial={{ y: 100, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 100, opacity: 0 }}
              className="absolute bottom-0 left-20 right-0 p-6"
              style={{
                background: 'linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.8) 20%)',
                backdropFilter: 'blur(20px)',
              }}
            >
              <div className="flex items-center justify-between max-w-4xl mx-auto">
                <div>
                  <div className="text-sm" style={{ color: '#9CA3AF' }}>
                    Товаров в корзине: {cart.length}
                  </div>
                  <div className="text-3xl font-bold">
                    Итого: {totalPrice} ₽
                  </div>
                </div>
                
                <div className="flex gap-3">
                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => setCart([])}
                    className="px-6 py-3 rounded-xl font-medium flex items-center gap-2"
                    style={{
                      backgroundColor: 'rgba(255, 255, 255, 0.1)',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                    }}
                  >
                    <X className="w-5 h-5" />
                    Очистить
                  </motion.button>
                  
                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    className="px-8 py-3 rounded-xl font-bold flex items-center gap-2"
                    style={{
                      background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                      boxShadow: '0 10px 40px rgba(124, 77, 255, 0.4)',
                    }}
                  >
                    <ShoppingCart className="w-5 h-5" />
                    Перейти к оплате
                  </motion.button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
