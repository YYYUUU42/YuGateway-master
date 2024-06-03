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
     * 根据给定的响应码构造 FullHttpResponse 对象
     */
    public static FullHttpResponse getHttpResponse(ResponseCode responseCode) {
        // 根据响应码构建 GatewayResponse 对象
        GatewayResponse gatewayResponse = GatewayResponse.buildGatewayResponse(responseCode);

        // 使用 GatewayResponse 对象的内容和状态创建 DefaultFullHttpResponse 对象
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                gatewayResponse.getHttpResponseStatus(),
                Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes(StandardCharsets.UTF_8))
        );

        // 设置响应的头部信息
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());

        // 返回构建的响应
        return httpResponse;
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

    /**
     * 构造 FullHttpResponse 对象   GatewayResponse -> FullHttpResponse
     */
    private static FullHttpResponse getHttpResponse(IContext context, GatewayResponse gatewayResponse) {
        ByteBuf content;
        // 检查 gatewayResponse 是否有 FutureResponse，如果有，使用其响应体作为 content
        if (Objects.nonNull(gatewayResponse.getFutureResponse())) {
            content = Unpooled.wrappedBuffer(gatewayResponse.getFutureResponse().getResponseBodyAsByteBuffer());
        } else if (gatewayResponse.getContent() != null) {
            // 如果没有 FutureResponse，但是有 content，那么使用 content 作为响应体
            content = Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes(StandardCharsets.UTF_8));
        } else {
            // 如果两者都没有，使用默认的 BLANK_SEPARATOR_1 作为响应体
            content = Unpooled.wrappedBuffer(BasicConst.BLANK_SEPARATOR_1.getBytes(StandardCharsets.UTF_8));
        }

        // 构建 FullHttpResponse
        if (Objects.isNull(gatewayResponse.getFutureResponse())) {
            // 如果没有 FutureResponse，使用 gatewayResponse 的 HttpResponseStatus 和 headers
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    gatewayResponse.getHttpResponseStatus(),
                    content);
            httpResponse.headers().add(gatewayResponse.getResponseHeaders());
            httpResponse.headers().add(gatewayResponse.getExtraResponseHeaders());
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());

            return httpResponse;
        } else {
            // 如果有 FutureResponse，使用其状态码和 headers
            gatewayResponse.getFutureResponse().getHeaders().add(gatewayResponse.getExtraResponseHeaders());
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(gatewayResponse.getFutureResponse().getStatusCode()),
                    content);
            httpResponse.headers().add(gatewayResponse.getFutureResponse().getHeaders());

            return httpResponse;
        }
    }
}
