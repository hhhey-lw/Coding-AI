package com.coding.admin.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类 - 支持双Token机制
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:7200000}") // 默认2小时
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}") // 默认7天
    private Long refreshTokenExpiration;

    /**
     * 生成Access Token（短期）
     */
    public String generateAccessToken(Long userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("tokenType", "access");
        return createToken(claims, email, accessTokenExpiration);
    }

    /**
     * 生成Refresh Token（长期）
     */
    public String generateRefreshToken(Long userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("tokenType", "refresh");
        return createToken(claims, email, refreshTokenExpiration);
    }

    /**
     * 创建Token
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("userId", Long.class) : null;
    }

    /**
     * 从Token中获取邮箱
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 获取Token类型
     */
    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("tokenType", String.class) : null;
    }

    /**
     * 验证Access Token是否有效
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token, "access");
    }

    /**
     * 验证Refresh Token是否有效
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token, "refresh");
    }

    /**
     * 验证Token是否有效
     */
    private boolean validateToken(String token, String expectedType) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return false;
            }

            // 验证Token类型
            String tokenType = claims.get("tokenType", String.class);
            if (!expectedType.equals(tokenType)) {
                log.warn("Token类型不匹配，期望：{}，实际：{}", expectedType, tokenType);
                return false;
            }

            // 验证是否过期
            return !isTokenExpired(claims);
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Token中获取Claims
     */
    private Claims getClaimsFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("解析Token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断Token是否过期
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    /**
     * 获取Access Token过期时间（秒）
     */
    public Long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    /**
     * 获取Refresh Token过期时间（秒）
     */
    public Long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration / 1000;
    }
}
