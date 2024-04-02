package com.yu.gateway.core.filter.loadBalance;

import com.alibaba.fastjson.JSON;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.config.ServiceInstance;
import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.common.enums.ResponseCode;
import com.yu.gateway.common.exception.NotFoundException;
import com.yu.gateway.core.context.GatewayContext;
import com.yu.gateway.core.filter.Filter;
import com.yu.gateway.core.filter.FilterAspect;
import com.yu.gateway.core.request.GatewayRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * @author yu
 * @description 负载均衡过滤器
 * @date 2024-04-02
 */
@FilterAspect(id = FilterConst.LOAD_BALANCE_FILTER_ID, name = FilterConst.LOAD_BALANCE_FILTER_NAME, order = FilterConst.LOAD_BALANCE_FILTER_ORDER)
public class LoadBalanceFilter implements Filter {

	private final Logger logger = LoggerFactory.getLogger(LoadBalanceFilter.class);

	@Override
	public void doFilter(GatewayContext ctx) throws Exception {
		// 验证输入
		if (ctx == null || ctx.getUniqueId() == null || ctx.getRequest() == null) {
			logger.error("Context or context's unique ID or request is null");
			throw new IllegalArgumentException("Context and its unique ID, request must not be null");
		}

		// 获取上下文服务ID
		String serviceId = ctx.getUniqueId();

		// 加载负载均衡策略，增加null检查
		LoadBalanceRule gatewayRule = getLoadBalanceRule(ctx);
		if (gatewayRule == null) {
			logger.error("Load balance rule is null for service ID: {}", serviceId);
			throw new IllegalStateException("Load balance rule must not be null");
		}

		// 选取服务实例，重新构造 Request 请求头
		ServiceInstance instance = gatewayRule.chooseByServiceId(serviceId, ctx.isGray());

		// 日志记录优化
		if (instance != null) {
			logger.info("ServiceInstance ip:{}, port:{}", instance.getIp(), instance.getPort());
		} else {
			logger.error("No instance available for service ID: {}", serviceId);
			throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
		}

		GatewayRequest gatewayRequest = ctx.getRequest();
		if (gatewayRequest != null) {
			String modifyHost = instance.getIp() + ":" + instance.getPort();
			gatewayRequest.setModifyHost(modifyHost);
		}
	}

	/**
	 * 获取负载均衡策略
	 *
	 * @param context
	 * @return
	 */
	public LoadBalanceRule getLoadBalanceRule(GatewayContext context) {
		LoadBalanceRule balanceRule = null;
		Rule rule = context.getRules();
		if (rule != null) {
			Set<Rule.FilterConfig> configFilters = rule.getFilterConfigs();
			for (Rule.FilterConfig filterConfig : configFilters) {
				if (filterConfig == null) {
					continue;
				}
				String filterId = filterConfig.getId();
				// 解析Rule配置的过滤器属性，获取过滤器类型描述
				if (filterId.equals(FilterConst.LOAD_BALANCE_FILTER_ID)) {
					balanceRule = parseLoadBalanceConfig(filterConfig.getConfig(), rule.getServiceId());
					// 找到负载均衡配置后即退出循环
					break;
				}
			}
		}
		return balanceRule;
	}

	/**
	 * 解析负载均衡配置
	 *
	 * @param config
	 * @param serviceId
	 * @return
	 */
	private LoadBalanceRule parseLoadBalanceConfig(String config, String serviceId) {
		String strategy = FilterConst.LOAD_BALANCE_STRATEGY_RANDOM;
		if (StringUtils.isNotEmpty(config)) {
			Map<String, String> map = JSON.parseObject(config, Map.class);
			strategy = map.getOrDefault(FilterConst.LOAD_BALANCE_KEY, strategy);
		}
		return getLoadBalanceRuleByStrategy(strategy, serviceId);
	}

	/**
	 * 根据策略获取负载均衡规则
	 *
	 * @param strategy
	 * @param serviceId
	 * @return
	 */
	private LoadBalanceRule getLoadBalanceRuleByStrategy(String strategy, String serviceId) {
		switch (strategy) {
			case FilterConst.LOAD_BALANCE_STRATEGY_RANDOM:
				return RandomLoadBalanceRule.getInstance(serviceId);
			case FilterConst.LOAD_BALANCE_STRATEGY_ROUND_ROBIN:
				return RoundRobinLoadBalanceRule.getInstance(serviceId);
			default:
				logger.warn("No load balance rule can be loaded for service={}, using default strategy: {}", serviceId, strategy);
				return RandomLoadBalanceRule.getInstance(serviceId);
		}
	}
}
