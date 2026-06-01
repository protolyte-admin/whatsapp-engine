package com.whatsapp.engine.security;

import com.whatsapp.engine.auth.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.expiration());

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("organizationId", user.getOrganization().getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    public boolean isTokenValid(String token, String username) {
        Claims claims = extractClaims(token);
        return username.equalsIgnoreCase(claims.getSubject());
    }

    public long expirationMillis() {
        return jwtProperties.expiration();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
