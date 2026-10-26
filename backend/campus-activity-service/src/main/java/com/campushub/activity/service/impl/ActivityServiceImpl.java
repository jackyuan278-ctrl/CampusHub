package com.campushub.activity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.common.BizException;
import com.campushub.common.constant.RedisKeys;
import com.campushub.activity.domain.dto.ActivityFormDTO;
import com.campushub.activity.domain.dto.ActivityPageQuery;
import com.campushub.activity.domain.po.Activity;
import com.campushub.activity.domain.po.Signup;
import com.campushub.activity.domain.vo.ActivityVO;
import com.campushub.activity.es.ActivityDoc;
import com.campushub.activity.es.ActivityRepository;
import com.campushub.activity.mapper.ActivityMapper;
import com.campushub.activity.service.IActivityService;
import com.campushub.activity.domain.dto.PageDTO;
import com.campushub.activity.mapper.SignupMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements IActivityService {

    private final SignupMapper signupMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ActivityRepository activityRepository;

    // ============ 以下为核心业务（面试深挖区），由你实现，依赖已注入 ============

    @Override
    public Long publish(ActivityFormDTO form, Long publisherId) {
        LocalDateTime signupStart = form.getSignupStartTime();
        LocalDateTime signupEnd = form.getSignupEndTime();
        LocalDateTime activityStart = form.getStartTime();
        LocalDateTime activityEnd = form.getEndTime();
        // 时间链：signupStart < signupEnd <= activityStart < activityEnd
        if (!signupStart.isBefore(signupEnd)) {
            throw new BizException(400, "报名开始时间必须早于报名截止时间");
        }
        if (signupEnd.isAfter(activityStart)) {
            throw new BizException(400, "报名截止时间不能晚于活动开始时间");
        }
        if (!activityStart.isBefore(activityEnd)) {
            throw new BizException(400, "活动开始时间必须早于结束时间");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activityStart.isBefore(now)) {
            throw new BizException(400, "活动开始时间不能早于当前时间");
        }
        if (activityEnd.isBefore(now)) {
            throw new BizException(400, "活动结束时间不能早于当前时间");
        }
        Activity activity = BeanUtil.copyProperties(form, Activity.class);
        activity.setPublisherId(publisherId);
        activity.setCreateTime(LocalDateTime.now());
        // 发布即报名中；1→2、2→3 的流转归 ActivityStatusTask
        activity.setStatus(1);
        save(activity);
        stringRedisTemplate.opsForValue().set(RedisKeys.signupStock(activity.getId()),
                activity.getQuota().toString());
        saveDoc(activity);
        return activity.getId();
    }

    @Override
    public PageDTO<ActivityVO> queryActivityPage(ActivityPageQuery query) {
        Integer status = query.getStatus();
        String keyword = query.getKeyword();
        LambdaQueryChainWrapper<Activity> chain = lambdaQuery()
                .eq(status!=null,Activity::getStatus, status)
                .like(StringUtils.hasText(keyword),Activity::getTitle, keyword)
                .orderByDesc(Activity::getCreateTime);
        Page<Activity> page = chain.page(query.toMpPage());
        List<Activity> activityList = page.getRecords();
        PageDTO<ActivityVO> pageDTO = new PageDTO<>();
        List<ActivityVO> activityVOList = new ArrayList<>();
        pageDTO.setTotal(page.getTotal());
        for (Activity activity : activityList) {
            ActivityVO activityVO = BeanUtil.copyProperties(activity, ActivityVO.class);
            if (activity.getStatus().equals(1)) {
                String remain = stringRedisTemplate.opsForValue().get(RedisKeys.signupStock(activity.getId()));
                if (remain == null) {
                    activityVO.setRemainingQuota(activity.getQuota());
                }else{
                    activityVO.setRemainingQuota(Long.valueOf(remain).intValue());
                }
            }
            activityVOList.add(activityVO);

        }
        pageDTO.setList(activityVOList);
        return pageDTO;
    }

    @Override
    public ActivityVO queryActivityById(Long id, Long userId) {
        Activity activity = lambdaQuery().eq(Activity::getId, id).one();
        if (activity == null) {
            throw new BizException(400,"活动不存在");
        }
        ActivityVO activityVO = BeanUtil.copyProperties(activity, ActivityVO.class);
        if (activity.getStatus().equals(1)) {
            String remain = stringRedisTemplate.opsForValue().get(RedisKeys.signupStock(activity.getId()));
            if (remain == null) {
                activityVO.setRemainingQuota(activity.getQuota());
            }else{
                activityVO.setRemainingQuota(Long.valueOf(remain).intValue());
            }
        }
        if (userId!=null){
            Signup signup = signupMapper.selectOne(Wrappers.<Signup>lambdaQuery()
                    .eq(Signup::getUserId, userId)
                    .in(Signup::getStatus, 1, 2)
                    .eq(Signup::getActivityId, activity.getId()));
            if (signup!=null){
                activityVO.setMySignupStatus(signup.getStatus());
            }
        }
        return activityVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelActivity(Long id, Long publisherId) {
        Activity activity = lambdaQuery().eq(Activity::getId, id).eq(Activity::getPublisherId, publisherId).one();
        if (activity==null){
            throw new BizException(400,"活动不存在或者权限不足");
        }
        // 仅报名中可取消，其余状态静默返回
        if (!activity.getStatus().equals(1)) {
            return;
        }
        activity.setStatus(4);
        updateById(activity);
        signupMapper.update(null, Wrappers.<Signup>lambdaUpdate()
                .eq(Signup::getActivityId, id)
                .in(Signup::getStatus, 1, 2)
                .set(Signup::getStatus, 3));
        stringRedisTemplate.delete(List.of(
                RedisKeys.signupStock(id),
                RedisKeys.signupUsers(id),
                RedisKeys.signupWaitlist(id)));
        deleteDoc(id);
    }

    @Override
    public void saveDoc(Activity activity) {
        // 索引是旁路：ES 异常只记日志，不拖垮发布主流程
        try {
            activityRepository.save(BeanUtil.copyProperties(activity, ActivityDoc.class));
        } catch (Exception e) {
            log.warn("活动索引写入失败，不影响主流程 activityId={}", activity.getId(), e);
        }
    }

    @Override
    public void deleteDoc(Long id) {
        try {
            activityRepository.deleteById(id);
        } catch (Exception e) {
            log.warn("活动索引删除失败，不影响主流程 activityId={}", id, e);
        }
    }
}





