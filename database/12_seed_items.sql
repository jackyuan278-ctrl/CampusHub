-- CampusHub 批量造数：二手商品
-- 目标：tb_item 追加 100 万行，用于验证深分页、索引与热榜排序的真实表现。
--
-- 设计要点：
--   1. 不用 WITH RECURSIVE 生成序列——cte_max_recursion_depth 默认 1000，百万级要改会话变量，
--      而临时表交叉连接（10×10×10×10）一次生成 1 万行序列，过程里反复复用，更稳。
--   2. 卖家从 tb_user 轮转取，不写死 ID：用户表以后加人重跑也不会挂。
--   3. 名称/分类/价格/成色/地点都按序号取模拼装，保证 100 万行有区分度而不是复制同一行。
--   4. create_time 铺满最近 180 天，让「最新」排序和热榜都有真实的时间分布。
--   5. status 按 93.5% 在售 / 4% 已售 / 2.5% 下架分布，购物车与热榜能测到非在售分支。
--
-- 幂等：按传入的起始 ID 落库，重复执行会主键冲突（这是有意的，避免手抖插两遍）。

USE campushub_secondhand;

DROP PROCEDURE IF EXISTS seed_items;

DELIMITER $$
CREATE PROCEDURE seed_items(IN v_base BIGINT, IN v_total INT, IN v_batch INT)
BEGIN
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_cnt INT DEFAULT 0;

    -- 卖家池
    DROP TEMPORARY TABLE IF EXISTS tmp_sellers;
    CREATE TEMPORARY TABLE tmp_sellers (rn INT PRIMARY KEY, seller_id BIGINT);
    INSERT INTO tmp_sellers
    SELECT ROW_NUMBER() OVER (ORDER BY id), id FROM campushub_user.tb_user;
    SELECT COUNT(*) INTO v_cnt FROM tmp_sellers;
    IF v_cnt = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'tb_user 没有数据，先把用户造出来';
    END IF;

    -- 0..9999 的序号表，逐批复用。
    -- 必须用 MEMORY 普通表而不是 TEMPORARY：MySQL 不允许同一条语句里用不同别名
    -- 多次引用同一张临时表（ERROR 1137 Can't reopen table），交叉连接会直接失败。
    DROP TABLE IF EXISTS tmp_seed_d;
    CREATE TABLE tmp_seed_d (i INT) ENGINE = MEMORY;
    INSERT INTO tmp_seed_d VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);
    DROP TABLE IF EXISTS tmp_seed_seq;
    CREATE TABLE tmp_seed_seq (n INT PRIMARY KEY) ENGINE = MEMORY;
    INSERT INTO tmp_seed_seq
    SELECT a.i + b.i * 10 + c.i * 100 + d.i * 1000
    FROM tmp_seed_d a, tmp_seed_d b, tmp_seed_d c, tmp_seed_d d;

    WHILE v_i < v_total DO
        INSERT INTO tb_item
            (id, seller_id, name, price, image, category_id,
             condition_desc, description, trade_location, status, create_time, update_time)
        SELECT
            v_base + v_i + q.n,
            s.seller_id,
            CONCAT(
                ELT(1 + MOD(g, 4), '数码产品', '教材书籍', '生活用品', '学习用品'),
                ' · ',
                ELT(1 + MOD(g DIV 3, 8), '九成新', '八成新', '几乎全新', '自用闲置',
                                          '毕业清仓', '宿舍搬空', '低价转让', '急出'),
                -- 物品索引用 g/4 而不是 g：分类按 g%4 走，两者同周期会让「分类 × 物品」
                -- 绑死成 4 种固定搭配。错开周期后组合才铺得开，但仍锁在本分类的候选池里。
                CASE MOD(g, 4)
                    WHEN 0 THEN ELT(1 + MOD(g DIV 4, 4), '机械键盘', '降噪耳机', '外接显示器', '移动电源')
                    WHEN 1 THEN ELT(1 + MOD(g DIV 4, 4), '高数教材', '考研资料', '四六级真题', '专业课课本')
                    WHEN 2 THEN ELT(1 + MOD(g DIV 4, 4), '护眼台灯', '桌面小风扇', '保温杯', '收纳箱')
                    ELSE        ELT(1 + MOD(g DIV 4, 4), '双肩背包', '木吉他', '折叠雨伞', '绘图工具')
                END,
                ' #', v_base + v_i + q.n
            ),
            300 + MOD(g * 37, 199700),
            '',
            ELT(1 + MOD(g, 4), 1, 2, 3, 20002),
            ELT(1 + MOD(g * 13, 5), '全新未拆', '九成新', '有轻微使用痕迹', '八成新', '正常使用痕迹'),
            '自用闲置，功能正常无暗病，校内面交可当面验货。',
            ELT(1 + MOD(g * 11, 6), '图书馆门口', '三食堂门口', '东区宿舍楼下',
                                     '理工楼大厅', '南区操场', '北门快递点'),
            CASE WHEN MOD(g, 25) = 0 THEN 3 WHEN MOD(g, 40) = 0 THEN 0 ELSE 1 END,
            NOW() - INTERVAL MOD(g * 613, 15552000) SECOND,
            NOW() - INTERVAL MOD(g * 613, 15552000) SECOND
        FROM (
            SELECT n, v_i + n AS g FROM tmp_seed_seq WHERE n < LEAST(v_batch, v_total - v_i)
        ) q
        JOIN tmp_sellers s ON s.rn = 1 + MOD(q.g, v_cnt);

        SET v_i = v_i + v_batch;
    END WHILE;

    DROP TABLE IF EXISTS tmp_seed_seq;
    DROP TABLE IF EXISTS tmp_seed_d;
    DROP TEMPORARY TABLE IF EXISTS tmp_sellers;
END$$
DELIMITER ;
