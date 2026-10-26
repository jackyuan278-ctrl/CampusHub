package com.campushub.activity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.common.BizException;
import com.campushub.common.constant.RedisKeys;
import com.campushub.activity.domain.po.Activity;
import com.campushub.activity.domain.po.Signup;
import com.campushub.activity.mapper.ActivityMapper;
import com.campushub.activity.domain.dto.SignupFormDTO;
import com.campushub.activity.domain.vo.SignupVO;
import com.campushub.activity.mapper.SignupMapper;
import com.campushub.activity.service.ISignupService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl extends ServiceImpl<SignupMapper, Signup> implements ISignupService {

    private final ActivityMapper activityMapper;
    private final StringRedisTemplate stringRedisTemplate;
    // 两个 Bean 按字段名注入（signupScript/cancelSignupScript），依赖 -parameters 编译参数
    private final DefaultRedisScript<Long> signupScript;
    private final DefaultRedisScript<String> cancelSignupScript;



    @Override
    public void signup(SignupFormDTO form, Long userId) {
        Long activityId = form.getActivityId();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(400,"活动不存在");
        }
        validateSignupWindow(activity);
        List<String> signList = new ArrayList<>();
        signList.add(RedisKeys.signupStock(activityId));
        signList.add(RedisKeys.signupUsers(activityId));
        long result = stringRedisTemplate.execute(signupScript, signList, userId.toString(), activity.getQuota().toString());
        if (result == 0) {
            try {
                boolean updated = lambdaUpdate().eq(Signup::getActivityId, activityId)
                        .set(Signup::getStatus, 1)
                        .eq(Signup::getUserId, userId)
                        .update();
                if (!updated) {
                    save(new Signup().setActivityId(activityId).setUserId(userId).setStatus(1));
                }
            } catch (Exception e) {
                // MySQL 写入失败时，回滚 Redis 中刚刚完成的报名操作
                stringRedisTemplate.opsForSet()
                        .remove(RedisKeys.signupUsers(activityId), userId.toString());
                stringRedisTemplate.opsForValue()
                        .increment(RedisKeys.signupStock(activityId));
                throw e;
            }
        }
        if (result == 1) {
            throw new BizException(400,"名额已满，可加入候补");
        }
        if (result == 2) {
            throw new BizException(400,"请勿重复报名");
        }
    }

    @Override
    public void joinWaitlist(SignupFormDTO form, Long userId) {
        Long activityId = form.getActivityId();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(400,"活动不存在");
        }
        validateSignupWindow(activity);
        Signup one = lambdaQuery().eq(Signup::getUserId, userId).eq(Signup::getActivityId, activityId).one();
        if (one == null) {
            try {
                save(new Signup().setActivityId(activityId).setUserId(userId).setStatus(2));
            } catch (DuplicateKeyException e) {
                throw new BizException(400,"请勿重复加入");
            }
        } else if (one.getStatus() == 3) {
            lambdaUpdate().eq(Signup::getId, one.getId()).set(Signup::getStatus, 2).update();
        } else {
            throw new BizException(400,"已报名或已在候补中，请勿重复加入");
        }

        stringRedisTemplate.opsForList().rightPush(RedisKeys.signupWaitlist(activityId),userId.toString());
    }

    private void validateSignupWindow(Activity activity) {
        if (activity.getStatus() != 1) {
            throw new BizException(400, "活动不在报名时间");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getSignupStartTime())) {
            throw new BizException(400, "报名尚未开始");
        }
        if (now.isAfter(activity.getSignupEndTime())) {
            throw new BizException(400, "报名已结束");
        }
    }

    @Override
    public void cancel(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(400,"活动不存在");
        }
        Integer status = activity.getStatus();
        if (status != 1) {
            throw new BizException(400,"活动不在报名时间");
        }
        Signup one = lambdaQuery().eq(Signup::getUserId, userId).eq(Signup::getActivityId, activityId).one();
        if (one == null) {
            throw new BizException(400,"未报名");
        }
        if (one.getStatus() == 2) {
            stringRedisTemplate.opsForList().remove(RedisKeys.signupWaitlist(activityId), 1, userId.toString());
            try {
                boolean updated = lambdaUpdate().eq(Signup::getId, one.getId())
                        .set(Signup::getStatus, 3)
                        .update();
                if (!updated) {
                    throw new BizException(500, "取消候补失败");
                }
            } catch (Exception e) {
                // MySQL 更新失败时，恢复 Redis 中的候补记录
                stringRedisTemplate.opsForList()
                        .rightPush(RedisKeys.signupWaitlist(activityId), userId.toString());
                throw e;
            }
            return;
        }
        if (one.getStatus() !=1) {
            throw new BizException(400,"未报名");
        }


        List<String> signupList = new ArrayList<>();
        signupList.add(RedisKeys.signupStock(activityId));
        signupList.add(RedisKeys.signupUsers(activityId));
        signupList.add(RedisKeys.signupWaitlist(activityId));
        String result = stringRedisTemplate.execute(cancelSignupScript, signupList, userId.toString());
        if (result.equals("-1")) {
            throw new BizException(400,"未报名");
        }
        try {
            boolean cancelled = lambdaUpdate().eq(Signup::getUserId, userId)
                    .eq(Signup::getActivityId, activityId)
                    .set(Signup::getStatus, 3)
                    .update();
            if (!cancelled) {
                throw new BizException(500, "取消报名失败");
            }
            if (!result.equals("0")) {
                boolean promoted = lambdaUpdate().eq(Signup::getUserId, Long.valueOf(result))
                        .eq(Signup::getActivityId, activityId)
                        .set(Signup::getStatus, 1)
                        .update();
                if (!promoted) {
                    throw new BizException(500, "候补补位失败");
                }
            }
        } catch (Exception e) {
            // MySQL 更新失败时，尽量恢复 Redis 中取消报名前的状态
            stringRedisTemplate.opsForSet()
                    .add(RedisKeys.signupUsers(activityId), userId.toString());
            if (result.equals("0")) {
                stringRedisTemplate.opsForValue()
                        .decrement(RedisKeys.signupStock(activityId));
            } else {
                stringRedisTemplate.opsForSet()
                        .remove(RedisKeys.signupUsers(activityId), result);
                // 候补用户补位失败时，恢复到候补队列末尾
                stringRedisTemplate.opsForList()
                        .rightPush(RedisKeys.signupWaitlist(activityId), result);
                // 原报名用户的数据库状态已经改为取消，失败时恢复为已报名
                lambdaUpdate().eq(Signup::getUserId, userId)
                        .eq(Signup::getActivityId, activityId)
                        .set(Signup::getStatus, 1)
                        .update();
            }
            throw e;
        }
    }

    @Override
    public List<SignupVO> queryMine(Long userId) {
        List<Signup> list = lambdaQuery().in(Signup::getStatus, 1, 2).eq(Signup::getUserId, userId).list();
        if (list.isEmpty()) {
            return List.of();
        }
        List<SignupVO> signupVOList = new ArrayList<>();
        List<Long> activityIds = new ArrayList<>();
        for (Signup signup : list) {
            activityIds.add(signup.getActivityId());
        }
        Map<Long, Activity> activityMap = activityMapper.selectByIds(activityIds).stream().collect(Collectors.toMap(a -> a.getId(), a -> a));
        for (Signup signup : list) {
            SignupVO signupVO = BeanUtil.copyProperties(signup, SignupVO.class);
            Activity act = activityMap.get(signup.getActivityId());
            if (act != null) {
                signupVO.setActivityTitle(act.getTitle());
                signupVO.setCoverUrl(act.getCoverUrl());
                signupVO.setLocation(act.getLocation());
                signupVO.setStartTime(act.getStartTime());
            }
            if (signupVO.getStatus().equals(2)) {
                Long range = stringRedisTemplate.opsForList().indexOf(RedisKeys.signupWaitlist(signup.getActivityId()), userId.toString());
                if (range != null) {
                    signupVO.setWaitPosition(range.intValue() + 1);
                }
            }
            signupVOList.add(signupVO);
        }

        return signupVOList;
    }
}





