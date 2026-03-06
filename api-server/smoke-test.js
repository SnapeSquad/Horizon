const { spawn } = require('child_process');

const BASE_URL = 'http://127.0.0.1:3000';
const STARTUP_TIMEOUT_MS = 20000;
const POLL_INTERVAL_MS = 400;

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function requestJson(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
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

  console.log('Smoke checks passed: auth, cosmetics, payment, forum categories');
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
