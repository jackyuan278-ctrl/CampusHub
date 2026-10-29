-- 取消报名：从名单移除，并从候补队列取一位补位
-- 名额增减交给 MySQL 的 remain_quota，这里不再碰库存
local userId   = ARGV[1]
local userSet  = KEYS[1]
local waitlist = KEYS[2]

if (redis.call('sismember', userSet, userId) == 0) then
    return "-1"
end

redis.call('srem', userSet, userId)

local newUserId = redis.call('lpop', waitlist)
if not newUserId then
    return "0"
end

redis.call('sadd', userSet, newUserId)
return newUserId
