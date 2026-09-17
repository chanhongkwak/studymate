package com.studymate.auth.infrastructure;

import com.studymate.member.domain.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    public String createAccessToken(Member member) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirationSeconds * 1000);

        return Jwts.builder()
                .subject(member.getId().toString())
                .claim("email", member.getEmail())
                .claim("role", member.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
