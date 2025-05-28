package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.Experimental;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;

@Experimental
public class ConnectHandler implements HttpHandler {

    private final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
//        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
//            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
//            return;
//        }
//        JsonObject req = JsonUtil.parseJsonFromIS(exchange.getRequestBody());
//        String gameId = req.get("gameId").getAsString();
//        String userId = req.get("userId").getAsString();
//
//        // Tworzenie nowej sesji (lub nadpisanie starej)
//        String sessionKey = sessionManager.createSession(userId, gameId);
//
//        // Tutaj możemy dodać dodatkową logikę np. przypisanie gracza do pokoju / wysłanie klucza AES itd.
//
//        JsonObject res = new JsonObject();
//        res.addProperty("status", "connected");
//        res.addProperty("gameId", gameId);
//        res.addProperty("userId", userId);
//        res.addProperty("sessionKey", sessionKey);
//
//        JsonUtil.sendJson(exchange, res);
   }
}
