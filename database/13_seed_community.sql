-- CampusHub 批量造数：问答
-- 目标：120 条问题 + 每问 2~6 条回答（约 480 条），用于验证列表渲染与热度排序。
--
-- 设计要点：
--   1. 热度分三档：每 20 条 1 条爆款（8000+ 浏览）、每 5 条 1 条中热、其余低热——
--      避免「所有问题都差不多热」，否则按热度排序看不出差异。
--   2. answer_count 由公式 2+MOD(g*7,5) 决定，回答按同样公式落库，两边天然一致，
--      不会出现「显示 5 条回答、点进去只有 2 条」。
--   3. 回答 id 由变量累加，保证唯一且连续，便于排查。
--   4. 顺带把问题挂上标签并回填 tb_tag.question_count，否则标签页全是 0。

USE campushub_community;

DROP PROCEDURE IF EXISTS seed_questions;

DELIMITER $$
CREATE PROCEDURE seed_questions(IN v_q_base BIGINT, IN v_q_total INT,
                                IN v_a_base BIGINT, IN v_qt_base BIGINT)
BEGIN
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_user_cnt INT DEFAULT 0;
    DECLARE v_ans_cnt INT DEFAULT 0;
    DECLARE v_ans_id BIGINT DEFAULT 0;
    DECLARE v_tag_cnt INT DEFAULT 0;

    -- 用户池：作者从真实用户里轮转，前端头像/昵称才查得到
    DROP TEMPORARY TABLE IF EXISTS tmp_q_users;
    CREATE TEMPORARY TABLE tmp_q_users (rn INT PRIMARY KEY, user_id BIGINT);
    INSERT INTO tmp_q_users
    SELECT ROW_NUMBER() OVER (ORDER BY id), id FROM campushub_user.tb_user;
    SELECT COUNT(*) INTO v_user_cnt FROM tmp_q_users;
    IF v_user_cnt = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'tb_user 没有数据';
    END IF;

    -- 0..9999 序号表（MEMORY 普通表，临时表不能在一条语句里多次别名引用）
    DROP TABLE IF EXISTS tmp_seed_d;
    CREATE TABLE tmp_seed_d (i INT) ENGINE = MEMORY;
    INSERT INTO tmp_seed_d VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);
    DROP TABLE IF EXISTS tmp_seed_seq;
    CREATE TABLE tmp_seed_seq (n INT PRIMARY KEY) ENGINE = MEMORY;
    INSERT INTO tmp_seed_seq
    SELECT a.i + b.i * 10 + c.i * 100 + d.i * 1000
    FROM tmp_seed_d a, tmp_seed_d b, tmp_seed_d c, tmp_seed_d d;

    -- ---------- 问题 ----------
    INSERT INTO tb_question
        (id, user_id, title, content, view_count, like_count, answer_count, comment_count,
         status, create_time, update_time)
    SELECT
        v_q_base + q.g,
        u.user_id,
        CONCAT(
            ELT(1 + MOD(q.g, 10), '高数期末', '数据结构', '线性代数', '操作系统', '英语四级',
                                  '社团招新', '宿舍关系', '食堂推荐', '考研择校', '实习简历'),
            ELT(1 + MOD(q.g DIV 3, 6), ' 怎么复习比较稳？', ' 有没有速通办法？', ' 求个有经验的学长',
                                        ' 这个坑谁知道怎么填？', ' 有推荐的资料吗？', ' 想听听大家的建议')
        ),
        CONCAT('具体情况：',
               ELT(1 + MOD(q.g DIV 11, 5), '还剩两周', '刚开学', '上次挂过', '准备保研', '纯小白'),
               '，想问问大家一般怎么处理，先谢过。'),
        -- 爆款用 19、中热用 7 取模：话题按 g%10 走，若这里也用 20/5 会和话题同相位，
        -- 结果是「爆款清一色高数期末」。19、7 与 10 互质，热度才真正散到各个话题上。
        CASE WHEN MOD(q.g, 19) = 0 THEN 8000 + MOD(q.g * 733, 30000)
             WHEN MOD(q.g, 7) = 0  THEN 600 + MOD(q.g * 233, 2400)
             ELSE 20 + MOD(q.g * 53, 280) END,
        CASE WHEN MOD(q.g, 19) = 0 THEN 120 + MOD(q.g * 17, 380)
             WHEN MOD(q.g, 7) = 0  THEN 12 + MOD(q.g * 7, 40)
             ELSE MOD(q.g * 3, 9) END,
        2 + MOD(q.g * 7, 5),
        0,
        1,
        NOW() - INTERVAL MOD(q.g * 977, 2592000) SECOND,
        NOW() - INTERVAL MOD(q.g * 977, 2592000) SECOND
    FROM (SELECT n, n AS g FROM tmp_seed_seq WHERE n < v_q_total) q
    JOIN tmp_q_users u ON u.rn = 1 + MOD(q.g, v_user_cnt);

    -- ---------- 回答：数量与上面 answer_count 的公式严格一致 ----------
    SET v_i = 0;
    SET v_ans_id = v_a_base;
    WHILE v_i < v_q_total DO
        SET v_ans_cnt = 2 + MOD(v_i * 7, 5);
        INSERT INTO tb_answer
            (id, question_id, user_id, content, like_count, comment_count, status,
             create_time, update_time)
        SELECT
            v_ans_id + s.n,
            v_q_base + v_i,
            u.user_id,
            CONCAT(
                ELT(1 + MOD(s.n + v_i, 6), '我之前也遇到过，', '说个有用的：', '简单总结一下，',
                                           '补充一点，', '别慌，', '顶一下，'),
                ELT(1 + MOD(s.n + v_i * 3, 5), '先刷历年真题。', '直接找老师问最快。',
                                               '问问同专业的学长学姐。', 'B 站有整套课，跟着走就行。',
                                               '把课本例题全部手推一遍。')
            ),
            CASE WHEN MOD(s.n + v_i, 15) = 0 THEN 40 + MOD(s.n * 13 + v_i, 160)
                 ELSE MOD(s.n * 5 + v_i * 2, 12) END,
            0, 1,
            NOW() - INTERVAL MOD((v_i * 31 + s.n) * 137, 2592000) SECOND,
            NOW() - INTERVAL MOD((v_i * 31 + s.n) * 137, 2592000) SECOND
        FROM tmp_seed_seq s
        JOIN tmp_q_users u ON u.rn = 1 + MOD(v_i * 5 + s.n, v_user_cnt)
        WHERE s.n < v_ans_cnt;
        SET v_ans_id = v_ans_id + v_ans_cnt;
        SET v_i = v_i + 1;
    END WHILE;

    -- ---------- 问题挂标签：按序号均匀分到现有标签上 ----------
    SELECT COUNT(*) INTO v_tag_cnt FROM tb_tag;
    IF v_tag_cnt > 0 THEN
        INSERT INTO tb_question_tag (id, question_id, tag_id, create_time)
        SELECT v_qt_base + q.g, v_q_base + q.g, t.tag_id, NOW()
        FROM (SELECT n, n AS g FROM tmp_seed_seq WHERE n < v_q_total) q
        JOIN (
            SELECT ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn, id AS tag_id FROM tb_tag
        ) t ON t.rn = MOD(q.g, v_tag_cnt);
    END IF;

    -- 回填标签计数
    UPDATE tb_tag t
       SET question_count = (SELECT COUNT(*) FROM tb_question_tag qt WHERE qt.tag_id = t.id);

    DROP TABLE IF EXISTS tmp_seed_seq;
    DROP TABLE IF EXISTS tmp_seed_d;
    DROP TEMPORARY TABLE IF EXISTS tmp_q_users;
END$$
DELIMITER ;
