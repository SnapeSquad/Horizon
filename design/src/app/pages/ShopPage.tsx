import Sidebar from '../components/Sidebar';
import { ShoppingCart, Star } from 'lucide-react';
import { motion } from 'motion/react';

const shopItems = [
  { id: 1, name: 'VIP Статус', price: 299, period: 'месяц', popular: true },
  { id: 2, name: 'Premium Статус', price: 499, period: 'месяц', popular: false },
  { id: 3, name: 'Набор скинов', price: 149, period: 'навсегда', popular: false },
  { id: 4, name: 'Донат валюта', price: 99, period: '1000 монет', popular: true },
  { id: 5, name: 'Кейс с предметами', price: 199, period: 'единоразово', popular: false },
  { id: 6, name: 'Питомец-дракон', price: 399, period: 'навсегда', popular: false },
];

export default function ShopPage() {
  return (
    <div className="flex h-screen">
      <Sidebar />
      
      <div className="flex-1 overflow-y-auto p-8">
        <div className="max-w-6xl mx-auto">
          <h1 className="text-3xl font-bold mb-8">Магазин</h1>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {shopItems.map((item) => (
              <motion.div
                key={item.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: item.id * 0.1 }}
                whileHover={{ scale: 1.02, y: -4 }}
                className="relative p-6 rounded-xl"
                style={{
                  backgroundColor: 'rgba(255, 255, 255, 0.05)',
                  backdropFilter: 'blur(20px)',
                  border: '1px solid rgba(124, 77, 255, 0.2)',
                }}
              >
                {/* Popular Badge */}
                {item.popular && (
                  <div 
                    className="absolute -top-3 -right-3 px-3 py-1 rounded-full flex items-center gap-1 text-sm font-medium"
                    style={{
                      background: 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)',
                      color: '#000',
                    }}
                  >
                    <Star className="w-4 h-4" />
                    Популярное
                  </div>
                )}

                {/* Item Icon */}
                <div 
                  className="w-20 h-20 rounded-xl mb-4 flex items-center justify-center text-4xl mx-auto"
                  style={{
                    background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                  }}
                >
                  {item.id === 1 && '👑'}
                  {item.id === 2 && '💎'}
                  {item.id === 3 && '👕'}
                  {item.id === 4 && '💰'}
                  {item.id === 5 && '📦'}
                  {item.id === 6 && '🐉'}
                </div>

                {/* Item Name */}
                <h3 className="text-xl font-bold mb-2 text-center">{item.name}</h3>
                
                {/* Period */}
                <div 
                  className="text-sm text-center mb-4"
                  style={{ color: '#9CA3AF' }}
                >
                  {item.period}
                </div>

                {/* Price */}
                <div 
                  className="text-3xl font-bold text-center mb-6"
                  style={{ color: '#7C4DFF' }}
                >
                  {item.price} ₽
                </div>

                {/* Buy Button */}
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  className="w-full py-3 px-4 rounded-lg flex items-center justify-center gap-2 font-medium"
                  style={{
                    background: 'linear-gradient(135deg, #7C4DFF 0%, #B794F6 100%)',
                    boxShadow: '0 4px 15px rgba(124, 77, 255, 0.3)',
                  }}
                >
                  <ShoppingCart className="w-5 h-5" />
                  Купить
                </motion.button>
              </motion.div>
            ))}
          </div>

          {/* Payment Methods */}
          <div 
            className="mt-8 p-6 rounded-xl"
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.05)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(124, 77, 255, 0.2)',
            }}
          >
            <h2 className="text-xl font-bold mb-4">Способы оплаты</h2>
            <div className="flex gap-4 flex-wrap">
              {['Банковская карта', 'QIWI', 'WebMoney', 'Яндекс.Деньги', 'Криптовалюта'].map((method) => (
                <div
                  key={method}
                  className="px-4 py-2 rounded-lg"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(124, 77, 255, 0.2)',
                    color: '#9CA3AF',
                  }}
                >
                  {method}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
