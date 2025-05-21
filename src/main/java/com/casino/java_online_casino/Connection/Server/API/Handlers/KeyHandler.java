package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KeyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("[DEBUG] Otrzymano zapytanie HTTP: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            System.out.println("[DEBUG] Nieobsługiwany typ metody: " + exchange.getRequestMethod());
            sendResponse(exchange, 405, "{\"error\": \"Tylko POST jest obsługiwany\"}");
            return;
        }

        KeyManager keyManager = new KeyManager();

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            JsonObject requestBody = JsonUtil.parseJsonFromISReader(reader);
            System.out.println("[DEBUG] Odebrano body żądania: " + requestBody.toString());

            if (!requestBody.has("clientPublicKey")) {
                System.out.println("[DEBUG] Brak pola 'clientPublicKey' w żądaniu");
                sendResponse(exchange, 400, "{\"error\": \"Brakuje pola 'clientPublicKey'\"}");
                return;
            }

            String clientPublicKeyBase64 = requestBody.get("clientPublicKey").getAsString();
            System.out.println("[DEBUG] Klucz publiczny klienta (Base64): " + clientPublicKeyBase64);

            // Zaimportuj klucz publiczny klienta i wygeneruj sekret współdzielony
            keyManager.importForeignKey(clientPublicKeyBase64);
            System.out.println("[DEBUG] Klucz klienta zaimportowany do KeyManagera.");
            System.out.println("[DEBUG] Klucz AES :"+keyManager.exportAesKey());

            // Wyeksportuj klucz publiczny serwera
            String serverPublicKeyBase64 = keyManager.exportEcPublicKey();
            System.out.println("[DEBUG] Wygenerowany klucz publiczny serwera (Base64): " + serverPublicKeyBase64);

            // Przygotuj token JWT z UUID
            UUID uuid = UUID.randomUUID();
            System.out.println("[DEBUG] Wygenerowany UUID sesji: " + uuid.toString());

            Map<String, Object> claims = new HashMap<>();
            claims.put("UUID", uuid.toString());

            String token = ServerTokenManager.createJwt(claims);
            System.out.println("[DEBUG] Wygenerowany token JWT: " + token);

            // Stwórz odpowiedź JSON
            JsonObject response = new JsonObject();
            response.addProperty("serverPublicKey", serverPublicKeyBase64);
            response.addProperty("token", token);

            sendResponse(exchange, 200, response.toString());

            // Zapisz keyManager pod UUID do sesji
            KeySessionManager.getInstance().putKeyManager(uuid, keyManager);
            System.out.println("[DEBUG] KeyManager zapisany w KeySessionManager pod UUID: " + uuid.toString());

        } catch (Exception e) {
            System.err.println("[ERROR] Wyjątek podczas obsługi KeyHandler:");
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\": \"Błąd serwera: " + e.getMessage() + "\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
