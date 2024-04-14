package com.yu.gateway.client.api;

import lombok.Getter;

/**
 * @author yu
 * @description 提供对http的支持
 * @date 2024-04-14
 */
@Getter
public enum ApiProtocol {
    HTTP("http", "http协议");

    private String protocol;

    private String desc;

    ApiProtocol(String protocol, String desc) {
        this.protocol = protocol;
        this.desc = desc;
    }

}
