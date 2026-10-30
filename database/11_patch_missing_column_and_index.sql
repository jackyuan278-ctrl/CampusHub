-- CampusHub 审查补丁：建表脚本与代码不一致的列/索引
-- 服务归属：campus-user-service、campus-activity-service
--
-- 背景：
--   1) 01_user.sql 的 tb_user 没有 balance 列，但 UserServiceImpl / UserVO / 余额支付
--      都在用它。本地库是手工加过列的，按脚本从零建库一启动就报
--      Unknown column 'balance'，登录链路直接挂。
--   2) tb_user.phone 只有普通索引，同一手机号可注册多个账号，短信登录会取到随机一条。
--   3) tb_activity 只有 idx_status，列表按 (status, create_time) 过滤加排序，
--      复合条件下会退化成 filesort。
--
-- 幂等：所有 DDL 先查 information_schema，可重复执行。

-- ---------- 1. tb_user 补 balance 列 ----------
SET @has_balance = (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = 'campushub_user'
                      AND TABLE_NAME = 'tb_user'
                      AND COLUMN_NAME = 'balance');
SET @ddl = IF(@has_balance = 0,
    'ALTER TABLE `campushub_user`.`tb_user` ADD COLUMN `balance` INT NOT NULL DEFAULT 0 COMMENT ''账户余额（分）'' AFTER `phone`',
    'SELECT ''tb_user.balance 已存在'' ');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 2. tb_user.phone 补唯一索引 ----------
-- 若线上已有重复手机号，这一步会失败（ERROR 1062），先执行下面这条查一遍：
--   SELECT phone, COUNT(*) FROM campushub_user.tb_user
--    WHERE phone IS NOT NULL AND phone <> '' GROUP BY phone HAVING COUNT(*) > 1;
SET @has_uk_phone = (SELECT COUNT(*) FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = 'campushub_user'
                       AND TABLE_NAME = 'tb_user'
                       AND INDEX_NAME = 'uk_phone');
SET @ddl = IF(@has_uk_phone = 0,
    'ALTER TABLE `campushub_user`.`tb_user` ADD UNIQUE KEY `uk_phone` (`phone`)',
    'SELECT ''uk_phone 已存在'' ');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 3. tb_activity 补 (status, create_time) ----------
SET @has_act_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = 'campushub_activity'
                      AND TABLE_NAME = 'tb_activity'
                      AND INDEX_NAME = 'idx_status_create_time');
SET @ddl = IF(@has_act_idx = 0,
    'ALTER TABLE `campushub_activity`.`tb_activity` ADD KEY `idx_status_create_time` (`status`, `create_time`)',
    'SELECT ''idx_status_create_time 已存在'' ');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 4. （需要人工确认，默认不执行）biz_order_no 类型不一致 ----------
-- tb_pay_order.biz_order_no / tb_pay_event_outbox.biz_order_no 建表是 VARCHAR(64)，
-- 而 Java 侧 PayOrder.bizOrderNo 是 Long，等值查询会做隐式类型转换，索引失效。
-- 现有值都是订单业务号（纯数字），可以安全转换；执行前先确认没有非数字值：
--   SELECT COUNT(*) FROM campushub_payment.tb_pay_order WHERE biz_order_no REGEXP '[^0-9]';
-- 结果为 0 再执行下面两条：
--   ALTER TABLE campushub_payment.tb_pay_order MODIFY COLUMN biz_order_no BIGINT NOT NULL COMMENT '订单服务的订单业务号';
--   ALTER TABLE campushub_payment.tb_pay_event_outbox MODIFY COLUMN biz_order_no BIGINT NOT NULL;
