package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Session.SessionManager;
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
import java.util.UUID;

public class LoginHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("[DEBUG LOGIN] Otrzymano zapytanie HTTP: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());
        try {
            if (!isPostMethod(exchange)) {
                logAndSend(exchange, 405, ServerJsonMessage.methodNotAllowed());
                return;
            }

            String token = exchange.getRequestHeaders().getFirst("Authorization");
            System.out.println("[DEBUG LOGIN] Authorization header: " + token);

            if (token == null || token.isBlank()) {
                logAndSend(exchange, 401, ServerJsonMessage.missingToken());
                return;
            }

            UUID clientId = extractClientIdFromToken(exchange, token);
            if (clientId == null) return;

            if (!KeySessionManager.getInstance().contains(clientId)) {
                System.out.println("[DEBUG LOGIN] Brak aktywnej sesji dla klienta: " + clientId);
                logAndSend(exchange, 403, ServerJsonMessage.accessDenied());
                return;
            }

            JsonObject requestJson = readRequestJson(exchange);
            if (requestJson == null || !requestJson.has(JsonFields.DATA)) {
                System.out.println("[DEBUG LOGIN] Brak pola 'data' w żądaniu.");
                logAndSend(exchange, 400, ServerJsonMessage.badRequest("Missing field: '" + JsonFields.DATA + "'"));
                return;
            }

            SessionManager.SessionToken session = KeySessionManager.getInstance().getOrCreateSession(clientId);
            String decryptedJson = decryptRequestData(exchange, requestJson.get(JsonFields.DATA).getAsString(), session.getKeyManager());
            if (decryptedJson == null) return;

            JsonObject credentials = JsonParser.parseString(decryptedJson).getAsJsonObject();
            if (!credentials.has(JsonFields.EMAIL) || !credentials.has(JsonFields.PASSWORD)) {
                System.out.println("[DEBUG LOGIN] Brak wymaganych pól email/hasło.");
                logAndSend(exchange, 400, ServerJsonMessage.badRequest("Missing fields: '" + JsonFields.EMAIL + "' and/or '" + JsonFields.PASSWORD + "'"));
                return;
            }

            String email = credentials.get(JsonFields.EMAIL).getAsString();
            String password = credentials.get(JsonFields.PASSWORD).getAsString();
            System.out.println("[DEBUG LOGIN] Próba logowania email: " + email);

            GamerDAO.getInstance();
            Gamer gamer = GamerDAO.login(email, password);
            if (gamer == null) {
                System.out.println("[DEBUG LOGIN] Nie znaleziono gracza lub błędne hasło.");
                logAndSend(exchange, ServerJsonMessage.accessDenied().get(JsonFields.HTTP_CODE).getAsInt(), ServerJsonMessage.accessDenied());
                return;
            }

            // Ustaw userId w sesji i odśwież czas
            session.setUserId(gamer.getUserId());
            session.updateLastAccess();

            String jwt = session.getNewToken();
            System.out.println("[DEBUG LOGIN] Logowanie OK, wygenerowano token: " + jwt);

            JsonObject response = ServerJsonMessage.ok("Zalogowano");
            response.addProperty(JsonFields.TOKEN, jwt);

            String encryptedResponse = session.getKeyManager().encryptAes(response.toString());
            JsonObject encryptedWrapper = new JsonObject();
            encryptedWrapper.addProperty(JsonFields.DATA, encryptedResponse);

            logAndSend(exchange, 200, encryptedWrapper);
            SessionManager.getInstance().updateSessionData(clientId, session);
            System.out.println("[DEBUG LOGIN] Odpowiedź OK wysłana i sesja zaktualizowana.");
        } catch (Exception e) {
            System.err.println("[ERROR LOGIN] Wyjątek: " + e.getMessage());
            logAndSend(exchange, 500, ServerJsonMessage.internalServerError());
        }
    }

    private boolean isPostMethod(HttpExchange exchange) {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            System.out.println("[DEBUG LOGIN] Nieobsługiwana metoda: " + exchange.getRequestMethod());
            return false;
        }
        return true;
    }

    private UUID extractClientIdFromToken(HttpExchange exchange, String token) throws IOException {
        try {
            Claims claims = ServerTokenManager.validateJwt(token);
            String uuidStr = claims.get(JsonFields.UUID, String.class);
            if (uuidStr == null) {
                logAndSend(exchange, 400, ServerJsonMessage.badRequest("Token does not contain " + JsonFields.UUID));
                return null;
            }
            System.out.println("[DEBUG LOGIN] Odczytano UUID z tokena: " + uuidStr);
            return UUID.fromString(uuidStr);
        } catch (Exception e) {
            System.out.println("[DEBUG LOGIN] Błąd walidacji tokena: " + e.getMessage());
            logAndSend(exchange, 401, ServerJsonMessage.invalidToken());
            return null;
        }
    }

    private JsonObject readRequestJson(HttpExchange exchange) {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonUtil.parseJsonFromISReader(reader);
            System.out.println("[DEBUG LOGIN] Odczytano JSON z żądania: " + obj);
            return obj;
        } catch (Exception e) {
            System.err.println("[ERROR LOGIN] Błąd parsowania JSON: " + e.getMessage());
            return null;
        }
    }

    private String decryptRequestData(HttpExchange exchange, String encryptedData, KeyManager keyManager) throws IOException {
        try {
            String decrypted = keyManager.decryptAes(encryptedData);
            System.out.println("[DEBUG LOGIN] Odszyfrowane dane logowania: " + decrypted);
            return decrypted;
        } catch (Exception e) {
            System.out.println("[DEBUG LOGIN] Błąd deszyfracji danych: " + e.getMessage());
            logAndSend(exchange, 400, ServerJsonMessage.badRequest("Failed to decrypt data"));
            return null;
        }
    }

    private void logAndSend(HttpExchange exchange, int status, JsonObject body) throws IOException {
        System.out.println("[DEBUG LOGIN] HTTP " + status + ": " + body);
        byte[] responseBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }
}
