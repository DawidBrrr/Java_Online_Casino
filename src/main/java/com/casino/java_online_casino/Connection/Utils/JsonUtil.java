package com.casino.java_online_casino.Connection.Utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class JsonUtil {
    private static final Gson gson = new Gson();

    /**
     * Parsuje ciało żądania HTTP do obiektu JsonObject.
     */
    public static JsonObject parseJsonFromISReader(InputStreamReader inputStreamReader) throws IOException {
            return JsonParser.parseReader(inputStreamReader).getAsJsonObject();
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

    /**
     * Parsuje JSON z InputStream, tworząc InputStreamReader z UTF-8.
     * @param is InputStream z którego odczytujemy JSON.
     * @return JsonObject sparsowany z InputStream.
     */
    public static JsonObject parseJsonFromIS(InputStream is) {
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return parseJsonFromISReader(reader);
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas parsowania JSON z InputStream", e);
        }
    }
}
