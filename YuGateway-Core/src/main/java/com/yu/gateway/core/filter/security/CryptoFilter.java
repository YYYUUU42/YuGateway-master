package com.yu.gateway.core.filter.security;

import com.alibaba.fastjson.JSON;
import com.yu.gateway.common.config.Rule;
import com.yu.gateway.common.utils.cipher.CryptoHelper;
import com.yu.gateway.common.utils.jwt.JWTUtil;
import com.yu.gateway.common.utils.redis.JedisUtil;
import com.yu.gateway.core.context.GatewayContext;
import com.yu.gateway.core.filter.Filter;
import com.yu.gateway.core.filter.FilterAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;


import java.util.*;

import static com.yu.gateway.common.constant.FilterConst.*;

/**
 * @author yu
 * @description 处理加密的 URL和验证 URL 的完整性
 * @date 2024-05-08
 */
@Slf4j
@FilterAspect(id = CRYPTO_FILTER_ID, name = CRYPTO_FILTER_NAME, order = CRYPTO_FILTER_ORDER)
public class CryptoFilter implements Filter {
	@Override
	public void doFilter(GatewayContext ctx) throws Exception {
		// 遍历所有的过滤器配置，查找ID为CRYPTO_FILTER_ID的配置
		for (Rule.FilterConfig config : ctx.getRules().getFilterConfigs()) {
			if (!config.getId().equalsIgnoreCase(CRYPTO_FILTER_ID)) {
				continue;
			}

			// 解析配置的JSON字符串，获取白名单列表
			Map<String, List<String>> configMap = JSON.parseObject(config.getConfig(), Map.class);
			List<String> white_lists = configMap.get(WHITE_LIST_KEY);

			// 如果请求的路径在白名单中，那么直接返回，不进行后续的处理
			if (white_lists.contains(ctx.getRequest().getPath())) {
				return;
			}
		}

		// 从Redis中获取对称密钥
		JedisUtil jedis = new JedisUtil();
		String userId = (String) JWTUtil.getClaimByToken(ctx.getRequest().getCookie(COOKIE_KEY).value(), TOKEN_SECRET).get(TOKEN_USERID_KEY);
		String securityKey = SECURITY_KEY_PREFIX + ":" + userId;
		String symmetricPublicKey = null;

		// 获取所有的对称密钥
		Set<String> securityKeyLists = jedis.listScoreSetString(securityKey, 0, -1, true);
		for (String key : securityKeyLists) {
			if (key.startsWith(SYMMETRIC_KEY_PREFIX)) {
				symmetricPublicKey = key;
				break;
			}
		}
		if (symmetricPublicKey == null) {
			log.error("symmetricPublicKey load from redis is null");
			return;
		}

		// 解码URL并验证URL的完整性
		String encryptUrl = ctx.getRequest().getUri();
		String path = ctx.getRequest().getPath();
		String encryptPathParams = path.substring(path.indexOf("/encrypt/") + 9);
		String decryptedPathParam = CryptoHelper.decryptUrl(encryptPathParams, symmetricPublicKey);
		String decryptUri = encryptUrl.substring(0, encryptUrl.indexOf("/encrypt/")).concat("?").concat(decryptedPathParam);

		// 修改URI和查询参数解码器
		ctx.getRequest().setUri(decryptUri);
		ctx.getRequest().setQueryStringDecoder(decryptUri);

		// 验证请求参数
		Map<String, List<String>> decryptedPathParams = ctx.getRequest().getQueryStringDecoder().parameters();
		if (decryptedPathParams.size() < 2) {
			throw new RuntimeException("decryptedPathParams size is too small");
		}
		String signature = decryptedPathParams.get("signature").get(0);
		if (!CryptoHelper.verifySignature(new LinkedMultiValueMap<>(decryptedPathParams), signature, symmetricPublicKey)) {
			throw new RuntimeException("the param has something wrong");
		}

		String decryptQueryParams = CryptoHelper.decryptUrl(signature.replace(" ", "+"), symmetricPublicKey);
		ctx.getRequest().setModifyPath(ctx.getRequest().getQueryStringDecoder().path() + "?" + decryptQueryParams);
	}
}
