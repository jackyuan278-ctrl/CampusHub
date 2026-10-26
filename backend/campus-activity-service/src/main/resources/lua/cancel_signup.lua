local userId =ARGV[1]
local stockey = KEYS[1]
local userSet = KEYS[2]
local waitlist = KEYS[3]

if(redis.call('sismember',userSet,userId)==0) then
    return "-1"
end

redis.call('srem',userSet,userId)

local newUserId  = redis.call('lpop',waitlist);
if not newUserId then
    redis.call('incrby',stockey,1)
    return "0"
end
redis.call('sadd',userSet,newUserId)
return newUserId
