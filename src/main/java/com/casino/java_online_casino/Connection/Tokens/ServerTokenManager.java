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
 * Szyfrowanie wartości claimsów (Base64 po RSA) dla własnych claimsów.
 */
@Experimental
public class ServerTokenManager {
    private static final long EXPIRATION_SECONDS = 3 * 60 * 60; // 3 godziny

    /**
     * Tworzy podpisany JWT (bez szyfrowania) przy użyciu RSA (RS256).
     * Wszystkie wartości claimsów z mapy są szyfrowane (Base64 po RSA).
     */
    public static String createJwt(Map<String, String> publicClaims) {
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
            publicClaims.forEach((k, v) -> {
                if (v != null) {
                    String encoded = ServerKeyManager.getInstance().encryptToBase64(v);
                    builder.claim(k, encoded);
                }
            });
        }

        return builder.compact();
    }

    /**
     * Waliduje JWT – podpis, termin ważności i podstawowe parametry.
     * Deszyfruje wartości claimsów, które są stringami (pozostałe kopiuje bez zmian).
     */
    public static Claims validateJwt(String jwtToken) {
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
            Claims encryptedClaims = jws.getBody();
            Claims decryptedClaims = Jwts.claims();

            encryptedClaims.forEach((k, v) -> {
                if (v instanceof String) {
                    try {
                        // Próbujemy odszyfrować, jeśli się nie uda, zostawiamy oryginał
                        String decoded = ServerKeyManager.getInstance().decryptFromBase64((String) v);
                        decryptedClaims.put(k, decoded);
                    } catch (Exception e) {
                        System.out.println("[DEBUG] Błąd deszyfracji claimu: " + k + " (" + e.getMessage() + ")");
                        decryptedClaims.put(k, v); // Zostaw oryginalną (zaszyfrowaną) wartość
                    }
                } else {
                    // Standardowe claims (np. daty, liczby) kopiujemy bez zmian
                    decryptedClaims.put(k, v);
                }
            });
            return decryptedClaims;

        } catch (JwtException e) {
            throw new IllegalArgumentException("Nieprawidłowy lub wygasły token: " + e.getMessage(), e);
        }
    }
}
