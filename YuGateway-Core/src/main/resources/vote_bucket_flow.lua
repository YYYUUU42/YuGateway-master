-- 获取限流资源令牌数key、时间戳key --
local tokens_key = KEYS[1]
local timestamp_key = KEYS[2]

-- 获取填充速率、令牌桶容量、当前时间戳、当前请求需要的令牌数 --
local rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- 计算出失效时间，预估设置为令牌桶填满时间两倍 --
local fill_time = capacity/rate
local ttl = math.floor(fill_time*2)

-- 获取最近一次请求的剩余令牌数，为空说明刚刚初始化 --
local last_tokens = tonumber(redis.call("get", tokens_key))
if last_tokens == nil then
    last_tokens = capacity
end

-- 获取上次消耗令牌的时间戳 --
local last_refreshed = tonumber(redis.call("get", timestamp_key))
if last_refreshed == nil then
    last_refreshed = 0
end

-- 计算两次时间间隔，并计算应放入的令牌数 --
local delta = math.max(0, now-last_refreshed)
local filled_tokens = math.min(capacity, last_tokens+(delta*rate))
local allowed = filled_tokens >= requested
local new_tokens = filled_tokens
local allowed_num = 0
if allowed then
    new_tokens = filled_tokens - requested
    allowed_num = 1
end

if ttl > 0 then
    redis.call("setex", tokens_key, ttl, new_tokens)
    redis.call("setex", timestamp_key, ttl, now)
end

return {allowed_num, new_tokens}