package com.yu.gateway.common.config;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author yu
 * @description 服务实例，一个服务定义对应多个服务实例
 * @date 2024-03-31
 */
@Getter
@Setter
public class ServiceInstance implements Serializable {
    /**
     * 服务实例ID————ip:port
     */
    protected String serviceInstanceId;

    /**
     * 服务定义ID————serviceId:version
     */
    protected String uniqueId;

    /**
     * 服务IP地址
     */
    protected String ip;

    protected int port;

    /**
     * 服务标签信息
     */
    protected String tags;

    /**
     * 权重
     */
    protected Integer weight;

    /**
     * 服务注册时间
     */
    protected long registerTime;

    /**
     * 服务可用
     */
    protected boolean enable = true;

    /**
     * 服务版本号
     */
    protected String version;
    /**
     * 是否灰度发布
     */
    protected boolean gray;


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (this == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceInstance obj = (ServiceInstance) o;
        return Objects.equals(obj.serviceInstanceId, serviceInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceInstanceId);
    }
}