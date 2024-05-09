package com.yu.gateway.common.utils.jwt;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
public class JWTUtil {
    private static final long DEFAULT_EXPIRE = 24 * 60 * 60 * 1000;

    public static String generateToken(Map<String, Object> params, String secret) {
        return generateToken(params, DEFAULT_EXPIRE, secret);
    }

    /**
     * JWT加密生成TOKEN
     */
    public static String generateToken(Map<String, Object> params, long expire, String secret) {
        Date date = new Date();
        Date expireTime = new Date(date.getTime() + expire * 1000);
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuedAt(date)
                .setClaims(params)
                .setExpiration(expireTime)
                .signWith(SignatureAlgorithm.HS256, secret).compact();
    }

    /**
     * JWT解密
     */
    public static Claims getClaimByToken(String token, String secret) {
        try {
            return Jwts.parser().setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("verify token failed, err : {}", e.getMessage());
        }
        return null;
    }
}
