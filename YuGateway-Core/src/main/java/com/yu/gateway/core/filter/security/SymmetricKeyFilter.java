package com.yu.gateway.core.filter.security;

import com.yu.gateway.common.utils.cipher.RSAUtil;
import com.yu.gateway.common.utils.jwt.JWTUtil;
import com.yu.gateway.common.utils.redis.JedisUtil;
import com.yu.gateway.core.context.GatewayContext;
import com.yu.gateway.core.filter.Filter;
import com.yu.gateway.core.filter.FilterAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;

import static com.yu.gateway.common.constant.FilterConst.*;

/**
 * @author yu
 * @description 保存 AES 密钥过滤器
 * @date 2024-05-08
 */
@Slf4j
@FilterAspect(id = SYMMETRIC_KEY_FILTER_ID, name = SYMMETRIC_KEY_FILTER_NAME, order = SYMMETRIC_KEY_FILTER_ORDER)
public class SymmetricKeyFilter implements Filter {

	@Override
	public void doFilter(GatewayContext ctx) throws Exception {
		// 从请求头中获取对称密钥并保存到 Redis 中
		String encryptSymmetricKey = ctx.getRequest().getHeaders().get(SYMMETRIC_KEY_PUBLIC_KEY);
		if (encryptSymmetricKey != null) {
			try {
				JedisUtil jedis = new JedisUtil();
				String userId = getUserId(ctx);
				String securityKey = SECURITY_KEY_PREFIX + ":" + userId;

				// 从 Redis 中获取所有的密钥
				Set<String> keys = jedis.listScoreSetString(securityKey, 0, -1, true);
				String privateKey = null;

				// 寻找私钥
				for (String key : keys) {
					if (key.startsWith(RSA_PRIVATE_KEY_PREFIX)) {
						privateKey = key.substring(RSA_PRIVATE_KEY_PREFIX.length() + 1);
					}
				}
				log.info("Get rsa-privateKey from redis {}", privateKey);

				// 如果私钥为空，则抛出异常
				if (StringUtils.isEmpty(privateKey)) {
					throw new RuntimeException("PrivateKey is empty in SymmetricKeyFilter");
				}

				// 解密 RSA 并获取 AES 密钥
				String symmetricPublicKey = RSAUtil.decryptPrivateKey(encryptSymmetricKey, privateKey);
				log.info("After decryptSymmetric, symmetric: {}", symmetricPublicKey);

				// 将 AES 密钥保存到 Redis 中
				// security:key:{userId}    symmetric:key:{symmetricKey}     {symmetric-expireTime}
				String symmetricKey = SYMMETRIC_KEY_PREFIX + ":" + symmetricPublicKey;

				// 检查 AES 密钥的过期时间，如果需要则创建新的
				if (jedis.isExistScoreSet(securityKey, symmetricKey)) {
					long expireTime = (long) jedis.getScore(securityKey, symmetricKey).doubleValue();
					if (expireTime > System.currentTimeMillis()) {
						return;
					}
				}
				if (!jedis.addScoreSet(securityKey, symmetricKey, System.currentTimeMillis() + SYMMETRIC_KEY_EXPIRE_TIME)) {
					throw new RuntimeException("save symmetricKey into redis failed");
				}
			} catch (Exception e) {
				throw new RuntimeException("SymmetricFilter decrypt symmetric failed, throws: " + e.getMessage());
			}
		}
	}

	/**
	 * 从上下文中获取用户ID
	 */
	public String getUserId(GatewayContext ctx) {
		String token = ctx.getRequest().getCookie(COOKIE_KEY).value();
		if (StringUtils.isEmpty(token)) {
			throw new RuntimeException("SymmetricKeyFilter token is null");
		}

		String userId = (String) Objects.requireNonNull(JWTUtil.getClaimByToken(token, TOKEN_SECRET)).get(TOKEN_USERID_KEY);
		if (ObjectUtils.isEmpty(token)) {
			throw new RuntimeException("token is empty");
		}
		return userId;
	}
}
