package com.yu.gateway.common.config;

/**
 * @author yu
 * @description 服务调用的接口模型描述
 * @date 2024-03-31
 */
public interface ServiceInvoker {

	/**
	 * 获取真正的服务调用的全路径
	 * @return
	 */
	String getInvokerPath();

	/**
	 * 设置真正的服务调用的全路径
	 * @param invokerPath
	 */
	void setInvokerPath(String invokerPath);

	/**
	 * 获取该服务调用(方法)的超时时间
	 * @return
	 */
	int getTimeout();

	/**
	 * 设置该服务调用(方法)的超时时间
	 * @param timeout
	 */
	void setTimeout(int timeout);
	
}
