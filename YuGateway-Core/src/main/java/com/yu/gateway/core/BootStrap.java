package com.yu.gateway.core;

import com.alibaba.fastjson.JSON;
import com.yu.gateway.common.config.DynamicConfigManager;
import com.yu.gateway.common.config.ServiceDefinition;
import com.yu.gateway.common.config.ServiceInstance;
import com.yu.gateway.common.constant.BasicConst;
import com.yu.gateway.common.utils.*;
import com.yu.gateway.config.center.api.ConfigCenter;
import com.yu.gateway.register.center.api.RegisterCenter;
import com.yu.gateway.register.center.api.RegisterCenterListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author yu
 * @description 启动类
 * @date 2024-03-31
 */
@Slf4j
public class BootStrap {

	public static void main(String[] args) {
		//加载网关核心静态配置
		Config config = ConfigLoader.getInstance().load(args);

		//插件初始化
		//配置中心管理器初始化，连接配置中心，监听配置的新增、修改、删除
		ServiceLoader<ConfigCenter> serviceLoader = ServiceLoader.load(ConfigCenter.class);
		final ConfigCenter configCenter = serviceLoader.findFirst().orElseThrow(() -> {
			log.error("can't found ConfigCenter impl");
			return new RuntimeException("can't found ConfigCenter impl");
		});

		//从配置中心获取数据
		configCenter.init(config.getRegistryAddress(), config.getEnv());
		configCenter.subscribeRulesChange(rules -> {
			DynamicConfigManager.getInstance().putAllRule(rules);
		});

		//启动容器
		Container container = new Container(config);
		container.start();

		//连接注册中心
		final RegisterCenter registerCenter = registerAndSubscribe(config);

		//服务停机
		//收到 kill 信号时触发，服务停机
		Runtime.getRuntime().addShutdownHook(new Thread() {

			/**
			 * 服务停机
			 */
			@Override
			public void run() {
				registerCenter.deregister(buildGatewayServiceDefinition(config),
						buildGatewayServiceInstance(config));
				container.shutdown();
			}
		});

	}

	/**
	 * 服务注册和订阅服务变更信息通知, spi 方式实现服务注册
	 */
	private static RegisterCenter registerAndSubscribe(Config config) {
		ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
		final RegisterCenter registerCenter = serviceLoader.findFirst().orElseThrow(() -> {
			log.error("not found RegisterCenter impl");
			return new RuntimeException("not found RegisterCenter impl");
		});


		registerCenter.init(config.getRegistryAddress(), config.getEnv());

		//构造网关服务定义和服务实例
		ServiceDefinition serviceDefinition = buildGatewayServiceDefinition(config);
		ServiceInstance serviceInstance = buildGatewayServiceInstance(config);

		//注册
		registerCenter.register(serviceDefinition, serviceInstance);

		//订阅
		registerCenter.subscribeAllServices(new RegisterCenterListener() {
			@Override
			public void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet) {
				log.info("refresh service and instance: {} {}", serviceDefinition.getUniqueId(), JSON.toJSON(serviceInstanceSet));
				DynamicConfigManager manager = DynamicConfigManager.getInstance();
				//将这次变更事件影响之后的服务实例再次添加到对应的服务实例集合
				manager.putServiceInstance(serviceDefinition.getUniqueId(), serviceInstanceSet);

				//修改发生对应的服务定义
				manager.putServiceDefinition(serviceDefinition.getUniqueId(), serviceDefinition);
			}
		});
		return registerCenter;
	}

	/**
	 * 获取服务定义信息
	 */
	private static ServiceDefinition buildGatewayServiceDefinition(Config config) {
		ServiceDefinition definition = new ServiceDefinition();
		definition.setServiceId(config.getApplicationName());
		definition.setEnvType(config.getEnv());
		definition.setUniqueId(config.getApplicationName());
		definition.setInvokerMap(Map.of());
		return definition;
	}

	/**
	 * 获取服务实例信息
	 */
	private static ServiceInstance buildGatewayServiceInstance(Config config) {
		String localIp = NetUtil.getLocalIp();
		int port = config.getPort();

		ServiceInstance instance = new ServiceInstance();
		instance.setPort(port);
		instance.setIp(localIp);
		instance.setServiceInstanceId(localIp + BasicConst.COLON_SEPARATOR + port);
		instance.setRegisterTime(TimeUtil.currentTimeMillis());

		return instance;
	}

}
