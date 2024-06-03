package com.yu.gateway.core.netty;

import com.yu.gateway.common.utils.RemotingHelper;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yu
 * 连接管理器，管理连接对生命周期
 * 当前类提供出站和入站事件的处理能力，能够管理网络链接的整个生命周期
 * 服务器连接管理器，用于监控和管理网络连接的生命周期事件
 * @date 2024-04-06
 */
@Slf4j
public class NettyServerConnectManagerHandler extends ChannelDuplexHandler {

	/**
	 * 当通道被注册到它的 EventLoop 时调用，即它可以开始处理 I/O 事件
	 *
	 * @param ctx 提供了操作网络通道的方法的上下文对象
	 */
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		// 获取远程客户端的地址
		final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
		log.debug("netty server pipeline: channelRegistered {}", remoteAddress);

		// 调用父类方法继续处理注册事件
		super.channelRegistered(ctx);
	}

	/**
	 * 当通道从它的 EventLoop 注销时调用，不再处理任何 I/O 事件
	 *
	 * @param ctx 提供了操作网络通道的方法的上下文对象
	 */
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
		log.debug("netty server pipeline: channelUnregistered {}", remoteAddress);

		super.channelUnregistered(ctx);
	}

	/**
	 * 当通道变为活跃状态，即连接到远程节点时被调用
	 *
	 * @param ctx 提供了操作网络通道的方法的上下文对象
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
		log.debug("netty server pipeline: channelActive {}", remoteAddress);

		super.channelActive(ctx);
	}

	/**
	 * 当通道变为不活跃状态，即不再连接远程节点时被调用
	 *
	 * @param ctx 提供了操作网络通道的方法的上下文对象
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
		log.debug("netty server pipeline: channelInactive {}", remoteAddress);

		super.channelInactive(ctx);
	}

	/**
	 * 当用户自定义事件被触发时调用，例如，可以用来处理空闲状态检测事件
	 *
	 * @param ctx 提供了操作网络通道的方法的上下文对象。
	 * @param evt 触发的用户事件。
	 */
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		// 检查事件是否为IdleStateEvent（空闲状态事件）
		if (evt instanceof IdleStateEvent event) {
			// 如果是所有类型的空闲事件，则关闭通道
			if (event.state().equals(IdleState.ALL_IDLE)) {
				final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
				log.warn("netty server pipeline: userEventTriggered: idle {}", remoteAddress);

				ctx.channel().close();
			}
		}

		// 传递事件给下一个ChannelHandler
		ctx.fireUserEventTriggered(evt);
	}

	/**
	 * 当处理过程中发生异常时调用，通常是网络层面的异常
	 *
	 * @param ctx   提供了操作网络通道的方法的上下文对象
	 * @param cause 异常对象
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());

		// 记录警告信息和异常堆栈
		log.warn("netty server pipeline: remoteAddress： {}, exceptionCaught {}", remoteAddress, cause.getMessage());

		// 发生异常时关闭通道
		ctx.channel().close();
	}
}
