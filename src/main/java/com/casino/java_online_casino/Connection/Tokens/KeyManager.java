package com.casino.java_online_casino.Connection.Tokens;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * KeyManager zarządza kluczami ECDH i AES.
 * Pozwala na generowanie kluczy EC, wyliczanie wspólnego sekretu,
 * oraz szyfrowanie/deszyfrowanie wiadomości w Base64.
 */
public class KeyManager {
    private static final String EC_CURVE = "secp256r1";
    private static final String ECDH_ALGO = "ECDH";
    private static final String AES_ALGO = "AES";
    private static final String AES_CIPHER = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_SIZE_BITS = 256;

    private final KeyPair ecKeyPair;
    private PublicKey foreignPublicKey;
    private SecretKey aesKey;
    private Cipher aesEncrypt;
    private Cipher aesDecrypt;


    public KeyManager() {
        this.ecKeyPair = generateEcKeyPair();
    }

    private KeyPair generateEcKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec(EC_CURVE));
            return gen.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Nie można wygenerować kluczy EC", e);
        }
    }

    /**
     * Wylicza wspólny sekret ECDH i inicjalizuje AES/GCM.
     */
    public PublicKey getForeignPublicKey(){
        return foreignPublicKey;
    }
    public boolean setForeignPublicKey(PublicKey publicKey){
        if(this.foreignPublicKey == null){
            this.foreignPublicKey = publicKey;
            return true;
        }
        return false;
    }
    public void deriveSharedSecret() {
        try {
            KeyAgreement ka = KeyAgreement.getInstance(ECDH_ALGO);
            ka.init(ecKeyPair.getPrivate());
            ka.doPhase(foreignPublicKey, true);
            byte[] secret = ka.generateSecret();

            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secret);
            aesKey = new SecretKeySpec(keyBytes, 0, AES_KEY_SIZE_BITS / 8, AES_ALGO);

            aesEncrypt = Cipher.getInstance(AES_CIPHER);
            aesDecrypt = Cipher.getInstance(AES_CIPHER);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Błąd inicjalizacji ECDH/AES", e);
        }
    }

    private void ensureAesReady() {
        if (aesKey == null || aesEncrypt == null || aesDecrypt == null) {
            throw new IllegalStateException("AES nie został zainicjalizowany");
        }
    }

    /**
     * Szyfruje tekst i zwraca Base64.
     */
    public String encryptAes(String plaintext) {
        ensureAesReady();
        try {
            byte[] iv = SecureRandom.getInstanceStrong().generateSeed(GCM_IV_LENGTH);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            aesEncrypt.init(Cipher.ENCRYPT_MODE, aesKey, spec);

            byte[] cipherText = aesEncrypt.doFinal(plaintext.getBytes());
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Błąd szyfrowania AES", e);
        }
    }

    /**
     * Deszyfruje Base64 i zwraca plaintext.
     */
    public String decryptAes(String base64Data) {
        ensureAesReady();
        try {
            byte[] data = Base64.getDecoder().decode(base64Data);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, iv.length);

            byte[] cipherText = new byte[data.length - iv.length];
            System.arraycopy(data, iv.length, cipherText, 0, cipherText.length);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            aesDecrypt.init(Cipher.DECRYPT_MODE, aesKey, spec);

            byte[] plain = aesDecrypt.doFinal(cipherText);
            return new String(plain);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Błąd deszyfrowania AES", e);
        }
    }

    /**
     * Zwraca publiczny klucz EC (ECDH + ES256 w JWT).
     */
    public PublicKey getEcPublicKey() {
        return ecKeyPair.getPublic();
    }

    public PrivateKey getEcPrivateKey() {
        return ecKeyPair.getPrivate();
    }
    public SecretKey getAesKey(){
        return aesKey;
    }
        /**
     * Zwraca publiczny klucz w Base64 do transmisji.
     */
    public String exportEcPublicKey() {
        return Base64.getEncoder().encodeToString(ecKeyPair.getPublic().getEncoded());
    }

    /**
     * Importuje cudzy publiczny klucz EC z Base64.
     */
    public PublicKey importEcPublicKey(String base64PublicKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64PublicKey);
            KeyFactory factory = KeyFactory.getInstance("EC");
            return factory.generatePublic(new java.security.spec.X509EncodedKeySpec(decoded));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Nie można zaimportować klucza publicznego EC", e);
        }
    }

    /**
     * Ustawia cudzy klucz EC z postaci Base64 i wylicza wspólny sekret.
     */
    public void importForeignKey(String base64ForeignKey) {
        PublicKey foreignKey = importEcPublicKey(base64ForeignKey);
        if (!setForeignPublicKey(foreignKey)) {
            throw new IllegalStateException("Klucz publiczny został już ustawiony");
        }
        deriveSharedSecret();
    }

    /**
     * Eksportuje aktualny klucz AES w Base64 (np. do przywrócenia sesji).
     */
    public String exportAesKey() {
        if (aesKey == null) throw new IllegalStateException("AES nie jest gotowy");
        return Base64.getEncoder().encodeToString(aesKey.getEncoded());
    }

    /**
     * Importuje klucz AES z Base64.
     */
    public void importAesKey(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        this.aesKey = new SecretKeySpec(decoded, AES_ALGO);

        try {
            this.aesEncrypt = Cipher.getInstance(AES_CIPHER);
            this.aesDecrypt = Cipher.getInstance(AES_CIPHER);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Błąd przy inicjalizacji AES po imporcie", e);
        }
    }



}
