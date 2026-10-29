-- CampusHub 订单 / 支付服务字段对齐脚本
-- 服务归属：campus-order-service、campus-payment-service
--
-- 背景：
--   早期建表脚本按「电商订单」设计，带了 order_no、seller_id、payment_no 等 NOT NULL 列，
--   但实体类没有对应字段，INSERT 时这些列既不出现也没有默认值。
--   线上 MySQL 开启 STRICT_TRANS_TABLES，插入会直接报
--   ERROR 1364 (HY000): Field 'order_no' doesn't have a default value。
--
-- 本脚本只补「实体有、表里没有」的列；「表里有、实体没有」的列只要有默认值就不动。
-- 幂等：先查 information_schema，已存在的列跳过，可重复执行。

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN p_schema VARCHAR(64),
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    DECLARE column_count INT DEFAULT 0;
    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = p_schema
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column;
    IF column_count = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table,
                          '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- tb_order：实体有 paymentType / consignTime，表里缺
CALL add_column_if_missing('campushub_order', 'tb_order', 'payment_type',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''0未选择 1余额 2模拟支付'' AFTER total_fee');
CALL add_column_if_missing('campushub_order', 'tb_order', 'consign_time',
    'DATETIME NULL COMMENT ''发货时间'' AFTER pay_time');

-- tb_order_detail：实体有 spec / updateTime，表里缺
CALL add_column_if_missing('campushub_order', 'tb_order_detail', 'spec',
    'VARCHAR(100) NOT NULL DEFAULT '''' COMMENT ''下单时规格快照'' AFTER image');
CALL add_column_if_missing('campushub_order', 'tb_order_detail', 'update_time',
    'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time');

DROP PROCEDURE IF EXISTS add_column_if_missing;
