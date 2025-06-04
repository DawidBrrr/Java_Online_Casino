package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LogoutHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("[DEBUG LOGOUT] Odebrano zapytanie do LogoutHandler");
        try {
            if (!isPostMethod(exchange)) {
                sendEncrypted(exchange, 405, null, ServerJsonMessage.methodNotAllowed());
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            System.out.println("[DEBUG LOGOUT] Nagłówek Authorization: " + authHeader);

            if (authHeader == null || authHeader.isBlank()) {
                sendEncrypted(exchange, 401, null, ServerJsonMessage.missingToken());
                return;
            }

            Claims claims;
            try {
                claims = ServerTokenManager.validateJwt(authHeader);
            } catch (Exception e) {
                sendEncrypted(exchange, 401, null, ServerJsonMessage.invalidToken());
                return;
            }

            String uuidStr = claims.get(JsonFields.UUID, String.class);
            Integer userId = null;
            try {
                userId = Integer.parseInt(claims.get(JsonFields.ID, String.class));
            } catch (Exception e) {
                // userId może być null
            }

            if (uuidStr == null) {
                sendEncrypted(exchange, 400, null, ServerJsonMessage.badRequest("Token nie zawiera UUID"));
                return;
            }

            UUID clientId;
            try {
                clientId = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                sendEncrypted(exchange, 400, null, ServerJsonMessage.badRequest("Niepoprawny format UUID w tokenie"));
                return;
            }

            KeyManager keyManager = KeySessionManager.getInstance().getOrCreateSession(clientId).getKeyManager();
            if (keyManager == null) {
                sendEncrypted(exchange, 403, null, ServerJsonMessage.accessDenied());
                return;
            }

            // Odczytaj i odszyfruj pole data
            JsonObject requestJson;
            try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                requestJson = JsonUtil.parseJsonFromISReader(reader);
            }

            if (!requestJson.has(JsonFields.DATA)) {
                sendEncrypted(exchange, 400, keyManager, ServerJsonMessage.badRequest("Brak pola 'data'"));
                return;
            }

            String encryptedPayload = requestJson.get(JsonFields.DATA).getAsString();
            String decryptedJson;
            try {
                decryptedJson = keyManager.decryptAes(encryptedPayload);
            } catch (IllegalStateException e) {
                sendEncrypted(exchange, 400, keyManager, ServerJsonMessage.badRequest("Nie udało się odszyfrować danych"));
                return;
            }

            // Sprawdź, czy klient żąda wylogowania
            if (!JsonFields.LOGOUT.equalsIgnoreCase(decryptedJson.trim())) {
                sendEncrypted(exchange, 400, keyManager, ServerJsonMessage.badRequest("Nieznana komenda"));
                return;
            }

            // Usuwanie sesji po UUID
            SessionManager.getInstance().deleteSessionByUUID(clientId);

            // Usuwanie wszystkich sesji po userId (jeśli userId jest znane i >0)
            if (userId != null && userId > 0) {
                SessionManager.getInstance().deleteSessionByUserId(String.valueOf(userId));
            }

            // Szyfrowana odpowiedź z użyciem ServerJsonMessage
            JsonObject response = ServerJsonMessage.ok("Wylogowano");
            sendEncrypted(exchange, 200, keyManager, response);

        } catch (Exception e) {
            System.err.println("[ERROR LOGOUT] Wyjątek: " + e.getMessage());
            sendEncrypted(exchange, 500, null, ServerJsonMessage.internalServerError());
        }
    }

    private void sendEncrypted(HttpExchange exchange, int status, KeyManager keyManager, JsonObject body) throws IOException {
        JsonObject wrapper = new JsonObject();
        if (keyManager != null) {
            String encrypted = keyManager.encryptAes(body.toString());
            wrapper.addProperty(JsonFields.DATA, encrypted);
        } else {
            // Brak szyfrowania (np. nie mamy klucza sesji)
            wrapper = body;
        }
        byte[] bytes = wrapper.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private boolean isPostMethod(HttpExchange exchange) {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            System.out.println("[DEBUG LOGOUT] Nieobsługiwana metoda: " + exchange.getRequestMethod());
            return false;
        }
        return true;
    }
}
