-- CampusHub 用户服务数据库
-- 服务归属：campus-user-service；外部 ID 仅作为业务字段保存，不建立跨库外键。
USE campushub_user;

CREATE TABLE IF NOT EXISTS tb_user (
    id BIGINT NOT NULL COMMENT '用户ID（业务ID）',
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) DEFAULT NULL,
    avatar_url VARCHAR(255) DEFAULT NULL,
    email VARCHAR(64) DEFAULT NULL,
    bio VARCHAR(255) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_phone (phone)
) ENGINE=InnoDB COMMENT='CampusHub用户';

CREATE TABLE IF NOT EXISTS tb_address (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL COMMENT '用户业务ID',
    receiver VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50) NOT NULL DEFAULT '',
    city VARCHAR(50) NOT NULL DEFAULT '',
    district VARCHAR(50) NOT NULL DEFAULT '',
    detail VARCHAR(255) NOT NULL,
    is_default TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='用户地址';
