package com.yu.gateway.core.disruptor;

/**
 * @author yu
 * @description 事件监听处理器
 * @date 2024-04-15
 */
public interface EventListener<E> {
    /**
     * 事件处理方法
     */
    void onEvent(E event);

    /**
     * 异常处理方法
     */
    void onException(Throwable ex, long sequence, E event);
}
