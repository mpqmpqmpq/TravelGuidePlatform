package com.mpq.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    // 使用更安全的密钥派生方式
    private SecretKey getSigningKey() {
        // 使用更安全的密钥处理方式
        if (secret == null || secret.trim().isEmpty()) {
            logger.error("JWT 秘密配置不正");
            throw new IllegalStateException("JWT 秘密未配置");
        }

        // 对于HS512，我们需要至少64字节的密钥
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // 如果密钥长度不足，使用安全哈希扩展（比简单的补零更安全）
        if (keyBytes.length < 64) {
            // 使用SHA-512哈希来扩展短密钥
            byte[] hashedKey = new byte[0];
            try {
                hashedKey = java.security.MessageDigest.getInstance("SHA-512")
                        .digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            return Keys.hmacShaKeyFor(hashedKey);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 生成Token
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateToken(String username, Integer userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        return createToken(claims, username);
    }


    // 验证Token
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername != null && extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // 提取用户名
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 提取用户ID
    public Integer extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Integer.class));
    }

    // 提取过期时间
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 通用声明提取方法
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // 解析所有声明（使用新API）
    private Claims extractAllClaims(String token) {
        JwtParser parser = Jwts.parser().setSigningKey(getSigningKey()).build();

        return parser.parseClaimsJws(token).getBody();
    }

    // 检查Token是否过期
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String getRoleFromToken(String substring) {
        return extractClaim(substring, claims -> claims.get("role", String.class));
    }
}