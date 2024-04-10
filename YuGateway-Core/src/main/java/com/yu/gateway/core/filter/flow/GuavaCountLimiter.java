package com.yu.gateway.core.filter.flow;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.RateLimiter;
import com.yu.gateway.common.constant.FilterConst;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import com.yu.gateway.common.config.Rule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author yu
 * @description 单机限流——Guava缓存
 * @date 2024-04-10
 */
public class GuavaCountLimiter {
	/**
	 * Guava 限流依据
	 */
	private RateLimiter rateLimiter;

	/**
	 * 最大允许请求量
	 */
	@Setter
	@Getter
	private double maxPermits;

	/**
	 * 没有预热，尽可能按照这个速率来分发许可。速率限制器不会考虑之前的请求，也不会允许短时间内的请求速率超过指定的速率。
	 * 这可能导致一些请求需要等待，以便速率限制器可以分发足够的许可。
	 *
	 * @param maxPermits 表示每秒的最大许可数量
	 */

	public GuavaCountLimiter(double maxPermits) {
		this.maxPermits = maxPermits;
		rateLimiter = RateLimiter.create(maxPermits);
	}

	/**
	 * 创建一个具有预热期的速率限制器。预热期是指在速率限制器刚开始使用时，速率限制器允许请求超过其平均速率。
	 * 预热结束后按照指定的速度分发许可。
	 * 提供预热意味着可以在应用启动或负载增加时，允许一些瞬时的高请求速率，然后逐渐调整到稳定的速率。
	 *
	 * @param maxPermits           表示每秒的最大许可数量
	 * @param warmUpPeriodAsSecond 预热时长
	 */
	public GuavaCountLimiter(double maxPermits, long warmUpPeriodAsSecond) {
		this.maxPermits = maxPermits;
		rateLimiter = RateLimiter.create(maxPermits, warmUpPeriodAsSecond, TimeUnit.SECONDS);
	}

	/**
	 * 请求路径.服务名 —— 限流器
	 */
	public static ConcurrentHashMap<String, GuavaCountLimiter> resourceRateLimiterMap = new ConcurrentHashMap<>();

	/**
	 * 根据服务ID和流控规则获取限流器
	 */
	public static GuavaCountLimiter getInstance(String serviceId, Rule.FlowControlConfig flowControlConfig) {
		if (StringUtils.isEmpty(serviceId)
				|| flowControlConfig == null
				|| StringUtils.isEmpty(flowControlConfig.getValue())
				|| StringUtils.isEmpty(flowControlConfig.getConfig())
				|| StringUtils.isEmpty(flowControlConfig.getType())) {
			return null;
		}

		String key = serviceId + "." + flowControlConfig.getValue();
		GuavaCountLimiter countLimiter = resourceRateLimiterMap.get(key);

		// 计算当前流控阈值
		Map<String, Integer> configMap = JSON.parseObject(flowControlConfig.getConfig(), Map.class);

		if (!configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_DURATION) || !configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_PERMITS)) {
			return null;
		}

		// 得到流控次数和时间
		double permits = configMap.get(FilterConst.FLOW_CTL_LIMIT_PERMITS);
		double duration = configMap.get(FilterConst.FLOW_CTL_LIMIT_DURATION);
		double perSecondRate = permits / duration;

		// 缓存当前请求流控规则
		if (countLimiter == null) {
			countLimiter = new GuavaCountLimiter(perSecondRate);
			resourceRateLimiterMap.putIfAbsent(key, countLimiter);
		} else if (countLimiter.getMaxPermits() != perSecondRate) {
			countLimiter = new GuavaCountLimiter(perSecondRate);
			resourceRateLimiterMap.put(key, countLimiter);
		}
		return countLimiter;
	}

	/**
	 * 获取令牌
	 *
	 * @param permits 需要获取的令牌数量
	 * @return 是否获取成功
	 */
	public boolean acquire(int permits) {
		boolean success = rateLimiter.tryAcquire(permits);
		if (success) {
			return true;
		} else {
			return false;
		}
	}
}
