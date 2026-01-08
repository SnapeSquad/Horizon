import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import AuthPage from './pages/AuthPage';
import DashboardPage from './pages/DashboardPage';
import WardrobePage from './pages/WardrobePage';
import SettingsPage from './pages/SettingsPage';
import ForumPage from './pages/ForumPage';
import ShopPage from './pages/ShopPage';

export default function App() {
  return (
    <div className="min-h-screen" style={{ 
      backgroundColor: '#0F0F13',
      fontFamily: 'Inter, sans-serif',
      color: '#E0E0E0'
    }}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navigate to="/auth" replace />} />
          <Route path="/auth" element={<AuthPage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/wardrobe" element={<WardrobePage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/forum" element={<ForumPage />} />
          <Route path="/shop" element={<ShopPage />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}
