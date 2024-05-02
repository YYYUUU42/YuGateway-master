package com.yu.gateway.core.filter.flow.algorithm;

/**
 * @author yu
 * @description 策略模式选择器
 * @date 2024-04-11
 */
public interface AbstractExecuteStrategy<REQUEST, RESPONSE> {
    /**
     * 执行策略的标识
     */
    default String mark() {return null;}

    /**
     * 执行策略范围匹配标识
     */
    default String patternMatchMark() {
        return null;
    }

    /**
     * 执行策略
     */
    default void execute(REQUEST requestParam) {}

    /**
     * 执行策略带返回值
     */
    default RESPONSE executeResp(REQUEST requestParam,String key) {return null;}
}
