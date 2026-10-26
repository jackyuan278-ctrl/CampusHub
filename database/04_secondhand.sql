-- CampusHub 二手交易服务数据库
-- 服务归属：campus-secondhand-service；seller_id、category_id 是业务ID，不建立跨服务外键。
USE campushub_secondhand;

CREATE TABLE IF NOT EXISTS tb_category (
    id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB COMMENT='二手物品分类';

CREATE TABLE IF NOT EXISTS tb_item (
    id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL COMMENT '用户业务ID',
    name VARCHAR(255) NOT NULL,
    price INT NOT NULL COMMENT '价格，单位：分',
    image VARCHAR(500) NOT NULL DEFAULT '',
    category_id BIGINT NOT NULL,
    condition_desc VARCHAR(100) NOT NULL DEFAULT '' COMMENT '成色描述',
    description VARCHAR(1000) NOT NULL DEFAULT '',
    trade_location VARCHAR(255) NOT NULL DEFAULT '',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 1在售 2预订 3已售',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_seller_id (seller_id),
    KEY idx_category_id (category_id),
    KEY idx_status_create_time (status, create_time)
) ENGINE=InnoDB COMMENT='校园二手物品';
