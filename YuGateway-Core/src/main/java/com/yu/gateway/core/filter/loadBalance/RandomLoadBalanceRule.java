package com.yu.gateway.core.filter.loadBalance;

import com.yu.gateway.common.config.DynamicConfigManager;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.config.ServiceInstance;
import com.yu.gateway.common.enums.ResponseCode;
import com.yu.gateway.common.exception.ResponseException;
import com.yu.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author yu
 * @description 负载均衡——随机策略
 * @date 2024-04-02
 */
@Slf4j
public class RandomLoadBalanceRule implements LoadBalanceRule {
	/**
	 * 服务ID
	 */
	private String serviceId;

	/**
	 * 服务ID——随机负载均衡策略
	 */
	private static ConcurrentHashMap<String, RandomLoadBalanceRule> loadBalanceMap = new ConcurrentHashMap<>();

	public RandomLoadBalanceRule(String serviceId) {
		this.serviceId = serviceId;
	}

	/**
	 * 根据服务 ID 获取负载均衡策略
	 */
	public static RandomLoadBalanceRule getInstance(String serviceId) {
		RandomLoadBalanceRule rule = loadBalanceMap.get(serviceId);
		if (rule == null) {
			rule = new RandomLoadBalanceRule(serviceId);
			loadBalanceMap.put(serviceId, rule);
		}
		return rule;
	}

	/**
	 * 负载均衡策略
	 */
	@Override
	public ServiceInstance choose(GatewayContext ctx, boolean gray) {
		// 获取上下文 Rule 对象
		Rule rule = ctx.getRules();
		return chooseByServiceId(rule.getServiceId(), gray);
	}

	/**
	 * 根据服务ID获取服务实例
	 */
	@Override
	public ServiceInstance chooseByServiceId(String serviceId, boolean gray) {
		// 根据服务ID获取服务实例集合
		Set<ServiceInstance> serviceSets = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId, gray);
		if (CollectionUtils.isEmpty(serviceSets)) {
			log.warn("serviceId {} don't match any serviceInstance", serviceId);
			throw new ResponseException(ResponseCode.SERVICE_INVOKER_NOT_FOUND);
		}
		List<ServiceInstance> serviceLists = new ArrayList<>(serviceSets);
		int index = ThreadLocalRandom.current().nextInt(serviceLists.size());
		return serviceLists.get(index);
	}
}
