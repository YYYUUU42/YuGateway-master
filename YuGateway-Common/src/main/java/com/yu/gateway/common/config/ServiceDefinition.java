package com.yu.gateway.common.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * @author yu
 * @description 服务定义
 * @date 2024-03-31
 */
@Builder
@Getter
@Setter
public class ServiceDefinition implements Serializable {
    /**
     * 服务定义唯一ID： serviceId:version
     */
    private String uniqueId;

    /**
     * 服务ID
     */
    private String serviceId;

    /**
     * 服务版本
     */
    private String version;

    /**
     * 服务支持协议
     */
    private String protocol;

    /**
     * 服务路径匹配规则
     */
    private String patternPath;

    /**
     * 环境名称
     */
    private String envType;

    /**
     * 可用性
     */
    private boolean available;

    /**
     * 服务资源调用信息
     */
    private Map<String, ServiceInvoker> invokerMap;

    public ServiceDefinition() {
    }

    public ServiceDefinition(String uniqueId, String serviceId, String version, String protocol, String patternPath, String envType, boolean available, Map<String, ServiceInvoker> invokerMap) {
        this.uniqueId = uniqueId;
        this.serviceId = serviceId;
        this.version = version;
        this.protocol = protocol;
        this.patternPath = patternPath;
        this.envType = envType;
        this.available = available;
        this.invokerMap = invokerMap;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (this == null || getClass() != o.getClass()) return false;
        ServiceDefinition definition = (ServiceDefinition) o;
        return  Objects.equals(uniqueId, ((ServiceDefinition) o).getUniqueId());
    }
}
