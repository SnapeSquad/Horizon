require('dotenv').config();
const express = require('express');
const session = require('express-session');
const axios = require('axios');
const path = require('path');

const app = express();
const PORT = process.env.ADMIN_PORT || 4000;
const API_URL = process.env.API_URL || 'http://localhost:3000';

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

app.use(session({
  secret: 'horizon-admin-secret-key',
  resave: false,
  saveUninitialized: false,
  cookie: { maxAge: 24 * 60 * 60 * 1000 } // 24 hours
}));

// Auth middleware
function requireAuth(req, res, next) {
  if (!req.session.user || !['owner', 'curator', 'admin'].includes(req.session.user.role)) {
    return res.redirect('/login');
  }
  next();
}

// Routes
app.get('/', (req, res) => {
  res.redirect('/dashboard');
});

app.get('/login', (req, res) => {
  if (req.session.user) {
    return res.redirect('/dashboard');
  }
  res.render('login', { error: null });
});

app.post('/login', async (req, res) => {
  const { username, password } = req.body;
  
  try {
    const response = await axios.post(`${API_URL}/api/auth/login`, { username, password });
    
    if (response.data.success) {
      // Проверяем роль
      const role = response.data.role || 'player';
      if (!['owner', 'curator', 'admin'].includes(role)) {
        return res.render('login', { error: 'У вас нет прав администратора' });
      }
      
      req.session.user = {
        username: response.data.username,
        role: role,
        token: response.data.token
      };
      
      return res.redirect('/dashboard');
    }
    
    res.render('login', { error: response.data.message || 'Неверный логин или пароль' });
  } catch (error) {
    console.error('Login error:', error);
    res.render('login', { error: 'Ошибка сервера' });
  }
});

app.get('/logout', (req, res) => {
  req.session.destroy();
  res.redirect('/login');
});

app.get('/dashboard', requireAuth, async (req, res) => {
  try {
    // Получаем статистику
    const stats = {
      users: 0,
      news: 0,
      shopItems: 0,
      forumTopics: 0,
      revenue: 0
    };
    
    res.render('dashboard', { user: req.session.user, stats });
  } catch (error) {
    console.error('Dashboard error:', error);
    res.status(500).send('Ошибка сервера');
  }
});

app.get('/users', requireAuth, (req, res) => {
  res.render('users', { user: req.session.user });
});

app.get('/news', requireAuth, (req, res) => {
  res.render('news', { user: req.session.user });
});

app.get('/shop', requireAuth, (req, res) => {
  res.render('shop', { user: req.session.user });
});

app.get('/forum', requireAuth, (req, res) => {
  res.render('forum', { user: req.session.user });
});

app.get('/servers', requireAuth, (req, res) => {
  res.render('servers', { user: req.session.user });
});

app.get('/payments', requireAuth, (req, res) => {
  res.render('payments', { user: req.session.user });
});

app.get('/transactions', requireAuth, (req, res) => {
  res.render('transactions', { user: req.session.user });
});

// API proxy endpoints
app.get('/api/*', requireAuth, async (req, res) => {
  try {
    const response = await axios.get(`${API_URL}${req.path}`, {
      params: req.query,
      headers: { 'Authorization': `Bearer ${req.session.user.token}` }
    });
    res.json(response.data);
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

app.post('/api/*', requireAuth, async (req, res) => {
  try {
    const response = await axios.post(`${API_URL}${req.path}`, req.body, {
      headers: { 'Authorization': `Bearer ${req.session.user.token}` }
    });
    res.json(response.data);
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

app.put('/api/*', requireAuth, async (req, res) => {
  try {
    const response = await axios.put(`${API_URL}${req.path}`, req.body, {
      headers: { 'Authorization': `Bearer ${req.session.user.token}` }
    });
    res.json(response.data);
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

app.delete('/api/*', requireAuth, async (req, res) => {
  try {
    const response = await axios.delete(`${API_URL}${req.path}`, {
      data: req.body,
      headers: { 'Authorization': `Bearer ${req.session.user.token}` }
    });
    res.json(response.data);
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`🔐 Админ-панель запущена: http://localhost:${PORT}`);
  console.log(`📡 API: ${API_URL}`);
});
