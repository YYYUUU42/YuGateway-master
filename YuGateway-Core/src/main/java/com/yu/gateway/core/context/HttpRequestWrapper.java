package com.yu.gateway.core.context;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Data;

/**
 * @author yu
 * @description Netty请求包装器：请求 + 上下文
 * @date 2024-04-01
 */
@Data
public class HttpRequestWrapper {
    /**
     * HTTP 请求对象
     */
    private FullHttpRequest request;

    /**
     * ChannelHandlerContext 对象，提供了操作网络通道的方法
     */
    private ChannelHandlerContext ctx;
}
