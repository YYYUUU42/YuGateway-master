package com.yu.gateway.core.filter.flow.algorithm;

import com.alibaba.fastjson.JSON;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.common.utils.redis.JedisUtil;
import com.yu.gateway.core.filter.flow.FlowAlgorithmConstant;

import java.util.Map;

/**
 * @author yu
 * @description 滑动窗口限流算法
 * @date 2024-05-05
 */
public class SlidingWindowAlgorithm implements AbstractExecuteStrategy<Rule.FlowControlConfig, Boolean> {

	private static final String PREFIX = "slidingWindowRateLimiter";

	protected JedisUtil jedisUtil;

	private static final Long SUCCESS_FLAG = 1L;

	public SlidingWindowAlgorithm(JedisUtil jedisUtil) {
		this.jedisUtil = jedisUtil;
	}

	@Override
	public String mark() {
		return FlowAlgorithmConstant.MOVE_WINDOWS_ALGORITHM;
	}

	@Override
	public String patternMatchMark() {
		return AbstractExecuteStrategy.super.patternMatchMark();
	}

	/**
	 * 限流具体操作
	 */
	@Override
	public Boolean executeResp(Rule.FlowControlConfig requestParam,String key) {
		Map<String, Integer> configMap = JSON.parseObject(requestParam.getConfig(), Map.class);
		if (!configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_DURATION) || !configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_PERMITS)) {
			return false;
		}
		double duration = configMap.get(FilterConst.FLOW_CTL_LIMIT_DURATION);
		double permits = configMap.get(FilterConst.FLOW_CTL_LIMIT_PERMITS);

		return isAllowed(key, (int) permits, (int) duration);
	}

	/**
	 * @param limit      请求限制数量
	 * @param windowSize 窗口大小
	 */
	public boolean isAllowed(String id, int limit, int windowSize) {
		String windowKey = PREFIX + ":" + id;
		long now = System.currentTimeMillis();
		long windowStart = now - windowSize * 1000L;

		// 在zset中添加一个成员，分数是当前时间戳
		jedisUtil.zadd(windowKey, now, String.valueOf(now));

		// 删除zset中所有分数小于窗口开始时间的成员
		jedisUtil.zremrangeByScore(windowKey, 0, windowStart);

		// 获取zset的大小
		long count = jedisUtil.zcard(windowKey);

		// 如果zset的大小超过了限制的请求数量，返回false
		return count <= limit;
	}
}