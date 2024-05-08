package com.yu.gateway.common.utils.cipher;

import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

/**
 * URL AES加解密工具类
 */
public class CryptoHelper {
	/**
	 * URL AES解密
	 */
	public static String decryptUrl(String encryptUrl, String symmetricKey) {
		return AESUtil.decrypt(encryptUrl, symmetricKey);
	}

	/**
	 * 验证数字签名
	 */
	public static boolean verifySignature(MultiValueMap<String, String> queryParams, String signature, String symmetricKey) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
			if (!"signature".equals(entry.getKey())) {
				builder.append(entry.getKey()).append("=").append(String.join(",", entry.getValue())).append("&");
			}
		}
		builder.setLength(builder.length() - 1);
		String computedSignature = encryptParams(builder.toString(), symmetricKey).replace("+", " ");
		return computedSignature.equals(signature);
	}

	/**
	 * AES加密请求参数
	 */
	public static String encryptParams(String requestParams, String symmetricKey) {
		return AESUtil.encrypt(requestParams, symmetricKey);
	}
}
