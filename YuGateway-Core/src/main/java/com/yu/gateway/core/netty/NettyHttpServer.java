package com.yu.gateway.core.netty;

import com.yu.gateway.common.utils.RemotingUtil;
import com.yu.gateway.core.Config;
import com.yu.gateway.core.LifeCycle;
import com.yu.gateway.core.netty.processor.NettyProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author yu
 * @description Netty 自定义服务端
 * @date 2024-04-06
 */
@Slf4j
public class NettyHttpServer implements LifeCycle {
	/**
	 * 服务器配置对象，用于获取如端口号等配置信息
	 */
	private final Config config;

	/**
	 * 自定义的Netty处理器接口，用于定义如何处理接收到的请求
	 */
	private final NettyProcessor processor;

	/**
	 * 服务器引导类，用于配置和启动Netty服务
	 */
	private ServerBootstrap serverBootstrap;

	/**
	 * boss线程组，用于处理新的客户端连接
	 */
	private EventLoopGroup eventLoopGroupBoss;

	/**
	 * worker线程组，用于处理已经建立的连接的后续操作
	 */
	private EventLoopGroup eventLoopGroupWorker;

	public NettyHttpServer(Config config, NettyProcessor processor) {
		this.config = config;
		this.processor = processor;
		init();
	}

	public EventLoopGroup getEventLoopGroupWorker() {
		return eventLoopGroupWorker;
	}


	/**
	 * 初始化服务器，设置线程组和选择线程模型
	 * 如果系统支持Epoll，将创建Epoll事件循环组，否则创建NIO事件循环组。这些事件循环组用于处理服务器的网络事件。
	 */
	@Override
	public void init() {
		this.serverBootstrap = new ServerBootstrap();
		if (useEpoll()) {
			this.eventLoopGroupBoss = new EpollEventLoopGroup(config.getEventLoopGroupBossNum(),
					new DefaultThreadFactory("netty-boss-nio"));
			this.eventLoopGroupWorker = new EpollEventLoopGroup(config.getEventLoopGroupWorkerNum(),
					new DefaultThreadFactory("netty-worker-nio"));
		} else {
			this.eventLoopGroupBoss = new NioEventLoopGroup(config.getEventLoopGroupBossNum(),
					new DefaultThreadFactory("netty-boss-nio"));
			this.eventLoopGroupWorker = new NioEventLoopGroup(config.getEventLoopGroupWorkerNum(),
					new DefaultThreadFactory("netty-worker-nio"));
		}
	}

	/**
	 * 是否选用 epoll 优化IO
	 * 检查当前系统是否可以使用Epoll事件模型。它返回一个布尔值，表示是否可以使用Epoll。Epoll是一种在Linux系统上提供更高性能的事件模型。
	 *
	 * @return
	 */
	public boolean useEpoll() {
		return RemotingUtil.isIsLinuxPlatform() && Epoll.isAvailable();
	}

	/**
	 * 启动服务器，监听端口并开始接收请求
	 */
	@Override
	public void start() {
		// 配置服务器参数，如端口、TCP参数等
		this.serverBootstrap
				.group(eventLoopGroupBoss, eventLoopGroupWorker)
				.channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
				//TCP连接的最大队列长度
				.option(ChannelOption.SO_BACKLOG, 1024)
				// 允许端口重用
				.option(ChannelOption.SO_REUSEADDR, true)
				// 保持连接检测
				.option(ChannelOption.SO_KEEPALIVE, true)
				// 禁用Nagle算法，适用于小数据即时传输
				.childOption(ChannelOption.TCP_NODELAY, true)
				// 设置发送缓冲区大小
				.childOption(ChannelOption.SO_SNDBUF, 65535)
				// 设置接收缓冲区大小
				.childOption(ChannelOption.SO_RCVBUF, 65535)
				// 绑定监听端口
				.localAddress(new InetSocketAddress(config.getPort()))
				// 定义处理新连接的管道初始化逻辑
				.childHandler(new ChannelInitializer<Channel>() {
					@Override
					protected void initChannel(Channel ch) throws Exception {
						// 配置管道中的处理器，如编解码器和自定义处理器
						ch.pipeline().addLast(
								new HttpServerCodec(), // 处理HTTP请求的编解码器
								new HttpObjectAggregator(config.getMaxContentLength()), // 聚合HTTP请求
								new HttpServerExpectContinueHandler(), // 处理HTTP 100 Continue请求
								new NettyHttpServerHandler(processor), // 自定义的处理器
								new NettyServerConnectManagerHandler() // 连接管理处理器
						);
					}
				});
		try {
			this.serverBootstrap.bind().sync();
			log.info("server startup on port {}", config.getPort());
		} catch (InterruptedException e) {
			log.error("NettyHttpServer start failed", e);
			throw new RuntimeException();
		}
	}

	/**
	 * 关闭Netty服务器，释放资源
	 * 关闭 eventLoopGroupBoss 和 eventLoopGroupWorker，释放资源，确保服务器可以安全地关闭。
	 */
	@Override
	public void shutdown() {
		if (eventLoopGroupBoss != null) {
			eventLoopGroupBoss.shutdownGracefully();
		}
		if (eventLoopGroupWorker != null) {
			eventLoopGroupWorker.shutdownGracefully();
		}
	}
}
