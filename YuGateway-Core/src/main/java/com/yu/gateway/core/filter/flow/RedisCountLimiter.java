package com.yu.gateway.core.filter.flow;

import com.yu.gateway.common.utils.redis.JedisUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yu
 * @description Redis限流器
 * @date 2024-04-10
 */
@Slf4j
public class RedisCountLimiter {
	protected JedisUtil jedisUtil;

	public RedisCountLimiter(JedisUtil jedisUtil) {
		this.jedisUtil = jedisUtil;
	}

	private static final int SCRIPT_SUCCESS = 1;
	private static final int SCRIPT_FAILURE = 0;

	/**
	 * Redis限流操作：
	 * 1.增加 服务/路径 对应键的值，如果返回值为1，则设定超时时间；
	 * 2.判断返回值是否超过最大限流阈值；
	 * 3.超过则返回0，否则为1；
	 *
	 * @param key    	限流key 服务+路径
	 * @param limit  	限流次数
	 * @param expire 	超时时间
	 */
	public boolean doFlowCtl(String key, int limit, int expire) {
		try {
			Object object = jedisUtil.executeScript(key, limit, expire);
			if (object == null) {
				return true;
			}
			long result = Long.parseLong(object.toString());

			if (SCRIPT_FAILURE == result) {
				log.debug("请求超限");
				return false;
			}

		} catch (Exception e) {
			log.error("分布式限流发送错误:{}", e.getMessage());
			throw e;
		}
		return true;
	}
}
