package com.casino.java_online_casino.Connection.Utils;

import io.jsonwebtoken.*;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

//
//    public String createJwt(Map<String, Object> publicClaims) {
//        Instant now = Instant.now();
//
//        JwtBuilder builder = Jwts.builder()
//                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
//                .setIssuer("CasinoServer")
//                .setSubject("CasinoSession")
//                .setAudience("CasinoClient")
//                .setIssuedAt(Date.from(now))
//                .setNotBefore(Date.from(now))
//                .setExpiration(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
//                .signWith(keyManager.getEcPrivateKey(), SignatureAlgorithm.ES256);
//
//        if (publicClaims != null && !publicClaims.isEmpty()) {
//            publicClaims.forEach(builder::claim);
//        }
//
//        return builder.compact(); // JWT jako string (header.payload.signature)
//    }
//
//    /**
//     * Waliduje JWT – podpis, termin ważności i podstawowe parametry.
//     */
//    public Claims validateJwt(String jwtToken) {
//        try {
//            if (keyManager.getEcPublicKey() == null && keyManager.getForeignPublicKey() == null)
//                throw new IllegalStateException("Brakuje klucza publicznego EC do weryfikacji");
//
//            var publicKey = (keyManager.getForeignPublicKey() != null)
//                    ? keyManager.getForeignPublicKey()
//                    : keyManager.getEcPublicKey();
//
//            JwtParser parser = Jwts.parserBuilder()
//                    .setSigningKey(publicKey)
//                    .requireIssuer("CasinoServer")
//                    .requireSubject("CasinoSession")
//                    .requireAudience("CasinoClient")
//                    .build();
//
//            Jws<Claims> jws = parser.parseClaimsJws(jwtToken);
//            return jws.getBody();
//
//        } catch (JwtException e) {
//            throw new IllegalArgumentException("Nieprawidłowy lub wygasły token: " + e.getMessage(), e);
//        }
//    }
}
