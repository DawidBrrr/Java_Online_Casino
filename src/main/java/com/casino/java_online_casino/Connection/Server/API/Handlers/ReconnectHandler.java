package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.Experimental;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
@Experimental
public class ReconnectHandler implements HttpHandler {

    private final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            return;
        }

        JsonObject req = JsonUtil.parseJsonFromIS(exchange.getRequestBody());
        String userId = req.get("userId").getAsString();

        JsonObject res = new JsonObject();

        // Próba odzyskania sesji
        String sessionKey = sessionManager.reconnectSession(userId);
        if (sessionKey == null) {
            res.addProperty("status", "error");
            res.addProperty("message", "Brak aktywnej sesji do przywrócenia.");
            exchange.sendResponseHeaders(404, 0);
        } else {
            res.addProperty("status", "reconnected");
            res.addProperty("userId", userId);
            res.addProperty("sessionKey", sessionKey);
            exchange.sendResponseHeaders(200, 0);
        }

        JsonUtil.sendJson(exchange, res);
    }
}
