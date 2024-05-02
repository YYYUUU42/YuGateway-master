package com.yu.gateway.core.filter.flow.algorithm;

import com.alibaba.fastjson.JSON;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.common.utils.redis.JedisUtil;
import com.yu.gateway.core.filter.flow.FlowAlgorithmConstant;

import java.util.Map;

/**
 * @author yu
 * @description 固定窗口限流算法
 * @date 2024-04-11
 */
public class StableAlgorithm implements AbstractExecuteStrategy<Rule.FlowControlConfig, Boolean> {

	private static final String PREFIX = "fixedWindowRateLimiter";

	protected JedisUtil jedisUtil;

	private static final Long SUCCESS_FLAG = 1L;

	public StableAlgorithm(JedisUtil jedisUtil) {
		this.jedisUtil = jedisUtil;
	}

	@Override
	public String mark() {
		return FlowAlgorithmConstant.FIXED_WINDOWS_ALGORITHM;
	}

	@Override
	public String patternMatchMark() {
		return AbstractExecuteStrategy.super.patternMatchMark();
	}

	/**
	 * 限流具体操作
	 */
	@Override
	public Boolean executeResp(Rule.FlowControlConfig requestParam) {
		Map<String, Integer> configMap = JSON.parseObject(requestParam.getConfig(), Map.class);
		if (!configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_DURATION) || !configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_PERMITS)) {
			return false;
		}
		double duration = configMap.get(FilterConst.FLOW_CTL_LIMIT_DURATION);
		double permits = configMap.get(FilterConst.FLOW_CTL_LIMIT_PERMITS);

		return isAllowed(requestParam.getValue(), (int) permits, (int) duration);
	}

	/**
	 * @param limit      请求限制数量
	 * @param windowSize 窗口大小
	 */
	public boolean isAllowed(String id, int limit, int windowSize) {
		String lockKey = PREFIX + ":" + "LOCK" + ":" + id;
		// 窗口初始化
		try {
			boolean isLock = jedisUtil.getDistributeLock(lockKey, id, windowSize);
			if (isLock) {
				String window_key = PREFIX + ":" + id;

				long current = jedisUtil.increment(window_key);
				if (current == 1) {
					jedisUtil.setExpire(window_key, windowSize);
				}

				return current <= limit;
			}
		} finally {
			jedisUtil.releaseDistributeLock(lockKey, id);
		}
		return false;
	}
}
