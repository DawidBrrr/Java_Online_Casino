package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("[DEBUG] Otrzymano zapytanie HTTP: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

        if (!isPostMethod(exchange)) {
            logAndSend(exchange, 405, ServerJsonMessage.methodNotAllowed());
            return;
        }

        String token = exchange.getRequestHeaders().getFirst("Authorization");
        if (token == null || token.isBlank()) {
            logAndSend(exchange, 401, ServerJsonMessage.methodNotAllowed());
            return;
        }

        UUID clientId = extractClientIdFromToken(exchange, token);
        if (clientId == null) return;

        if(!KeySessionManager.getInstance().contains(clientId)) {
            logAndSend(exchange, 403, ServerJsonMessage.accessDenied());
            return;
        }

        JsonObject requestJson = readRequestJson(exchange);
        if (requestJson == null || !requestJson.has("data")) {
            logAndSend(exchange, 400, ServerJsonMessage.badRequest("Missing field: 'data'"));
            return;
        }

        SessionManager.SessionToken session = KeySessionManager.getInstance().getOrCreateSession(clientId);
        String decryptedJson = decryptRequestData(exchange, requestJson.get("data").getAsString(), session.getKeyManager());
        if (decryptedJson == null) return;

        JsonObject credentials = JsonParser.parseString(decryptedJson).getAsJsonObject();
        if (!credentials.has("email") || !credentials.has("password")) {
            logAndSend(exchange, 400, ServerJsonMessage.badRequest("Missing fields: 'email' and/or 'password'"));
            return;
        }

        String email = credentials.get("email").getAsString();
        String password = credentials.get("password").getAsString();

        System.out.println("[DEBUG] Dane logowania - email: " + email + ", password: " + password);

        String jwt = session.getNewToken();

        JsonObject response = ServerJsonMessage.ok("Zalogowano");
        response.addProperty("token", jwt);

        String encryptedResponse = session.getKeyManager().encryptAes(response.toString());
        JsonObject encryptedWrapper = new JsonObject();
        encryptedWrapper.addProperty("data", encryptedResponse);

        logAndSend(exchange, 200, encryptedWrapper);
        session.setUserId(email);
        SessionManager.getInstance().updateSessionData(clientId, session);
    }

    private boolean isPostMethod(HttpExchange exchange) {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            System.out.println("[DEBUG] Nieobsługiwana metoda: " + exchange.getRequestMethod());
            return false;
        }
        return true;    
    }

    private UUID extractClientIdFromToken(HttpExchange exchange, String token) throws IOException {
        try {
            Claims claims = ServerTokenManager.validateJwt(token);
            String uuidStr = claims.get("UUID", String.class);
            if (uuidStr == null) {
                logAndSend(exchange, 400, ServerJsonMessage.badRequest("Token does not contain UUID"));
                return null;
            }
            return UUID.fromString(uuidStr);
        } catch (Exception e) {
            logAndSend(exchange, 401, ServerJsonMessage.invalidToken());
            return null;
        }
    }

    private JsonObject readRequestJson(HttpExchange exchange) {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            return JsonUtil.parseJsonFromISReader(reader);
        } catch (Exception e) {
            System.err.println("[ERROR] Błąd parsowania JSON: " + e.getMessage());
            return null;
        }
    }

    private String decryptRequestData(HttpExchange exchange, String encryptedData, KeyManager keyManager) throws IOException {
        try {
            return keyManager.decryptAes(encryptedData);
        } catch (Exception e) {
            logAndSend(exchange, 400, ServerJsonMessage.badRequest("Failed to decrypt data"));
            return null;
        }
    }

    private void logAndSend(HttpExchange exchange, int status, JsonObject body) throws IOException {
        System.out.println("[DEBUG] HTTP " + status + ": " + body);
        byte[] responseBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }
}
