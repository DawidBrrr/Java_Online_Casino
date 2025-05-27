package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.UUID;

public class TokenHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

//        UUID sessionId = UUID.randomUUID();
//        String token = JwtUtil.generateTemporaryToken(sessionId); // ważność np. 5 minut
//
//        ServerData.getInstance().getSessions().put(sessionId, new SessionData(sessionId));
//
//        JsonObject res = new JsonObject();
//        res.addProperty("token", token);
//
//        JsonUtil.sendJson(exchange, res);
    }
}
