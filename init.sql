-- =============================================
-- CampusRes — 校园上网指南资源库 初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS campus_resources
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE campus_resources;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'user',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 分类表
CREATE TABLE IF NOT EXISTS categories (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    sort_order INT         DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预设分类
INSERT INTO categories (id, name, sort_order) VALUES
    (1, 'Steam 相关',        1),
    (2, 'VPN与加速器',       2),
    (3, 'BT/种子与下载',     3),
    (4, 'GitHub 与开源',     4),
    (5, 'API 对接实战',       5),
    (6, '其他工具',          6)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order);

-- 3. 资源表
CREATE TABLE IF NOT EXISTS resources (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(100) NOT NULL,
    description    TEXT,
    category_id    INT,
    file_path      VARCHAR(255),
    file_size      BIGINT       DEFAULT 0,
    original_name  VARCHAR(255),
    download_count INT          DEFAULT 0,
    uploader_id    INT,
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 文章表
CREATE TABLE IF NOT EXISTS articles (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    content     TEXT,
    category_id INT,
    author_id   INT,
    view_count  INT          DEFAULT 0,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (author_id)   REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 可选：管理员账号（密码是 BCrypt 哈希，明文为 admin123）
-- INSERT INTO users (username, password, role) VALUES ('admin', '$2a$10$...', 'admin');
