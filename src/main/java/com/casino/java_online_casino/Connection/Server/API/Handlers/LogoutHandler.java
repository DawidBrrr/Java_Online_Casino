package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.Connection.Utils.LogManager;
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
        System.out.println("[DEBUG LOGOUT] Metoda HTTP: " + exchange.getRequestMethod());
        try {
            // Metoda HTTP
            if (!isPostMethod(exchange)) {
                System.out.println("[DEBUG LOGOUT] Błąd: Nieprawidłowa metoda HTTP: " + exchange.getRequestMethod());
                LogManager.logToFile("[DEBUG LOGOUT] Błąd: Nieprawidłowa metoda HTTP: " + exchange.getRequestMethod());
                sendEncrypted(exchange, 405, null, ServerJsonMessage.methodNotAllowed());
                return;
            }
            System.out.println("[DEBUG LOGOUT] Metoda HTTP poprawna: " + exchange.getRequestMethod());
            LogManager.logToFile("[DEBUG LOGOUT] Metoda HTTP poprawna: " + exchange.getRequestMethod());

            // Nagłówek Authorization
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            System.out.println("[DEBUG LOGOUT] Nagłówek Authorization: " + authHeader);
            LogManager.logToFile("[DEBUG LOGOUT] Nagłówek Authorization: " + authHeader);

            if (authHeader == null || authHeader.isBlank()) {
                System.out.println("[DEBUG LOGOUT] Błąd: Brak tokenu JWT w nagłówku.");
                LogManager.logToFile("[DEBUG LOGOUT] Błąd: Brak tokenu JWT w nagłówku.");
                sendEncrypted(exchange, 401, null, ServerJsonMessage.missingToken());
                return;
            }
            System.out.println("[DEBUG LOGOUT] Token JWT obecny.");
            LogManager.logToFile("[DEBUG LOGOUT] Token JWT obecny.");

            // Walidacja tokena JWT
            Claims claims;
            try {
                claims = ServerTokenManager.validateJwt(authHeader);
                System.out.println("[DEBUG LOGOUT] Token JWT poprawnie zweryfikowany.");
                LogManager.logToFile("[DEBUG LOGOUT] Token JWT poprawnie zweryfikowany.");
            } catch (Exception e) {
                System.out.println("[DEBUG LOGOUT] Błąd walidacji tokena JWT: " + e.getMessage());
                LogManager.logToFile("[DEBUG LOGOUT] Błąd walidacji tokena JWT: " + e.getMessage());
                sendEncrypted(exchange, 401, null, ServerJsonMessage.invalidToken());
                return;
            }

            // Pobranie UUID i userId z tokena
            String uuidStr = claims.get(JsonFields.UUID, String.class);
            Integer userId = null;
            try {
                userId = Integer.parseInt(claims.get(JsonFields.ID, String.class));
                System.out.println("[DEBUG LOGOUT] userId z tokena: " + userId);
                LogManager.logToFile("[DEBUG LOGOUT] userId z tokena: " + userId);
            } catch (Exception e) {
                System.out.println("[DEBUG LOGOUT] Brak userId w tokenie lub niepoprawny format.");
                LogManager.logToFile("[DEBUG LOGOUT] Brak userId w tokenie lub niepoprawny format.");
            }

            if (uuidStr == null) {
                System.out.println("[DEBUG LOGOUT] Błąd: Token nie zawiera UUID.");
                LogManager.logToFile("[DEBUG LOGOUT] Błąd: Token nie zawiera UUID.");
                sendEncrypted(exchange, 400, null, ServerJsonMessage.badRequest("Token nie zawiera UUID"));
                return;
            }

            UUID clientId;
            try {
                clientId = UUID.fromString(uuidStr);
                System.out.println("[DEBUG LOGOUT] UUID z tokena: " + clientId);
                LogManager.logToFile("[DEBUG LOGOUT] UUID z tokena: " + clientId);
            } catch (IllegalArgumentException e) {
                System.out.println("[DEBUG LOGOUT] Błąd: Niepoprawny format UUID w tokenie.");
                sendEncrypted(exchange, 400, null, ServerJsonMessage.badRequest("Niepoprawny format UUID w tokenie"));
                return;
            }

            // Pobranie KeyManagera z sesji
            KeyManager keyManager = null;
            try {
                keyManager = KeySessionManager.getInstance().getOrCreateSession(clientId).getKeyManager();
                if (keyManager == null) {
                    System.out.println("[DEBUG LOGOUT] Błąd: Brak KeyManagera dla sesji.");
                    LogManager.logToFile("[DEBUG LOGOUT] Błąd: Brak KeyManagera dla sesji.");
                    sendEncrypted(exchange, 403, null, ServerJsonMessage.accessDenied());
                    return;
                }
                System.out.println("[DEBUG LOGOUT] KeyManager pobrany poprawnie.");
                LogManager.logToFile("[DEBUG LOGOUT] KeyManager pobrany poprawnie.");
            } catch (Exception e) {
                System.out.println("[DEBUG LOGOUT] Błąd podczas pobierania KeyManagera: " + e.getMessage());
                LogManager.logToFile("[DEBUG LOGOUT] Błąd podczas pobierania KeyManagera: " + e.getMessage());
                sendEncrypted(exchange, 500, null, ServerJsonMessage.internalServerError());
                return;
            }

            // Odczytanie i odszyfrowanie pola data
            JsonObject requestJson;
            try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                requestJson = JsonUtil.parseJsonFromISReader(reader);
                System.out.println("[DEBUG LOGOUT] Odczytano JSON z body: " + requestJson);
                LogManager.logToFile("[DEBUG LOGOUT] Odczytano JSON z body: " + requestJson);
            } catch (Exception e) {
                System.out.println("[DEBUG LOGOUT] Błąd podczas odczytu JSON z body: " + e.getMessage());
                LogManager.logToFile("[DEBUG LOGOUT] Błąd podczas odczytu JSON z body: " + e.getMessage());
                sendEncrypted(exchange, 400, keyManager, ServerJsonMessage.badRequest("Błąd odczytu JSON z body"));
                return;
            }

            if (!requestJson.has(JsonFields.DATA)) {
                System.out.println("[DEBUG LOGOUT] Błąd: Brak pola 'data' w żądaniu.");
                LogManager.logToFile("[DEBUG LOGOUT] Błąd: Brak pola 'data' w żądaniu.");
                sendEncrypted(exchange, 400, keyManager, ServerJsonMessage.badRequest("Brak pola 'data'"));
                return;
            }

            String encryptedPayload = requestJson.get(JsonFields.DATA).getAsString();
            String decryptedJson;
            try {
                decryptedJson = keyManager.decryptAes(encryptedPayload);
                System.out.println("[DEBUG LOGOUT] Odszyfrowano dane: " + decryptedJson);
                LogManager.logToFile("[DEBUG LOGOUT] Odszyfrowano dane: " + decryptedJson);
            } catch (IllegalStateException e) {
                System.out.println("[DEBUG LOGOUT] Błąd odszyfrowywania danych: " + e.getMessage());
                LogManager.logToFile("[DEBUG LOGOUT] Błąd odszyfrowywania danych: " + e.getMessage());
                sendEncrypted(exchange, 400, keyManager, ServerJsonMessage.badRequest("Nie udało się odszyfrować danych"));
                return;
            }

            // Sprawdzenie komendy logout
            if (!JsonFields.LOGOUT.equalsIgnoreCase(decryptedJson.trim())) {
                System.out.println("[DEBUG LOGOUT] Błąd: Nieznana komenda w żądaniu: " + decryptedJson);
                LogManager.logToFile("[DEBUG LOGOUT] Błąd: Nieznana komenda w żądaniu: " + decryptedJson);
                sendEncrypted(exchange, 400, keyManager, ServerJsonMessage.badRequest("Nieznana komenda"));
                return;
            }
            System.out.println("[DEBUG LOGOUT] Komenda logout poprawna.");

            // Usuwanie sesji po UUID
            try {
                SessionManager.getInstance().deleteSessionByUUID(clientId);
                System.out.println("[DEBUG LOGOUT] Usunięto sesję po UUID: " + clientId);
                LogManager.logToFile("[DEBUG LOGOUT] Usunięto sesję po UUID: " + clientId);
            } catch (Exception e) {
                System.out.println("[DEBUG LOGOUT] Błąd przy usuwaniu sesji po UUID: " + e.getMessage());
                LogManager.logToFile("[DEBUG LOGOUT] Błąd przy usuwaniu sesji po UUID: " + e.getMessage());
            }

            // Usuwanie wszystkich sesji po userId (jeśli userId jest znane i >0)
            if (userId != null && userId > 0) {
                try {
                    SessionManager.getInstance().deleteSessionByUserId(String.valueOf(userId));
                    System.out.println("[DEBUG LOGOUT] Usunięto wszystkie sesje po userId: " + userId);
                    LogManager.logToFile("[DEBUG LOGOUT] Usunięto wszystkie sesje po userId: " + userId);
                } catch (Exception e) {
                    System.out.println("[DEBUG LOGOUT] Błąd przy usuwaniu sesji po userId: " + e.getMessage());
                    LogManager.logToFile("[DEBUG LOGOUT] Błąd przy usuwaniu sesji po userId: " + e.getMessage());
                }
            }

            // Szyfrowana odpowiedź z użyciem ServerJsonMessage
            JsonObject response = ServerJsonMessage.ok("Wylogowano");
            System.out.println("[DEBUG LOGOUT] Wylogowanie zakończone sukcesem, wysyłam odpowiedź.");
            LogManager.logToFile("[DEBUG LOGOUT] Wylogowanie zakończone sukcesem, wysyłam odpowiedź.");
            sendEncrypted(exchange, 200, keyManager, response);

        } catch (Exception e) {
            System.err.println("[ERROR LOGOUT] Wyjątek główny: " + e.getMessage());
            LogManager.logToFile("[ERROR LOGOUT] Wyjątek główny: " + e.getMessage());
            e.printStackTrace();
            sendEncrypted(exchange, 500, null, ServerJsonMessage.internalServerError());
        }
    }

    private void sendEncrypted(HttpExchange exchange, int status, KeyManager keyManager, JsonObject body) throws IOException {
        JsonObject wrapper = new JsonObject();
        if (keyManager != null) {
            try {
                String encrypted = keyManager.encryptAes(body.toString());
                wrapper.addProperty(JsonFields.DATA, encrypted);
                System.out.println("[DEBUG LOGOUT] Odpowiedź zaszyfrowana.");
                LogManager.logToFile("[DEBUG LOGOUT] Odpowiedź zaszyfrowana.");
            } catch (Exception e) {
                System.out.println("[DEBUG LOGOUT] Błąd szyfrowania odpowiedzi: " + e.getMessage());
                LogManager.logToFile("[DEBUG LOGOUT] Błąd szyfrowania odpowiedzi: " + e.getMessage());
                wrapper = body;
            }
        } else {
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
            LogManager.logToFile("[DEBUG LOGOUT] Nieobsługiwana metoda: " + exchange.getRequestMethod());
            return false;
        }
        return true;
    }
}
