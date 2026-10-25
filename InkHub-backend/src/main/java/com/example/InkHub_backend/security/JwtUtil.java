package com.example.InkHub_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** JWT 工具：生成 / 解析 Token（userId + role）*/
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire:7200000}")
    private Long expire;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
        //生成JWT令，牌就是token。  token的结构：header.payload.signature
    public String createToken(Long userId, Integer role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expire))
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact();
    }
        //解析并校验 token，拿到载荷 Claims(userid,role)
    public Claims parseToken(String token) {
        return Jwts.parser()  //Jwts.parser()拿解析器
                .verifyWith(getKey())  //拿对称密钥SecretKey，然后注册到解析器：后面用来校验 token 的签名
                .build()
                .parseSignedClaims(token)   //这里就解析token了
                .getPayload();
    }

    public Long getUserId(String token) {
        try {
            return parseToken(token).get("userId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getRole(String token) {
        try {
            return parseToken(token).get("role", Integer.class);
        } catch (Exception e) {
            return null;
        }
    }
}
