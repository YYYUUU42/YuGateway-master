package com.yu.gateway.core.helper;

import com.yu.gateway.common.constant.BasicConst;
import com.yu.gateway.common.enums.ResponseCode;
import com.yu.gateway.core.context.ContextStatus;
import com.yu.gateway.core.context.IContext;
import com.yu.gateway.core.response.GatewayResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author yu
 * @description 响应辅助类
 * @date 2024-04-02
 */
public class ResponseHelper {
    /**
     * 构造FullHttpResponse对象
     */
    public static FullHttpResponse getHttpResponse(ResponseCode responseCode) {
        GatewayResponse gatewayResponse = GatewayResponse.buildGatewayResponse(responseCode);
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes(StandardCharsets.UTF_8)));
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + "charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
        return httpResponse;
    }

    /**
     * 构造FullHttpResponse对象
     * gatewayResponse ——> FullHttpResponse
     */
    private static FullHttpResponse getHttpResponse(IContext context, GatewayResponse gatewayResponse) {
        ByteBuf content;
        // build byteBuf for gatewayResponse
        if (Objects.nonNull(gatewayResponse.getFutureResponse())) {
            content = Unpooled.wrappedBuffer(gatewayResponse.getFutureResponse()
                    .getResponseBodyAsByteBuffer());
        } else if (gatewayResponse.getContent() != null) {
            content = Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes(StandardCharsets.UTF_8));
        } else {
            content = Unpooled.wrappedBuffer(BasicConst.BLANK_SEPARATOR_1.getBytes(StandardCharsets.UTF_8));
        }
        // build FullHttpResponse
        if (Objects.isNull(gatewayResponse.getFutureResponse())) {
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    gatewayResponse.getHttpResponseStatus(), content);
            httpResponse.headers().add(gatewayResponse.getResponseHeaders());
            httpResponse.headers().add(gatewayResponse.getExtraResponseHeaders());
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            return httpResponse;
        } else {
            gatewayResponse.getFutureResponse().getHeaders().add(gatewayResponse.getExtraResponseHeaders());
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(gatewayResponse.getFutureResponse().getStatusCode()),
                    content);
            httpResponse.headers().add(gatewayResponse.getFutureResponse().getHeaders());
            return httpResponse;
        }
    }

    /**
     * 写回响应
     */
    public static void writeResponse(IContext context) {
        // 释放请求资源
        context.releaseRequest();

        // 开始写回响应
        if (context.judgeContextStatus(ContextStatus.Written)) {
            FullHttpResponse response = getHttpResponse(context, (GatewayResponse) context.getResponse());

            // 如果不是保持连接的情况，响应后关闭通道
            if (!context.isKeepAlive()) {
                context.getNettyContext().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                // 如果是保持连接的情况，设置响应头部的连接为保持连接
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                // 写回响应
                context.getNettyContext().writeAndFlush(response);
            }
            // 改变上下文状态为已完成
            context.setContextStatus(ContextStatus.Completed);
        } else if (context.judgeContextStatus(ContextStatus.Completed)) {
            // 如果上下文状态已经是已完成，执行回调函数
            context.invokeCompletedCallBacks();
        }
    }
}
