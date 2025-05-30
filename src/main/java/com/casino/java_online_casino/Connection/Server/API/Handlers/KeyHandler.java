package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Session.SessionManager.SessionToken;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
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

        if (!isPostMethod(exchange)) {
            logAndSend(exchange, 405, ServerJsonMessage.methodNotAllowed());
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            JsonObject requestBody = JsonUtil.parseJsonFromISReader(reader);
            System.out.println("[DEBUG] Odebrano body żądania: " + requestBody);

            if (!hasClientPublicKey(requestBody)) {
                logAndSend(exchange, 400, ServerJsonMessage.badRequest("Missing field: '" + JsonFields.CL_PUBLIC_KEY + "'"));
                return;
            }

            handleKeyExchange(exchange, requestBody);
        } catch (Exception e) {
            System.err.println("[ERROR] Wyjątek podczas obsługi KeyHandler:");
            logAndSend(exchange, 500, ServerJsonMessage.internalServerError());
        }
    }

    private boolean isPostMethod(HttpExchange exchange) {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            System.out.println("[DEBUG] Nieobsługiwany typ metody: " + exchange.getRequestMethod());
            return false;
        }
        return true;
    }

    private boolean hasClientPublicKey(JsonObject requestBody) {
        if (!requestBody.has(JsonFields.CL_PUBLIC_KEY)) {
            System.out.println("[DEBUG] Brak pola '" + JsonFields.CL_PUBLIC_KEY + "' w żądaniu");
            return false;
        }
        return true;
    }

    private void handleKeyExchange(HttpExchange exchange, JsonObject requestBody) throws IOException {
        SessionToken session  = SessionManager.getInstance().getUnregisteredSession();
        String clientPublicKeyBase64 = requestBody.get(JsonFields.CL_PUBLIC_KEY).getAsString();
        System.out.println("[DEBUG] Klucz publiczny klienta (Base64): " + clientPublicKeyBase64);

        session.getKeyManager().importForeignKey(clientPublicKeyBase64);
        System.out.println("[DEBUG] Klucz klienta zaimportowany do KeyManagera.");
        System.out.println("[DEBUG] Klucz AES: " + session.getKeyManager().exportAesKey());

        String serverPublicKeyBase64 = session.getKeyManager().exportEcPublicKey();
        System.out.println("[DEBUG] Wygenerowany klucz publiczny serwera (Base64): " + serverPublicKeyBase64);

        UUID uuid = session.getUuid();
        System.out.println("[DEBUG] Wygenerowany UUID sesji: " + uuid);

        String token = session.getNewToken();
        System.out.println("[DEBUG] Wygenerowany token JWT: " + token);

        JsonObject response = ServerJsonMessage.ok("Key exchange completed successfully.");
        response.addProperty(JsonFields.SR_PUBLIC_KEY, serverPublicKeyBase64);
        response.addProperty(JsonFields.TOKEN, token);

        logAndSend(exchange, 200, response);

        KeySessionManager.getInstance().putKeyManager(uuid, session);
        System.out.println("[DEBUG] Sesja zapisana w KeySessionManager pod UUID: " + uuid);
    }

    private void logAndSend(HttpExchange exchange, int statusCode, JsonObject body) throws IOException {
        System.out.println("[DEBUG] Wysyłany JSON: " + body);
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
