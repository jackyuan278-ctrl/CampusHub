USE campushub_pay;
CREATE TABLE IF NOT EXISTS tb_pay_order (
 id BIGINT NOT NULL, biz_order_no BIGINT NOT NULL, pay_user_id BIGINT NOT NULL, amount INT NOT NULL,
 status TINYINT NOT NULL DEFAULT 1 COMMENT '1待支付 2已支付 3处理中', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, pay_time DATETIME NULL, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_biz_order_no(biz_order_no)
) ENGINE=InnoDB COMMENT='支付单';
CREATE TABLE IF NOT EXISTS tb_pay_event_outbox (
 id BIGINT NOT NULL AUTO_INCREMENT, biz_order_no BIGINT NOT NULL, published TINYINT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, published_time DATETIME NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_biz_order_no(biz_order_no), KEY idx_published_id(published,id)
) ENGINE=InnoDB COMMENT='支付成功事件Outbox';
