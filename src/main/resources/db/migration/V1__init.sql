-- =========================================================================
-- 脆弱版（Vulnerable）と対策版（Secure）で同じスキーマを 2 セット用意し、
-- 同名の機能でも DB レベルで完全に分離する。
-- =========================================================================

-- -------------------------------------------------------------------------
-- Vulnerable 側
-- -------------------------------------------------------------------------
CREATE TABLE vuln_users (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(128) NOT NULL,
    role        VARCHAR(16)  NOT NULL DEFAULT 'USER',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vuln_posts (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_vuln_posts_user FOREIGN KEY (user_id) REFERENCES vuln_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vuln_comments (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    post_id     BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    content     TEXT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_vuln_comments_post FOREIGN KEY (post_id) REFERENCES vuln_posts(id),
    CONSTRAINT fk_vuln_comments_user FOREIGN KEY (user_id) REFERENCES vuln_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------------------
-- Secure 側（同じ DDL のミラー）
-- -------------------------------------------------------------------------
CREATE TABLE sec_users (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(128) NOT NULL,
    role        VARCHAR(16)  NOT NULL DEFAULT 'USER',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sec_posts (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_sec_posts_user FOREIGN KEY (user_id) REFERENCES sec_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sec_comments (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    post_id     BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    content     TEXT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_sec_comments_post FOREIGN KEY (post_id) REFERENCES sec_posts(id),
    CONSTRAINT fk_sec_comments_user FOREIGN KEY (user_id) REFERENCES sec_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================================
-- 初期データ（Vulnerable 側のみ SQL で投入）
--   * password カラムには「平文」がそのまま入っているのが教材としての肝。
--   * Secure 側は DataSeeder が BCrypt ハッシュ化して投入する。
-- =========================================================================

INSERT INTO vuln_users (username, password, email, role) VALUES
    ('admin', 'admin123',     'admin@example.com', 'ADMIN'),
    ('alice', 'wonderland',   'alice@example.com', 'USER'),
    ('bob',   'builder',      'bob@example.com',   'USER');

INSERT INTO vuln_posts (user_id, title, content) VALUES
    (1, 'ようこそ', '管理者からのお知らせです。投稿は HTML がそのまま表示されることがあります。'),
    (2, '初投稿', 'こんにちは、Alice です。'),
    (3, '雑談', 'Bob です。よろしく。');

INSERT INTO vuln_comments (post_id, user_id, content) VALUES
    (1, 2, 'お知らせ確認しました。'),
    (1, 3, 'よろしくお願いします。');
