package com.casino.java_online_casino.Connection.Tokens;

import com.casino.java_online_casino.Experimental;
import io.jsonwebtoken.*;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * TokenFactory odpowiada WYŁĄCZNIE za tworzenie i weryfikację tokenów JWT podpisanych algorytmem ES256.
 * Szyfrowanie AES zostało przeniesione do osobnej klasy.
 */
@Experimental
public class ServerTokenManger {

    private final KeyManager keyManager;
    private static final long EXPIRATION_SECONDS = 3 * 60 * 60; // 3 godziny

    public ServerTokenManger(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    /**
     * Tworzy podpisany JWT (bez szyfrowania).
     */
    public String createJwt(Map<String, Object> publicClaims) {
        if (keyManager.getEcPrivateKey() == null)
            throw new IllegalStateException("Brakuje klucza EC prywatnego");

        Instant now = Instant.now();

        JwtBuilder builder = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer("CasinoServer")
                .setSubject("CasinoSession")
                .setAudience("CasinoClient")
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
                .signWith(keyManager.getEcPrivateKey(), SignatureAlgorithm.ES256);

        if (publicClaims != null && !publicClaims.isEmpty()) {
            publicClaims.forEach(builder::claim);
        }

        return builder.compact(); // JWT jako string (header.payload.signature)
    }

    /**
     * Waliduje JWT – podpis, termin ważności i podstawowe parametry.
     */
    public Claims validateJwt(String jwtToken) {
        try {
            if (keyManager.getEcPublicKey() == null && keyManager.getForeignPublicKey() == null)
                throw new IllegalStateException("Brakuje klucza publicznego EC do weryfikacji");

            var publicKey = (keyManager.getForeignPublicKey() != null)
                    ? keyManager.getForeignPublicKey()
                    : keyManager.getEcPublicKey();

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
