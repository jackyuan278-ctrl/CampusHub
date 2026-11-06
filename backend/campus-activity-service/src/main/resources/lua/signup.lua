-- 报名预扣脚本：校验 + 扣名额 + 记临时占用，在一个原子操作里完成。
-- 只有抢到资格（返回 0）的请求才允许进 MySQL，其余在 Redis 这一层就被挡掉。
--
-- KEYS[1] = signup:stock:{activityId}   剩余名额
-- KEYS[2] = signup:users:{activityId}   已报名用户集合
-- KEYS[3] = signup:hold:{activityId}    临时占用（ZSet，score = 到期毫秒时间戳）
-- ARGV[1] = userId
-- ARGV[2] = 当前毫秒时间戳
-- ARGV[3] = 占用有效期（毫秒）
--
-- 返回值：0=抢到  1=名额已满  2=重复报名  3=缓存未就绪  4=处理中（重复提交）
local userId = ARGV[1]
local now    = tonumber(ARGV[2])
local ttl    = tonumber(ARGV[3])

local stock = redis.call('get', KEYS[1])
if not stock then
    return 3
end

local n = tonumber(stock)
if not n then
    return 3
end

if redis.call('sismember', KEYS[2], userId) == 1 then
    return 2
end

-- 上一次提交还在处理中（占用未过期）：直接拒绝，避免同一个用户扣两次名额
local holdScore = redis.call('zscore', KEYS[3], userId)
if holdScore and tonumber(holdScore) > now then
    return 4
end

if n <= 0 then
    return 1
end

-- 真正扣减：只有走到这里的请求才能进 DB
redis.call('decr', KEYS[1])
redis.call('sadd', KEYS[2], userId)
redis.call('zadd', KEYS[3], now + ttl, userId)
return 0
