package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
import com.casino.java_online_casino.Database.GamerDAO;
import com.casino.java_online_casino.User.Gamer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class RegisterHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("[DEBUG REGISTER] Odebrano zapytanie do RegisterHandler");
        try {
            if (!isPostMethod(exchange)) {
                logAndSend(exchange, 405, ServerJsonMessage.methodNotAllowed());
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            System.out.println("[DEBUG REGISTER] Nagłówek Authorization: " + authHeader);

            if (authHeader == null || authHeader.isBlank()) {
                logAndSend(exchange, 401, ServerJsonMessage.missingToken());
                return;
            }

            Claims claims = ServerTokenManager.validateJwt(authHeader);
            String uuidStr = claims.get(JsonFields.UUID, String.class);
            System.out.println("[DEBUG REGISTER] Odczytane UUID z tokena JWT: " + uuidStr);

            if (uuidStr == null) {
                logAndSend(exchange, 400, ServerJsonMessage.badRequest("Token nie zawiera UUID"));
                return;
            }

            UUID clientId;
            try {
                clientId = UUID.fromString(uuidStr);
                System.out.println("[DEBUG REGISTER] Parsowanie UUID zakończone sukcesem: " + clientId);
            } catch (IllegalArgumentException e) {
                logAndSend(exchange, 400, ServerJsonMessage.badRequest("Niepoprawny format UUID w tokenie"));
                return;
            }

            KeyManager keyManager = KeySessionManager.getInstance().getOrCreateSession(clientId).getKeyManager();
            System.out.println("[DEBUG REGISTER] Pobranie KeyManager dla klienta: " + clientId);
            if (keyManager == null) {
                logAndSend(exchange, 403, ServerJsonMessage.accessDenied());
                return;
            }

            JsonObject requestJson;
            try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                requestJson = JsonUtil.parseJsonFromISReader(reader);
            }
            System.out.println("[DEBUG REGISTER] Parsowanie JSON z żądania zakończone");

            if (!requestJson.has(JsonFields.DATA)) {
                logAndSend(exchange, 400, ServerJsonMessage.badRequest("Brak pola '" + JsonFields.DATA + "'"));
                return;
            }

            String encryptedPayload = requestJson.get(JsonFields.DATA).getAsString();
            System.out.println("[DEBUG REGISTER] Otrzymano zaszyfrowane dane: " + encryptedPayload);

            String decryptedJson;
            try {
                decryptedJson = keyManager.decryptAes(encryptedPayload);
                System.out.println("[DEBUG REGISTER] Dane po odszyfrowaniu AES: " + decryptedJson);
            } catch (IllegalStateException e) {
                logAndSend(exchange, 400, ServerJsonMessage.badRequest("Nie udało się odszyfrować danych"));
                return;
            }

            JsonObject credentials = JsonParser.parseString(decryptedJson).getAsJsonObject();
            if (!credentials.has(JsonFields.FIRST_NAME) || !credentials.has(JsonFields.LAST_NAME) ||
                    !credentials.has(JsonFields.EMAIL) || !credentials.has(JsonFields.PASSWORD) ||
                    !credentials.has(JsonFields.BIRTH_DATE)) {
                logAndSend(exchange, 400, ServerJsonMessage.badRequest("Brak wymaganych pól do rejestracji"));
                return;
            }

            boolean registered;
            try {
                long birthMillis = credentials.get(JsonFields.BIRTH_DATE).getAsLong();
                Date birthDate = new Date(birthMillis);
                Gamer gamer = new Gamer(
                        -1, credentials.get(JsonFields.FIRST_NAME).getAsString(),
                        credentials.get(JsonFields.LAST_NAME).getAsString(),
                        credentials.has(JsonFields.NICKNAME) ? credentials.get(JsonFields.NICKNAME).getAsString() : "",
                        credentials.get(JsonFields.EMAIL).getAsString(),
                        credentials.get(JsonFields.PASSWORD).getAsString(),
                        0, birthDate
                );
                GamerDAO dao = GamerDAO.getInstance();
                registered = dao.register(gamer); // ✅ używamy instancji
            } catch (Exception e) {
                System.out.println("[DEBUG REGISTER] Błąd podczas rejestracji: " + e.getMessage());
                logAndSend(exchange, 500, ServerJsonMessage.internalServerError());
                return;
            }

            if (!registered) {
                logAndSend(exchange, 500, ServerJsonMessage.internalServerError());
                return;
            }

            JsonObject responseData = new JsonObject();
            responseData.addProperty(JsonFields.HTTP_STATUS, "ok");
            responseData.addProperty(JsonFields.HTTP_MESSAGE, "Zarejestrowano");

            String responseDataString = responseData.toString();
            String encryptedResponseBase64 = keyManager.encryptAes(responseDataString);

            JsonObject responseWrapper = new JsonObject();
            responseWrapper.addProperty(JsonFields.DATA, encryptedResponseBase64);
            logAndSend(exchange, 200, responseWrapper);
            System.out.println("[DEBUG REGISTER] Rejestracja zakończona sukcesem.");
        } catch (Exception e) {
            System.err.println("[ERROR REGISTER] Wyjątek: " + e.getMessage());
            logAndSend(exchange, 500, ServerJsonMessage.internalServerError());
        }
    }

    private void logAndSend(HttpExchange exchange, int status, JsonObject body) throws IOException {
        System.out.println("[DEBUG REGISTER] HTTP " + status + ": " + body);
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private boolean isPostMethod(HttpExchange exchange) {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            System.out.println("[DEBUG REGISTER] Nieobsługiwana metoda: " + exchange.getRequestMethod());
            return false;
        }
        return true;
    }
}
