package com.yu.gateway.common.config;

/**
 * @author yu
 * @description 抽象的服务调用接口实现类
 * @date 2024-03-31
 */
public class AbstractServiceInvoker implements ServiceInvoker {
	
	protected String invokerPath;
	
	protected int timeout = 5000;

	@Override
	public String getInvokerPath() {
		return invokerPath;
	}

	@Override
	public void setInvokerPath(String invokerPath) {
		this.invokerPath = invokerPath;
	}

	/**
	 * 获取服务方法调用超时时间
	 */
	@Override
	public long getTimeOut() {
		return 0;
	}

	/**
	 * 设置该服务调用(方法)的超时时间
	 */
	@Override
	public void setTimeOut(long timeout) {

	}


}
