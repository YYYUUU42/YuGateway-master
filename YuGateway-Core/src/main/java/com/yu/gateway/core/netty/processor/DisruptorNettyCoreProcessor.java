package com.yu.gateway.core.netty.processor;

import com.lmax.disruptor.dsl.ProducerType;
import com.yu.gateway.common.enums.ResponseCode;
import com.yu.gateway.core.Config;
import com.yu.gateway.core.context.HttpRequestWrapper;
import com.yu.gateway.core.disruptor.EventListener;
import com.yu.gateway.core.disruptor.ParallelQueueHandler;
import com.yu.gateway.core.disruptor.WaitStrategyFactory;
import com.yu.gateway.core.helper.ResponseHelper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * @author yu
 * Disruptor 提升 Netty 性能
 * 通过 Disruptor 异步处理 HTTP 请求提升性能
 * 并行处理器
 * @date 2024-04-16
 */
@Slf4j
public class DisruptorNettyCoreProcessor implements NettyProcessor{
    /**
     * 线程前缀
     */
    private static final String THREAD_NAME_PREFIX = "gateway-queue-";

    private Config config;

    /**
     * Disruptor 只是缓存依然需要使用到 Netty 核心处理器
     */
    private NettyCoreProcessor nettyCoreProcessor;

    /**
     * 处理类
     */
    private ParallelQueueHandler<HttpRequestWrapper> parallelQueueHandler;

    /**
     * 构造方法，初始化 DisruptorNettyCoreProcessor
     */
    public DisruptorNettyCoreProcessor(Config config, NettyCoreProcessor nettyCoreProcessor) {
        this.config = config;
        this.nettyCoreProcessor = nettyCoreProcessor;

        ParallelQueueHandler.Builder<HttpRequestWrapper> builder = new ParallelQueueHandler.Builder<HttpRequestWrapper>()
                .setBufferSize(config.getBufferSize())
                .setNamePrefix(THREAD_NAME_PREFIX)
                .setThreads(config.getProcessThread())
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(WaitStrategyFactory.getWaitStrategy(config.getWaitStrategy()));

        // 监听事件并处理
        BatchEventListenerProcessor processor = new BatchEventListenerProcessor();
        builder.setEventListener(processor);
        this.parallelQueueHandler = builder.build();
    }

    @Override
    public void process(HttpRequestWrapper wrapper) {
        this.parallelQueueHandler.add(wrapper);
    }

    @Override
    public void start() {
        parallelQueueHandler.start();
    }

    @Override
    public void shutDown() {
        parallelQueueHandler.shutdown();
    }

    /**
     * 监听处理从 disruptor 队列中取出的事件
     */
    public class BatchEventListenerProcessor implements EventListener<HttpRequestWrapper> {
        @Override
        public void onEvent(HttpRequestWrapper event) {
            // 调用 Netty 处理事件
            nettyCoreProcessor.process(event);
        }

        @Override
        public void onException(Throwable ex, long sequence, HttpRequestWrapper event) {
            FullHttpRequest request = event.getRequest();
            ChannelHandlerContext context = event.getCtx();

            try {
                log.error("BatchEventListenerProcessor onException failed, request:{}, errMsg:{}", request, ex.getMessage(), ex);
                // 构建响应对象
                FullHttpResponse response = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);

                // 非长连接直接关闭
                if (!HttpUtil.isKeepAlive(request)) {
                    context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    context.writeAndFlush(response);
                }
            } catch (Exception e) {
                log.error("BatchEventListenerProcessor onException, request:{}, errMsg:{}", request, e.getMessage(), e);
            }
        }
    }
}
