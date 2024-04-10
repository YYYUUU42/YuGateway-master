package com.yu.gateway.core.netty.processor;


import com.yu.gateway.core.context.HttpRequestWrapper;

/**
 * @author yu
 * @description Netty请求处理器
 * @date 2024-04-06
 */
public interface NettyProcessor {

    /**
     * 处理请求
     */
    void process(HttpRequestWrapper wrapper);

    /**
     * 启动
     */
    void  start();

    /**
     * 关闭
     */
    void shutDown();
}
