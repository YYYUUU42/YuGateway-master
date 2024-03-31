package com.yu.gateway.core.context;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Data;


@Data
public class HttpRequestWrapper {
    private FullHttpRequest request;
    private ChannelHandlerContext context;
}
