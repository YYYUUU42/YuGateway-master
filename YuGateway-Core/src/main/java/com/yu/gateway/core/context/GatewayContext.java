package com.yu.gateway.core.context;

import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.utils.AssertUtil;
import com.yu.gateway.core.request.GatewayRequest;
import com.yu.gateway.core.response.GatewayResponse;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;

/**
 *  网关上下文对象，封装以下：
 *  1）request-object；
 *  2）response-object；
 *  3）请求规则；
 *  4）灰度发布、请求重试次数等；
 */
@Getter
@Setter
public class GatewayContext extends BaseContext {
    /**
     * 自定义协议请求体
     */
    private GatewayRequest request;

    /**
     * 自定义协议响应体
     */
    private GatewayResponse response;

    /**
     * url映射规则
     */
    private Rule rules;

    /**
     * 灰度发布
     */
    private boolean gray;

    /**
     * 最大重试次数
     */
    private int currentRetryTimes;

    /**
     * 记录应用程序中的方法调用或服务请求所花费的时间
     */
    @Setter
    @Getter
    private Timer.Sample timerSample;

    public static Builder newBuilder() {
        return new Builder();
    }

    public GatewayContext(String protocol, boolean keepAlive, ChannelHandlerContext nettyCtx, GatewayRequest request, Rule rules, int currentRetryTimes) {
        super(protocol, keepAlive, nettyCtx);
        this.request = request;
        this.rules = rules;
        this.currentRetryTimes = currentRetryTimes;
    }

    /**
     * 静态内部类建造者模式
     */
    public static class Builder {
        private String protocol;
        private ChannelHandlerContext nettyCtx;
        private GatewayRequest request;
        private Rule rule;
        private boolean keepAlive;
        private int currentRetryTimes = 0;

        public Builder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setNettyCtx(ChannelHandlerContext nettyCtx) {
            this.nettyCtx = nettyCtx;
            return this;
        }

        public Builder setRequest(GatewayRequest request) {
            this.request = request;
            return this;
        }

        public Builder setRule(Rule rule) {
            this.rule = rule;
            return this;
        }

        public Builder setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public Builder setCurrentRetryTimes(int currentRetryTimes) {
            this.currentRetryTimes = currentRetryTimes;
            return this;
        }

        public GatewayContext build() {
            AssertUtil.notNull(protocol, "protocol can't be empty");
            AssertUtil.notNull(nettyCtx, "nettyCtx can't be empty");
            AssertUtil.notNull(rule, "rule can't be empty");
            return new GatewayContext(protocol, keepAlive, nettyCtx, request, rule, currentRetryTimes);
        }
    }

    public Rule.FilterConfig getFilterConfig(String filterId) {
        return rules.getFilterConfigById(filterId);
    }

    public String getUniqueId() {
        return request.getUniqueId();
    }

    /**
     *  资源释放,减少引用计数
     */
    @Override
    public void releaseRequest() {
        if (requestReleased.compareAndSet(false, true)) {
            ReferenceCountUtil.release(request.getFullHttpRequest());
        }
    }

    /**
     * 获取指定 key 的上下文参数
     */
    public <T> T getRequireAttribute(String key, T defaultValue) {
        return (T) attributes.getOrDefault(key, defaultValue);
    }
}
