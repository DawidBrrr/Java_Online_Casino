package com.casino.java_online_casino.Connection.Tokens;

import com.casino.java_online_casino.Experimental;
import io.jsonwebtoken.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * TokenFactory odpowiada za tworzenie i weryfikację tokenów JWT.
 * Payload jest szyfrowany AES (pochodzącym z ECDH), a cały token podpisany EC (ES256).
 */
@Experimental
public class TokenFactory {

    private final KeyManager keyManager;
    private PublicKey verifierPublicKey; // do zewnętrznej weryfikacji podpisu (opcjonalne)
    private static final long EXPIRATION_SECONDS = 3 * 60 * 60; // 3 godziny

    public TokenFactory(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    /**
     * Tworzy JWT z zaszyfrowanym JSON-em jako "encryptedData" i podpisem ES256.
     */
    public String createToken(String jsonToEncrypt, Map<String, Object> publicClaims) {
        if (keyManager.getAesKey() == null || keyManager.getEcPrivateKey() == null)
            throw new IllegalStateException("Brakuje klucza AES lub EC prywatnego");

        String encryptedData = keyManager.encryptAes(jsonToEncrypt);
        Instant now = Instant.now();

        JwtBuilder builder = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer("CasinoServer")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
                .claim("encryptedData", encryptedData)
                .signWith(keyManager.getEcPrivateKey(), SignatureAlgorithm.ES256);

        if (publicClaims != null && !publicClaims.isEmpty()) {
            publicClaims.forEach(builder::claim);
        }

        return builder.compact();
    }

    /**
     * Weryfikuje i deszyfruje token JWT, zwraca zaszyfrowany wcześniej JSON.
     * Używa domyślnie własnego klucza EC (z `KeyManager`), chyba że ustawiono inny.
     */
    public String validateAndDecryptToken(String token) {
        try {
            PublicKey publicKey = (verifierPublicKey != null)
                ? verifierPublicKey
                : keyManager.getEcPublicKey();

            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build();

            Jws<Claims> jws = parser.parseClaimsJws(token);
            Claims claims = jws.getBody();

            String encryptedData = claims.get("encryptedData", String.class);
            return keyManager.decryptAes(encryptedData);

        } catch (JwtException e) {
            throw new IllegalArgumentException("Nieprawidłowy lub wygasły token: " + e.getMessage(), e);
        }
    }

    /**
     * Pozwala ustawić klucz publiczny do weryfikacji podpisu (np. od zdalnego nadawcy).
     */
    public void setVerifierPublicKey(PublicKey verifierPublicKey) {
        this.verifierPublicKey = verifierPublicKey;
    }

    /**
     * Pozwala ustawić klucz publiczny do weryfikacji podpisu w formie Base64.
     */
    public void setVerifierPublicKeyBase64(String base64Key) {
        this.verifierPublicKey = keyManager.importEcPublicKey(base64Key);
    }
}
