-- CampusHub 补丁：二手商品列表默认排序改 id 倒序所需的联合索引
--
-- 背景：
--   列表查询是 WHERE status = 1 ORDER BY id DESC LIMIT ?，深页游标是 id < cursorId。
--   原有 idx_status_create_time(status, create_time) 的前缀只能定位 status，
--   create_time 的顺序对「按 id 排序」没有帮助，MySQL 只能取出全部在售行再 filesort。
--   InnoDB 二级索引叶子自带主键，所以 (status, id) 等价于「按 status 分区的 id 顺序表」，
--   反向扫描即可在 O(limit) 内拿到最新 N 条，深页也不会退化成全表扫描。
--
-- 幂等：先查 information_schema.STATISTICS，已存在就跳过，重复执行安全。
-- 注意：百万行表上 ALTER 会重建二级索引，线上请在低峰执行（本机造数库约 20~40 秒）。

USE campushub_secondhand;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'campushub_secondhand'
      AND TABLE_NAME = 'tb_item'
      AND INDEX_NAME = 'idx_status_id'
);

SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE tb_item ADD INDEX idx_status_id (status, id)',
    'SELECT ''idx_status_id 已存在，跳过'' AS msg');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
