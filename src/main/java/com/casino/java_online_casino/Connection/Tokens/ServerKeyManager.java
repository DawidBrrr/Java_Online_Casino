package com.example.security;

import java.security.*;
import java.util.Base64;
import javax.crypto.Cipher;

public class ServerKeyManager {

    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    private static volatile ServerKeyManager instance;

    // ===== Singleton =====
    private ServerKeyManager() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not supported", e);
        }
    }

    public static ServerKeyManager getInstance() {
        if (instance == null) {
            synchronized (ServerKeyManager.class) {
                if (instance == null) {
                    instance = new ServerKeyManager();
                }
            }
        }
        return instance;
    }

    // ===== Dostęp do kluczy =====
    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    // ===== Low-level =====
    public byte[] encryptBytes(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public byte[] decryptBytes(byte[] encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // ===== High-level API =====

    /**
     * Szyfruje wiadomość tekstową i zwraca ją jako zakodowany Base64 string
     */
    public String encryptToBase64(String message) {
        byte[] encryptedBytes = encryptBytes(message.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Odszyfrowuje wiadomość z postaci Base64 do tekstu
     */
    public String decryptFromBase64(String base64Encrypted) {
        byte[] encryptedBytes = Base64.getDecoder().decode(base64Encrypted);
        byte[] decryptedBytes = decryptBytes(encryptedBytes);
        return new String(decryptedBytes);
    }

    // ===== Jeśli chcesz jawne Base64 <-> byte[] API osobno =====

    public byte[] base64ToBytes(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    public String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
