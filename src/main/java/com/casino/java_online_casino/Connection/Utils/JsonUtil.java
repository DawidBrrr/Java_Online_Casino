package com.casino.java_online_casino.Connection.Utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;

public class JsonUtil {
    private static final Gson gson = new Gson();

    /**
     * Parsuje ciało żądania HTTP do obiektu JsonObject.
     */
    public static JsonObject parseJson(InputStream inputStream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /**
     * Wysyła odpowiedź JSON do klienta.
     */
    public static void sendJson(HttpExchange exchange, JsonObject json) throws IOException {
        byte[] responseBytes = gson.toJson(json).getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
