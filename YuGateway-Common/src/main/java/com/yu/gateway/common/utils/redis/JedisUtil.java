package com.yu.gateway.common.utils.redis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class JedisUtil {
    public static ReentrantLock lock = new ReentrantLock();

    private final String DIST_LOCK_SUCCESS = "OK";

    private final Long DIST_LOCK_RELEASE_SUCCESS = 1L;

    private final String SET_IF_NOT_EXISTS = "NX";

    private final String SET_WITH_EXPIRE_TIME = "PX";

    private JedisPoolUtil jedisPool = new JedisPoolUtil();

    public boolean isExist(String key) {
        try {
            Jedis jedis = jedisPool.getJedis();
            return jedis.exists(key);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean setString(String key, String value) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.set(key, value);
            return true;
        } catch (Exception e) {
            log.debug("setString method key {} throws:{}", key, e.getMessage());
        } finally {
            close(jedis);
        }
        return false;
    }

    public boolean setStringEx(String key, int seconds, String value) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.setex(key, seconds, value);
            return true;
        } catch (Exception e) {
            log.debug("setStringEx() key {} throws:{}", key, e.getMessage());
            return false;
        } finally {
            jedis.close();
        }
    }

    public String getString(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.get(key);
        } catch (Exception e) {
            log.debug("getString key {} throws:{}", key, e.getMessage());
            return null;
        } finally {
            jedis.close();
        }
    }

    public boolean delString(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.del(key);
            return true;
        } catch (Exception e) {
            log.debug("delString key {} throws:{}", key, e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }

    public boolean delHash(String key, String mKey) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.hdel(key, mKey);
            return true;
        } catch (Exception e) {
            log.debug("delHash key {} throws:{}", key, e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }

    public boolean setHash(String key, String mKey, String value) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.hset(key, mKey, value);
            return true;
        } catch (Exception e) {
            log.debug("setHash key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }

    /**
     * 设置key的过期时间
     */
    public boolean setExpire(String key, int seconds) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.expire(key, seconds);
            return true;
        } catch (Exception e) {
            log.error("setExpire for key {} throws:{}", key, e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }

    public String getHash(String key, String mKey) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hget(key, mKey);
        } catch (Exception e) {
            log.debug("setHash key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }

    public boolean setHashMulti(String key, Map<String, String> map) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.hmset(key, map);
            return true;
        } catch (Exception e) {
            log.debug("setMHash key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }

    public List<String> getHashMulti(String key, String[] members) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hmget(key, members);
        } catch (Exception e) {
            log.debug("getHashMulti key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }

    public List<String> getHashValsAll(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hvals(key);
        } catch (Exception e) {
            log.debug("getHashValsAll key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }

    public Set<String> getHashKeysAll(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hkeys(key);
        } catch (Exception e) {
            log.debug("getHashValsAll key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }

    public boolean addScoreSet(String key, String mKey, int score) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.zadd(key, score, mKey);
            return true;
        } catch (Exception e) {
            log.debug("addScoreSet key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }

    public boolean delScoreSet(String key, String mKey) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.zrem(key, mKey);
            return true;
        } catch (Exception e) {
            log.debug("delScoreSet key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }

    public boolean changeScoreSet(String key, String mKey, int score) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.zincrby(key, score, mKey);
            return true;
        } catch (Exception e) {
            log.debug("changeScoreSet key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }

    public Set<String> listScoreSetString(String key, int start, int end, boolean asc) {
        Jedis jedis = jedisPool.getJedis();
        try {
            if (asc) {
                return jedis.zrange(key, start, end);
            } else {
                return jedis.zrevrange(key, start, end);
            }
        } catch (Exception e) {
            log.debug("listScoreSetString() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }

    public Set<Tuple> listScoreSetTuple(String key, int start, int end, boolean asc) {
        Jedis jedis = jedisPool.getJedis();
        try {
            if (asc) {
                return jedis.zrangeWithScores(key, start, end);
            } else {
                return jedis.zrevrangeWithScores(key, start, end);
            }
        } catch (Exception e) {
            log.debug("listScoreSetString() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }

    public Double getScore(String key, String mKey) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.zscore(key, mKey);
        } catch (Exception e) {
            log.error("getScore() key{} mKey{} throws:{}", key, mKey, e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }

    public Boolean isExistScoreSet(String key, String mKey) {
        Jedis jedis = jedisPool.getJedis();
        try {
            if (jedis.zrank(key, mKey) == null) {
                return false;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("isExistScoreSet() key{} mKey{} throws:{}", key, mKey, e.getMessage());
        } finally {
            close(jedis);
        }
        return false;
    }

    /**
     *  获取分布式锁：value值(防止过期释放其它刚刚获取锁的线程)、过期时间
     * @param lockKey
     * @param requestId
     * @param expireTime
     * @return
     */
    public boolean getDistributeLock(String lockKey, String requestId, int expireTime) {
        Jedis jedis = jedisPool.getJedis();
        try {
            String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXISTS, SET_WITH_EXPIRE_TIME, expireTime);
            if (DIST_LOCK_SUCCESS.equals(result)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("getDistributeLock throws:{}", e.getMessage());
        } finally {
            close(jedis);
        }
        return false;
    }

    /**
     * 释放分布式锁：
     * 1.比较value值是否一致；
     * 2.释放分布式锁，删除对应的 key；
     * 3.原子操作；
     * @param lockKey
     * @param requestId
     * @return
     */
    public boolean releaseDistributeLock(String lockKey, String requestId) {
        Jedis jedis = jedisPool.getJedis();
        try {
            String releaseScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(releaseScript, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            return DIST_LOCK_RELEASE_SUCCESS.equals(result);
        } catch (Exception e) {
            log.debug("releaseDistributeLock throws:{}", e.getMessage());
        } finally {
            close(jedis);
        }
        return false;
    }

    public void close(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }

    /**
     * 限流脚本
     */
    public Object executeScript(String key, int limit, int expire) {
        Jedis jedis = jedisPool.getJedis();
        String lua = buildLuaScript();
        String scriptLoad = jedis.scriptLoad(lua);
        try {
            Object result = jedis.evalsha(scriptLoad, Arrays.asList(key), Arrays.asList(String.valueOf(expire), String.valueOf(limit)));
            System.out.println(result);
            return result;
        } catch (Exception e) {
            log.error("executeScript throws:", e);
        } finally {
            try {
                jedis.close();
            } catch (Exception e) {
                log.error("executeScript close jedis throws:", e);
            }
        }
        return null;
    }

    /**
     * 限流脚本
     * 1.key对应value每次增加1；
     * 2.如果 value==1，第一次设置值时设置过期时间；
     * 3.否则检查 value > limit，超过返回0，否则返回1
     */
    public static String buildLuaScript() {
        String lua = "local num = redis.call('incr', KEYS[1])\n" +
                "if tonumber(num) == 1 then\n" +
                "\tredis.call('expire', KEYS[1], ARGV[1])\n" +
                "\treturn 1\n" +
                "elseif tonumber(num) > tonumber(ARGV[2]) then\n" +
                "\treturn 0\n" +
                "else \n" +
                "\treturn 1\n" +
                "end\n";
        return lua;
    }

    /**
     * 递增操作
     */
    public Long increment(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.incr(key);
        } catch (Exception e) {
            log.error("increment key {} throws:{}", key, e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }

    /**
     * 执行 Lua 脚本
     * 1.key 对应 value 每次增加 1；
     * 2.如果 value == 1，第一次设置值时设置过期时间；
     * 3.否则检查 value > limit，超过返回 0，否则返回 1
     *
     * @param script 表示要执行的 Lua 脚本
     * @param keys   表示Lua脚本中使用的 key
     * @param args   表示Lua脚本中使用的参数
     */
    public Object executeLuaScript(String script, List<String> keys, String... args) {
        try (Jedis jedis = jedisPool.getJedis()) {
            return jedis.eval(script, keys, Arrays.asList(args));
        } catch (Exception e) {
            log.error("executeLuaScript throws:", e);
        }
        return null;
    }
}
