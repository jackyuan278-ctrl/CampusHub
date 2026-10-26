package com.campushub.common.constant;

/**
 * 点赞目标类型：tb_like.target_type 与 like:user:{userId}:{targetType} 的后缀共用同一套值。
 */
public final class LikeTarget {

    private LikeTarget() {
    }

    /** 问题 */
    public static final int QUESTION = 1;

    /** 回答 */
    public static final int ANSWER = 2;
}





