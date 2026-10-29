-- CampusHub 活动服务数据库
-- 服务归属：campus-activity-service；publisher_id、user_id 仅保存用户业务ID。
USE campushub_activity;

CREATE TABLE IF NOT EXISTS tb_activity (
    id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    cover_url VARCHAR(255),
    location VARCHAR(100) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    signup_start_time DATETIME NOT NULL,
    signup_end_time DATETIME NOT NULL,
    quota INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1报名中 2进行中 3已结束 4已取消',
    publisher_id BIGINT NOT NULL COMMENT '发布者用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_publisher_id (publisher_id),
    KEY idx_signup_time (signup_start_time, signup_end_time)
) ENGINE=InnoDB COMMENT='校园活动';

CREATE TABLE IF NOT EXISTS tb_signup (
    id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL COMMENT '活动业务ID',
    user_id BIGINT NOT NULL COMMENT '用户业务ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1已报名 2候补 3已取消',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_user (activity_id, user_id),
    KEY idx_user_id (user_id),
    KEY idx_activity_status (activity_id, status)
) ENGINE=InnoDB COMMENT='活动报名';

-- 签到是「用户 × 自然日」维度的连续签到，与具体活动无关。
-- 早期按活动签到设计带 activity_id，实体和业务逻辑都没有这个维度，故移除。
CREATE TABLE IF NOT EXISTS tb_checkin (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL COMMENT '用户业务ID',
    checkin_date DATE NOT NULL,
    streak INT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_date (user_id, checkin_date),
    KEY idx_date (checkin_date)
) ENGINE=InnoDB COMMENT='用户每日签到';
