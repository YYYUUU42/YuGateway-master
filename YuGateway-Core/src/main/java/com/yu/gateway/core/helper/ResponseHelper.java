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
     * @param responseCode
     * @return
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
     * gatewayResponse——>FullHttpResponse
     * @param context
     * @param gatewayResponse
     * @return
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
     * @param context
     */
    public static void writeResponse(IContext context) {
        // release request resources
        context.releaseRequest();
        // start writeback
        if (context.judgeContextStatus(ContextStatus.Written)) {
            FullHttpResponse response = getHttpResponse(context, (GatewayResponse) context.getResponse());
            // not keepAlive connection, close channel after response
            if (!context.isKeepAlive()) {
                context.getNettyContext().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                context.getNettyContext().writeAndFlush(response);
            }
            // change contextStatus
            context.setContextStatus(ContextStatus.Completed);
        } else if (context.judgeContextStatus(ContextStatus.Completed)) {
            // 重复触发写回操作时，执行回调函数
            context.invokeCompletedCallBacks();
        }
    }
}
