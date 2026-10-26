package com.campushub.common.constant;

/**
 * Redis key 统一定义。
 * 口诀：cache: 只做 GET/SET/DEL，lock: 只做 setIfAbsent/DEL，账本 key 只做 Z* 命令。
 * 需要拼 id 的 key 一律走这里的静态方法，不要在业务里手搓冒号。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 热度账本（ZSet：member=问题ID，score=浏览*1 + 点赞*3 + 回复*5） */
    public static final String HOT_QUESTIONS = "hot:questions";

    /** 热榜快照（String，TTL 由 queryHot 随机 5-11 分钟） */
    public static final String HOT_QUESTIONS_CACHE = "cache:hot:questions";

    /** 热榜重建互斥锁（30 秒兜底过期） */
    public static final String HOT_QUESTIONS_LOCK = "lock:cache:hot:questions";

    /** 问题详情快照（TTL 30 分钟，改 DB 后必须删） */
    public static String questionCache(Long questionId) {
        return "cache:question:" + questionId;
    }

    /** 该用户 24 小时内是否已浏览过该问题（浏览量去重） */
    public static String questionViewed(Long questionId, Long userId) {
        return "question:viewed:" + questionId + ":" + userId;
    }

    /** 某用户对某类目标的已点赞集合，targetType 见 {@link LikeTarget} */
    public static String likeSet(Long userId, int targetType) {
        return "like:user:" + userId + ":" + targetType;
    }

    /** 活动剩余名额（String：发布 SET、报名 DECR、取消 DEL；为 null 时按 quota 初始化） */
    public static String signupStock(Long activityId) {
        return "signup:stock:" + activityId;
    }

    /** 活动已报名用户集合（Set：Lua 内 SISMEMBER 防重复、SADD 补位；取消活动 DEL） */
    public static String signupUsers(Long activityId) {
        return "signup:users:" + activityId;
    }

    /** 活动候补队列（List：RPUSH 入队、LREM 撤报、LRANGE 排位次；取消活动 DEL） */
    public static String signupWaitlist(Long activityId) {
        return "signup:waitlist:" + activityId;
    }

    /** 用户当月签到 Bitmap（offset = 当月日 - 1），yearMonth 形如 2026-09 */
    public static String checkinSign(Long userId, String yearMonth) {
        return "checkin:sign:" + userId + ":" + yearMonth;
    }
}





