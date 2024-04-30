package com.yu.gateway.core.filter.auth;

import com.alibaba.fastjson.JSON;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.common.enums.ResponseCode;
import com.yu.gateway.common.exception.ResponseException;
import com.yu.gateway.core.context.GatewayContext;
import com.yu.gateway.core.filter.Filter;
import com.yu.gateway.core.filter.FilterAspect;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.DefaultClaims;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yu
 * @description 鉴权过滤器
 * @date 2024-04-01
 */
@Slf4j
@FilterAspect(id= FilterConst.AUTH_FILTER_ID, name = FilterConst.AUTH_FILTER_NAME, order = FilterConst.AUTH_FILTER_ORDER)
public class AuthFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    @Override
	public void doFilter(GatewayContext ctx) throws Exception {
		// 遍历所有的过滤器配置
		for (Rule.FilterConfig config : ctx.getRules().getFilterConfigs()) {
			// 如果当前的过滤器ID不是我们需要的过滤器ID，那么就跳过这个过滤器配置
			if (!config.getId().equals(FilterConst.AUTH_FILTER_ID)) {
				continue;
			}


			// 解析过滤器配置，获取到我们需要的认证路径
			List<String> authPaths = new ArrayList<>();
			Map<String, List<String>> configMap = new ConcurrentHashMap<>();

			if (config.getConfig() != null) {
				configMap = JSON.parseObject(config.getConfig(), Map.class);
				authPaths = configMap.getOrDefault(FilterConst.AUTH_FILTER_KEY, new ArrayList<>());
			}

			// 获取当前请求的路径
			String curRequestKey = ctx.getRequest().getPath();

			// 如果当前请求的路径不是我们需要的认证路径，那么就返回，不进行后续的处理
			if (!authPaths.contains(curRequestKey)) {
				return;
			}

			// 从请求中获取token，如果token不存在，那么就抛出一个未授权的异常
			String token = Optional.ofNullable(ctx.getRequest().getCookie(FilterConst.COOKIE_KEY))
					.map(Cookie::value)
					.orElseThrow(() -> new ResponseException(ResponseCode.UNAUTHORIZED));

			// 对获取到的token进行验证
			authenticateToken(ctx, token);
		}
	}

	/**
	 * 验证token
	 */
	private void authenticateToken(GatewayContext ctx, String token) {
		try {
			long tokenUserId = parseUserIdFromToken(token);

			String headerUserId = ctx.getRequest().getHeaders().get("userId");
			String pathUserId = ctx.getRequest().getQueryParametersMultiple("userId").get(0);
			String actualUserId = headerUserId != null ? headerUserId : pathUserId;

			if (actualUserId == null || Long.parseLong(actualUserId) != tokenUserId) {
				throw new ResponseException(ResponseCode.USERID_MISMATCH);
			}

			ctx.getRequest().setUserId(tokenUserId);
			log.info("AuthFilter 解析 token 成功, userId {}", tokenUserId);
		} catch (Exception e) {
			log.error("AuthFilter 解析 token 失败, 请求路径 {}", ctx.getRequest().getPath());
			throw new ResponseException(ResponseCode.USERID_MISMATCH);
		}
	}


	/**
	 * 解析token中的载荷——用户ID
	 */
	private long parseUserIdFromToken(String token) {
		// 使用Optional来处理可能为null的值
		Optional.ofNullable(token)
				.filter(t -> !t.isEmpty())
				.orElseThrow(() -> new IllegalArgumentException("Token cannot be null or empty."));

		Jwt jwt = null;

		try {
			// 使用静态解析器实例，提高性能
			jwt = Jwts.parser().setSigningKey(FilterConst.TOKEN_SECRET).parse(token);
		} catch (SignatureException | ExpiredJwtException e) {
			throw new RuntimeException("Token 验证错误: ", e);
		}

		try {
			// 验证字符串是否可以转换为long类型，并检查范围
			DefaultClaims claims = (DefaultClaims) jwt.getBody();
			String jwtUserId = claims.get("userId", String.class);
			long userId = Long.parseLong(jwtUserId);

			if (userId == Long.MIN_VALUE || userId == Long.MAX_VALUE) {
				throw new IllegalArgumentException("UseId 超出范围.");
			}
			return userId;
		} catch (NumberFormatException e) {
			logger.error("UserId 解析错误: ", e);
			throw new IllegalArgumentException("无效 userId 格式");
		}

	}


}
