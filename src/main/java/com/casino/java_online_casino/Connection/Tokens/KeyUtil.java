package com.casino.java_online_casino.Connection.Tokens;

import javax.crypto.SecretKey;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public class KeyUtil {

    // ======================= RSA =======================

    public static String encodeKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static PublicKey decodeRSAPublicKey(String base64Key) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(new X509EncodedKeySpec(bytes));
    }

    public static PrivateKey decodeRSAPrivateKey(String base64Key) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    // ======================= AES =======================

    public static SecretKey decodeAESKey(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        return new javax.crypto.spec.SecretKeySpec(decoded, 0, decoded.length, "AES");
    }

    public static String encodeAESKey(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // =================== BASE64 for byte[] ===================

    /**
     * Koduje dane binarne (np. zaszyfrowane bajty) do Stringa base64.
     */
    public static String encodeToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Dekoduje String base64 do oryginalnych bajtów.
     */
    public static byte[] decodeFromBase64(String base64Data) {
        return Base64.getDecoder().decode(base64Data);
    }
}
