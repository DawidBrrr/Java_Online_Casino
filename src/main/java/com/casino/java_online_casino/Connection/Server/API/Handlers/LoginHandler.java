package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.Experimental;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
@Experimental
public class LoginHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        JsonObject requestJson = JsonUtil.parseJson(exchange.getRequestBody());

        String login = requestJson.get("username").getAsString();
        String password = requestJson.get("password").getAsString();

        // Tutaj miejsce na JWT i logikę uwierzytelniania

        JsonObject response = new JsonObject();
        response.addProperty("status", "ok");
        response.addProperty("message", "Zalogowano");
        // response.addProperty("token", "jwt_token_here");

        JsonUtil.sendJson(exchange, response);
    }
}
