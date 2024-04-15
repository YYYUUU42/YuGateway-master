package com.yu.gateway.core.disruptor;

/**
 * @author yu
 * @description 并行队列接口，定义了添加事件、启动、关闭等方法
 * @date 2024-04-15
 */
public interface ParallelQueue<E> {
    /**
     * 添加一个事件到队列
     */
    void add(E event);

    /**
     * 添加多个事件到队列
     */
    void add(E... event);

    /**
     * 尝试添加一个事件到队列
     */
    boolean tryAdd(E event);

    /**
     * 尝试添加多个事件到队列
     */
    boolean tryAdd(E... event);

    /**
     * 启动队列
     */
    void start();

    /**
     * 关闭队列
     */
    void shutdown();

    /**
     * 判断队列是否已关闭
     */
    boolean isShutDown();
}
