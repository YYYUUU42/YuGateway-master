package com.yu.gateway.core.netty;

import com.yu.gateway.core.context.HttpRequestWrapper;
import com.yu.gateway.core.netty.processor.NettyProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yu
 * NettyHttpServerHandler 用于处理通过 Netty 传入的 HTTP 请求,
 * 继承自 ChannelInboundHandlerAdapter，这样可以覆盖回调方法来处理入站事件。
 * @date 2024-04-06
 */
@Slf4j
public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 用于处理具体的业务逻辑
     */
    private NettyProcessor processor;

    public NettyHttpServerHandler(NettyProcessor processor) {
        this.processor = processor;
    }

    /**
     * 当从服务端接收到数据时，该方法会被调用
     * 这里将入站的数据（HTTP请求）包装后，传递给业务逻辑处理器处理
     *
     * @param ctx ChannelHandlerContext，提供了操作网络通道的方法。
     * @param msg 接收到的消息，预期是一个 FullHttpRequest 对象。
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 将接收到的消息转换为 FullHttpRequest 对象
        FullHttpRequest request = (FullHttpRequest) msg;

        // 创建 HttpRequestWrapper 对象，并设置上下文和请求
        HttpRequestWrapper httpRequestWrapper = new HttpRequestWrapper();
        httpRequestWrapper.setCtx(ctx);
        httpRequestWrapper.setRequest(request);

        // 调用业务逻辑处理器的 process 方法处理请求
        processor.process(httpRequestWrapper);
    }

    /**
     * 处理在处理入站事件时发生的异常
     *
     * @param ctx   ChannelHandlerContext，提供了操作网络通道的方法。
     * @param cause 异常对象。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 调用父类的 exceptionCaught 方法，它将按照 ChannelPipeline 中的下一个处理器继续处理异常
        super.exceptionCaught(ctx, cause);
        log.error("Netty occur exception", cause);
    }
}
