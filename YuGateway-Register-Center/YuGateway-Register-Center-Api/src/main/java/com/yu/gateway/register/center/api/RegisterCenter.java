package com.yu.gateway.register.center.api;

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
     * @param registerAddress  注册中心地址
     * @param env  要注册到的环境
     */
    void init(String registerAddress, String env);

    /**
     * 注册服务信息
     * @param serviceDefinition 服务定义信息
     * @param serviceInstance 服务实例信息
     */
    void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 注销服务信息
     * @param serviceDefinition 服务定义信息
     * @param serviceInstance 服务实例信息
     */
    void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 订阅服务信息
     * @param registerCenterListener 注册中心监听器
     */
    void subscribeAllServices(RegisterCenterListener registerCenterListener);
}
