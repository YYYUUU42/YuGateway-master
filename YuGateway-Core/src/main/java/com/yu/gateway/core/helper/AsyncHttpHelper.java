package com.yu.gateway.core.helper;

import org.asynchttpclient.*;

import java.util.concurrent.CompletableFuture;


/**
 * @author yu
 * @description 异步的 http 辅助类
 * @date 2024-04-06
 */
public class AsyncHttpHelper {

	/**
	 * 使用静态内部类实现单例模式
	 */
	private static final class SingletonHolder {
		private static final AsyncHttpHelper INSTANCE = new AsyncHttpHelper();
	}

	private AsyncHttpHelper() {}

	/**
	 * 提供一个公共方法，返回 SingletonHolder 中的 AsyncHttpHelper 实例
	 */
	public static AsyncHttpHelper getInstance() {
		return SingletonHolder.INSTANCE;
	}

	private AsyncHttpClient asyncHttpClient;

	public void initialized(AsyncHttpClient asyncHttpClient) {
		this.asyncHttpClient = asyncHttpClient;
	}

	/**
	 * 执行 HTTP 请求，并返回一个 CompletableFuture 对象
	 */
	public CompletableFuture<Response> executeRequest(Request request) {
		ListenableFuture<Response> future = asyncHttpClient.executeRequest(request);
		return future.toCompletableFuture();
	}

	/**
	 * 执行 HTTP 请求，并使用一个 AsyncHandler 处理响应，返回一个 CompletableFuture 对象
	 */
	public <T> CompletableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
		ListenableFuture<T> future = asyncHttpClient.executeRequest(request, handler);
		return future.toCompletableFuture();
	}
}
