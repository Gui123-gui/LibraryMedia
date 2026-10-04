package com.guilhermedev.librarymedia.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private static final long TOKEN_LIFETIME_SECONDS = 3600;
    private final SecretKey signingKey;

    public JwtService(@Value("${app.security.jwt.secret}") String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String createToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(TOKEN_LIFETIME_SECONDS)))
                .signWith(signingKey)
                .compact();
    }

    public long expiresInSeconds() {
        return TOKEN_LIFETIME_SECONDS;
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
