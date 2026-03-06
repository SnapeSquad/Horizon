// Скрипт для очистки всех пользователей из БД
const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const DB_PATH = path.join(__dirname, 'users.db');

const db = new sqlite3.Database(DB_PATH, (err) => {
    if (err) {
        console.error('❌ Ошибка подключения к БД:', err.message);
        process.exit(1);
    } else {
        console.log('✅ Подключено к SQLite БД');
        
        // Удаляем всех пользователей
        db.run('DELETE FROM users', (err) => {
            if (err) {
                console.error('❌ Ошибка при удалении пользователей:', err);
                process.exit(1);
            } else {
                console.log('✅ Все пользователи успешно удалены из БД');
                
                // Также очищаем связанные данные
                db.run('DELETE FROM forum_posts', (err) => {
                    if (err) console.warn('⚠️ Ошибка при очистке постов форума:', err);
                });
                
                db.run('DELETE FROM forum_topics', (err) => {
                    if (err) console.warn('⚠️ Ошибка при очистке тем форума:', err);
                });
                
                db.run('DELETE FROM forum_likes', (err) => {
                    if (err) console.warn('⚠️ Ошибка при очистке лайков:', err);
                });
                
                db.close((err) => {
                    if (err) {
                        console.error('❌ Ошибка при закрытии БД:', err);
                        process.exit(1);
                    } else {
                        console.log('✅ БД закрыта');
                        process.exit(0);
                    }
                });
            }
        });
    }
});
