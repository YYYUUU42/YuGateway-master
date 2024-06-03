package com.yu.gateway.core.netty;

import com.yu.gateway.core.Config;
import com.yu.gateway.core.LifeCycle;
import com.yu.gateway.core.helper.AsyncHttpHelper;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.IOException;

/**
 * @author yu
 * @description NettyHttpClient 类负责创建和管理基于 Netty 的异步 HTTP 客户端
 * @date 2024-04-06
 */
@Slf4j
public class NettyHttpClient implements LifeCycle {
    /**
     * 配置信息对象，包含 HTTP 客户端的配置参数
     */
    private final Config config;

    /**
     * Netty 的事件循环组，用于处理客户端的网络事件
     */
    private final EventLoopGroup eventLoopGroupWorker;

    /**
     * 异步 HTTP 客户端实例
     */
    private AsyncHttpClient asyncHttpClient;

    /**
     * 构造函数，创建 NettyHttpClient 的实例。
     *
     * @param config    包含客户端配置的对象
     * @param eventLoopGroupWorker  用于客户端事件处理的 Netty 事件循环组
     */
    public NettyHttpClient(Config config, EventLoopGroup eventLoopGroupWorker) {
        this.config = config;
        this.eventLoopGroupWorker = eventLoopGroupWorker;
        init();
    }

    /**
     * 初始化异步 HTTP 客户端，设置其配置参数。
     */
    @Override
    public void init() {
        // 创建一个 DefaultAsyncHttpClientConfig.Builder 对象，用于构建异步 HTTP 客户端的配置
        DefaultAsyncHttpClientConfig.Builder httpClientBuilder = new DefaultAsyncHttpClientConfig.Builder()
                // 设置工作线程组，这是 Netty 的事件循环组，用于处理客户端的网络事件
                .setEventLoopGroup(eventLoopGroupWorker)
                // 设置连接超时时间，单位是毫秒
                .setConnectTimeout(config.getHttpConnectTimeout())
                // 设置请求超时时间，单位是毫秒
                .setRequestTimeout(config.getHttpRequestTimeout())
                // 设置最大重试请求次数，如果请求失败，客户端会尝试重新发送请求
                .setMaxRequestRetry(config.getHttpMaxRetryTimes())
                // 设置 ByteBuf 分配器，这里使用的是池化的分配器，可以提高内存利用率
                .setAllocator(PooledByteBufAllocator.DEFAULT)
                // 设置是否强制使用压缩，如果设置为 true，那么请求和响应都将被压缩
                .setCompressionEnforced(true)
                // 设置最大连接数，这是客户端可以同时打开的最大连接数
                .setMaxConnections(config.getHttpMaxConnections())
                // 设置每个主机的最大连接数，这是客户端可以同时打开到每个主机的最大连接数
                .setMaxConnectionsPerHost(config.getHttpMaxConnectionsPerHost())
                // 设置连接的空闲超时时间，单位是毫秒，如果连接在这段时间内没有任何活动，那么它将被关闭
                .setPooledConnectionIdleTimeout(config.getHttpPooledConnectionIdleTimeout());

        // 使用上面的配置创建一个新的异步 HTTP 客户端实例
        this.asyncHttpClient = new DefaultAsyncHttpClient(httpClientBuilder.build());
    }

    /**
     * 启动客户端，通常在这里进行资源分配和启动必要的服务
     */
    @Override
    public void start() {
        // 使用AsyncHttpHelper单例模式初始化异步HTTP客户端
        AsyncHttpHelper.getInstance().initialized(asyncHttpClient);
    }

    /**
     * 关闭客户端，通常在这里进行资源释放和清理工作
     */
    @Override
    public void shutdown() {
        // 如果客户端实例不为空，则尝试关闭它
        if (asyncHttpClient != null) {
            try {
                // 关闭客户端，并处理可能的异常
                this.asyncHttpClient.close();
            } catch (IOException e) {
                log.error("NettyHttpClient shutdown error", e);
            }
        }
    }
}