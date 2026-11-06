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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupServiceImpl extends ServiceImpl<SignupMapper, Signup> implements ISignupService {

    /** 挡流结果：可以继续走 DB */
    private static final long GATE_PASS = 0L;
    /** 挡流结果：名额已满 */
    private static final long GATE_FULL = 1L;
    /** 挡流结果：重复报名 */
    private static final long GATE_DUP = 2L;
    /** 挡流结果：缓存未就绪或 Redis 不可用，由调用方拒绝（不偷偷放行） */
    private static final long GATE_UNAVAILABLE = 3L;
    /** 挡流结果：同一用户的上一笔还在处理中（占用未过期），拒绝重复提交 */
    private static final long GATE_PROCESSING = 4L;
    /** 临时占用有效期：远大于一次 DB 事务（约 10 ms），又短到不会长期挡住重试 */
    private static final long HOLD_TTL_MS = 30_000L;
    /** Redis 故障后的冷却时间，避免每个请求都等一次连接超时 */
    private static final long REDIS_COOLDOWN_MS = 5_000L;

    private volatile long redisDownUntil = 0L;

    private final ActivityMapper activityMapper;
    private final StringRedisTemplate stringRedisTemplate;
    // 两个 Bean 按字段名注入（signupScript/cancelSignupScript），依赖 -parameters 编译参数
    private final DefaultRedisScript<Long> signupScript;
    private final DefaultRedisScript<String> cancelSignupScript;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signup(SignupFormDTO form, Long userId) {
        Long activityId = form.getActivityId();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(400, "活动不存在");
        }
        validateSignupWindow(activity);

        // ① 预扣：Lua 里一次性完成「校验名额 + 扣名额 + 记临时占用」。
        //    只有返回 0 的请求才允许进 DB，其余（满/重复/处理中/缓存未就绪）在这里就被挡掉。
        long gate = tryGate(activityId, userId);
        if (gate == GATE_DUP) {
            throw new BizException(400, "请勿重复报名");
        }
        if (gate == GATE_FULL) {
            throw new BizException(400, "名额已满，可加入候补");
        }
        if (gate == GATE_PROCESSING) {
            throw new BizException(400, "报名处理中，请稍候重试");
        }
        if (gate == GATE_UNAVAILABLE) {
            throw new BizException(500, "服务器繁忙，请稍后重试");
        }

        // ② 落库：Redis 已经把名额扣掉了，这一段落库失败必须把名额还回去，否则就是「少卖」
        try {
            // ②-1 仍以 DB 为唯一裁决：Redis 与 DB 漂移时（比如缓存被人改过）以 DB 为准
            if (activityMapper.takeQuota(activityId) == 0) {
                throw new BizException(400, "名额已满，可加入候补");
            }

            // ②-2 明细：只允许把「已取消(3)」的记录恢复成已报名；已经是 1 或 2 的行不能动，
            //      否则「匹配行数=1」会被当成更新成功，重复提交就会二次扣名额。
            boolean updated = lambdaUpdate()
                    .eq(Signup::getActivityId, activityId)
                    .eq(Signup::getUserId, userId)
                    .eq(Signup::getStatus, 3)
                    .set(Signup::getStatus, 1)
                    .update();
            if (!updated) {
                save(new Signup().setActivityId(activityId).setUserId(userId).setStatus(1));
            }
        } catch (DuplicateKeyException e) {
            // 库里已经有这条报名记录：名额退回去，但人要在名单里（他确实是已报名状态）
            releaseHold(activityId, userId, true);
            throw new BizException(400, "请勿重复报名");
        } catch (RuntimeException e) {
            releaseHold(activityId, userId, false);
            throw e;
        }

        // ③ 提交成功后再清占用：占用只是幂等标记，清掉之后同一用户能再次提交（比如取消后重新报名）
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearHold(activityId, userId);
            }
        });
    }

    /**
     * 落库失败：把 Lua 预扣的名额回退。宁可少卖，也不能出现「Redis 扣了、库里没有」的少名额。
     *
     * @param keepInUsers 为 true 时保留 users 集合成员——用于「库里已存在报名记录」的场景，
     *                    那种情况下这个人本来就该在名单里，不能把他踢出去
     */
    private void releaseHold(Long activityId, Long userId, boolean keepInUsers) {
        try {
            stringRedisTemplate.opsForValue().increment(RedisKeys.signupStock(activityId));
            if (!keepInUsers) {
                stringRedisTemplate.opsForSet().remove(RedisKeys.signupUsers(activityId), userId.toString());
            }
            stringRedisTemplate.opsForZSet().remove(RedisKeys.signupHold(activityId), userId.toString());
        } catch (Exception e) {
            // 补偿也失败只能靠对账兜底：DB 是真相，下次重建会把 Redis 拉回来
            log.error("报名失败后回退 Redis 预扣失败，activityId={}, userId={}", activityId, userId, e);
        }
    }

    /** 提交成功：只清占用标记，名额已经扣掉了，不能再动 stock */
    private void clearHold(Long activityId, Long userId) {
        try {
            stringRedisTemplate.opsForZSet().remove(RedisKeys.signupHold(activityId), userId.toString());
        } catch (Exception e) {
            log.warn("清除报名占用失败，activityId={}, userId={}", activityId, userId, e);
        }
    }

    /**
     * 问一次"能不能报"。
     * 缓存缺失时用 DB 的真实余量补一次再问；补不上或 Redis 不可用，一律返回 GATE_UNAVAILABLE，
     * 由调用方决定拒绝，绝不在这里偷偷放行。
     */
    private long tryGate(Long activityId, Long userId) {
        if (System.currentTimeMillis() < redisDownUntil) {
            return GATE_UNAVAILABLE;
        }
        List<String> keys = List.of(
                RedisKeys.signupStock(activityId),
                RedisKeys.signupUsers(activityId),
                RedisKeys.signupHold(activityId));
        try {
            long result = executeGate(keys, userId);
            if (result == GATE_UNAVAILABLE) {
                syncStockFromDb(activityId);
                result = executeGate(keys, userId);
            }
            return result;
        } catch (Exception e) {
            redisDownUntil = System.currentTimeMillis() + REDIS_COOLDOWN_MS;
            log.error("Redis 挡流不可用，activityId={}", activityId, e);
            return GATE_UNAVAILABLE;
        }
    }

    private long executeGate(List<String> keys, Long userId) {
        // 预扣需要两个额外参数：当前时间和占用有效期，Lua 里用它们判断占用是否过期
        Long result = stringRedisTemplate.execute(signupScript, keys,
                userId.toString(),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(HOLD_TTL_MS));
        return result == null ? GATE_UNAVAILABLE : result;
    }

    /**
     * 用 DB 的真实余量重建 Redis 缓存。只负责填数，没有任何决策权。
     */
    private void syncStockFromDb(Long activityId) {
        Integer remain = activityMapper.selectRemainQuota(activityId);
        if (remain == null) {
            log.warn("活动 {} 不存在，无法重建库存缓存", activityId);
            return;
        }
        stringRedisTemplate.opsForValue()
                .set(RedisKeys.signupStock(activityId), String.valueOf(remain));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        // 入队失败必须抛出：让上面的状态写入随事务回滚。
        // 若只是吞掉异常，库里会留下 status=2 却不在队列里的记录，用户重试会被"请勿重复加入"永久挡住。
        try {
            stringRedisTemplate.opsForList()
                    .rightPush(RedisKeys.signupWaitlist(activityId), userId.toString());
        } catch (Exception e) {
            log.error("候补入队失败，回滚状态写入，activityId={}, userId={}", activityId, userId, e);
            throw new BizException(500, "服务器繁忙，请稍后重试");
        }
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
    @Transactional(rollbackFor = Exception.class)
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
            // 候补取消：不占名额，只从队列移除
            stringRedisTemplate.opsForList().remove(RedisKeys.signupWaitlist(activityId), 1, userId.toString());
            try {
                boolean updated = lambdaUpdate().eq(Signup::getId, one.getId())
                        .set(Signup::getStatus, 3)
                        .update();
                if (!updated) {
                    throw new BizException(500, "取消候补失败");
                }
            } catch (Exception e) {
                stringRedisTemplate.opsForList()
                        .rightPush(RedisKeys.signupWaitlist(activityId), userId.toString());
                throw e;
            }
            return;
        }
        if (one.getStatus() != 1) {
            throw new BizException(400,"未报名");
        }

        // Redis 只负责名单和队列，名额增减由 DB 裁决
        List<String> keys = List.of(
                RedisKeys.signupUsers(activityId),
                RedisKeys.signupWaitlist(activityId));
        String result = stringRedisTemplate.execute(cancelSignupScript, keys, userId.toString());
        if ("-1".equals(result)) {
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
            // 归还名额（条件更新，上限不超过总名额）
            activityMapper.giveBackQuota(activityId);

            if (!"0".equals(result)) {
                // 补位：只转正"仍在候补"的人，避免把已取消的人复活
                boolean promoted = lambdaUpdate().eq(Signup::getUserId, Long.valueOf(result))
                        .eq(Signup::getActivityId, activityId)
                        .eq(Signup::getStatus, 2)
                        .set(Signup::getStatus, 1)
                        .update();
                if (promoted) {
                    // 名额转手：刚归还的这一个立即被占用
                    activityMapper.takeQuota(activityId);
                } else {
                    // 候补者状态已变更，把 Redis 名单里的登记撤掉
                    stringRedisTemplate.opsForSet()
                            .remove(RedisKeys.signupUsers(activityId), result);
                    log.warn("候补者 {} 已不在候补状态，跳过补位，activityId={}", result, activityId);
                }
            }
        } catch (Exception e) {
            // 回滚 Redis 中"已取消 + 已补位"的痕迹
            try {
                stringRedisTemplate.opsForSet()
                        .add(RedisKeys.signupUsers(activityId), userId.toString());
                if (!"0".equals(result)) {
                    stringRedisTemplate.opsForSet()
                            .remove(RedisKeys.signupUsers(activityId), result);
                    stringRedisTemplate.opsForList()
                            .rightPush(RedisKeys.signupWaitlist(activityId), result);
                }
            } catch (Exception ignore) {
                log.error("取消报名失败后回滚 Redis 也失败，activityId={}, userId={}", activityId, userId);
            }
            throw e;
        }

        // 余量变化后直接让缓存失效，下次报名从 DB 重建，杜绝漂移
        try {
            stringRedisTemplate.delete(RedisKeys.signupStock(activityId));
        } catch (Exception e) {
            log.warn("删除库存缓存失败，activityId={}", activityId, e);
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
                // 排位只是锦上添花：Redis 不可用或队列里找不到人时留空，不能让整个"我的报名"接口失败
                try {
                    Long range = stringRedisTemplate.opsForList()
                            .indexOf(RedisKeys.signupWaitlist(signup.getActivityId()), userId.toString());
                    // indexOf 找不到人返回 -1（不是 null），直接 +1 会变成"第 0 位"
                    if (range != null && range >= 0) {
                        signupVO.setWaitPosition(range.intValue() + 1);
                    }
                } catch (Exception e) {
                    log.warn("查询候补排位失败，activityId={}, userId={}", signup.getActivityId(), userId, e);
                }
            }
            signupVOList.add(signupVO);
        }

        return signupVOList;
    }
}





