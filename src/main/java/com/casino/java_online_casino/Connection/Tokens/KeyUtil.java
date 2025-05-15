package com.casino.java_online_casino.Connection.Tokens;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public class KeyUtil {

    // ======================= RSA =======================

    /**
     * Koduje dowolny klucz (RSA/EC) do Base64.
     */
    public static String encodeKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Dekoduje RSA publiczny klucz z formatu Base64.
     */
    public static PublicKey decodeRSAPublicKey(String base64Key) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(new X509EncodedKeySpec(bytes));
    }

    /**
     * Dekoduje RSA prywatny klucz z formatu Base64.
     */
    public static PrivateKey decodeRSAPrivateKey(String base64Key) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    // ======================= EC (ECDH) =======================

    /**
     * Dekoduje EC publiczny klucz z formatu Base64.
     */
    public static PublicKey decodeECPublicKey(String base64Key) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        KeyFactory factory = KeyFactory.getInstance("EC");
        return factory.generatePublic(new X509EncodedKeySpec(bytes));
    }

    /**
     * Dekoduje EC prywatny klucz z formatu Base64.
     */
    public static PrivateKey decodeECPrivateKey(String base64Key) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        KeyFactory factory = KeyFactory.getInstance("EC");
        return factory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    // ======================= AES =======================

    /**
     * Dekoduje klucz AES z formatu Base64.
     */
    public static SecretKey decodeAESKey(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decoded, 0, decoded.length, "AES");
    }

    /**
     * Koduje klucz AES do formatu Base64.
     */
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
