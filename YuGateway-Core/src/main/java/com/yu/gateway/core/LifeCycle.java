package com.yu.gateway.core;

/**
 * @author yu
 * @description 组件生命周期接口
 * @date 2024-04-06
 */
public interface LifeCycle {

    /**
     * 初始化
     */
    void init();

    /**
     * 启动
     */
    void start();

    /**
     * 关闭
     */
    void shutdown();
}
