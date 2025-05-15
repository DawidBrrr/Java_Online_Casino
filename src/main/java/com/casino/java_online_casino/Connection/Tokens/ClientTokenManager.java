package com.casino.java_online_casino.Connection.Tokens;

import io.jsonwebtoken.*;

import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;

public class ClientTokenManager {

    private final KeyManager keyManager;
    private String tokenJwt; // odszyfrowany JWT
    private Claims tokenClaims;

    public ClientTokenManager(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    public void verifyToken(String jwtToken) {
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(keyManager.getForeignPublicKey())
                    .requireIssuer("CasinoServer")
                    .requireSubject("CasinoSession")
                    .requireAudience("CasinoClient")
                    .build();

            Jws<Claims> jws = parser.parseClaimsJws(jwtToken);
            Claims claims = jws.getBody();

            // Walidacja exp
            Instant now = Instant.now();
            if (claims.getExpiration().before(Date.from(now))) {
                throw new IllegalArgumentException("Token wygasł");
            }

            this.tokenJwt = jwtToken;
            this.tokenClaims = claims;

        } catch (JwtException e) {
            throw new IllegalArgumentException("Token nieprawidłowy: " + e.getMessage(), e);
        }
    }

    public boolean isAuthorized() {
        return tokenClaims != null && tokenClaims.getExpiration().after(new Date());
    }

    public String getTokenForHeader() {
        return tokenJwt;
    }

    // Dostęp tylko do technicznych claimsów
    public Date getExpiration() {
        return tokenClaims != null ? tokenClaims.getExpiration() : null;
    }

    public String getSubject() {
        return tokenClaims != null ? tokenClaims.getSubject() : null;
    }
}
