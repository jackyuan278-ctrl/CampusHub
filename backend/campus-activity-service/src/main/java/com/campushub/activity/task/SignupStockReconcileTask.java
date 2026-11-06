package com.campushub.activity.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.activity.domain.po.Activity;
import com.campushub.activity.mapper.ActivityMapper;
import com.campushub.common.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报名库存对账。
 *
 * <p>预扣模式下 Redis 的 {@code signup:stock} 是 DB {@code remain_quota} 的副本，
 * 因为补偿可能失败（进程被杀、Redis 抖动、人工改库），两边会慢慢漂移。
 * 这个任务定期以 DB 为准把 Redis 覆盖回去，并在日志里留下痕迹。
 *
 * <p>刻意做的事：
 * <ul>
 *   <li>只扫「报名中且报名窗口未结束」的活动，不全表扫；</li>
 *   <li>Redis 里没有 key 的直接跳过——那是「没预热」，下次报名会自己重建，不算漂移；</li>
 *   <li>顺手清掉过期的临时占用，避免 ZSet 无限增长（占用只是幂等标记，过期即失效）。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignupStockReconcileTask {

    /** 单次最多扫多少个活动，避免一次任务把库拖慢 */
    private static final int BATCH_SIZE = 200;
    /** 一次任务最多打印几条漂移明细，避免刷屏 */
    private static final int DETAIL_LOG_LIMIT = 5;

    private final ActivityMapper activityMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(initialDelay = 120_000, fixedDelay = 300_000)
    public void reconcile() {
        List<Activity> activities;
        try {
            activities = activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                    .select(Activity::getId, Activity::getRemainQuota)
                    .eq(Activity::getStatus, 1)
                    .gt(Activity::getSignupEndTime, LocalDateTime.now())
                    .last("LIMIT " + BATCH_SIZE));
        } catch (Exception e) {
            log.error("报名库存对账：查询活动列表失败", e);
            return;
        }

        int checked = 0;
        int fixed = 0;
        for (Activity activity : activities) {
            Long activityId = activity.getId();
            try {
                String cached = stringRedisTemplate.opsForValue()
                        .get(RedisKeys.signupStock(activityId));
                if (cached == null) {
                    continue;
                }
                checked++;

                int dbRemain = activity.getRemainQuota() == null ? 0 : activity.getRemainQuota();
                if (!String.valueOf(dbRemain).equals(cached)) {
                    stringRedisTemplate.opsForValue()
                            .set(RedisKeys.signupStock(activityId), String.valueOf(dbRemain));
                    fixed++;
                    if (fixed <= DETAIL_LOG_LIMIT) {
                        log.error("报名库存漂移，已按 DB 修正 activityId={} redis={} db={}",
                                activityId, cached, dbRemain);
                    }
                }

                // 清理过期占用：占用只是「同一用户别重复提交」的标记，过期就没有意义了
                stringRedisTemplate.opsForZSet().removeRangeByScore(
                        RedisKeys.signupHold(activityId), 0, System.currentTimeMillis());
            } catch (Exception e) {
                log.warn("报名库存对账：活动 {} 处理失败", activityId, e);
            }
        }

        if (checked > 0) {
            log.info("报名库存对账完成：检查 {} 个活动，修正 {} 个", checked, fixed);
        }
    }
}
