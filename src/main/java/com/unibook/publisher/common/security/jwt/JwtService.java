package com.unibook.publisher.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration.access}") long accessExpirationToken,
            @Value("${security.jwt.expiration.refresh}") long refreshTokenExpirationToken
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMillis = accessExpirationToken;
        this.refreshExpirationMillis = refreshTokenExpirationToken;
    }

    public String generateAccessToken(UserDetails details) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(accessExpirationMillis);
        List<String> authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(details.getUsername())
                .claim("role", authorities)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UserDetails details, String jti) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(refreshExpirationMillis);
        List<String> authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .id(jti)
                .subject(details.getUsername())
                .claim("role", authorities)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(parseClaims(token).get("type"));
        } catch (JwtException | IllegalArgumentException _) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public long getAccessExpirationInSeconds() {
        return accessExpirationMillis / 1000;
    }

    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getRefreshExpirationInSeconds() {
        return refreshExpirationMillis / 1000;
    }
}
