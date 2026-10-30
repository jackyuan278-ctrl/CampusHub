-- 剩余名额落库，作为报名/取消的唯一裁决依据
-- 背景：原 quota 只记总名额，剩余名额仅存活在 Redis（signup:stock:{id}），
--       key 丢失后 signup.lua 会按 quota 重置为满额，导致 50 人活动超卖到 100 人。
-- 改造后：
--   quota        总名额，发布时写一次，此后只读
--   remain_quota 剩余名额，报名 -1、取消 +1，条件更新保证不超卖

-- 幂等：列已存在时跳过 ALTER，重复执行不会报 1060
SET @has_remain = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'campushub_activity'
                     AND TABLE_NAME = 'tb_activity'
                     AND COLUMN_NAME = 'remain_quota');
SET @ddl = IF(@has_remain = 0,
    'ALTER TABLE campushub_activity.tb_activity ADD COLUMN remain_quota INT NOT NULL DEFAULT 0 COMMENT ''剩余名额（报名-1/取消+1），填报名的裁决依据''',
    'SELECT ''tb_activity.remain_quota 已存在'' ');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 按已报名人数回填（status=1 为已报名，2 候补中不占名额）
UPDATE campushub_activity.tb_activity a
    LEFT JOIN (SELECT activity_id, COUNT(*) AS c
               FROM campushub_activity.tb_signup
               WHERE status = 1
               GROUP BY activity_id) s ON s.activity_id = a.id
SET a.remain_quota = a.quota - IFNULL(s.c, 0);

-- 校验：remain_quota 必须落在 [0, quota] 区间
SELECT id, title, quota, remain_quota
FROM campushub_activity.tb_activity
ORDER BY id;
