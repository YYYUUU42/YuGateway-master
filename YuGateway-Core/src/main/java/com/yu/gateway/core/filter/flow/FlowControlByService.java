package com.yu.gateway.core.filter.flow;

import com.alibaba.fastjson.JSON;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.common.utils.redis.JedisUtil;
import com.yu.gateway.core.filter.flow.algorithm.StableAlgorithm;
import com.yu.gateway.core.filter.flow.algorithm.VoteBucketAlgorithm;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.yu.gateway.common.constant.FilterConst.FLOW_CTL_LIMIT_DURATION;
import static com.yu.gateway.common.constant.FilterConst.FLOW_CTL_LIMIT_PERMITS;
import static com.yu.gateway.core.filter.flow.FlowAlgorithmConstant.FIXED_WINDOWS_ALGORITHM;
import static com.yu.gateway.core.filter.flow.FlowAlgorithmConstant.VOTE_BUCKET_ALGORITHM;

/**
 * @author yu
 * @description 根据请求服务获取具体的流控规则过滤器
 * @date 2024-05-02
 */
public class FlowControlByService implements GatewayFlowControlRule {
	/**
	 * 服务ID
	 */
	private String serviceId;

	/**
	 * 限流提示语言
	 */
	private static final String LIMIT_MESSAGE = "您的请求过于频繁，请稍后重试";

	/**
	 * 存放路径-流控规则的map
	 */
	private static ConcurrentHashMap<String, FlowControlByService> pathRuleMap = new ConcurrentHashMap<>();

	public FlowControlByService(String serviceId) {
		this.serviceId = serviceId;
	}

	/**
	 * 根据服务ID、请求路径获取限流规则
	 */
	public static FlowControlByService getInstance(String serviceId) {
		String key = serviceId;
		return pathRuleMap.computeIfAbsent(key, k -> new FlowControlByService(serviceId));
	}

	/**
	 * 执行流控规则过滤器
	 *
	 * @param flowControlConfig 注册中心限流配置；
	 * @param serviceId         服务ID
	 */
	@Override
	public void doFlowControlFilter(Rule.FlowControlConfig flowControlConfig, String serviceId) {
		if (flowControlConfig == null || StringUtils.isEmpty(serviceId) || StringUtils.isEmpty(flowControlConfig.getConfig())) {
			return;
		}

		//获得当前路径对应的流控次数
		Map<String, Integer> configMap = JSON.parseObject(flowControlConfig.getConfig(), Map.class);

		//判断是否包含流控规则   FLOW_CTL_LIMIT_DURATION：限流时间单位---秒  FLOW_CTL_LIMIT_PERMITS：限流请求次数---次
		if (!configMap.containsKey(FLOW_CTL_LIMIT_DURATION) || !configMap.containsKey(FLOW_CTL_LIMIT_PERMITS)) {
			return;
		}

		//得到流控时间和时间内限制次数
		double duration = configMap.get(FLOW_CTL_LIMIT_DURATION);
		double permits = configMap.get(FLOW_CTL_LIMIT_PERMITS);

		//当前请求是否触发流控标志位
		boolean flag = false;
		String key = serviceId;

		//如果是分布式项目 那么我们就需要使用Redis来实现流控  单机则可以直接使用Guava
		if (FilterConst.FLOW_CTL_MODE_DISTRIBUTED.equalsIgnoreCase(flowControlConfig.getMode())) {
			flag = switch (flowControlConfig.getAlgorithm()) {
				case VOTE_BUCKET_ALGORITHM ->
						new VoteBucketAlgorithm(new JedisUtil()).executeResp(flowControlConfig, key);
				case FIXED_WINDOWS_ALGORITHM ->
						new StableAlgorithm(new JedisUtil()).executeResp(flowControlConfig, key);
				default -> new VoteBucketAlgorithm(new JedisUtil()).executeResp(flowControlConfig, key);
			};
		} else {
			//单机版限流 直接用Guava
			GuavaCountLimiter guavaCountLimiter = GuavaCountLimiter.getInstance(serviceId, flowControlConfig);

			if (guavaCountLimiter == null) {
				throw new RuntimeException("获取单机限流工具类为空");
			}

			double count = Math.ceil(permits / duration);
			flag = guavaCountLimiter.acquire((int) count);
		}
		if (!flag) {
			throw new RuntimeException(LIMIT_MESSAGE);
		}
	}
}
