package com.yu.gateway.common.utils.cipher;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 非对称加密工具类
 */
@Slf4j
public class RSAUtil {
	/**
	 * RSA密钥长度必须为 64 的倍数（512~65536）,默认为 1024
	 */
	public static final int KEY_SIZE = 2048;

	public static KeyPairInfo getKeyPair() {
		return getKeyPair(KEY_SIZE);
	}

	/**
	 * 生成公私钥对
	 */
	public static KeyPairInfo getKeyPair(int keySize) {
		try {
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
			keyPairGen.initialize(keySize);

            // 生成一个密钥对，保存在keyPair中
			KeyPair keyPair = keyPairGen.generateKeyPair();

            // 得到私钥
			RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();

            // 得到公钥
			RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();

			KeyPairInfo pairInfo = new KeyPairInfo(keySize);

			//公钥
			byte[] publicKeyByte = rsaPublicKey.getEncoded();
			String publicKeyString = Base64.getEncoder().encodeToString(publicKeyByte);
			pairInfo.setPublicKey(publicKeyString);

			//私钥
			byte[] privateKeyByte = rsaPrivateKey.getEncoded();
			String privateKeyString = Base64.getEncoder().encodeToString(privateKeyByte);
			pairInfo.setPrivateKey(privateKeyString);

			return pairInfo;
		} catch (Exception e) {
			log.error("RSAUtil generate KeyPair failed, err: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 获取公钥对象
	 */
	public static PublicKey getPublicKey(String publicKeyBase64) throws InvalidKeySpecException, NoSuchAlgorithmException {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64));
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		return publicKey;
	}

	/**
	 * 获取私钥对象
	 */
	public static PrivateKey getPrivateKey(String privateKeyBase64) throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64));
		PrivateKey privateKey = keyFactory.generatePrivate(spec);
		return privateKey;
	}

	/**
	 * 公钥加密
	 */
	public static String encryptPublicKey(String content, String publicKeyBase64) {
		return encrypt(content, publicKeyBase64, KEY_SIZE / 8 - 11);
	}

	/**
	 * 公钥分段加密
	 */
	public static String encrypt(String content, String publicKeyBase64, int segmentSize) {
		try {
			PublicKey publicKey = getPublicKey(publicKeyBase64);
			return encrypt(content, publicKey, segmentSize);
		} catch (Exception e) {
			log.error("RSAUtil encrypt failed, err: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 分段加密
	 */
	private static String encrypt(String ciphertext, Key key, int segmentSize) {
		try {
			byte[] srcBytes = ciphertext.getBytes();
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] resultBytes = null;

			if (segmentSize > 0) {
				resultBytes = cipherDoFinal(cipher, srcBytes, segmentSize);
			} else {
				resultBytes = cipher.doFinal(srcBytes);
			}
			return Base64Utils.encodeToString(resultBytes);
		} catch (Exception e) {
			log.error("RSAUtil encrypt with segmentSize {} failed, err: {}", segmentSize, e.getMessage());
			return null;
		}
	}

	/**
	 * 分段加密
	 */
	public static byte[] cipherDoFinal(Cipher cipher, byte[] srcBytes, int segmentSize) throws IllegalBlockSizeException, BadPaddingException, IOException {
		if (segmentSize <= 0)
			throw new RuntimeException("RSAUtil cipher segmentSize must bigger than 0");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int inputLen = srcBytes.length;
		int offset = 0;
		byte[] cache;
		int i = 0;
		// encrypt each segment
		while (inputLen - offset > 0) {
			if (inputLen - offset > segmentSize) {
				cache = cipher.doFinal(srcBytes, offset, segmentSize);
			} else {
				cache = cipher.doFinal(srcBytes, offset, inputLen - offset);
			}
			out.write(cache, 0, cache.length);
			i++;
			offset = i * segmentSize;
		}
		byte[] data = out.toByteArray();
		out.close();
		return data;
	}

	/**
	 * 私钥解密
	 */
	public static String decryptPrivateKey(String dataBase64, String privateKeyBase64) {
		return decrypt(dataBase64, privateKeyBase64, KEY_SIZE / 8);
	}

	public static String decrypt(String content, String privateKeyBase64, int segmentSize) {
		try {
			PrivateKey privateKey = getPrivateKey(privateKeyBase64);
			return decrypt(content, privateKey, segmentSize);
		} catch (Exception e) {
			log.error("RSAUtil decrypt failed, err: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 分段解密
	 */
	public static String decrypt(String content, Key key, int segmentSize) {
		try {
			byte[] srcBytes = Base64Utils.decodeFromString(content);
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] decBytes = null;
			if (segmentSize > 0) {
				decBytes = cipherDoFinal(cipher, srcBytes, segmentSize);
			} else {
				decBytes = cipher.doFinal(srcBytes);
			}
			return new String(decBytes);
		} catch (Exception e) {
			log.error("decrypt by segmentSize {} failed, err: {}", segmentSize, e.getMessage());
			return null;
		}
	}

	/**
	 * 密钥对
	 */
	@Data
	public static class KeyPairInfo {
		String privateKey;
		String publicKey;
		int keySize = 0;

		public KeyPairInfo(int keySize) {
			this.keySize = keySize;
		}
	}
}
