-- 秒杀抢购：库存扣减 + 防重复（原子执行，防超卖）
-- KEYS[1] = 秒杀库存 key: seckill:stock:{seckillGoodId}
-- KEYS[2] = 用户已抢 key: seckill:user:{memberId}:{seckillGoodId}
-- ARGV[1] = 本次抢购数量（默认 1）
-- ARGV[2] = 每人限购数（limit_per_user）
-- 返回值：1-成功  2-已抢过  3-库存不足  0-异常

local stockKey = KEYS[1]
local userKey = KEYS[2]
local qty = tonumber(ARGV[1]) or 1
local limit = tonumber(ARGV[2]) or 1

-- 已抢过（存在 userKey 则已被记录过）
local userVal = redis.call('EXISTS', userKey)
if userVal == 1 then
    return 2
end

local stock = tonumber(redis.call('GET', stockKey) or '0')
if stock < qty then
    return 3
end

-- 原子扣减库存
redis.call('DECRBY', stockKey, qty)
-- 记录用户已抢（TLL 设置跟随活动，防止过期；此处简单设置 24h）
redis.call('SET', userKey, 1, 'EX', 86400)

return 1
