package com.yu.gateway.common.constant;

/**
 * @author yu
 * @description 网关协议
 * @date 2024-04-01
 */
public interface GatewayProtocol {
	String HTTP = "http";
	String DUBBO = "dubbo";

	/**
	 * 判断是否是http协议
	 *
	 * @param protocol
	 * @return
	 */
	static boolean isHttp(String protocol) {
		return HTTP.equals(protocol);
	}

	/**
	 * 判断是否是dubbo协议
	 *
	 * @param protocol
	 * @return
	 */
	static boolean isDubbo(String protocol) {
		return DUBBO.equals(protocol);
	}
}
