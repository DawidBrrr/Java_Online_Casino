package com.casino.java_online_casino.Connection.Server.API.Handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StatHandler implements HttpHandler {

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        // Skonfiguruj dane do bazy zgodnie z Twoim serwerem!
        // Jeśli masz inne hasło/port, popraw!
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/casino",
                "root",        // <-- Twój user
                ""        // <-- Twoje hasło
        );
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                response.addProperty("status", "error");
                response.addProperty("message", "Method not allowed");
                sendResponse(exchange, 405, response);
                return;
            }

            List<StatEntry> stats = new ArrayList<>();

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT stats.user_id, gamers.nickname, stats.blackjack_wins, stats.poker_wins " +
                                 "FROM stats JOIN gamers ON stats.user_id = gamers.user_id")) {

                while (rs.next()) {
                    stats.add(new StatEntry(
                            rs.getInt("user_id"),
                            rs.getString("nickname"),
                            rs.getInt("blackjack_wins"),
                            rs.getInt("poker_wins")
                    ));
                }
            }

            JsonArray arr = new JsonArray();
            for (StatEntry s : stats) {
                JsonObject obj = new JsonObject();
                obj.addProperty("user_id", s.userId);
                obj.addProperty("nickname", s.nickname);
                obj.addProperty("blackjack_wins", s.blackjackWins);
                obj.addProperty("poker_wins", s.pokerWins);
                arr.add(obj);
            }
            response.addProperty("status", "ok");
            response.add("ranking", arr);

            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("status", "error");
            response.addProperty("message", e.getMessage());
            sendResponse(exchange, 500, response);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, JsonObject body) throws IOException {
        byte[] responseBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    // Pomocnicza klasa na wynik SQL
    private static class StatEntry {
        int userId;
        String nickname;
        int blackjackWins;
        int pokerWins;
        StatEntry(int userId, String nickname, int blackjackWins, int pokerWins) {
            this.userId = userId;
            this.nickname = nickname;
            this.blackjackWins = blackjackWins;
            this.pokerWins = pokerWins;
        }
    }
}
