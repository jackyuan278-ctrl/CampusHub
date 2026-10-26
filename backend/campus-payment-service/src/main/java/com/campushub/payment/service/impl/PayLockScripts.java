package com.campushub.payment.service.impl;

import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

/** Redis 锁释放脚本：只有锁值仍属于当前请求时才删除。 */
final class PayLockScripts {
    static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private PayLockScripts() {
    }
}




