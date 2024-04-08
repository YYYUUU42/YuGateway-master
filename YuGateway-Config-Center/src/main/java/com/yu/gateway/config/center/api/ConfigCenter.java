package com.yu.gateway.config.center.api;

/**
 * @author yu
 * @description 配置中心接口方法
 * @date 2024-04-08
 */
public interface ConfigCenter {

	/**
	 * 初始化配置中心配置
	 *
	 * @param serverAddr 配置中心地址
	 * @param env        环境
	 */
	void init(String serverAddr, String env);


	/**
	 * 订阅配置中心配置变更
	 *
	 * @param listener 配置变更监听器
	 */
	void subscribeRulesChange(RulesChangeListener listener);
}
