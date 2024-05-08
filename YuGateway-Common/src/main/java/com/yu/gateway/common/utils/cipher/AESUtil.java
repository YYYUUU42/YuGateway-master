package com.yu.gateway.common.utils.cipher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AES对称加密工具类
 */
@Slf4j
public class AESUtil {
	private static final String KEY_ALGORITHM = "AES";

	/**
	 * 默认的加密算法
	 */
	private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

	/**
	 * AES 加密操作
	 *
	 * @param content  待加密内容
	 * @param password 加密密码
	 * @return 返回Base64转码后的加密数据
	 */
	public static String encrypt(String content, String password) {
		try {
			// 创建密码器
			Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);

			byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);

			// 初始化为加密模式的密码器
			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(password));

			// 加密
			byte[] result = cipher.doFinal(byteContent);

			//通过Base64转码返回
			return Base64Utils.encodeToString(result);
		} catch (Exception ex) {
			Logger.getLogger(AESUtil.class.getName()).log(Level.SEVERE, null, ex);
		}

		return null;
	}

	/**
	 * AES 解密操作
	 */
	public static String decrypt(String content, String password) {
		try {
			//实例化
			Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);

			//使用密钥初始化，设置为解密模式
			cipher.init(Cipher.DECRYPT_MODE, getSecretKey(password));

			//执行操作
			byte[] result = cipher.doFinal(Base64Utils.decodeFromString(content));

			return new String(result, StandardCharsets.UTF_8);
		} catch (Exception ex) {
			Logger.getLogger(AESUtil.class.getName()).log(Level.SEVERE, null, ex);
		}

		return null;
	}

	/**
	 * 生成加密秘钥
	 */
	private static SecretKeySpec getSecretKey(String password) {
		//返回生成指定算法密钥生成器的 KeyGenerator 对象
		KeyGenerator kg = null;
		try {
			kg = KeyGenerator.getInstance(KEY_ALGORITHM);

			//AES 要求密钥长度为 128
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(password.getBytes());
			kg.init(128, secureRandom);

			//生成一个密钥
			SecretKey secretKey = kg.generateKey();

			// 转换为AES专用密钥
			return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(AESUtil.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
}
