package com.yu.gateway.core.disruptor;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yu
 * @description 基于 Disruptor 实现的多生产者多消费者无锁队列
 * @date 2024-04-15
 */
public class ParallelQueueHandler<E> implements ParallelQueue<E>{
    /**
     * 环形数据缓冲区
     */
    private RingBuffer<Holder> ringBuffer;

    /**
     * 事件监听器
     */
    private EventListener<E> eventListener;

    /**
     * 工作线程池
     */
    private WorkerPool<Holder> workerPool;

    /**
     * 线程池
     */
    private ExecutorService executorService;

    /**
     * 事件发布时，数据填充到事件对象中
     */
    private EventTranslatorOneArg<Holder, E> eventTranslator;

    public ParallelQueueHandler(Builder<E> builder) {
        this.executorService = Executors.newFixedThreadPool(builder.threads,
                new ThreadFactoryBuilder().setNameFormat("ParallelQueueHandler" + builder.namePrefix + "-pool-%d").build());
        this.eventListener = builder.eventListener;
        this.eventTranslator = new HoldEventTranslator();

        // 数据缓冲区初始化
        RingBuffer<Holder> ringBuffer = RingBuffer.create(builder.producerType, new HolderEventFactory(), builder.bufferSize, builder.waitStrategy);
        SequenceBarrier barrier = ringBuffer.newBarrier();

        // 创建消费者组
        WorkHandler<Holder>[] workHandlers = new WorkHandler[builder.threads];
        for (int i = 0; i < workHandlers.length; i++) {
            workHandlers[i] = new HolderWorkHandler();
        }

        // 创建消费者线程池
        WorkerPool<Holder> workerPool = new WorkerPool<Holder>(ringBuffer, barrier, new HolderExceptionHandler(), workHandlers);
        // 设置消费者序号便于统计消费进度
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
        this.workerPool = workerPool;
    }

    /**
     * 添加事件到缓冲队列
     */
    @Override
    public void add(E event) {
        final RingBuffer<Holder> holderRingBuffer = ringBuffer;
        if (holderRingBuffer == null) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler is shutdown now"), event);
        }
        try {
            this.ringBuffer.publishEvent(this.eventTranslator, event);
        } catch (Exception e) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler is shutdown now"), event);
        }
    }

    /**
     * 添加多个事件到缓冲队列
     */
    @Override
    public void add(E... events) {
        final RingBuffer<Holder> holderRingBuffer = ringBuffer;
        if (holderRingBuffer == null) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler is shutdown now"), events);
        }
        try {
            this.ringBuffer.publishEvents(this.eventTranslator, events);
        } catch (Exception e) {
            process(this.eventListener, new IllegalStateException("ParallelQueueHandler is shutdown now"), events);
        }
    }

    /**
     * 尝试添加一个事件到队列
     */
    @Override
    public boolean tryAdd(E event) {
        final RingBuffer<Holder> holderRingBuffer = ringBuffer;
        if (holderRingBuffer == null) {
            return false;
        }
        try {
            this.ringBuffer.tryPublishEvent(this.eventTranslator, event);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 尝试添加多个事件到队列
     */
    @Override
    public boolean tryAdd(E... events) {
        final RingBuffer<Holder> holderRingBuffer = ringBuffer;
        if (holderRingBuffer == null) {
            return false;
        }
        try {
            this.ringBuffer.tryPublishEvents(this.eventTranslator, events);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 启动队列
     */
    @Override
    public void start() {
        this.ringBuffer = workerPool.start(executorService);
    }

    /**
     * 关闭队列
     */
    @Override
    public void shutdown() {
        RingBuffer<Holder> holderRingBuffer = ringBuffer;
        ringBuffer = null;
        if (holderRingBuffer == null) {
            return;
        }
        if (workerPool != null) {
            workerPool.drainAndHalt();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * 是否关闭
     */
    @Override
    public boolean isShutDown() {
        return ringBuffer == null;
    }

    /**
     * 异常处理
     */
    private static <E> void process(EventListener<E> listener, Throwable ex, E event) {
        listener.onException(ex, -1, event);
    }

    private static <E> void process(EventListener<E> listener, Throwable ex, E... event) {
        for (E e : event) {
            process(listener, ex, e);
        }
    }


    /**
     * 事件对象
     */
    public class Holder {
        private E event;
        private void setValue(E event) {
            this.event = event;
        }

        @Override
        public String toString() {
            return "Holder{" +
                    "event=" + event +
                    '}';
        }
    }

    /**
     * 消费者事件处理器
     */
    private class HolderWorkHandler implements WorkHandler<Holder> {
        @Override
        public void onEvent(Holder holder) throws Exception {
            eventListener.onEvent(holder.event);
            holder.setValue(null);
        }
    }

    /**
     * 消费者异常处理器
     */
    private class HolderExceptionHandler implements ExceptionHandler<Holder> {

        @Override
        public void handleEventException(Throwable ex, long sequence, Holder event) {
            Holder holder = (Holder) event;
            try {
                eventListener.onException(ex, sequence, holder.event);
            } catch (Exception e) {
                // 消息队列异常处理逻辑.....
                // .......
            } finally {
                holder.setValue(null);
            }
        }

        @Override
        public void handleOnStartException(Throwable ex) {
            throw new UnsupportedOperationException(ex);
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    /**
     * 建造者模式
     * @param <E>
     */
    public static class Builder<E> {
        /**
         * 生产者类型
         */
        private ProducerType producerType = ProducerType.MULTI;
        /**
         * 线程队列大小
         */
        private int bufferSize = 1024 * 16;
        /**
         * 工作线程数
         */
        private int threads = 1;
        /**
         * 前缀定位
         */
        private String namePrefix = "";
        /**
         * 等待策略
         */
        private WaitStrategy waitStrategy = new BlockingWaitStrategy();
        /**
         * 监听器
         */
        private EventListener eventListener;

        public Builder<E> setProducerType(ProducerType producerType) {
            Preconditions.checkNotNull(producerType);
            this.producerType = producerType;
            return this;
        }

        public Builder<E> setBufferSize(int bufferSize) {
            Preconditions.checkArgument(Integer.bitCount(bufferSize) == 1);
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder<E> setThreads(int threads) {
            Preconditions.checkArgument(threads > 0);
            this.threads = threads;
            return this;
        }

        public Builder<E> setNamePrefix(String namePrefix) {
            Preconditions.checkNotNull(namePrefix);
            this.namePrefix = namePrefix;
            return this;
        }

        public Builder<E> setWaitStrategy(WaitStrategy waitStrategy) {
            Preconditions.checkNotNull(waitStrategy);
            this.waitStrategy = waitStrategy;
            return this;
        }

        public Builder<E> setEventListener(EventListener eventListener) {
            Preconditions.checkNotNull(eventListener);
            this.eventListener = eventListener;
            return this;
        }

        public ParallelQueueHandler<E> build() {
            return new ParallelQueueHandler<>(this);
        }
    }

    /**
     * 事件填充器
     */
    private class HoldEventTranslator implements EventTranslatorOneArg<Holder, E> {
        @Override
        public void translateTo(Holder event, long sequence, E arg0) {
            event.setValue(arg0);
        }
    }

    /**
     * 事件工厂，创建事件对象
     */
    private class HolderEventFactory implements EventFactory<Holder> {
        @Override
        public Holder newInstance() {
            return new Holder();
        }
    }
}
