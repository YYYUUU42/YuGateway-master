package com.yu.gateway.register.center.api;


import com.yu.gateway.common.config.ServiceDefinition;
import com.yu.gateway.common.config.ServiceInstance;

import java.util.Set;

/**
 * @author yu
 * 注册中心监听器
 * 监听注册中心服务信息的变化并及时更新本地状态
 * @date 2024-04-07
 */
public interface RegisterCenterListener {
    /**
     * 注册中心服务信息变化
     * @param definition   服务定义信息
     * @param instanceSet  服务实例信息
     */
    void onChange(ServiceDefinition definition, Set<ServiceInstance>instanceSet);
}
