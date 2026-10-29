-- 报名挡流脚本：只读不写，不做任何账本初始化
-- 返回 0=继续  1=名额已满  2=重复报名  3=缓存未就绪（由 Java 从 DB 重建后再问一次）
local userId   = ARGV[1]
local stockKey = KEYS[1]
local userSet  = KEYS[2]

local stock = redis.call('get', stockKey)
if not stock then
    return 3
end

local n = tonumber(stock)
if not n then
    return 3
end

if n <= 0 then
    return 1
end

if redis.call('sismember', userSet, userId) == 1 then
    return 2
end

return 0
