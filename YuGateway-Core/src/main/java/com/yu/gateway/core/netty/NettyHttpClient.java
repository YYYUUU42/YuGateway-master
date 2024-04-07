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
 * @description NettyHttpClient 类负责创建和管理基于Netty的异步HTTP客户端
 * @date 2024-04-06
 */
@Slf4j
public class NettyHttpClient implements LifeCycle {
    /**
     * 配置信息对象，包含HTTP客户端的配置参数
     */
    private final Config config;

    /**
     * Netty的事件循环组，用于处理客户端的网络事件
     */
    private final EventLoopGroup eventLoopGroupWorker;

    /**
     * 异步HTTP客户端实例
     */
    private AsyncHttpClient asyncHttpClient;

    /**
     * 构造函数，创建NettyHttpClient的实例。
     *
     * @param config    包含客户端配置的对象。
     * @param eventLoopGroupWorker  用于客户端事件处理的Netty事件循环组。
     */
    public NettyHttpClient(Config config, EventLoopGroup eventLoopGroupWorker) {
        this.config = config;
        this.eventLoopGroupWorker = eventLoopGroupWorker;
        init();
    }

    /**
     * 初始化异步HTTP客户端，设置其配置参数。
     */
    @Override
    public void init() {
        DefaultAsyncHttpClientConfig.Builder httpClientBuilder = new DefaultAsyncHttpClientConfig.Builder()
                // 工作线程组
                .setEventLoopGroup(eventLoopGroupWorker)
                // 连接超时
                .setConnectTimeout(config.getHttpConnectTimeout())
                // 请求超时
                .setRequestTimeout(config.getHttpRequestTimeout())
                // 最大重试请求次数
                .setMaxRequestRetry(config.getHttpMaxRetryTimes())
                // 池化ByteBuf分配器
                .setAllocator(PooledByteBufAllocator.DEFAULT)
                .setCompressionEnforced(true)
                // 最大连接数
                .setMaxConnections(config.getHttpMaxConnections())
                .setMaxConnectionsPerHost(config.getHttpMaxConnectionsPerHost())
                .setPooledConnectionIdleTimeout(config.getHttpPooledConnectionIdleTimeout());
        this.asyncHttpClient = new DefaultAsyncHttpClient(httpClientBuilder.build());
    }

    /**
     * 启动客户端，通常在这里进行资源分配和启动必要的服务。
     */
    @Override
    public void start() {
        // 使用AsyncHttpHelper单例模式初始化异步HTTP客户端
        AsyncHttpHelper.getInstance().initialized(asyncHttpClient);
    }

    /**
     * 关闭客户端，通常在这里进行资源释放和清理工作。
     */
    @Override
    public void shutdown() {
        // 如果客户端实例不为空，则尝试关闭它
        if (asyncHttpClient != null) {
            try {
                // 关闭客户端，并处理可能的异常
                this.asyncHttpClient.close();
            } catch (IOException e) {
                // 记录关闭时发生的错误
                log.error("NettyHttpClient shutdown error", e);
            }
        }
    }
}