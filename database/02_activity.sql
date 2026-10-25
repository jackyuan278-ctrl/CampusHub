USE campushub_activity;
CREATE TABLE IF NOT EXISTS tb_activity (
 id BIGINT NOT NULL, title VARCHAR(100) NOT NULL, description TEXT, cover_url VARCHAR(255), location VARCHAR(100) NOT NULL,
 start_time DATETIME NOT NULL, end_time DATETIME NOT NULL, signup_start_time DATETIME NOT NULL, signup_end_time DATETIME NOT NULL,
 quota INT NOT NULL, status TINYINT NOT NULL DEFAULT 1 COMMENT '1报名中 2进行中 3已结束 4已取消', publisher_id BIGINT NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), KEY idx_status(status), KEY idx_signup_time(signup_start_time, signup_end_time)
) ENGINE=InnoDB COMMENT='校园活动';
CREATE TABLE IF NOT EXISTS tb_signup (
 id BIGINT NOT NULL, activity_id BIGINT NOT NULL, user_id BIGINT NOT NULL, status TINYINT NOT NULL DEFAULT 1 COMMENT '1已报名 2候补 3已取消',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_activity_user(activity_id,user_id), KEY idx_user(user_id), KEY idx_activity_status(activity_id,status)
) ENGINE=InnoDB COMMENT='活动报名';
CREATE TABLE IF NOT EXISTS tb_checkin (
 id BIGINT NOT NULL, user_id BIGINT NOT NULL, checkin_date DATE NOT NULL, streak INT NOT NULL DEFAULT 1,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id), UNIQUE KEY uk_user_date(user_id,checkin_date), KEY idx_date(checkin_date)
) ENGINE=InnoDB COMMENT='活动/校园签到记录';
