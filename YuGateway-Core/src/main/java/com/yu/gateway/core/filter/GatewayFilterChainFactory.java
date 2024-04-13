package com.yu.gateway.core.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author yu
 * 过滤器工厂具体实现类
 * 1、根据SPI 动态加载驱动实现的过滤器类对象，并存储到本地内存；
 * 2、根据注册中心配置的规则策略，加载实时可用的过滤器，组装为网关过滤器链。
 */
@Slf4j
public class GatewayFilterChainFactory implements FilterChainFactory{

	/**
	 * 本地实现的过滤器对象缓存
	 */
	private Map<String, Filter> processorFilterIdMap = new ConcurrentHashMap<>();
	private Map<String, String> processFilterIdName = new ConcurrentHashMap<>();

	/**
	 * 单例模式
	 */
	private static class SingleInstanceHolder {
		private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
	}

	/**
	 * 饿汉式获取单例对象
	 */
	public static GatewayFilterChainFactory getInstance() {
		return SingleInstanceHolder.INSTANCE;
	}

	/**
	 * 过滤器链缓存（服务ID ——> 过滤器链）
	 * ruleId —— GatewayFilterChain
	 */
	private Cache<String, GatewayFilterChain> chainCache = Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.SECONDS).build();

	/**
	 * SPI加载本地过滤器实现类对象
	 * 过滤器存储映射 过滤器id - 过滤器对象
	 */
	public GatewayFilterChainFactory() {
		//加载所有过滤器
		ServiceLoader<Filter> serviceLoader = ServiceLoader.load(Filter.class);
		serviceLoader.stream().forEach(filterProvider -> {
			Filter filter = filterProvider.get();
			FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
			log.info("load filter success:{},{},{},{}", filter.getClass(), annotation.id(), annotation.name(), annotation.order());

			//添加到过滤集合
			String filterId = annotation.id();
			if (StringUtils.isEmpty(filterId)) {
				filterId = filter.getClass().getName();
			}

			processorFilterIdMap.put(filterId, filter);
			processFilterIdName.put(filterId, annotation.name());
		});

	}

	/**
	 * 构建过滤器链条
	 */
	@Override
	public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception {
		// 获取规则ID
		String ruleId = ctx.getRules().getId();

		// 从缓存中获取过滤器链
		GatewayFilterChain chain = chainCache.getIfPresent(ruleId);

		// 如果缓存中没有过滤器链，那么构建一个新的过滤器链
		if (chain == null) {
			chain = doBuildFilterChain(ctx.getRules());
			// 将新构建的过滤器链添加到缓存中
			chainCache.put(ruleId, chain);
		}

		// 返回过滤器链
		return chain;
	}

	/**
	 * 通过过滤器ID获取过滤器
	 */
	@Override
	public Filter getFilterInfo(String filterId) throws Exception {
		return processorFilterIdMap.get(filterId);
	}

	/**
	 * 构建过滤器链
	 */
	public GatewayFilterChain doBuildFilterChain(Rule rule) {
		GatewayFilterChain chain = new GatewayFilterChain();
		List<Filter> contextFilters = new ArrayList<>();
		if (rule != null) {
			Set<Rule.FilterConfig> configFilters = rule.getFilterConfigs();

			for (Rule.FilterConfig config : configFilters) {
				if (config == null) {
					continue;
				}

				String filterConfigId = config.getId();
				if (StringUtils.isNotEmpty(filterConfigId) && processorFilterIdMap.containsKey(filterConfigId)) {
					Filter filter = processorFilterIdMap.get(filterConfigId);
					log.info("set filter into filterChain, {} {}", filterConfigId, processFilterIdName.get(filterConfigId));
					contextFilters.add(filter);
				}
			}
		}

		// 每个服务请求最终最后需要添加路由过滤器
		contextFilters.add(processorFilterIdMap.get(FilterConst.ROUTER_FILTER_ID));
		// 过滤器排序
		contextFilters.sort(Comparator.comparingInt(Filter::getOrder));
		//添加到链表中
		chain.addFilterList(contextFilters);
		return chain;
	}

	/**
	 * 测试
	 */
	public static void main(String[] args) {
		new GatewayFilterChainFactory();
	}

}
