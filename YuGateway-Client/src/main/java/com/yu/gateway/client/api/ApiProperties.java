package com.yu.gateway.client.api;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yu
 * @description 存放环境和注册中心地址
 * @date 2024-04-14
 */
@Data
@ConfigurationProperties(prefix = "api")
public class ApiProperties {
    /**
     * 注册中心地址
     */
    private String registerAddress;
    /**
     * 环境
     */
    private String env = "dev";
    /**
     * 是否灰度发布
     */
    private boolean gray;
}
