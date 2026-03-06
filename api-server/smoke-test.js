const path = require('path');
const { spawn } = require('child_process');

require('dotenv').config({ path: path.join(__dirname, '.env') });

const BASE_URL = 'http://127.0.0.1:3000';
const STARTUP_TIMEOUT_MS = 20000;
const POLL_INTERVAL_MS = 400;

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function requestJson(path, options = {}) {
  const defaultHeaders = options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' };
  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: { ...defaultHeaders, ...(options.headers || {}) },
  });

  let json = null;
  try {
    json = await response.json();
  } catch (error) {
    throw new Error(`Invalid JSON for ${path}: ${error.message}`);
  }

  return { response, json };
}

async function isServerReady() {
  try {
    const { response } = await requestJson('/api/cosmetics/available');
    return response.ok;
  } catch {
    return false;
  }
}

async function waitForServerReady(timeoutMs) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeoutMs) {
    if (await isServerReady()) {
      return true;
    }
    await sleep(POLL_INTERVAL_MS);
  }
  return false;
}

async function runSmokeChecks() {
  const username = `smoke_${Date.now().toString(36).slice(-8)}`;
  const password = 'smokePass123';
  const adminToken = process.env.ADMIN_TOKEN || 'horizon_admin_2024';

  const register = await requestJson('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  if (!register.response.ok || !register.json.success || typeof register.json.token !== 'string') {
    throw new Error('POST /api/auth/register failed smoke contract');
  }

  const login = await requestJson('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  if (!login.response.ok || !login.json.success || typeof login.json.token !== 'string') {
    throw new Error('POST /api/auth/login failed smoke contract');
  }

  const cosmetics = await requestJson('/api/cosmetics/available');
  if (!cosmetics.response.ok || !cosmetics.json.success || !Array.isArray(cosmetics.json.cosmetics)) {
    throw new Error('GET /api/cosmetics/available failed smoke contract');
  }

  const payment = await requestJson('/api/payment/generate', {
    method: 'POST',
    body: JSON.stringify({ username: 'smoke_user', amount: 100 }),
  });
  if (!payment.response.ok || !payment.json.success || typeof payment.json.paymentUrl !== 'string') {
    throw new Error('POST /api/payment/generate failed smoke contract');
  }

  const forum = await requestJson('/api/forum/categories');
  if (!forum.response.ok || !forum.json.success || !Array.isArray(forum.json.categories)) {
    throw new Error('GET /api/forum/categories failed smoke contract');
  }

  if (forum.json.categories.length === 0) {
    throw new Error('Forum categories are empty');
  }

  const categoryId = forum.json.categories[0].id;
  const topicTitle = `Smoke Topic ${Date.now()}`;
  const topicContent = 'Smoke topic content';

  const topicCreate = await requestJson('/api/forum/topics', {
    method: 'POST',
    body: JSON.stringify({
      category_id: categoryId,
      title: topicTitle,
      content: topicContent,
      author_username: username,
    }),
  });
  if (!topicCreate.response.ok || !topicCreate.json.success || !topicCreate.json.topic_id) {
    throw new Error('POST /api/forum/topics failed smoke contract');
  }

  const topics = await requestJson(`/api/forum/topics?category_id=${encodeURIComponent(categoryId)}`);
  if (!topics.response.ok || !topics.json.success || !Array.isArray(topics.json.topics)) {
    throw new Error('GET /api/forum/topics failed smoke contract');
  }

  const createdTopic = topics.json.topics.find((t) => t.id === topicCreate.json.topic_id);
  if (!createdTopic || typeof createdTopic.author_role !== 'string') {
    throw new Error('Forum topic response missing author_role');
  }

  const postContent = 'Smoke post content';
  const postCreate = await requestJson('/api/forum/posts', {
    method: 'POST',
    body: JSON.stringify({
      topic_id: topicCreate.json.topic_id,
      content: postContent,
      author_username: username,
    }),
  });
  if (!postCreate.response.ok || !postCreate.json.success || !postCreate.json.post_id) {
    throw new Error('POST /api/forum/posts failed smoke contract');
  }

  const posts = await requestJson(
    `/api/forum/posts?topic_id=${encodeURIComponent(topicCreate.json.topic_id)}&username=${encodeURIComponent(username)}`
  );
  if (!posts.response.ok || !posts.json.success || !Array.isArray(posts.json.posts)) {
    throw new Error('GET /api/forum/posts failed smoke contract');
  }

  const createdPost = posts.json.posts.find((p) => p.id === postCreate.json.post_id);
  if (!createdPost || typeof createdPost.author_role !== 'string') {
    throw new Error('Forum post response missing author_role');
  }

  const adminUsers = await requestJson('/api/admin/users', {
    headers: { 'x-admin-token': adminToken },
  });
  if (!adminUsers.response.ok || !adminUsers.json.success || !Array.isArray(adminUsers.json.users)) {
    throw new Error('GET /api/admin/users failed smoke contract');
  }

  const newsTitle = `Smoke News ${Date.now()}`;
  const newsCreate = await requestJson('/api/admin/news', {
    method: 'POST',
    headers: { 'x-admin-token': adminToken },
    body: JSON.stringify({
      title: newsTitle,
      content: 'Smoke news content',
      author: 'smoke-test',
    }),
  });
  if (!newsCreate.response.ok || !newsCreate.json.success || !newsCreate.json.news_id) {
    throw new Error('POST /api/admin/news failed smoke contract');
  }

  const newsList = await requestJson('/api/admin/news', {
    headers: { 'x-admin-token': adminToken },
  });
  if (!newsList.response.ok || !newsList.json.success || !Array.isArray(newsList.json.news)) {
    throw new Error('GET /api/admin/news failed smoke contract');
  }

  const createdNews = newsList.json.news.find((entry) => entry.id === newsCreate.json.news_id);
  if (!createdNews) {
    throw new Error('Created news item not found in /api/admin/news response');
  }

  const newsDelete = await requestJson(`/api/admin/news/${newsCreate.json.news_id}`, {
    method: 'DELETE',
    headers: { 'x-admin-token': adminToken },
  });
  if (!newsDelete.response.ok || !newsDelete.json.success) {
    throw new Error('DELETE /api/admin/news/:id failed smoke contract');
  }

  const cosmeticName = `Smoke Cosmetic ${Date.now()}`;
  const formData = new FormData();
  formData.append('name', cosmeticName);
  formData.append('description', 'smoke cosmetic');
  formData.append('pivot_point', 'head');
  formData.append('price', '5');
  formData.append('rarity', 'common');
  formData.append(
    'model',
    new File([JSON.stringify({ format_version: '1.12.0', 'minecraft:geometry': [] })], 'model.json', {
      type: 'application/json',
    })
  );
  formData.append('texture', new File(['smoke-texture'], 'texture.png', { type: 'image/png' }));

  const cosmeticCreate = await requestJson('/api/admin/cosmetics', {
    method: 'POST',
    headers: { 'x-admin-token': adminToken },
    body: formData,
  });
  if (!cosmeticCreate.response.ok || !cosmeticCreate.json.success || !cosmeticCreate.json.cosmetic?.id) {
    throw new Error('POST /api/admin/cosmetics failed smoke contract');
  }

  const cosmeticDelete = await requestJson(`/api/admin/cosmetics/${cosmeticCreate.json.cosmetic.id}`, {
    method: 'DELETE',
    headers: { 'x-admin-token': adminToken },
  });
  if (!cosmeticDelete.response.ok || !cosmeticDelete.json.success) {
    throw new Error('DELETE /api/admin/cosmetics/:id failed smoke contract');
  }

  console.log('Smoke checks passed: auth, cosmetics, payment, forum, admin(news/cosmetics/users) + role contracts');
}

(async () => {
  let serverProcess = null;
  let startedByTest = false;

  try {
    const alreadyRunning = await isServerReady();

    if (!alreadyRunning) {
      serverProcess = spawn('node', ['server.js'], {
        cwd: __dirname,
        stdio: 'inherit',
        env: {
          ...process.env,
          NODE_ENV: 'test',
          HORIZON_SILENT_TELEGRAM: '1',
          TELEGRAM_BOT_TOKEN: process.env.TELEGRAM_BOT_TOKEN || '',
        },
      });
      startedByTest = true;

      const ready = await waitForServerReady(STARTUP_TIMEOUT_MS);
      if (!ready) {
        throw new Error('API server did not become ready in time');
      }
    }

    await runSmokeChecks();
    process.exitCode = 0;
  } catch (error) {
    console.error(`Smoke test failed: ${error.message}`);
    process.exitCode = 1;
  } finally {
    if (startedByTest && serverProcess && !serverProcess.killed) {
      serverProcess.kill('SIGTERM');
    }
  }
})();

