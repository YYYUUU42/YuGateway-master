package com.yu.gateway.core.filter.auth;

import com.alibaba.fastjson.JSON;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.common.enums.ResponseCode;
import com.yu.gateway.common.exception.ResponseException;
import com.yu.gateway.common.utils.jwt.JWTUtil;
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
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.Map;

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
        // 判断是否需要执行认证过滤
        Iterator<Rule.FilterConfig> iterator = ctx.getRules().getFilterConfigs().iterator();
        while (iterator.hasNext()) {
            Rule.FilterConfig config = iterator.next();

            // 使用精确匹配来提高安全性
            if (!config.getId().equals(FilterConst.AUTH_FILTER_ID)) {
                continue;
            }

            Map<String, String> configMap = JSON.parseObject(config.getConfig(), Map.class);
            String authPath = configMap.get(FilterConst.AUTH_FILTER_KEY);
            String curRequestKey = ctx.getRequest().getPath();

            // 确保路径匹配是必要的
            if (!authPath.equals(curRequestKey)) {
                return;
            }

            // 解析负载
            Cookie cookie = ctx.getRequest().getCookie(FilterConst.COOKIE_KEY);
            String token = cookie != null ? cookie.value() : null;
            if (StringUtils.isEmpty(token)) {
                throw new ResponseException(ResponseCode.UNAUTHORIZED);
            }

            // jwt认证并解析载荷——UserId
            try {
                long userId = (Long) JWTUtil.getClaimByToken(token, FilterConst.TOKEN_SECRET).get(FilterConst.TOKEN_USERID_KEY);
                ctx.getRequest().setUserId(userId);
                log.info("AuthFilter 解析 token 成功, userId {}", userId);
            } catch (Exception e) {
                log.info("AuthFilter 解析 token 失败, 请求路径 {}", ctx.getRequest().getPath());
                throw new ResponseException(ResponseCode.UNAUTHORIZED);
            }
        }
        return;
    }

    /**
     * 解析token中的载荷——用户ID
     * @param token
     * @return
     */
    private long parseUserIdFromToken(String token) {
        // 检查输入token是否为空或null
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty.");
        }

        Jwt jwt;
        try {
            // 使用静态解析器实例，提高性能
            jwt = Jwts.parser().setSigningKey(FilterConst.TOKEN_SECRET).parse(token);
        } catch (SignatureException | ExpiredJwtException e) {
            // 记录异常信息，可以更详细地记录问题，这里简化为日志输出
            logger.error("Token 验证错误: ", e);
            throw new RuntimeException("无效 token.", e);
        }

        String subject = ((DefaultClaims) jwt.getBody()).getSubject();
        try {
            // 验证字符串是否可以转换为long类型，并检查范围
            long userId = Long.parseLong(subject);
            if (userId == Long.MIN_VALUE || userId == Long.MAX_VALUE) {
                throw new IllegalArgumentException("UseId 超出范围.");
            }
            return userId;
        } catch (NumberFormatException e) {
            Logger logger = LoggerFactory.getLogger(this.getClass());
            logger.error("UserId 解析错误: ", e);
            throw new IllegalArgumentException("无效 userId 格式");
        }
    }
}
