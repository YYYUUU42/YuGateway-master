package com.yu.gateway.core.filter.flow.algorithm;

import com.alibaba.fastjson.JSON;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.common.utils.redis.JedisUtil;
import com.yu.gateway.core.filter.flow.FlowAlgorithmConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.time.Instant;
import java.util.*;

/**
 * @author yu
 * @description 令牌桶限流算法
 * @date 2024-04-11
 */
@Slf4j
public class VoteBucketAlgorithm implements AbstractExecuteStrategy<Rule.FlowControlConfig, Boolean> {
	private static final String PREFIX = "voteBucketRateLimiter";

	protected JedisUtil jedisUtil;

	public VoteBucketAlgorithm(JedisUtil jedisUtil) {
		this.jedisUtil = jedisUtil;
	}

	/**
	 * 成功标识
	 */
	private static final Long SUCCESS_FLAG = 1L;

	@Override
	public String mark() {
		return FlowAlgorithmConstant.VOTE_BUCKET_ALGORITHM;
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
		return isAllowed(key, (int)(permits/duration), (int)permits, 1);
	}

	/**
	 * 判断当前桶是否还有剩余令牌
	 *
	 * @param id       服务ID+请求路径
	 * @param rate     填充速率
	 * @param capacity 容量
	 * @param tokens   需要令牌数
	 */
	public boolean isAllowed(String id, int rate, int capacity, int tokens) {

		// 读取 lua 脚本
		BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("vote_bucket_flow.lua"))));

		StringBuilder builder = new StringBuilder();
		String line = null;
		try {
			while (StringUtils.isNotEmpty(line = reader.readLine())) {
				builder.append(line);
			}
			Object ans = jedisUtil.executeLuaScript(builder.toString(), getKey(id), String.valueOf(rate), String.valueOf(capacity),
					String.valueOf(Instant.now().getEpochSecond()), String.valueOf(tokens));
			return SUCCESS_FLAG.equals(ans);
		} catch (IOException e) {
			log.error("VoteBucketAlgorithm isAllowed error", e);
		}
		return false;
	}

	/**
	 * 得到 key
	 */
	private List<String> getKey(String id) {
		String prefix = PREFIX + ":" + id;
		String tokenKey = prefix + ":tokens";
		String timestampKey = prefix + ":timestamp";
		return Arrays.asList(tokenKey, timestampKey);
	}
}
