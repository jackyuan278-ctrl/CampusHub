 local userId =ARGV[1]
 local quota = ARGV[2]

 local stockey = KEYS[1]
 local userSet = KEYS[2]
 local stock = redis.call('get', stockey)
 if (not stock) then
     redis.call('set', stockey, quota)
     stock = quota
 end

 if (tonumber(stock) <= 0) then
     return 1
 end

 if(redis.call('sismember',userSet,userId)==1) then
     return 2
 end

 redis.call('incrby',stockey,-1)
 redis.call('sadd',userSet,userId)

 return 0