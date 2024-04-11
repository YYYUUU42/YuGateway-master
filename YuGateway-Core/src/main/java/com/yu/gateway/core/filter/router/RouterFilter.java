package com.yu.gateway.core.filter.router;

import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesFactory;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.enums.ResponseCode;
import com.yu.gateway.common.exception.ConnectException;
import com.yu.gateway.common.exception.ResponseException;
import com.yu.gateway.core.ConfigLoader;
import com.yu.gateway.core.context.ContextStatus;
import com.yu.gateway.core.context.GatewayContext;
import com.yu.gateway.core.filter.Filter;
import com.yu.gateway.core.filter.FilterAspect;
import com.yu.gateway.core.helper.AsyncHttpHelper;
import com.yu.gateway.core.helper.ResponseHelper;
import com.yu.gateway.core.response.GatewayResponse;
import io.netty.handler.timeout.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.yu.gateway.common.constant.FilterConst.*;


/**
 * @author yu
 * 路由过滤器 执行路由转发操作
 * 1.最终的过滤器组件，用于向下游服务转发请求；
 * 2.请求异常重试；
 * 3.服务熔断降级；
 * 4.以上配置均支持配置中心动态更新。
 * @date 2024-04-10
 */
@Slf4j
@FilterAspect(id = ROUTER_FILTER_ID, name = ROUTER_FILTER_NAME, order = ROUTER_FILTER_ORDER)
public class RouterFilter implements Filter {

	private ConcurrentHashMap<String, RouterHystrixCommand> commandMap = new ConcurrentHashMap<>();

	/**
	 * 执行过滤器
	 */
	@Override
	public void doFilter(GatewayContext gatewayContext) throws Exception {
		//首先获取熔断降级的配置
		Optional<Rule.HystrixConfig> hystrixConfig = getHystrixConfig(gatewayContext);
		//如果存在对应配置就走熔断降级的逻辑
		if (hystrixConfig.isPresent()) {
			routeWithHystrix(gatewayContext, hystrixConfig);
		} else {
			route(gatewayContext, hystrixConfig);
		}
	}

	/**
	 * 获取 hystrix 的配置
	 * 1.对比请求路径和注册中心注册的路径参数；
	 * 2.判断当前请求是否需要走熔断策略分支；
	 */
	private static Optional<Rule.HystrixConfig> getHystrixConfig(GatewayContext gatewayContext) {
		Rule rule = gatewayContext.getRules();
		Optional<Rule.HystrixConfig> hystrixConfig = rule.getHystrixConfigs().stream()
				.filter(c -> StringUtils.equals(c.getPath(), gatewayContext.getRequest().getPath()))
				.findFirst();

		return hystrixConfig;
	}

	/**
	 * 默认路由逻辑：
	 * 根据 whenComplete 	判断执行回调的线程是否阻塞执行；
	 * whenComplete 		当异步操作完成时（无论成功还是失败），会立即执行回调函数；
	 * whenCompleteAsync 	当异步操作完成时，会创建一个新的异步任务来执行回调函数。
	 */
	private CompletableFuture<Response> route(GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
		// 异步请求发送
		Request request = gatewayContext.getRequest().build();
		CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);
		boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();

