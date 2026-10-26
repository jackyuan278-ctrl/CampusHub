package com.campushub.activity.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.activity.domain.po.Activity;
import com.campushub.activity.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatusTask {

    private final ActivityMapper activityMapper;
    /**
     * 每分钟扫描一次：
     * status=1（报名中）且 now >= startTime → 2（进行中）；
     * status=2（进行中）且 now >= endTime → 3（已结束）；
     * 报名截止时间已过但活动未开始的属于正常状态，不动。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void updateActivityStatus() {
        LocalDateTime now = LocalDateTime.now();
        activityMapper.update(null, Wrappers.<Activity>lambdaUpdate()
                .eq(Activity::getStatus, 1)
                .le(Activity::getStartTime, now)
                .set(Activity::getStatus, 2));
        activityMapper.update(null, Wrappers.<Activity>lambdaUpdate()
                .eq(Activity::getStatus, 2)
                .le(Activity::getEndTime, now)
                .set(Activity::getStatus, 3));
    }
}





