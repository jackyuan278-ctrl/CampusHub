-- CampusHub 批量造数：活动与报名
-- 目标：120 条活动 + 对应报名记录，覆盖「有余量」「已报满并进候补」两种形态。
--
-- 设计要点：
--   1. 每 5 个活动里 1 个是满员档：quota 压到 6~13，报名人数报满、remain_quota = 0，
--      另有 2~4 条 status=2 的候补记录。其余活动 quota 40~199，只报 4~19 人，余量充足。
--   2. 报名人数受限于 tb_user 实际人数（tb_signup 有 (activity_id,user_id) 唯一键），
--      所以满员档的 quota 必须小于用户总数，不能拍脑袋给 50。
--   3. 状态分布 60% 报名中 / 15% 进行中 / 25% 已结束，三种状态的时间窗各自闭包，
--      避免出现「报名中但报名已截止」这种自相矛盾的数据。
--   4. remain_quota 一律等于 quota - 已报名人数，和业务口径保持一致。

USE campushub_activity;

DROP PROCEDURE IF EXISTS seed_activities;

DELIMITER $$
CREATE PROCEDURE seed_activities(IN v_base BIGINT, IN v_total INT, IN v_signup_base BIGINT)
BEGIN
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_user_cnt INT DEFAULT 0;
    DECLARE v_quota INT;
    DECLARE v_taken INT;
    DECLARE v_wait INT;
    DECLARE v_status TINYINT;
    DECLARE v_publisher BIGINT;
    DECLARE v_signup_id BIGINT;

    DROP TEMPORARY TABLE IF EXISTS tmp_a_users;
    CREATE TEMPORARY TABLE tmp_a_users (rn INT PRIMARY KEY, user_id BIGINT);
    INSERT INTO tmp_a_users
    SELECT ROW_NUMBER() OVER (ORDER BY id), id FROM campushub_user.tb_user;
    SELECT COUNT(*) INTO v_user_cnt FROM tmp_a_users;
    IF v_user_cnt < 10 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'tb_user 少于 10 人，满员+候补场景造不出来';
    END IF;

    DROP TABLE IF EXISTS tmp_seed_seq;
    CREATE TABLE tmp_seed_seq (n INT PRIMARY KEY) ENGINE = MEMORY;
    INSERT INTO tmp_seed_seq
    SELECT a.i + b.i * 10 + c.i * 100
    FROM (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
         (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
         (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c;

    SET v_signup_id = v_signup_base;

    WHILE v_i < v_total DO
        -- 状态：20 个一轮 -> 12 报名中、3 进行中、5 已结束
        SET v_status = CASE WHEN MOD(v_i, 20) < 12 THEN 1
                            WHEN MOD(v_i, 20) < 15 THEN 2
                            ELSE 3 END;

        IF MOD(v_i, 5) = 0 THEN
            -- 满员档
            SET v_quota = 6 + MOD(v_i * 3, 8);
            SET v_taken = v_quota;
            SET v_wait  = 2 + MOD(v_i, 3);
        ELSE
            -- 有余量档
            SET v_quota = 40 + MOD(v_i * 17, 160);
            SET v_taken = 4 + MOD(v_i * 11, 16);
            SET v_wait  = 0;
        END IF;

        -- 兜底：报名 + 候补不能超过用户总数（否则唯一键冲突）
        IF v_taken + v_wait > v_user_cnt THEN
            SET v_taken = v_user_cnt - v_wait;
            IF v_taken < 0 THEN SET v_taken = 0; SET v_wait = 0; END IF;
        END IF;

        SELECT user_id INTO v_publisher
          FROM tmp_a_users WHERE rn = 1 + MOD(v_i, v_user_cnt);

        INSERT INTO tb_activity
            (id, title, description, cover_url, location,
             start_time, end_time, signup_start_time, signup_end_time,
             quota, remain_quota, status, publisher_id, create_time, update_time)
        VALUES (
            v_base + v_i,
            CONCAT(
                ELT(1 + MOD(v_i, 12), '编程马拉松', '校园歌手大赛', '羽毛球联赛', '读书分享会',
                                      '志愿者招募', 'AI 技术讲座', '校园摄影展', '创业沙龙',
                                      '期末互助自习', '心理健康工作坊', '社团联合招新', '荧光夜跑'),
                ' · ',
                ELT(1 + MOD(v_i * 7, 5), '春季场', '总决赛', '初赛', '体验场', '分享会')
            ),
            '活动详情：名额有限，报名从速；现场签到可领纪念品，迟到 15 分钟视为放弃名额。',
            '',
            ELT(1 + MOD(v_i * 3, 6), '体育馆', '图书馆报告厅', '大学生活动中心',
                                     '南区操场', '教学楼 A101', '创新创业园'),
            -- 活动开始
            CASE v_status WHEN 1 THEN NOW() + INTERVAL 7 DAY
                          WHEN 2 THEN NOW() - INTERVAL 2 HOUR
                          ELSE NOW() - INTERVAL 15 DAY END,
            -- 活动结束
            CASE v_status WHEN 1 THEN NOW() + INTERVAL 7 DAY + INTERVAL 3 HOUR
                          WHEN 2 THEN NOW() + INTERVAL 2 HOUR
                          ELSE NOW() - INTERVAL 15 DAY + INTERVAL 3 HOUR END,
            -- 报名开始
            CASE v_status WHEN 1 THEN NOW() - INTERVAL 3 DAY
                          WHEN 2 THEN NOW() - INTERVAL 10 DAY
                          ELSE NOW() - INTERVAL 20 DAY END,
            -- 报名截止
            CASE v_status WHEN 1 THEN NOW() + INTERVAL 5 DAY
                          WHEN 2 THEN NOW() - INTERVAL 2 DAY
                          ELSE NOW() - INTERVAL 18 DAY END,
            v_quota,
            v_quota - v_taken,
            v_status,
            v_publisher,
            NOW() - INTERVAL MOD(v_i * 13, 2000) HOUR,
            NOW() - INTERVAL MOD(v_i * 13, 2000) HOUR
        );

        -- 已报名
        IF v_taken > 0 THEN
            INSERT INTO tb_signup (id, activity_id, user_id, status, create_time, update_time)
            SELECT v_signup_id + s.n, v_base + v_i, u.user_id, 1,
                   NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY
            FROM tmp_seed_seq s
            JOIN tmp_a_users u ON u.rn = 1 + MOD(v_i * 3 + s.n, v_user_cnt)
            WHERE s.n < v_taken;
            SET v_signup_id = v_signup_id + v_taken;
        END IF;

        -- 候补（只有满员档才有）
        IF v_wait > 0 THEN
            INSERT INTO tb_signup (id, activity_id, user_id, status, create_time, update_time)
            SELECT v_signup_id + s.n, v_base + v_i, u.user_id, 2,
                   NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY
            FROM tmp_seed_seq s
            JOIN tmp_a_users u ON u.rn = 1 + MOD(v_i * 3 + v_taken + s.n, v_user_cnt)
            WHERE s.n < v_wait;
            SET v_signup_id = v_signup_id + v_wait;
        END IF;

        SET v_i = v_i + 1;
    END WHILE;

    DROP TABLE IF EXISTS tmp_seed_seq;
    DROP TEMPORARY TABLE IF EXISTS tmp_a_users;
END$$
DELIMITER ;
