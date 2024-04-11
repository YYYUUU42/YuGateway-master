package com.yu.gateway.core.filter.flow;

import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.core.context.GatewayContext;
import com.yu.gateway.core.filter.Filter;
import com.yu.gateway.core.filter.FilterAspect;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Set;

/**
 * @author yu
 * 限流过滤器：
 * 1.支持动态选择限流算法，实现不同服务不同限流方式；
 * 2.支持动态调整限流配置，从配置中心更新限流相关参数；
 * 3.支持按路径/服务实现限流；
 * 4.支持分布式/单机限流；
 * @date 2024-04-11
 */
@Slf4j
@FilterAspect(id = FilterConst.FLOW_CTL_FILTER_ID, name = FilterConst.FLOW_CTL_FILTER_NAME, order = FilterConst.FLOW_CTL_FILTER_ORDER)
public class FlowControlFilter implements Filter {
	@Override
	public void doFilter(GatewayContext ctx) throws Exception {
		Rule rule = ctx.getRules();
		if (rule != null) {
			// 获取配置中心限流规则
			GatewayFlowControlRule flowControlRule = null;
			Rule.FlowControlConfig flowControlConfig = null;
			Set<Rule.FlowControlConfig> flowControlConfigs = rule.getFlowControlConfigs();

			// 根据配置中心获取请求的限流规则
			for (Rule.FlowControlConfig controlConfig : flowControlConfigs) {
				flowControlConfig = controlConfig;
				if (flowControlConfig == null) {
					continue;
				}
				String path = ctx.getRequest().getPath();

				// 根据限流类型进行区分
				if (flowControlConfig.getType().equalsIgnoreCase(FilterConst.FLOW_CTL_TYPE_PATH)
						&& path.equals(flowControlConfig.getValue())) {
					flowControlRule = FlowControlByPathRule.getInstance(rule.getServiceId(), path);
				} else if (flowControlConfig.getType().equalsIgnoreCase(FilterConst.FLOW_CTL_TYPE_SERVICE)
						&& path.equals(flowControlConfig.getValue())) {
					// 基于服务限流
				}
				if (flowControlRule != null) {
					// 执行具体限流逻辑
					flowControlRule.doFlowControlFilter(flowControlConfig, rule.getServiceId());
				}
			}
		}
	}
}
