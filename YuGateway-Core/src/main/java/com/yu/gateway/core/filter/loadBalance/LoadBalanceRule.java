package com.yu.gateway.core.filter.loadBalance;


import com.yu.gateway.common.config.ServiceInstance;
import com.yu.gateway.core.context.GatewayContext;

/**
 * @author yu
 * @description 负载均衡策略接口
 * @date 2024-04-02
 */
public interface LoadBalanceRule {
	/**
	 * 通过上下文参数获取服务实例
	 *
	 * @param ctx
	 * @param gray
	 * @return
	 */
	ServiceInstance choose(GatewayContext ctx, boolean gray);

	/**
	 * 通过服务ID拿到对应的服务实例
	 *
	 * @param serviceId
	 * @param gray
	 * @return
	 */
	ServiceInstance chooseByServiceId(String serviceId, boolean gray);
}