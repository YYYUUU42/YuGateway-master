package com.yu.gateway.core.filter;


import com.yu.gateway.core.context.GatewayContext;

/**
 * @author yu
 * @description 过滤器链工厂 用于生成过滤器链
 * @date 2024-04-01
 */
public interface FilterChainFactory {

    /**
     * 构建过滤器链条
     * @param ctx
     * @return
     * @throws Exception
     */
    GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception;

    /**
     * 通过过滤器ID获取过滤器
     * @param filterId
     * @return
     * @param <T>
     * @throws Exception
     */
    <T> T getFilterInfo(String filterId) throws Exception;

}
