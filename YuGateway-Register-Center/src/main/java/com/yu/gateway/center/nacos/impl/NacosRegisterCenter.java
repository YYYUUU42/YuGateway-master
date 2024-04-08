package com.yu.gateway.center.nacos.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import com.yu.gateway.center.api.RegisterCenter;
import com.yu.gateway.center.api.RegisterCenterListener;
import com.yu.gateway.common.config.ServiceDefinition;
import com.yu.gateway.common.config.ServiceInstance;
import com.yu.gateway.common.constant.GatewayConst;


import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author yu
 * @description Nacos 注册中心接口实现
 * @date 2024-04-08
 */
@Slf4j
public class NacosRegisterCenter implements RegisterCenter {
	/**
	 * 注册中心地址
	 */
	private String registerAddress;

	/**
	 * 使用环境
	 */
	private String env;

	/**
	 * 服务实例信息
	 */
	private NamingService namingService;

	/**
	 * 服务信息维护实例
	 */
	private NamingMaintainService namingMaintainService;

	/**
	 * 监听器列表，多个服务多个监听器，同时修改存在并发问题
	 */
	private List<RegisterCenterListener> registerCenterListenerList = new CopyOnWriteArrayList<>();

	/**
	 * 初始化
	 *
	 * @param registerAddress  注册中心地址
	 * @param env  要注册到的环境
	 */
	@Override
	public void init(String registerAddress, String env) {
		this.registerAddress = registerAddress;
		this.env = env;
		try {
			this.namingMaintainService = NamingMaintainFactory.createMaintainService(registerAddress);
			this.namingService = NamingFactory.createNamingService(registerAddress);
		} catch (NacosException e) {
			log.warn("can't init nacosRegisterCenter");
			throw new RuntimeException(e);
		}
	}

