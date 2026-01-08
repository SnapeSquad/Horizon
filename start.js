const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('========================================');
console.log('   HORIZON LAUNCHER - AUTO START');
console.log('========================================\n');

// Проверка Node.js
try {
  const version = process.version;
  console.log(`✅ Node.js ${version} detected`);
} catch (error) {
  console.error('❌ Node.js not found! Please install Node.js.');
  process.exit(1);
}

// Функция для проверки и установки зависимостей
async function checkAndInstall(dir, name) {
  const nodeModulesPath = path.join(dir, 'node_modules');
  
  if (!fs.existsSync(nodeModulesPath)) {
    console.log(`📦 Installing ${name} dependencies...`);
    
    return new Promise((resolve, reject) => {
      const npm = spawn('npm', ['install'], {
        cwd: dir,
        shell: true,
        stdio: 'inherit'
      });
      
      npm.on('close', (code) => {
        if (code === 0) {
          console.log(`✅ ${name} dependencies installed\n`);
          resolve();
        } else {
          console.error(`❌ Failed to install ${name} dependencies`);
          reject(new Error(`npm install failed with code ${code}`));
        }
      });
    });
  } else {
    console.log(`✅ ${name} dependencies already installed`);
    return Promise.resolve();
  }
}

// Функция запуска процесса
function startProcess(dir, command, args, title) {
  console.log(`🚀 Starting ${title}...`);
  
  const proc = spawn(command, args, {
    cwd: dir,
    shell: true,
    stdio: 'inherit'
  });
  
  proc.on('error', (error) => {
    console.error(`❌ Failed to start ${title}:`, error.message);
  });
  
  return proc;
}

// Главная функция
async function main() {
  try {
    console.log('\n[1/4] Checking dependencies...\n');
    
    await checkAndInstall('api-server', 'API Server');
    await checkAndInstall('horizon-ui', 'React UI');
    await checkAndInstall('electron-launcher', 'Electron Launcher');
    
    console.log('\n[2/4] Starting API Server (port 3000)...');
    const apiServer = startProcess('api-server', 'npm', ['start'], 'API Server');
    
    // Ждем 3 секунды
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    console.log('\n[3/4] Starting React UI (port 5173/5174)...');
    const reactUI = startProcess('horizon-ui', 'npm', ['run', 'dev'], 'React UI');
    
    // Ждем 5 секунд
    await new Promise(resolve => setTimeout(resolve, 5000));
    
    console.log('\n[4/4] Starting Electron Launcher...\n');
    console.log('========================================');
    console.log('   ALL SERVICES STARTED!');
    console.log('========================================');
    console.log('API Server:   http://localhost:3000');
    console.log('React UI:     http://localhost:5173 (or 5174)');
    console.log('Electron:     Opening window...');
    console.log('========================================\n');
    console.log('Press Ctrl+C to stop all services.\n');
    
    const electron = startProcess('electron-launcher', 'npm', ['run', 'dev'], 'Electron Launcher');
    
    // Обработка выхода
    process.on('SIGINT', () => {
      console.log('\n\n🛑 Stopping all services...');
      apiServer.kill();
      reactUI.kill();
      electron.kill();
      process.exit(0);
    });
    
    // Если Electron закрылся, завершаем все
    electron.on('close', () => {
      console.log('\n✅ Electron closed. Stopping other services...');
      apiServer.kill();
      reactUI.kill();
      process.exit(0);
    });
    
  } catch (error) {
    console.error('\n❌ Error:', error.message);
    process.exit(1);
  }
}

main();

