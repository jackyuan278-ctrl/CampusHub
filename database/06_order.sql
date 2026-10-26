-- CampusHub 订单服务数据库
-- 服务归属：campus-order-service
-- 订单只保存外部业务ID和必要快照，不访问用户、二手物品、支付数据库。
USE campushub_order;

CREATE TABLE IF NOT EXISTS tb_order (
    id BIGINT NOT NULL COMMENT '订单内部ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单业务号，跨服务传递此字段',
    user_id BIGINT NOT NULL COMMENT '用户业务ID',
    total_fee INT NOT NULL COMMENT '订单金额，单位：分',
    order_type VARCHAR(32) NOT NULL DEFAULT 'SECONDHAND' COMMENT '订单来源类型',
    payment_no VARCHAR(64) DEFAULT NULL COMMENT '支付服务业务号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1待支付 2已支付 3交易中 4已完成 5已关闭 6退款中 7已退款',
    receiver VARCHAR(50) NOT NULL DEFAULT '',
    phone VARCHAR(20) NOT NULL DEFAULT '',
    address VARCHAR(500) NOT NULL DEFAULT '' COMMENT '下单时地址快照',
    remark VARCHAR(200) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pay_time DATETIME NULL,
    end_time DATETIME NULL,
    close_time DATETIME NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='校园二手交易订单';

CREATE TABLE IF NOT EXISTS tb_order_detail (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL COMMENT '本订单库内的订单ID',
    item_id BIGINT NOT NULL COMMENT '二手物品业务ID，不建立跨库外键',
    seller_id BIGINT NOT NULL COMMENT '卖家用户业务ID',
    name VARCHAR(255) NOT NULL COMMENT '下单时物品名称快照',
    price INT NOT NULL COMMENT '下单时单价，单位：分',
    num INT NOT NULL DEFAULT 1,
    image VARCHAR(500) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB COMMENT='订单明细';
