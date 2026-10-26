-- CampusHub 购物车服务数据库
-- 服务归属：campus-cart-service；item_id 指向二手交易服务的物品业务ID，不跨库联表。
USE campushub_cart;

CREATE TABLE IF NOT EXISTS tb_cart (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL COMMENT '用户业务ID',
    item_id BIGINT NOT NULL COMMENT '二手物品业务ID',
    num INT NOT NULL DEFAULT 1,
    selected TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_item (user_id, item_id),
    KEY idx_user_id (user_id),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB COMMENT='二手交易购物车';
