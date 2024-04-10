package com.yu.gateway.core.filter.flow;


import com.yu.gateway.common.config.Rule;

/**
 * @author yu
 * @description 网关流控规则接口
 * @date 2024-04-10
 */
public interface GatewayFlowControlRule {

	/**
	 * 执行流控规则过滤器
	 *
	 * @param flowControlConfig 注册中心限流配置；
	 * @param serviceId         服务ID
	 */
	void doFlowControlFilter(Rule.FlowControlConfig flowControlConfig, String serviceId);
}
