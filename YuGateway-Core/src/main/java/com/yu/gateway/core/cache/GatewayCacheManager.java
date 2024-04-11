package com.yu.gateway.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @author yu
 * @description 网关缓存管理类
 * @date 2024-04-11
 */
public class GatewayCacheManager {

    public GatewayCacheManager() {}

    /**
     * 全局缓存，双层缓存
     */
    private final ConcurrentHashMap<String, Cache<String, ?>> cacheMap = new ConcurrentHashMap<>();


    private static class SingletonInstance {
        private static final GatewayCacheManager INSTANCE = new GatewayCacheManager();
    }

    public static GatewayCacheManager getInstance() {
        return SingletonInstance.INSTANCE;
    }

    /**
     * 根据全局缓存 ID 创建一个 Caffeine 对象
     */
    public <V> Cache<String, V> create(String cacheId) {
        Cache<String, V> cache = Caffeine.newBuilder().build();
        cacheMap.put(cacheId, cache);
        return (Cache<String, V>) cacheMap.get(cacheId);
    }

    /**
     * 根据 CacheID 以及对象 Key 删除对应地 Caffeine 对象
     */
    public <V> void remove(String cacheId, String key) {
        Cache<String, V> cache = (Cache<String, V>) cacheMap.get(cacheId);
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    /**
     * 根据 CacheID 删除 Caffeine 对象
     */
    public <V> void remove(String cacheId) {
        Cache<String, V> cache = (Cache<String, V>) cacheMap.get(cacheId);
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    /**
     * 清除所有缓存
     */
    public <V> void removeAll() {
        cacheMap.values().forEach(cache -> cache.invalidateAll());
    }

}
