package com.yu.gateway.center.api;


import com.yu.gateway.common.config.ServiceDefinition;
import com.yu.gateway.common.config.ServiceInstance;

/**
 * @author yu
 * @description 注册中心接口
 * @date 2024-04-07
 */
public interface RegisterCenter {
    /**
     * 初始化注册中心相关配置
     * @param registerAddress
     * @param env
     */
    void init(String registerAddress, String env);

    /**
     * 注册服务信息
     * @param definition
     * @param serviceInstance
     */
    void register(ServiceDefinition definition, ServiceInstance serviceInstance);

    /**
     * 注销服务信息
     * @param definition
     * @param serviceInstance
     */
    void deregister(ServiceDefinition definition, ServiceInstance serviceInstance);

    /**
     * 订阅服务信息
     * @param registerCenterListener
     */
    void subscribeAllServices(RegisterCenterListener registerCenterListener);
}
