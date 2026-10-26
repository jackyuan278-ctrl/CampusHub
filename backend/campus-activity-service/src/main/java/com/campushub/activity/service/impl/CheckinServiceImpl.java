package com.campushub.activity.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.common.BizException;
import com.campushub.common.constant.RedisKeys;
import com.campushub.activity.domain.po.Checkin;
import com.campushub.activity.mapper.CheckinMapper;
import com.campushub.activity.service.ICheckinService;
import com.campushub.activity.domain.vo.CheckinVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 签到：Bitmap checkin:sign:{userId}:{yyyy-MM}，offset = 当月日 - 1。
 * streak 以 tb_checkin 为准（查今天→查昨天→0），天然跨月。
 */
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl extends ServiceImpl<CheckinMapper, Checkin> implements ICheckinService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CheckinVO checkin(Long userId) {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        String monthKey = RedisKeys.checkinSign(userId, ym.toString());

        // SETBIT 返回旧值做原子去重：旧值 true 说明今天已签，位本就是 1，重复置位无副作用
        Boolean oldBit = stringRedisTemplate.opsForValue()
                .setBit(monthKey, today.getDayOfMonth() - 1L, true);
        if (Boolean.TRUE.equals(oldBit)) {
            throw new BizException(400, "今日已签到");
        }
        try {
            Checkin yesterday = queryByDate(userId, today.minusDays(1));
            Checkin checkin = new Checkin();
            checkin.setUserId(userId);
            checkin.setCheckinDate(today);
            checkin.setCreateTime(LocalDateTime.now());
            checkin.setStreak(yesterday == null ? 1 : yesterday.getStreak() + 1);
            save(checkin);
        } catch (Exception e) {
            // 落库失败则回滚今天的位，否则用户当天被锁死且 streak 断档
            stringRedisTemplate.opsForValue().setBit(monthKey, today.getDayOfMonth() - 1L, false);
            throw e;
        }
        return buildVO(userId, today, ym, monthKey);
    }

    @Override
    public CheckinVO queryMine(Long userId) {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        String monthKey = RedisKeys.checkinSign(userId, ym.toString());
        return buildVO(userId, today, ym, monthKey);
    }

    private CheckinVO buildVO(Long userId, LocalDate today, YearMonth ym, String monthKey) {
        CheckinVO vo = new CheckinVO();
        vo.setSignedToday(BooleanUtil.isTrue(
                stringRedisTemplate.opsForValue().getBit(monthKey, today.getDayOfMonth() - 1L)));
        vo.setStreak(calcStreak(userId, today));
        vo.setCheckinDates(listMonthCheckinDates(ym, monthKey));
        return vo;
    }

    /** 先查今天（签过用今天的值），再查昨天（未签不断签），都没有则为 0 */
    private int calcStreak(Long userId, LocalDate today) {
        Checkin record = queryByDate(userId, today);
        if (record != null) {
            return record.getStreak();
        }
        Checkin yesterday = queryByDate(userId, today.minusDays(1));
        return yesterday == null ? 0 : yesterday.getStreak();
    }

    /** 遍历当月 Bitmap，收集已签日期（yyyy-MM-dd） */
    private List<String> listMonthCheckinDates(YearMonth ym, String monthKey) {
        List<String> dates = new ArrayList<>();
        for (int offset = 0; offset < ym.lengthOfMonth(); offset++) {
            if (BooleanUtil.isTrue(stringRedisTemplate.opsForValue().getBit(monthKey, offset))) {
                dates.add(ym.atDay(offset + 1).toString());
            }
        }
        return dates;
    }

    private Checkin queryByDate(Long userId, LocalDate date) {
        return lambdaQuery().eq(Checkin::getUserId, userId)
                .eq(Checkin::getCheckinDate, date).one();
    }
}





