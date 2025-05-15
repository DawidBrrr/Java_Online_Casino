package com.casino.java_online_casino.Connection.Server;

import com.casino.java_online_casino.Connection.Tokens.ClientTokenManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManger;

public class Test {
    public static void main(String[] args) {
        // === 1. Inicjalizacja kluczy klienta i serwera ===
        KeyManager serwer = new KeyManager();
        KeyManager client = new KeyManager();
        System.out.println(client.exportEcPublicKey());

        // === 2. Negocjacja klucza AES (ECDH) ===
        serwer.setForeignPublicKey(client.getEcPublicKey());
        serwer.deriveSharedSecret();
        client.setForeignPublicKey(serwer.getEcPublicKey());
        client.deriveSharedSecret();
        // === 3. TokenFactory dla obu stron ===
        ClientTokenManager clientTokenFactory = new ClientTokenManager(client);
        ServerTokenManger serverTokenFactory = new ServerTokenManger(serwer);

        // === 5. Server tworzy token ===
        String token = serverTokenFactory.createJwt(null);
        System.out.println("TOKEN - GEN : " + token+"\n");

        // === 7. Serwer weryfikuje i deszyfruje dane ===
        String encryptedToken = serwer.encryptAes(token);
        String decryptedToken = client.decryptAes(encryptedToken);
        System.out.println("TOKEN - DECR "+decryptedToken+"\n");

        clientTokenFactory.verifyToken(decryptedToken);
        System.out.println(clientTokenFactory.getTokenForHeader());
    }
}
