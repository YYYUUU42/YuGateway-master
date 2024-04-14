package com.yu.gateway.core.filter.loadBalance;

import com.yu.gateway.common.config.DynamicConfigManager;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.config.ServiceInstance;
import com.yu.gateway.common.enums.ResponseCode;
import com.yu.gateway.common.exception.ResponseException;
import com.yu.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yu
 * @description 负载均衡策略 -- 加权轮询
 * @date 2024-04-14
 */
@Slf4j
public class WeightedRoundRobinLoadBalanceRule implements LoadBalanceRule {
    /**
     * 服务ID
     */
    private String serviceId;

    /**
     * 存储服务ID和对应的负载均衡规则
     */
    private static ConcurrentHashMap<String, WeightedRoundRobinLoadBalanceRule> loadBalanceMap = new ConcurrentHashMap<>();

    /**
     * 用于记录当前轮询的位置
     */
    private AtomicInteger position = new AtomicInteger(0);

    /**
     * 构造函数，初始化服务ID
     */
    public WeightedRoundRobinLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * 根据服务ID获取对应的负载均衡规则，如果不存在则创建一个新的
     */
    public static WeightedRoundRobinLoadBalanceRule getInstance(String serviceId) {
        WeightedRoundRobinLoadBalanceRule rule = loadBalanceMap.get(serviceId);
        if (rule == null) {
            rule = new WeightedRoundRobinLoadBalanceRule(serviceId);
            loadBalanceMap.put(serviceId, rule);
        }
        return rule;
    }

    /**
     * 根据上下文和是否灰度发布选择服务实例
     */
    @Override
    public ServiceInstance choose(GatewayContext ctx, boolean gray) {
        Rule rule = ctx.getRules();
        return chooseByServiceId(rule.getServiceId(), gray);
    }

    /**
     * 根据服务ID和是否灰度发布选择服务实例
     */
    @Override
    public ServiceInstance chooseByServiceId(String serviceId, boolean gray) {
        // 获取服务实例集合
        Set<ServiceInstance> serviceSets = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId, gray);
        // 如果服务实例集合为空，则抛出异常
        if (CollectionUtils.isEmpty(serviceSets)) {
            log.warn("serviceId {} don't match any serviceInstance", serviceId);
            throw new ResponseException(ResponseCode.SERVICE_INVOKER_NOT_FOUND);
        }
        // 将服务实例集合转换为列表
        List<ServiceInstance> serviceLists = new ArrayList<>(serviceSets);

        // 计算总权重
        int totalWeight = serviceLists.stream().mapToInt(ServiceInstance::getWeight).sum();
        int currentWeight = 0;

        // 计算当前位置
        int index = position.getAndIncrement() % totalWeight;

        // 遍历服务实例列表，根据权重和当前位置选择服务实例
        for (ServiceInstance instance : serviceLists) {
            currentWeight += instance.getWeight();
            if (currentWeight > index) {
                return instance;
            }
        }

        // 如果没有找到合适的服务实例，则返回列表中的第一个
        return serviceLists.get(0);
    }
}