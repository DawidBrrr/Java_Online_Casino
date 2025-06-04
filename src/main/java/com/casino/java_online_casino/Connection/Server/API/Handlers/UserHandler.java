package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Client.ServiceHelper;
import com.casino.java_online_casino.Connection.Server.DTO.GamerDTO;
import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
import com.casino.java_online_casino.Database.GamerDAO;
import com.casino.java_online_casino.User.Gamer;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UserHandler implements HttpHandler {

    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, ServerJsonMessage.methodNotAllowed());
                return;
            }

            String token = exchange.getRequestHeaders().getFirst("Authorization");
            if (token == null || token.isBlank()) {
                System.err.println("[DEBUG] Brak tokenu w nagłówku Authorization.");
                LogManager.logToFile("[DEBUG] Brak tokenu w nagłówku Authorization.");
                sendJson(exchange, 401, ServerJsonMessage.missingToken());
                return;
            }

            UUID clientId = extractClientIdFromToken(exchange, token);
            if (clientId == null) {
                System.err.println("[DEBUG] Błąd walidacji tokenu JWT.");
                LogManager.logToFile("[DEBUG] Błąd walidacji tokenu JWT.");
                return;
            }

            SessionManager.SessionToken session = KeySessionManager.getInstance().getOrCreateSession(clientId);
            if (session == null) {
                System.err.println("[DEBUG] Brak aktywnej sesji dla klienta: " + clientId);
                LogManager.logToFile("[DEBUG] Brak aktywnej sesji dla klienta: " + clientId);
                sendJson(exchange, 403, ServerJsonMessage.accessDenied());
                return;
            }

            int userId = session.getUserId();
            if (userId < 0) {
                System.err.println("[DEBUG] Brak powiązanego userId dla sesji klienta: " + clientId);
                LogManager.logToFile("[DEBUG] Brak powiązanego userId dla sesji klienta: " + clientId);
                sendJson(exchange, 403, ServerJsonMessage.accessDenied());
                return;
            }

            String body = readRequestBody(exchange);
            String operation = "";

            if (body != null && !body.isBlank()) {
                try {
                    JsonObject bodyJson = JsonParser.parseString(body).getAsJsonObject();

                    if (!bodyJson.has("data")) {
                        sendJson(exchange, 400, ServerJsonMessage.badRequest("Brak pola 'data' w żądaniu."));
                        return;
                    }

                    String encryptedPayload = bodyJson.get("data").getAsString();

                    String decrypted = session.getKeyManager().decryptAes(encryptedPayload);
                    JsonObject req = JsonParser.parseString(decrypted).getAsJsonObject();

                    if (req.has(ServiceHelper.OPERATION)) {
                        operation = req.get(ServiceHelper.OPERATION).getAsString();
                    }

                } catch (Exception e) {
                    System.err.println("[DEBUG] Błąd odszyfrowywania/podziału JSON body: " + e.getMessage());
                    LogManager.logToFile("[DEBUG] Błąd odszyfrowywania/podziału JSON body: " + e.getMessage());
                    sendJson(exchange, 400, ServerJsonMessage.badRequest("Nieprawidłowe zaszyfrowane dane wejściowe."));
                    return;
                }
            }

            GamerDAO dao = GamerDAO.getInstance();
            Gamer gamer = null;
            try {
                gamer = dao.findById(userId);
            } catch (Exception e) {
                System.err.println("[DEBUG] Błąd podczas pobierania gracza z bazy: " + e.getMessage());
                LogManager.logToFile("[DEBUG] Błąd podczas pobierania gracza z bazy: " + e.getMessage());
                sendJson(exchange, 500, ServerJsonMessage.internalServerError());
                return;
            }

            if (gamer == null) {
                System.err.println("[DEBUG] Nie znaleziono gracza o userId: " + userId);
                LogManager.logToFile("[DEBUG] Nie znaleziono gracza o userId: " + userId);
                sendJson(exchange, 404, ServerJsonMessage.userNotFound());
                return;
            }

            boolean updated = false;

            switch (operation) {
                case ServiceHelper.DEPOSIT:
                    try {
                        gamer.setCredits(gamer.getCredits() + 500.0f);
                        updated = dao.updateCredits(userId, gamer.getCredits());
                    } catch (Exception e) {
                        System.err.println("[DEBUG] Błąd przy wpłacie kredytów: " + e.getMessage());
                        LogManager.logToFile("[DEBUG] Błąd przy wpłacie kredytów: " + e.getMessage());
                        sendJson(exchange, 500, ServerJsonMessage.internalServerError());
                        return;
                    }
                    break;
                case ServiceHelper.WITHDRAW:
                    if (gamer.getCredits() >= 500.0f) {
                        try {
                            gamer.setCredits(gamer.getCredits() - 500.0f);
                            updated = dao.updateCredits(userId, gamer.getCredits());
                        } catch (Exception e) {
                            System.err.println("[DEBUG] Błąd przy wypłacie kredytów: " + e.getMessage());
                            LogManager.logToFile("[DEBUG] Błąd przy wypłacie kredytów: " + e.getMessage());
                            sendJson(exchange, 500, ServerJsonMessage.internalServerError());
                            return;
                        }
                    } else {
                        System.err.println("[DEBUG] Próba wypłaty większej niż dostępne saldo: " + gamer.getCredits());
                        LogManager.logToFile("[DEBUG] Próba wypłaty większej niż dostępne saldo: " + gamer.getCredits());
                        // Zwróć aktualne dane bez zmiany
                    }
                    break;
                default:
                    // Brak operacji = tylko pobierz dane użytkownika
                    break;
            }

            // Odśwież dane po ewentualnej aktualizacji
            if (updated) {
                try {
                    gamer = dao.findById(userId);
                } catch (Exception e) {
                    System.err.println("[DEBUG] Błąd przy ponownym pobieraniu gracza po aktualizacji: " + e.getMessage());
                    LogManager.logToFile("[DEBUG] Błąd przy ponownym pobieraniu gracza po aktualizacji: " + e.getMessage());
                    sendJson(exchange, 500, ServerJsonMessage.internalServerError());
                    return;
                }
            }

            GamerDTO gamerDTO = new GamerDTO(
                    gamer.getName(),
                    gamer.getLastName(),
                    gamer.getNickName(),
                    gamer.getEmail(),
                    gamer.getCredits()
            );

            String gamerJson;
            try {
                gamerJson = gson.toJson(gamerDTO);
            } catch (Exception e) {
                System.err.println("[DEBUG] Błąd serializacji DTO: " + e.getMessage());
                LogManager.logToFile("[DEBUG] Błąd serializacji DTO: " + e.getMessage());
                sendJson(exchange, 500, ServerJsonMessage.internalServerError());
                return;
            }

            String encrypted;
            try {
                encrypted = session.getKeyManager().encryptAes(gamerJson);
            } catch (Exception e) {
                System.err.println("[DEBUG] Błąd szyfrowania odpowiedzi: " + e.getMessage());
                LogManager.logToFile("[DEBUG] Błąd szyfrowania odpowiedzi: " + e.getMessage());
                sendJson(exchange, 500, ServerJsonMessage.internalServerError());
                return;
            }

            JsonObject response = new JsonObject();
            response.addProperty(JsonFields.DATA, encrypted);

            sendJson(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("[DEBUG] Wyjątek główny w handlerze: " + e.getMessage());
            LogManager.logToFile("[DEBUG] Wyjątek główny w handlerze: " + e.getMessage());
            sendJson(exchange, 500, ServerJsonMessage.internalServerError());
        }
    }

    private UUID extractClientIdFromToken(HttpExchange exchange, String token) throws IOException {
        try {
            Claims claims = ServerTokenManager.validateJwt(token);
            String uuidStr = claims.get(JsonFields.UUID, String.class);
            if (uuidStr == null) {
                System.err.println("[DEBUG] Token nie zawiera UUID.");
                LogManager.logToFile("[DEBUG] Token nie zawiera UUID.");
                sendJson(exchange, 400, ServerJsonMessage.badRequest("Token does not contain " + JsonFields.UUID));
                return null;
            }
            return UUID.fromString(uuidStr);
        } catch (Exception e) {
            System.err.println("[DEBUG] Błąd walidacji tokenu JWT: " + e.getMessage());
            LogManager.logToFile("[DEBUG] Błąd walidacji tokenu JWT: " + e.getMessage());
            sendJson(exchange, 401, ServerJsonMessage.invalidToken());
            return null;
        }
    }

    private void sendJson(HttpExchange exchange, int status, JsonObject body) throws IOException {
        byte[] responseBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] Błąd podczas odczytu body żądania: " + e.getMessage());
            LogManager.logToFile("[DEBUG] Błąd podczas odczytu body żądania: " + e.getMessage());
        }
        return sb.toString();
    }
}
