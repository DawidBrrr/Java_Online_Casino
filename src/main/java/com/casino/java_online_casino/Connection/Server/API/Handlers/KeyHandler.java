package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class KeyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Tylko POST jest obsługiwany");
            return;
        }
        KeyManager keyManager = new KeyManager();

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            JsonObject requestBody = JsonUtil.parseJsonFromISReader(reader);

            if (!requestBody.has("clientPublicKey")) {
                sendResponse(exchange, 400, "Brakuje pola 'clientPublicKey'");
                return;
            }

            String clientPublicKeyBase64 = requestBody.get("clientPublicKey").getAsString();

            // Zapisz klucz klienta do KeyManagera (zakładamy EC/RSA w Base64)
            keyManager.importForeignKey(clientPublicKeyBase64);

            // Odpowiedź z kluczem publicznym serwera (zakładamy EC)
            String serverPublicKeyBase64 = keyManager.exportEcPublicKey();

            JsonObject response = new JsonObject();
            response.addProperty("serverPublicKey", serverPublicKeyBase64);

            sendResponse(exchange, 200, response.toString());
        } catch (Exception e) {
            sendResponse(exchange, 500, "Błąd serwera: " + e.getMessage());
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