		// 单异步/双异步模型
		if (whenComplete) {
			future.whenComplete(((response, throwable) -> {
				complete(request, response, throwable, gatewayContext);
			}));
		} else {
			future.whenCompleteAsync(((response, throwable) -> {
				complete(request, response, throwable, gatewayContext);
			}));
		}
		return future;
	}

	/**
	 * 熔断降级请求策略：
	 * 1.命令执行超过配置超时时间；
	 * 2.命令执行出现异常或错误；
	 * 3.连续失败率达到配置的阈值；
	 */
	private void routeWithHystrix(GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
		String key = gatewayContext.getUniqueId() + "." + gatewayContext.getRequest().getPath();
		RouterHystrixCommand proxyCommand = null;
		if (commandMap.containsKey(key)) {
			proxyCommand = commandMap.get(key);
			if (!hystrixConfig.get().equals(commandMap.get(key))) {
				log.info("previous HystrixCommand instance hashCode: {}", proxyCommand.hashCode());
				proxyCommand.updateHystrixCommandProperties(proxyCommand.getCommandKey().name());
				proxyCommand = new RouterHystrixCommand(gatewayContext, hystrixConfig);

				log.info("after HystrixCommand instance hashCode: {}", proxyCommand.hashCode());
				commandMap.put(key, proxyCommand);
			}
		} else {
			proxyCommand = new RouterHystrixCommand(gatewayContext, hystrixConfig);
			commandMap.put(key, proxyCommand);
		}
		proxyCommand.execute();
	}

	/**
	 * 响应回调处理
	 */
	private void complete(Request request, Response response, Throwable throwable, GatewayContext gatewayContext) {
// 请求已经处理完毕 释放请求资源
		gatewayContext.releaseRequest();
		// 获取上下文请求配置规则
		Rule rule = gatewayContext.getRules();
		// 获取重试次数
		int currentRetryTimes = gatewayContext.getCurrentRetryTimes();
		int confRetryTimes = rule.getRetryConfig().getTimes();
		// 异常重试
		if ((throwable instanceof TimeoutException || throwable instanceof IOException) &&
				currentRetryTimes <= confRetryTimes) {
			doRetry(gatewayContext, currentRetryTimes);
		}
		String url = request.getUrl();
		try {
			if (Objects.nonNull(throwable)) {
				if (throwable instanceof TimeoutException) {
					log.warn("complete timeout {}", url);
					gatewayContext.setThrowable(throwable);
					gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.REQUEST_TIMEOUT));
				} else {
					gatewayContext.setThrowable(new ConnectException(throwable, gatewayContext.getUniqueId(), url, ResponseCode.HTTP_RESPONSE_ERROR));
					gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
				}
			} else {
				gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(response));
			}
		} catch (Throwable t) {
			gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
			gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
			log.error("complete process failed", t);
		} finally {
			gatewayContext.setContextStatus(ContextStatus.Written);
			ResponseHelper.writeResponse(gatewayContext);
			log.info("{} {} {} {} {} {} {}",
					System.currentTimeMillis() - gatewayContext.getRequest().getBeginTime(),
					gatewayContext.getRequest().getClientIp(),
					gatewayContext.getRequest().getUniqueId(),
					gatewayContext.getRequest().getMethod(),
					gatewayContext.getRequest().getPath(),
					gatewayContext.getResponse().getHttpResponseStatus().code(),
					gatewayContext.getResponse().getFutureResponse().getResponseBodyAsBytes().length);
		}
	}

	/**
	 * 重试策略
	 */
	private void doRetry(GatewayContext gatewayContext, int retryTimes) {
		gatewayContext.setCurrentRetryTimes(retryTimes + 1);
		try {
			// 重新执行过滤器逻辑
			doFilter(gatewayContext);
		} catch (Exception e) {
			log.warn("重试请求失败, requestId={}", gatewayContext.getUniqueId(), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Hystrix命令集合
	 */
	private class RouterHystrixCommand extends HystrixCommand<Object> {
		private GatewayContext context;
		private Optional<Rule.HystrixConfig> config;

		public RouterHystrixCommand(GatewayContext context, Optional<Rule.HystrixConfig> config) {
			super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(context.getUniqueId()))
					.andCommandKey(HystrixCommandKey.Factory.asKey(context.getRequest().getPath()))
					.andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
							// 核心线程数
							.withCoreSize(config.get().getCoreThreadSize()))
					.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
							// 线程隔离类型
							.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
							// 命令执行超时
							.withExecutionTimeoutInMilliseconds(config.get().getTimeoutInMilliseconds())
							// 超时中断
							.withExecutionIsolationThreadInterruptOnTimeout(true)
							.withExecutionTimeoutEnabled(true)));

			this.config = config;
			this.context = context;
		}

		@Override
		protected Object run() throws Exception {
			// 实际路由操作
			route(context, config).get();
			return null;
		}

		/**
		 * 熔断降级操作
		 */
		@Override
		protected Object getFallback() {
			// 是否是超时引发的熔断
			if (isFailedExecution() || getExecutionException() instanceof HystrixTimeoutException) {
				// 针对超时的异常处理
				context.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.GATEWAY_FALLBACK_TIMEOUT));
			} else {
				// 其它类型异常熔断处理
				context.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.GATEWAY_FALLBACK_ERROR, config.get().getFallbackResponse()));
			}
			context.setContextStatus(ContextStatus.Written);
			return null;
		}

		/**
		 * 动态更新 CommandProperties 配置
		 * 1.因为 Hystrix 内部使用了缓存，如果仅仅修改 HystrixCommand.Setter 是没有用的；
		 * 2.利用反射获取 HystrixPropertiesFactory 的 commandProperties 字段，并更新
		 */
		protected void updateHystrixCommandProperties(String commandKey) {
			try {
				Field field = HystrixPropertiesFactory.class.getDeclaredField("commandProperties");
				field.setAccessible(true);
				ConcurrentHashMap<String, HystrixCommandProperties> commandProperties = (ConcurrentHashMap<String, HystrixCommandProperties>) field.get(null);
				System.out.println(commandProperties.toString());
				commandProperties.remove(commandKey);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				log.error("Remove cache in HystrixCommandFactory failed, commandKey: {}", commandKey, e);
			}
		}
	}

}
