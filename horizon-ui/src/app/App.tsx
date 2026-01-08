import React, { useState, useEffect } from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import AuthPage from './pages/AuthPage';
import DashboardPage from './pages/DashboardPage';
import WardrobePage from './pages/WardrobePage';
import SettingsPage from './pages/SettingsPage';
import ForumPage from './pages/ForumPage';
import ShopPage from './pages/ShopPage';
import Sidebar from './components/Sidebar';
import ParticleBackground from './components/ParticleBackground';

// Проверка Electron API
const isElectron = typeof window !== 'undefined' && (window as any).electronAPI;

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkSession();

    // Слушаем автологин из Electron
    if (isElectron) {
      (window as any).electronAPI.onAutoLogin((session: any) => {
        setUser(session);
        setIsAuthenticated(true);
      });
    }
  }, []);

  const checkSession = async () => {
    if (isElectron) {
      try {
        const session = await (window as any).electronAPI.getSession();
        if (session && session.token) {
          setUser(session);
          setIsAuthenticated(true);
        }
      } catch (error) {
        console.error('Session check failed:', error);
      }
    }
    setLoading(false);
  };

  const handleLogin = (session: any) => {
    setUser(session);
    setIsAuthenticated(true);
  };

  const handleLogout = async () => {
    if (isElectron) {
      await (window as any).electronAPI.logout();
    }
    setUser(null);
    setIsAuthenticated(false);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: '#0F0F13' }}>
        <div style={{ color: '#E0E0E0', fontSize: '20px' }}>Загрузка...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen" style={{ 
      backgroundColor: '#0F0F13',
      fontFamily: 'Inter, sans-serif',
      color: '#E0E0E0'
    }}>
      <ParticleBackground />
      <HashRouter>
        <Routes>
          <Route 
            path="/auth" 
            element={
              isAuthenticated ? 
                <Navigate to="/dashboard" replace /> : 
                <AuthPage onLogin={handleLogin} />
            } 
          />
          <Route
            path="/*"
            element={
              isAuthenticated ? (
                <div className="flex min-h-screen">
                  <Sidebar user={user} onLogout={handleLogout} />
                  <div className="flex-1">
                    <Routes>
                      <Route path="/dashboard" element={<DashboardPage user={user} />} />
                      <Route path="/shop" element={<ShopPage user={user} />} />
                      <Route path="/forum" element={<ForumPage user={user} />} />
                      <Route path="/settings" element={<SettingsPage user={user} />} />
                      <Route path="/wardrobe" element={<WardrobePage user={user} />} />
                      <Route path="/" element={<Navigate to="/dashboard" replace />} />
                      <Route path="*" element={<Navigate to="/dashboard" replace />} />
                    </Routes>
                  </div>
                </div>
              ) : (
                <Navigate to="/auth" replace />
              )
            }
          />
        </Routes>
      </HashRouter>
    </div>
  );
}
