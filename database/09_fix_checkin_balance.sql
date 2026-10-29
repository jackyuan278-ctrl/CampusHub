-- CampusHub 签到表 / 余额扣款流水表修复脚本
-- 服务归属：campus-activity-service、campus-user-service
--
-- 背景：
--   1) tb_checkin 按「活动签到」设计，带 activity_id BIGINT NOT NULL 且无默认值，
--      但 Checkin 实体与 CheckinServiceImpl 都没有活动维度（签到是用户 × 自然日的连续签到），
--      INSERT 时该列既不出现也没有默认值，线上 STRICT_TRANS_TABLES 下直接报
--      ERROR 1364: Field 'activity_id' doesn't have a default value，签到功能 100% 失败。
--   2) tb_balance_deduction 在代码里被 UserServiceImpl.deductBalance 用作幂等流水，
--      但建表脚本和线上库都没有这张表，余额支付路径必然失败，
--      且失去 uk_biz_order_no 这道「重试不重复扣款」的最终保证。
--
-- 幂等：建表用 IF NOT EXISTS，删列/删索引先查 information_schema，可重复执行。

-- ---------- 1. 补建余额扣款幂等流水表 ----------
CREATE TABLE IF NOT EXISTS campushub_user.tb_balance_deduction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    biz_order_no BIGINT NOT NULL COMMENT '业务订单ID，幂等键',
    user_id BIGINT NOT NULL COMMENT '扣款用户ID',
    amount INT NOT NULL COMMENT '扣款金额，单位：分',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_order_no (biz_order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='余额扣款幂等流水';

-- ---------- 2. tb_checkin 去掉 activity_id 维度 ----------
DROP PROCEDURE IF EXISTS drop_index_if_exists;
DROP PROCEDURE IF EXISTS drop_column_if_exists;

DELIMITER $$
CREATE PROCEDURE drop_index_if_exists(
    IN p_schema VARCHAR(64),
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64)
)
BEGIN
    DECLARE idx_count INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = p_schema
      AND TABLE_NAME = p_table
      AND INDEX_NAME = p_index;
    IF idx_count > 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table,
                          '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE drop_column_if_exists(
    IN p_schema VARCHAR(64),
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64)
)
BEGIN
    DECLARE column_count INT DEFAULT 0;
    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = p_schema
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column;
    IF column_count > 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table,
                          '` DROP COLUMN `', p_column, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 先删依赖 activity_id 的索引（唯一键 + 普通索引），再删列
CALL drop_index_if_exists('campushub_activity', 'tb_checkin', 'uk_activity_user_date');
CALL drop_index_if_exists('campushub_activity', 'tb_checkin', 'idx_activity_user');
CALL drop_column_if_exists('campushub_activity', 'tb_checkin', 'activity_id');

-- 补上代码真正需要的唯一键：同一用户同一天只能有一条签到
SET @has_uk = (SELECT COUNT(*) FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = 'campushub_activity'
                 AND TABLE_NAME = 'tb_checkin'
                 AND INDEX_NAME = 'uk_user_date');
SET @ddl = IF(@has_uk = 0,
    'ALTER TABLE `campushub_activity`.`tb_checkin` ADD UNIQUE KEY `uk_user_date` (`user_id`, `checkin_date`)',
    'SELECT ''uk_user_date 已存在'' ');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP PROCEDURE IF EXISTS drop_index_if_exists;
DROP PROCEDURE IF EXISTS drop_column_if_exists;
