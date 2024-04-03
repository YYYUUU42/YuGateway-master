package com.yu.gateway.core;

import lombok.Data;

/**
 * @author yu
 * @description 核心配置类
 * @date 2024-04-03
 */
@Data
public class Config {
    private int port = 8888;

    /**
     * 服务名
     */
    private String applicationName = "api-gateway";

    /**
     * 注册中心地址
     */
    private String registryAddress = "192.168.220.1:8848";

    /**
     * 多环境配置
     */
    private String env = "dev";

    // Netty相关配置
    private int eventLoopGroupBossNum = 1;
    private int eventLoopGroupWorkerNum = Runtime.getRuntime().availableProcessors();
    private int maxContentLength = 64 * 1024 * 1024;

    /**
     * 异步模式（单异步/双异步）
     */
    private boolean whenComplete = true;

    // Http Async
    private int httpConnectTimeout = 30 * 1000;
    private int httpRequestTimeout = 30 * 1000;
    private int httpMaxRetryTimes = 2;
    private int httpMaxConnections = 10000;
    private int httpMaxConnectionsPerHost = 8000;

    /**
     * 客户端空闲连接超时时间
     */
    private int httpPooledConnectionIdleTimeout = 60 * 1000;

    /**
     * disruptor队列前缀
     */
    private String bufferType = "parallel";

    /**
     * disruptor缓冲区大小
     */
    private int bufferSize = 1024 * 16;

    /**
     * 工作线程数
     */
    private int processThread = Runtime.getRuntime().availableProcessors();

    /**
     * 等待策略
     */
    private String waitStrategy = "blocking";
}