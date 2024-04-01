package com.yu.gateway.core.filter;

import com.yu.gateway.core.context.GatewayContext;

/**
 * @author yu
 * 过滤器工厂具体实现类
 * 1、根据SPI 动态加载驱动实现的过滤器类对象，并存储到本地内存；
 * 2、根据注册中心配置的规则策略，加载实时可用的过滤器，组装为网关过滤器链。
 */
public class GatewayFilterChainFactory implements FilterChainFactory{
	/**
	 * 构建过滤器链条
	 *
	 * @param ctx
	 * @return
	 * @throws Exception
	 */
	@Override
	public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception {
		return null;
	}

	/**
	 * 通过过滤器ID获取过滤器
	 *
	 * @param filterId
	 * @return
	 * @throws Exception
	 */
	@Override
	public <T> T getFilterInfo(String filterId) throws Exception {
		return null;
	}
}
