package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Experimental;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
@Experimental
public class RegisterHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("[DEBUG] Odebrano zapytanie do LoginHandler");

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            System.out.println("[DEBUG] Nieobsługiwana metoda HTTP: " + exchange.getRequestMethod());
            sendError(exchange, 405, "Tylko POST jest obsługiwany");
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        System.out.println("[DEBUG] Nagłówek Authorization: " + authHeader);

        if (authHeader == null || authHeader.isBlank()) {
            System.out.println("[DEBUG] Brak nagłówka Authorization");
            sendError(exchange, 401, "Brak nagłówka Authorization");
            return;
        }

        Claims claims = ServerTokenManager.validateJwt(authHeader);
        String uuidStr = claims.get("UUID", String.class);
        System.out.println("[DEBUG] Odczytane UUID z tokena JWT: " + uuidStr);

        if (uuidStr == null) {
            System.out.println("[DEBUG] Token JWT nie zawiera UUID");
            sendError(exchange, 400, "Token nie zawiera UUID");
            return;
        }

        UUID clientId;
        try {
            clientId = UUID.fromString(uuidStr);
            System.out.println("[DEBUG] Parsowanie UUID zakończone sukcesem: " + clientId);
        } catch (IllegalArgumentException e) {
            System.out.println("[DEBUG] Niepoprawny format UUID w tokenie: " + uuidStr);
            sendError(exchange, 400, "Niepoprawny format UUID w tokenie");
            return;
        }

        KeyManager keyManager = KeySessionManager.getInstance().getOrCreateSession(clientId).getKeyManager();
        System.out.println("[DEBUG] Pobranie KeyManager dla klienta: " + clientId);
        if (keyManager == null) {
            System.out.println("[DEBUG] Brak KeyManager dla klienta o UUID: " + clientId);
            sendError(exchange, 403, "Nie znaleziono klienta o podanym UUID");
            return;
        }

        System.out.println("[DEBUG] Klucz AES (Base64) klienta: " + keyManager.exportAesKey());

        JsonObject requestJson;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestJson = JsonUtil.parseJsonFromISReader(reader);
        }
        System.out.println("[DEBUG] Parsowanie JSON z żądania zakończone");

        if (!requestJson.has("data")) {
            System.out.println("[DEBUG] Brak pola 'data' w żądaniu");
            sendError(exchange, 400, "Brak pola 'data'");
            return;
        }

        String encryptedPayload = requestJson.get("data").getAsString();
        System.out.println("[DEBUG] Otrzymano zaszyfrowane dane: " + encryptedPayload);

        String decryptedJson;
        try {
            decryptedJson = keyManager.decryptAes(encryptedPayload);
            System.out.println("[DEBUG] Dane po odszyfrowaniu AES: " + decryptedJson);
        } catch (IllegalStateException e) {
            System.out.println("[DEBUG] Nie udało się odszyfrować danych AES: " + e.getMessage());
            sendError(exchange, 400, "Nie udało się odszyfrować danych");
            return;
        }

        JsonObject credentials = JsonParser.parseString(decryptedJson).getAsJsonObject();
        if (!credentials.has("first_name") || !credentials.has("last_name") ||
                !credentials.has("email") || !credentials.has("password") ||
                !credentials.has("birth_date")) {

            System.out.println("[DEBUG] Brak jednego z wymaganych pól: first_name, last_name, email, password, birth_date");
            sendError(exchange, 400, "Brak wymaganych pól do rejestracji");
            return;
        }

        String firstName = credentials.get("first_name").getAsString();
        String lastName = credentials.get("last_name").getAsString();
        String nickname = credentials.get("nickname").getAsString();
        String email = credentials.get("email").getAsString();
        String password = credentials.get("password").getAsString();
        String birthDate = credentials.get("birth_date").getAsString(); // format: "YYYY-MM-DD" (np. 2000-01-01)
       String credits = credentials.get("credits").getAsString();


        System.out.println("[DEBUG] Dane rejestracyjne:");
        System.out.println("  Imię: " + firstName);
        System.out.println("  Nazwisko: " + lastName);
        System.out.println("  Email: " + email);
        System.out.println("  Hasło: " + password);
        System.out.println("  Data urodzenia: " + birthDate);
        System.out.println("  Credits: " + credits);
        System.out.println("  Nickname: " + nickname);

        System.out.println("[DEBUG] Otrzymano dane logowania - email: " + email + ", password: " + password);

        // TODO: Logika uwierzytelniania
        JsonObject responseData = new JsonObject();
        responseData.addProperty("status", "ok");
        responseData.addProperty("message", "Zalogowano");

// Zamień na String
        String responseDataString = responseData.toString();

// Zaszyfruj responseDataString za pomocą AES
        String encryptedResponseBase64 = keyManager.encryptAes(responseDataString);

// Utwórz obiekt JSON z polem encryptedData
        JsonObject responseWrapper = new JsonObject();
        responseWrapper.addProperty("data", encryptedResponseBase64);
        sendJson(exchange, 200, responseWrapper);
// Wyślij zaszyfrowaną odpowiedź
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("status", "error");
        error.addProperty("message", message);
        sendJson(exchange, status, error);
    }

    private void sendJson(HttpExchange exchange, int status, JsonObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
        System.out.println("[DEBUG] Wysłano odpowiedź HTTP " + status + ": " + body);
    }
}