	/**
	 * 服务注册
	 *
	 * @param serviceDefinition 服务定义信息
	 * @param serviceInstance 服务实例信息
	 */
	@Override
	public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
		try {
			//构造 nacos 实例信息
			Instance instance = new Instance();
			instance.setInstanceId(serviceInstance.getServiceInstanceId());
			instance.setIp(serviceInstance.getIp());
			instance.setPort(serviceInstance.getPort());

			//实例信息可以放入到metadata中
			instance.setMetadata(Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceInstance)));

			//注册
			namingService.registerInstance(serviceDefinition.getServiceId(), env, instance);

			//更新服务定义
			namingMaintainService.updateService(serviceDefinition.getServiceId(), env, 0,
					Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceDefinition)));
			log.info("register {} {}", serviceDefinition, serviceInstance);
		} catch (NacosException e) {
			log.error("register {} failed", serviceDefinition, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 服务注销
	 * @param serviceDefinition 服务定义信息
	 * @param serviceInstance 服务实例信息
	 */
	@Override
	public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
		try {
			namingService.deregisterInstance(serviceDefinition.getServiceId(), env, serviceInstance.getIp(), serviceInstance.getPort());
		} catch (NacosException e) {
			log.error("deregister {} failed", serviceDefinition, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 订阅所有服务
	 *
	 * @param registerCenterListener 注册中心监听器
	 */
	@Override
	public void subscribeAllServices(RegisterCenterListener registerCenterListener) {
		//服务订阅首先需要将我们的监听器加入到我们的服务列表中
		registerCenterListenerList.add(registerCenterListener);

		//进行服务订阅
		doSubscribeServices();

		//可能有新服务加入，所以需要有一个定时任务来检查
		ScheduledExecutorService scheduledPool = (ScheduledExecutorService) new ThreadPoolExecutor(
				// 核心线程数
				1,
				// 最大线程数，此处保持与核心线程数一致，因为只需要一个定时任务线程
				1,
				// 线程空闲存活时间
				60L,
				// 时间单位
				TimeUnit.SECONDS,
				// 使用无界队列，但此处应考虑实际需求，可能需要设置适当容量的有界队列
				new LinkedBlockingQueue<>(),
				// 线程工厂
				new NameThreadFactory("doSubscribeAllServices"),
				// 拒绝策略，这里仅作为示例，实际应根据业务需求选择或自定义拒绝策略
				new ThreadPoolExecutor.AbortPolicy()
		);

		//循环执行服务发现与订阅操作使用，Lambda 表达式捕获异常并处理
		scheduledPool.scheduleWithFixedDelay(() -> {
			try {
				doSubscribeServices();
			} catch (Exception e) {
				log.error("doSubscribeAllServices failed", e);
			}
		}, 10, 10, TimeUnit.SECONDS);
	}

	/**
	 * 订阅服务
	 * 当服务注册中心发起 NamingEvent 事件时更新本地服务列表信息
	 */
	private void doSubscribeServices() {
		try {
			// 获取已订阅的服务列表
			//这里其实已经在init的时候初始化过 namingService 了，所以这里可以直接拿到当前服务已经订阅的服务
			Set<String> subscribeServiceSet = namingService.getSubscribeServices().stream().map(ServiceInfo::getName).collect(Collectors.toSet());
			int pageNo = 1;
			int pageSize = 100;

			// 分页获取所有服务列表，缓存未订阅的服务信息
			List<String> serviceList = namingService.getServicesOfServer(pageNo, pageSize, env).getData();

			while (CollectionUtils.isNotEmpty(serviceList)) {
				log.info("service list size {}", serviceList.size());

				for (String service : serviceList) {

					//判断是否已经订阅了当前服务
					if (subscribeServiceSet.contains(service)) {
						continue;
					}

					//nacos 事件监听器 订阅当前服务
					//这里需要自己实现一个 nacos 的事件订阅类 来具体执行订阅执行时的操作
					EventListener eventListener = new NacosRegisterListener();

					//当前服务之前不存在 调用监听器方法进行添加处理
					eventListener.onEvent(new NamingEvent(service, null));

					// 订阅指定运行环境下对应的服务名，注册中心服务发生变动时调用 onEvent() 方法更新本地缓存的服务信息
					namingService.subscribe(service, env, eventListener);
					log.info("subscribe a service, serviceName {} env{}", service, env);
				}

				//遍历下一页的服务列表
				serviceList = namingService.getServicesOfServer(++pageNo, pageSize, env).getData();
			}
		} catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 实现对 nacos 事件的监听器 这个事件监听器会在 nacos 发生事件变化的时候进行回调
	 * NamingEvent 是一个事件对象，用于表示与服务命名空间（Naming）相关的事件。
	 * NamingEvent 的作用是用于监听和处理命名空间中的服务实例（Service Instance）的变化，
	 * 以便应用程序可以根据这些变化来动态地更新服务实例列表，以保持与注册中心的同步。
	 */
	private class NacosRegisterListener implements EventListener {
		@Override
		public void onEvent(Event event) {
			//先判断是否是注册中心事件
			if (event instanceof NamingEvent) {
				log.info("Nacos event is {}", JSON.toJSON(event));

				// 获取变更服务名
				NamingEvent namingEvent = (NamingEvent) event;
				String serviceName = namingEvent.getServiceName();

				try {
					// 获取最新的服务信息
					Service service = namingMaintainService.queryService(serviceName, env);
					ServiceDefinition definition = JSON.parseObject(service.getMetadata().get(GatewayConst.META_DATA_KEY), ServiceDefinition.class);

					// 获取服务实例信息
					List<Instance> instances = namingService.getAllInstances(service.getName(), env);
					Set<ServiceInstance> instanceSet = new HashSet<>();
					for (Instance instance : instances) {
						ServiceInstance serviceInstance = JSON.parseObject(instance.getMetadata().get(GatewayConst.META_DATA_KEY), ServiceInstance.class);
						instanceSet.add(serviceInstance);
					}
					// 调用订阅监听器接口
					registerCenterListenerList.forEach(registerCenterListener -> {
						registerCenterListener.onChange(definition, instanceSet);
					});
				} catch (NacosException e) {
					log.error("Nacos update ServiceInfo failed", e);
					throw new RuntimeException(e);
				}
			}
		}
	}
}
