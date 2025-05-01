package com.casino.java_online_casino.Connection.Tokens;

import javax.crypto.*;
import java.security.*;


public class KeyManager {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private SecretKey secretKey;

    private Cipher rsaEncrypt;
    private Cipher rsaDecrypt;
    private Cipher aesEncrypt;
    private Cipher aesDecrypt;

    public KeyManager() {
        initRSA();
    }

    public void initAES(SecretKey secretKey) {
        this.secretKey = secretKey;
        try {
            aesEncrypt = Cipher.getInstance("AES");
            aesEncrypt.init(Cipher.ENCRYPT_MODE, secretKey);

            aesDecrypt = Cipher.getInstance("AES");
            aesDecrypt.init(Cipher.DECRYPT_MODE, secretKey);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Błąd inicjalizacji AES: " + e.getMessage(), e);
        }
    }

    public void generateAESKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            initAES(generator.generateKey());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Brak algorytmu AES: " + e.getMessage(), e);
        }
    }

    private void initRSA() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048); // lepsze bezpieczeństwo niż 1024
            KeyPair keyPair = generator.generateKeyPair();

            this.publicKey = keyPair.getPublic();
            this.privateKey = keyPair.getPrivate();

            rsaEncrypt = Cipher.getInstance("RSA");
            rsaEncrypt.init(Cipher.ENCRYPT_MODE, publicKey);

            rsaDecrypt = Cipher.getInstance("RSA");
            rsaDecrypt.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Błąd inicjalizacji RSA: " + e.getMessage(), e);
        }
    }

    public byte[] encryptRSA(byte[] data) throws GeneralSecurityException {
        return rsaEncrypt.doFinal(data);
    }

    public byte[] decryptRSA(byte[] data) throws GeneralSecurityException {
        return rsaDecrypt.doFinal(data);
    }

    public byte[] encryptAES(String data) throws GeneralSecurityException {
        return aesEncrypt.doFinal(data.getBytes());
    }

    public String decryptAES(byte[] encryptedData) throws GeneralSecurityException {
        byte[] decrypted = aesDecrypt.doFinal(encryptedData);
        return new String(decrypted);
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }
}
