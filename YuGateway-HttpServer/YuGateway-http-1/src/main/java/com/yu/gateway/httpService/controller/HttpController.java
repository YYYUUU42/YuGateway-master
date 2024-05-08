package com.yu.gateway.httpService.controller;

import com.yu.gateway.client.api.ApiInvoker;
import com.yu.gateway.client.api.ApiProperties;
import com.yu.gateway.client.api.ApiProtocol;
import com.yu.gateway.client.api.ApiService;
import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.common.utils.cipher.RSAUtil;
import com.yu.gateway.common.utils.jwt.JWTUtil;
import com.yu.gateway.common.utils.redis.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.stream.Stream;


@RestController
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, patternPath = "/http-server/**")
@Slf4j
public class HttpController {

	@Autowired
	private ApiProperties apiProperties;

	/**
	 * ping 测试
	 */
	@ApiInvoker(path = "/http-server/ping")
	@GetMapping("/http-server/ping")
	public String ping() {
		log.info("{}", apiProperties);
		return "pong1";
	}

	/**
	 * 模拟灰度发布的服务对象
	 */
	@ApiInvoker(path = "/http-server/gray")
	@GetMapping("/http-server/gray")
	public String grayRelease() {
		log.info("start exec gray release service");
		return "gray exec success";
	}

	/**
	 * 给前端发送 RSA 公钥
	 */
	@ApiInvoker(path = "/http-server/public-key")
	@GetMapping("/http-server/public-key")
	public String getRSASecretKey(HttpServletRequest request) {
		//获得秘钥
		RSAUtil.KeyPairInfo keyPair = RSAUtil.getKeyPair();

		//通过 token 得到 userId
		Cookie token = Stream.of(request.getCookies())
				.filter(cookie -> cookie.getName().equals(FilterConst.COOKIE_KEY))
				.findFirst()
				.orElse(null);
		if (ObjectUtils.isEmpty(token)) {
			throw new RuntimeException("token is empty");
		}
		String userId = String.valueOf(Objects.requireNonNull(JWTUtil.getClaimByToken(token.getValue(), FilterConst.TOKEN_SECRET)).get(FilterConst.TOKEN_USERID_KEY));
		if (StringUtils.isEmpty(userId)) {
			throw new RuntimeException("parse token failed");
		}

		//将 RSA 私钥放进 redis（ZSet） 中
		// 这里使用 ZSet 数据结构来记录后端为不同用户生成的 RSA私钥，因为不同用户创建私钥的时间不同导致过期时间不同，hash无法为内部元素设置过期时间
		JedisUtil jedis = new JedisUtil();

		//security:key:{userId}    rsa:key:{rsa-privateKey}     {rsa-expireTime}
		String securityKey = FilterConst.SECURITY_KEY_PREFIX + ":" + userId;
		String rsaPrivateKey = FilterConst.RSA_PRIVATE_KEY_PREFIX + ":" + keyPair.getPrivateKey();
		long rsaExpireTime = System.currentTimeMillis() + FilterConst.RSA_PRIVATE_KEY_EXPIRE_TIME;

		if (!jedis.addScoreSet(securityKey, rsaPrivateKey, rsaExpireTime)) {
			throw new RuntimeException("save rsa-private-key into redis failed");
		}

		log.info("save rsa-privateKey into redis success, key: {}, expire: {}", FilterConst.RSA_PRIVATE_KEY_PREFIX, FilterConst.RSA_PRIVATE_KEY_EXPIRE_TIME);
		jedis.setExpire(FilterConst.RSA_PRIVATE_KEY_PREFIX, FilterConst.RSA_PRIVATE_KEY_EXPIRE_TIME);

		log.info("create rsa-publicKey: {}, rsa-privateKey: {}", keyPair.getPublicKey(), keyPair.getPrivateKey());

		return keyPair.getPublicKey();
	}
}