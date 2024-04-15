package com.yu.gateway.core;

import com.yu.gateway.common.constant.GatewayConst;
import com.yu.gateway.core.netty.NettyHttpClient;
import com.yu.gateway.core.netty.NettyHttpServer;
import com.yu.gateway.core.netty.processor.DisruptorNettyCoreProcessor;
import com.yu.gateway.core.netty.processor.NettyCoreProcessor;
import com.yu.gateway.core.netty.processor.NettyProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yu
 * @description Netty组件集合封装
 * @date 2024-04-06
 */
@Slf4j
public class Container implements LifeCycle{
    private final Config config;
    private NettyHttpServer nettyHttpServer;
    private NettyHttpClient nettyHttpClient;
    private NettyProcessor nettyProcessor;

    public Container(Config config) {
        this.config = config;
        init();
    }

    @Override
    public void init() {
        NettyCoreProcessor nettyCoreProcessor = new NettyCoreProcessor();
        // 多生产者多消费者模式
        if (GatewayConst.BUFFER_TYPE_PARALLEL.equals(config.getBufferType())) {
            this.nettyProcessor = new DisruptorNettyCoreProcessor(config, nettyCoreProcessor);
        } else {
            this.nettyProcessor = nettyCoreProcessor;
        }
        this.nettyHttpServer = new NettyHttpServer(config, nettyProcessor);
        // nettyClient、nettyServer 公用相同 work_threadGroup
        this.nettyHttpClient = new NettyHttpClient(config, nettyHttpServer.getEventLoopGroupWorker());
    }

    @Override
    public void start() {
        nettyProcessor.start();
        nettyHttpServer.start();
        nettyHttpClient.start();
        log.info("api gateway starting!");
    }

    @Override
    public void shutdown() {
        nettyProcessor.shutDown();
        nettyHttpClient.shutdown();
        nettyHttpServer.shutdown();
    }
}
