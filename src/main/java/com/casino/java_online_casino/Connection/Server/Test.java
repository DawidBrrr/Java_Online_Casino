package com.casino.java_online_casino.Connection.Server;

import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.TokenFactory;
import com.casino.java_online_casino.User.Gamer;
import com.google.gson.Gson;

public class Test {
    public static void main(String[] args) {
        // === 1. Inicjalizacja kluczy klienta i serwera ===
        KeyManager serwer = new KeyManager();
        KeyManager client = new KeyManager();

        // === 2. Negocjacja klucza AES (ECDH) ===
        serwer.setForeignPublicKey(client.getEcPublicKey());
        serwer.deriveSharedSecret();
        client.setForeignPublicKey(serwer.getEcPublicKey());
        client.deriveSharedSecret();
        // === 3. Tworzenie przykładowego obiektu ===
        Gamer gamer = new Gamer(0, "Michał", "Myszkowski", "Mysza", 400);
        Gson gson = new Gson();
        String json = gson.toJson(gamer);

        // === 4. TokenFactory dla obu stron ===
        TokenFactory clientTokenFactory = new TokenFactory(client);
        TokenFactory serverTokenFactory = new TokenFactory(serwer);

        // === 5. Klient tworzy token ===
        String token = clientTokenFactory.createToken(json, null);
        System.out.println("TOKEN: " + token);

        // === 6. Serwer ustawia klucz publiczny klienta do weryfikacji podpisu ===
        serverTokenFactory.setVerifierPublicKey(client.getEcPublicKey());

        // === 7. Serwer weryfikuje i deszyfruje dane ===
        String decryptedJson = serverTokenFactory.validateAndDecryptToken(token);
        Gamer decryptedGamer = gson.fromJson(decryptedJson, Gamer.class);
        System.out.println(decryptedJson);
    }
}
