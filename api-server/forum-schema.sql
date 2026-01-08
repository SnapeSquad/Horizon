-- Схема базы данных для форума
CREATE TABLE IF NOT EXISTS forum_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    icon TEXT,
    order_index INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS forum_topics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER NOT NULL,
    author_username TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT,
    is_pinned INTEGER DEFAULT 0,
    is_locked INTEGER DEFAULT 0,
    views INTEGER DEFAULT 0,
    replies_count INTEGER DEFAULT 0,
    last_reply_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES forum_categories(id)
);

CREATE TABLE IF NOT EXISTS forum_posts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    topic_id INTEGER NOT NULL,
    author_username TEXT NOT NULL,
    content TEXT NOT NULL,
    is_edited INTEGER DEFAULT 0,
    edited_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (topic_id) REFERENCES forum_topics(id)
);

CREATE TABLE IF NOT EXISTS forum_likes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER NOT NULL,
    username TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES forum_posts(id),
    UNIQUE(post_id, username)
);

-- Схема для косметики и модов
CREATE TABLE IF NOT EXISTS cosmetic_mods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    version TEXT NOT NULL,
    description TEXT,
    author TEXT NOT NULL,
    file_path TEXT NOT NULL,
    is_active INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cosmetic_animations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cosmetic_id TEXT NOT NULL,
    animation_type TEXT NOT NULL,
    frames TEXT NOT NULL,
    duration INTEGER DEFAULT 1000,
    loop INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);





