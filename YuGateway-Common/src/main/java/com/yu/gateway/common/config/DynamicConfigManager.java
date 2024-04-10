package com.yu.gateway.common.config;

import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author yu
 * 1.缓存从配置中心获取的配置信息（Rule规则配置、Service服务配置）；
 * 2.动态更新配置信息；
 * @date 2024-04-02
 */
public class DynamicConfigManager {
    /**
     * 服务定义信息集合  serviceId——>ServiceDefinition
     */
    private ConcurrentHashMap<String, ServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();

    /**
     * 服务实例集合     serviceId——>Set<ServiceInstance>
     */
    private ConcurrentHashMap<String, Set<ServiceInstance>> serviceInstanceMap = new ConcurrentHashMap<>();

    /**
     * 规则集合        ruleId——>Rule
     */
    private ConcurrentHashMap<String, Rule> ruleMap = new ConcurrentHashMap<>();

    /**
     * 路径以及规则集合  serviceId.requestPath——>Rule
     */
    private ConcurrentHashMap<String, Rule> pathRuleMap = new ConcurrentHashMap<>();

    /**
     * 路径集合        service——>List<Rule>
     */
    private ConcurrentHashMap<String, List<Rule>> serviceRuleMap = new ConcurrentHashMap<>();

    public DynamicConfigManager() {
    }

    private static class SingletonHolder {
        private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
    }

    public static DynamicConfigManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /******* 对服务定义缓存的相关方法 ********/
    public void putServiceDefinition(String uniqueId, ServiceDefinition definition) {
        serviceDefinitionMap.put(uniqueId, definition);
    }

    public void removeServiceDefinition(String uniqueId) {
        serviceDefinitionMap.remove(uniqueId);
    }

    public ServiceDefinition getServiceDefinition(String uniqueId) {
        return serviceDefinitionMap.get(uniqueId);
    }

    public ConcurrentHashMap<String, ServiceDefinition> getServiceDefinitionMap() {
        return serviceDefinitionMap;
    }

    /******* 对服务实例缓存的相关方法 ********/
    public void putServiceInstance(String uniqueId, ServiceInstance instance) {
        Set<ServiceInstance> instanceSet = serviceInstanceMap.get(uniqueId);
        instanceSet.add(instance);
    }

    public void putServiceInstance(String uniqueId, Set<ServiceInstance> instanceSet) {
        serviceInstanceMap.put(uniqueId, instanceSet);
    }

    /**
     * 根据服务ID获取服务实例
     */
    public Set<ServiceInstance> getServiceInstanceByUniqueId(String uniqueId, boolean gray) {
        Set<ServiceInstance> instanceSet = serviceInstanceMap.get(uniqueId);
        if (CollectionUtils.isEmpty(instanceSet)) {
            return Collections.EMPTY_SET;
        }
        // 为灰度流量,返回灰度服务实例
        if (gray) {
            return instanceSet.stream().filter(ServiceInstance::isGray).collect(Collectors.toSet());
        }
        return instanceSet;
    }

    public void updateServiceInstance(String uniqueId, ServiceInstance instance) {
        Set<ServiceInstance> instanceSet = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> it = instanceSet.iterator();
        while(it.hasNext()) {
            ServiceInstance next = it.next();
            if (next.getServiceInstanceId().equals(instance.getServiceInstanceId())) {
                it.remove();
                break;
            }
        }
        instanceSet.add(instance);
    }

    public void removeServiceInstance(String uniqueId, String serviceInstanceId) {
        Set<ServiceInstance> instanceSet = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> iterator = instanceSet.iterator();
        while (iterator.hasNext()) {
            ServiceInstance next = iterator.next();
            if (next.getServiceInstanceId().equals(serviceInstanceId)) {
                iterator.remove();
                break;
            }
        }
    }

    public void removeServiceInstanceByUniqueId(String uniqueId) {
        serviceInstanceMap.remove(uniqueId);
    }

    /******* 缓存规则相关操作方法 ********/
    public void putRule(String ruleId, Rule rule) {
        ruleMap.put(ruleId, rule);
    }

    public void putAllRule(List<Rule> ruleList) {
        ConcurrentHashMap<String, Rule> newRuleMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Rule> newPathMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<Rule>> newServiceMap = new ConcurrentHashMap<>();
        for (Rule rule : ruleList) {
            newRuleMap.put(rule.getId(), rule);
            List<Rule> rules = newServiceMap.get(rule.getServiceId());
            if (rules == null) {
                rules = new ArrayList<>();
            }
            rules.add(rule);
            newServiceMap.put(rule.getServiceId(), rules);

            List<String> paths = rule.getPaths();
            for (String path : paths) {
                String key = rule.getServiceId() + "." + path;
                newPathMap.put(key, rule);
            }
        }
        ruleMap = newRuleMap;
        pathRuleMap = newPathMap;
        serviceRuleMap = newServiceMap;
    }

    public Rule getRule(String ruleId) {
        return ruleMap.get(ruleId);
    }

    public void removeRule(String ruleId) {
        ruleMap.remove(ruleId);
    }

    public ConcurrentHashMap<String, Rule> getRuleMap() {
        return ruleMap;
    }

    public Rule getRulePath(String path) {
        return pathRuleMap.get(path);
    }

    public List<Rule> getRuleByServiceId(String serviceId) {
        return serviceRuleMap.get(serviceId);
    }
}
