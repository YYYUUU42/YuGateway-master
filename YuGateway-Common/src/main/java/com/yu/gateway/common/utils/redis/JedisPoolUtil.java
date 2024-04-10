package com.yu.gateway.common.utils.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import redis.clients.jedis.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  Jedis 连接池工具类
 */
@Slf4j
public class JedisPoolUtil {
    static class JedisPoolWrapper {
        public JedisPool jedisPool;
        private String password;

        private JedisPoolWrapper(JedisPool jedisPool, String password) {
            this.jedisPool = jedisPool;
            this.password = password;
        }

        public JedisPool getJedisPool() {
            return jedisPool;
        }

        public void setJedisPool(JedisPool jedisPool) {
            this.jedisPool = jedisPool;
        }

        public Jedis getResource() {
            Jedis jedis = jedisPool.getResource();
            //jedis.auth(password);
            return jedis;
        }
    }

    public static JedisPoolWrapper jedisPoolWrapper;
    private String host;
    private int port;
    private int maxTotal;
    private int minIdle;
    private int maxIdle;
    private boolean blockWhenExhausted;
    private int maxWaitTimeMillis;
    private boolean testOnBorrow;
    private boolean testOnReturn;
    private String password;

    public static Lock lock = new ReentrantLock();

    /**
     * 初始化连接池配置
     */
    private void initialConfig() {
        try {
            Properties prop = new Properties();
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("gateway.properties");
            if (ObjectUtils.isEmpty(inputStream)) {
                throw new RuntimeException("InputStream load null, filePath: gateway.properties");
            }
            // 加载解析配置文件（当前线程会有问题，用类加载器加载文件）
            prop.load(inputStream);
            host = prop.getProperty("redis.host");
            port = Integer.parseInt(prop.getProperty("redis.port"));
            maxTotal = Integer.parseInt(prop.getProperty("redis.maxTotal"));
            maxIdle = Integer.parseInt(prop.getProperty("redis.maxIdle"));
            minIdle = Integer.parseInt(prop.getProperty("redis.minIdle"));
            password = prop.getProperty("redis.password");
        } catch (IOException e) {
            log.error("parse gateway.properties failed", e);
        }
    }

    /**
     * 初始化连接池
     */
    private void initialPool() {
        if (lock.tryLock()) {
            try {
                initialConfig();
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxIdle(maxIdle);
                config.setMaxTotal(maxTotal);
                config.setMaxWaitMillis(maxWaitTimeMillis);
                config.setTestOnBorrow(testOnBorrow);
                jedisPoolWrapper = new JedisPoolWrapper(new JedisPool(config, host, port), password);
            } catch (Exception e) {
                log.warn("init jedisPool failed : {}", e.getMessage());
            } finally {
                lock.unlock();
            }
        } else {
            log.debug("other thread is init pool");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Jedis getJedis() {
        if (jedisPoolWrapper == null) {
            initialPool();
        }
        try {
            return jedisPoolWrapper.getResource();
        } catch (Exception e) {
            log.debug("getJedis() throws : {}", e.getMessage());
        }
        return null;
    }

    public Pipeline getPipeline() {
        BinaryJedis binaryJedis = new BinaryJedis(host, port);
        return binaryJedis.pipelined();
    }
}
