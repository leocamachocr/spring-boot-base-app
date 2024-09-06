package dev.leocamacho.demo.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import dev.leocamacho.demo.models.AuthenticatedUser;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

public class JwtProvider {

    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60 * 1000;
    private final SecretKey key;

    public JwtProvider(String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        // Ensure the key size is appropriate for HS512
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("The secret key must be at least 64 bytes long for HS512");
        }
        key = new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String generateToken(AuthenticatedUser user) {
        Map<String, Object> claims = user.toMap();
        return doGenerateToken(claims, user.getUsername());
    }

    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(key, SignatureAlgorithm.HS512).compact();
    }

    public Boolean isValidToken(String token) {
        return token != null && !isTokenExpired(token);
    }

}