package com.casino.java_online_casino.Connection.Tokens;

import com.casino.java_online_casino.Experimental;
import io.jsonwebtoken.*;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
@Experimental
public class TokenFactory {

    private final KeyManager keyManager;
    private static final int expirationTime = 3*60*60;

    public TokenFactory(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    public String createToken(String jsonToEncrypt, Map<String, Object> publicClaims) {
        try {
            String encryptedBase64 = KeyUtil.encodeToBase64(keyManager.encryptAES(jsonToEncrypt));

            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(expirationTime);

            JwtBuilder builder = Jwts.builder()
                    .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                    .setIssuer("CasinoServer")
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(expiration))
                    .claim("encryptedData", encryptedBase64)
                    .signWith(keyManager.getPrivateKey(), SignatureAlgorithm.RS256);

            if (publicClaims != null) {
                for (Map.Entry<String, Object> entry : publicClaims.entrySet()) {
                    builder.claim(entry.getKey(), entry.getValue());
                }
            }

            return builder.compact();

        } catch (Exception e) {
            throw new RuntimeException("Błąd przy tworzeniu tokena: " + e.getMessage(), e);
        }
    }

    public String validateAndDecryptToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(keyManager.getPublicKey())
                    .build()
                    .parseClaimsJws(token);

            String encryptedBase64 = jws.getBody().get("encryptedData", String.class);
            byte[] encryptedBytes = KeyUtil.decodeFromBase64(encryptedBase64);
            return keyManager.decryptAES(encryptedBytes);

        } catch (JwtException e) {
            throw new RuntimeException("Nieprawidłowy lub wygasły token: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Błąd deszyfrowania danych z tokena: " + e.getMessage(), e);
        }
    }
}
