package com.yu.gateway.core.netty.processor;

import com.yu.gateway.common.enums.ResponseCode;
import com.yu.gateway.common.exception.BaseException;
import com.yu.gateway.core.context.GatewayContext;
import com.yu.gateway.core.context.HttpRequestWrapper;
import com.yu.gateway.core.helper.RequestHelper;
import com.yu.gateway.core.helper.ResponseHelper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * @author yu
 * @description NettyCoreProcessor 是负责在基于 Netty 的服务器中处理 HTTP 请求的组件
 * @date 2024-04-06
 */
@Slf4j
public class NettyCoreProcessor implements NettyProcessor {


	/**
	 * 处理传入的 HTTP 请求。
	 *
	 * @param wrapper 包含 FullHttpRequest 和 ChannelHandlerContext 的 HttpRequestWrapper。
	 */
	@Override
	public void process(HttpRequestWrapper wrapper) {
		FullHttpRequest request = wrapper.getRequest();
		ChannelHandlerContext ctx = wrapper.getCtx();

		try {
			// 创建并填充 GatewayContext 以保存有关传入请求的信息。
			GatewayContext gatewayContext = RequestHelper.doContext(request, ctx);
		} catch (BaseException e) {
			// 通过记录日志并发送适当的 HTTP 响应处理已知异常。
			log.error("处理错误 {} {}", e.getCode().getCode(), e.getCode().getMessage());
			FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
			doWriteAndRelease(ctx, request, httpResponse);
		} catch (Throwable t) {
			// 通过记录日志并发送内部服务器错误响应处理未知异常。
			log.error("处理未知错误", t);
			FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
			doWriteAndRelease(ctx, request, httpResponse);
		}
	}

	/**
	 * 将 HTTP 响应写入通道并释放资源。
	 *
	 * @param ctx          用于写入响应的 ChannelHandlerContext。
	 * @param request      从客户端接收的 FullHttpRequest。
	 * @param httpResponse 作为响应发送的 FullHttpResponse。
	 */
	private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse httpResponse) {
		// 发送响应后关闭通道
		ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
		// 释放与请求相关联的资源。
		ReferenceCountUtil.release(request);
	}

	/**
	 * 启动 NettyCoreProcessor。（目前为空方法）
	 */
	@Override
	public void start() {
	}

	/**
	 * 关闭 NettyCoreProcessor。（目前为空方法）
	 */
	@Override
	public void shutDown() {
	}
}