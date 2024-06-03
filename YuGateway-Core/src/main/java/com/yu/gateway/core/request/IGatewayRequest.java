package com.yu.gateway.core.request;

import org.asynchttpclient.Request;
import org.asynchttpclient.cookie.Cookie;


/**
 * @author yu
 * @description 请求对象
 * @date 2024-03-31
 */
public interface IGatewayRequest {

	/**
	 * 修改域名
	 */
	void setModifyHost(String host);


	/**
	 * 获取修改后的域名
	 */
	String getModifyHost();

	/**
	 * 设置/获取路径
	 */
	void setModifyPath(String path);

	/**
	 * 获取修改后的路径
	 */
	String getModifyPath();

	/**
	 * 添加请求头信息
	 */
	void addHeader(CharSequence name, String value);

	/**
	 * 设置请求头信息
	 */
	void setHeader(CharSequence name, String value);

	/**
	 * Get 请求参数
	 */
	void addQueryParam(String name, String value);

	/**
	 * POST 请求参数
	 */
	void addFormParam(String name, String value);

	/**
	 * 添加或者替换Cookie
	 */
	void addOrReplaceCookie(Cookie cookie);

	/**
	 * 设置请求超时时间
	 */
	void setRequestTimeout(int requestTimeout);

	/**
	 * 获取最终的请求路径
	 */
	String getFinalUrl();

	/**
	 * 构造最终的请求对象
	 */
	Request build();

}
