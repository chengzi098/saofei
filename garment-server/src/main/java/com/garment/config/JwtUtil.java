package com.garment.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT token
     */
    public String generateToken(Long userId, String username, Integer userType) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("userType", userType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 token 获取用户ID
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /**
     * 从 token 获取用户名
     */
    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    /**
     * 从 token 获取用户类型
     */
    public Integer getUserType(String token) {
        Number num = parseToken(token).get("userType", Number.class);
        return num != null ? num.intValue() : null;
    }

    /**
     * 验证 token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
