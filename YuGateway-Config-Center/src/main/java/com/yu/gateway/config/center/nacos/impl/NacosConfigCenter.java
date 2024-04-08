package com.yu.gateway.config.center.nacos.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.config.center.api.ConfigCenter;
import com.yu.gateway.config.center.api.RulesChangeListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author yu
 * @description Nacos 配置中心实现
 * @date 2024-04-08
 */
@Slf4j
public class NacosConfigCenter implements ConfigCenter {
	/**
	 * 服务器地址
	 */
	private String serverAddr;

	/**
	 * 服务环境
	 */
	private String env;

	/**
	 * 需要拉取的服务配置的 DATA_ID
	 */
	private static final String DATA_ID = "api-gateway";

	/**
	 * Nacos 提供的与配置中心进行交互的接口
	 */
	private ConfigService configService;

	/**
	 * 初始化配置中心配置
	 *
	 * @param serverAddr 配置中心地址
	 * @param env        环境
	 */
	@Override
	public void init(String serverAddr, String env) {
		this.serverAddr = serverAddr;
		this.env = env;
		try {
			this.configService = NacosFactory.createConfigService(serverAddr);
		} catch (NacosException e) {
			log.error("NacosConfigCenter init failed {}", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 订阅配置中心配置变更
	 *
	 * @param listener 配置变更监听器
	 */
	@Override
	public void subscribeRulesChange(RulesChangeListener listener) {
		try {
			//初始化通知 DATA_ID是自己定义的 返回值就是一个json configJson : {"rules":[{}, {}]}
			String msg = configService.getConfig(DATA_ID, env, 5000);

			log.info("Rules-Config From Nacos: {}", msg);
			List<Rule> rules = JSON.parseObject(msg).getJSONArray("rules").toJavaList(Rule.class);

			// 保存配置信息到本地，调用我们的监听器 参数就是我们拿到的rules
			listener.onRulesChange(rules);

			// 配置远程注册中心配置变更的回调函数
			configService.addListener(DATA_ID, env, new Listener() {
				//是否使用额外线程执行
				@Override
				public Executor getExecutor() {
					return null;
				}

				//调用监听器的 onRulesChange 方法，将解析得到的 Rule 对象列表作为参数传递给监听器，以通知配置变更。
				@Override
				public void receiveConfigInfo(String configInfo) {
					log.info("new Config from Nacos: {}", configInfo);
					List<Rule> newRules = JSON.parseObject(configInfo).getJSONArray("rules").toJavaList(Rule.class);
					listener.onRulesChange(newRules);
				}
			});
		} catch (NacosException e) {
			log.error("subscribeRulesChange failed", e);
			throw new RuntimeException(e);
		}
	}
}
