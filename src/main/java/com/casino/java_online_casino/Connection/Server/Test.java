package com.casino.java_online_casino.Connection.Server;

import com.casino.java_online_casino.Connection.Tokens.ClientTokenManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;

import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Test {
    public static void main(String[] args) {
        System.out.println("=== TEST BEZ INTERNETU: ECDH + JWT + AES ===");

        // === 1. Tworzenie managerów ===
        KeyManager clientKeyManager = new KeyManager();
        KeyManager serverKeyManager = new KeyManager();

        // === 2. Wymiana kluczy publicznych ===
        String clientPub = clientKeyManager.exportEcPublicKey();
        String serverPub = serverKeyManager.exportEcPublicKey();

        clientKeyManager.importForeignKey(serverPub);
        serverKeyManager.importForeignKey(clientPub);

        // === 3. Uzgadnianie klucza AES ===
        clientKeyManager.deriveSharedSecret();
        serverKeyManager.deriveSharedSecret();

        // === 4. Serwer generuje UUID i statyczny token JWT ===
        UUID uuid = UUID.randomUUID();
        System.out.println("UUID: " + uuid);

        Map<String, Object> claims = new HashMap<>();
        claims.put("UUID", uuid.toString());


        // === 6. Klient szyfruje dane JSON za pomocą AES ===
        String json = "{ \"email\": \"user@example.com\", \"password\": \"secret\" }";
        String encrypted = clientKeyManager.encryptAes(json);
        System.out.println("Zaszyfrowany JSON przez klienta (Base64): " + encrypted);

        // === 7. Serwer odszyfrowuje dane używając AES ===
        String decrypted = serverKeyManager.decryptAes(encrypted);
        System.out.println("ODSZYFROWANY JSON po stronie serwera: " + decrypted);
    }
}
