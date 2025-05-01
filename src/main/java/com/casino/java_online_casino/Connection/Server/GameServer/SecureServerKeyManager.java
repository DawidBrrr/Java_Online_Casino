package com.casino.java_online_casino.Connection.Server.GameServer;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;
import java.util.Base64;
@Deprecated
public class SecureServerKeyManager {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private SecretKey secretKey;
    private Cipher rsaEncrypt;
    private Cipher rsaDecrypt;
    private Cipher aesEncrypt;
    private Cipher aesDecrypt;

    public SecureServerKeyManager(SecretKey secretKey) throws NullPointerException{
        this();
        if (secretKey != null) {
            this.secretKey = secretKey;
            aesInit();
        } else {
            throw new NullPointerException("Klucz jest NULL");
        }
    }

    public SecureServerKeyManager() {
        rsaInit();
    }

    public void aesKeyInstance() {
        if (this.secretKey == null) {
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                this.secretKey = keyGen.generateKey();
                aesInit();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

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

    private void rsaInit() {
        try {
            KeyPairGenerator pair = KeyPairGenerator.getInstance("RSA");
            pair.initialize(1024);
            KeyPair keyPair = pair.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Problem z generowaniem pary kluczy RSA");
        }

        try {
            rsaEncrypt = Cipher.getInstance("RSA");
            rsaEncrypt.init(Cipher.ENCRYPT_MODE, publicKey);
            rsaDecrypt = Cipher.getInstance("RSA");
            rsaDecrypt.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Problem z inicjalizacją RSA: " + e.getMessage());
        }
    }

    private void aesInit() {
        try {
            aesEncrypt = Cipher.getInstance("AES");
            aesEncrypt.init(Cipher.ENCRYPT_MODE, secretKey);
            aesDecrypt = Cipher.getInstance("AES");
            aesDecrypt.init(Cipher.DECRYPT_MODE, secretKey);
        } catch (Exception e) {
            throw new RuntimeException("Problem z inicjalizacją AES: " + e.getMessage());
        }
    }

    public void setAESKey(SecretKey secretKey) {
        if (this.secretKey == null) {
            this.secretKey = secretKey;
            aesInit();
        }
    }
    
    public String encryptRSA(byte[] data) throws Exception {
        byte[] encryptedData = rsaEncrypt.doFinal(data);
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    public byte[] decryptRSA(String encryptedData) throws Exception {
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        return rsaDecrypt.doFinal(decodedData);
    }

    public byte[] encryptAES(String data) throws Exception {
        return aesEncrypt.doFinal(data.getBytes());
    }

    public String decryptAES(byte[] data) throws Exception {
        byte[] decryptedData = aesDecrypt.doFinal(data);
        return new String(decryptedData);
    }

    public static void main(String[] args) {
        SecureServerKeyManager manager = new SecureServerKeyManager();
        manager.aesKeyInstance();
        try {
            byte[] b = manager.encryptAES("Dominik");
            String c = manager.decryptAES(b);
            System.out.println(b);
            System.out.println(c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
