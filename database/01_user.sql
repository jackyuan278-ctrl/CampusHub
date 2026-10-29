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

-- 余额扣款幂等流水：一个业务订单号最多产生一笔扣款。
-- uk_biz_order_no 是支付重试不重复扣款的最终保证，不能省。
CREATE TABLE IF NOT EXISTS tb_balance_deduction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    biz_order_no BIGINT NOT NULL COMMENT '业务订单ID，幂等键',
    user_id BIGINT NOT NULL COMMENT '扣款用户ID',
    amount INT NOT NULL COMMENT '扣款金额，单位：分',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_order_no (biz_order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='余额扣款幂等流水';
