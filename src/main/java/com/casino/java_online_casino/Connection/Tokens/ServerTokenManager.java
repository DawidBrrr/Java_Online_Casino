package com.casino.java_online_casino.Connection.Tokens;

import com.casino.java_online_casino.Experimental;
import com.example.security.ServerKeyManager;
import io.jsonwebtoken.*;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * TokenFactory odpowiada WYŁĄCZNIE za tworzenie i weryfikację tokenów JWT podpisanych algorytmem RS256.
 * Szyfrowanie AES zostało przeniesione do osobnej klasy.
 */
@Experimental
public class ServerTokenManager {
    private static final long EXPIRATION_SECONDS = 3 * 60 * 60; // 3 godziny
    /**
     * Tworzy podpisany JWT (bez szyfrowania) przy użyciu RSA (RS256).
     */
    public static  String createJwt(Map<String, Object> publicClaims) {
        PrivateKey privateKey = ServerKeyManager.getInstance().getPrivateKey();
        if (privateKey == null)
            throw new IllegalStateException("Brakuje prywatnego klucza RSA do podpisu");

        Instant now = Instant.now();

        JwtBuilder builder = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer("CasinoServer")
                .setSubject("CasinoSession")
                .setAudience("CasinoClient")
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
                .signWith(privateKey, SignatureAlgorithm.RS256);

        if (publicClaims != null && !publicClaims.isEmpty()) {
            publicClaims.forEach(builder::claim);
        }

        return builder.compact();
    }

    /**
     * Waliduje JWT – podpis, termin ważności i podstawowe parametry.
     */
    public static  Claims validateJwt(String jwtToken) {
        PublicKey publicKey = ServerKeyManager.getInstance().getPublicKey();
        if (publicKey == null)
            throw new IllegalStateException("Brakuje publicznego klucza RSA do weryfikacji podpisu JWT");

        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .requireIssuer("CasinoServer")
                    .requireSubject("CasinoSession")
                    .requireAudience("CasinoClient")
                    .build();

            Jws<Claims> jws = parser.parseClaimsJws(jwtToken);
            return jws.getBody();

        } catch (JwtException e) {
            throw new IllegalArgumentException("Nieprawidłowy lub wygasły token: " + e.getMessage(), e);
        }
    }
}
