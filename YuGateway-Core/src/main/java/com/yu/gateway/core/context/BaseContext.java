package com.yu.gateway.core.context;

import com.yu.gateway.common.config.Rule;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author yu
 * @description IContext接口的基本实现
 * @date 2024-04-01
 */
public class BaseContext implements IContext{
    /**
     * 后台协议
     */
    protected final String protocol;

    /**
     * 状态修饰
     */
    protected volatile ContextStatus status = ContextStatus.Running;

    /**
     * 请求异常
     */
    protected Throwable throwable;

    /**
     * 长连接
     */
    protected final boolean keepAlive;

    /**
     * 上下文相关参数
     */
    protected final Map<String, Object> attributes = new HashMap<String,Object>();

    /**
     * 回调函数集合
     */
    protected List<Consumer<IContext>> completedCallBacks;

    /**
     * 资源释放
     */
    protected final AtomicBoolean requestReleased = new AtomicBoolean(false);

    /**
     * netty上下文
     */
    protected final ChannelHandlerContext nettyCtx;

    public BaseContext(String protocol, boolean keepAlive, ChannelHandlerContext nettyCtx) {
        this.protocol = protocol;
        this.keepAlive = keepAlive;
        this.nettyCtx = nettyCtx;
    }

    @Override
    public void setContextStatus(ContextStatus status) {
        this.status = status;
    }

    @Override
    public boolean judgeContextStatus(ContextStatus status) {
        return this.status == status;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    /**
     * 获取请求转换规则
     *
     * @return
     */
    @Override
    public Rule getRule() {
        return null;
    }

    @Override
    public Object getRequest() {
        return null;
    }

    @Override
    public Object getResponse() {
        return null;
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public Object getAttribute(Map<String, Object> key) {
        return attributes.get(key);
    }

    @Override
    public ChannelHandlerContext getNettyContext() {
        return nettyCtx;
    }

    @Override
    public void setResponse() {

    }

    @Override
    public void setThrowable(Throwable throwable) {

    }

    @Override
    public void setAttribute(String key, Object value) {

    }

    @Override
    public boolean isKeepAlive() {
        return this.keepAlive;
    }

    @Override
    public void releaseRequest() {

    }

    @Override
    public void setCompletedCallBack(Consumer<IContext> consumer) {
        if (completedCallBacks == null) {
            completedCallBacks = new ArrayList<>();
        }
        completedCallBacks.add(consumer);
    }

    @Override
    public void invokeCompletedCallBacks() {
        if (completedCallBacks != null) {
            completedCallBacks.forEach(call->call.accept(this));
        }
    }
}
