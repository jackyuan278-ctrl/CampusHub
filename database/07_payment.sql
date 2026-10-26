-- CampusHub 支付服务数据库
-- 服务归属：campus-payment-service
-- payment_no、biz_order_no 是跨服务业务号，不建立订单库外键。
USE campushub_payment;

CREATE TABLE IF NOT EXISTS tb_pay_order (
    id BIGINT NOT NULL COMMENT '支付记录内部ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付业务号',
    biz_order_no VARCHAR(64) NOT NULL COMMENT '订单服务的订单业务号',
    biz_type VARCHAR(32) NOT NULL DEFAULT 'SECONDHAND' COMMENT '业务类型',
    pay_user_id BIGINT NOT NULL COMMENT '用户业务ID',
    amount INT NOT NULL COMMENT '支付金额，单位：分',
    channel VARCHAR(32) NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1待支付 2处理中 3已支付 4失败 5已关闭',
    third_party_no VARCHAR(128) DEFAULT NULL,
    expire_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pay_time DATETIME NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_biz_order_no (biz_order_no),
    KEY idx_pay_user_id (pay_user_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='支付单';

CREATE TABLE IF NOT EXISTS tb_pay_event_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_no VARCHAR(64) NOT NULL,
    biz_order_no VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL DEFAULT 'PAYMENT_SUCCEEDED',
    published TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_time DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_event (payment_no, event_type),
    KEY idx_published_id (published, id)
) ENGINE=InnoDB COMMENT='支付事件Outbox';
