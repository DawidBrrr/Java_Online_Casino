package com.casino.java_online_casino.games.poker.controller;

import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PokerTCPClient {
    private final String token;
    private final KeyManager keyManager;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final Gson gson = new Gson();
    private String roomId = "room1";

    public PokerTCPClient(String token, KeyManager keyManager) {
        this.token = token;
        this.keyManager = keyManager;
    }

    public PokerDTO startGame() throws Exception {
        return sendCommand("start_game", 0);
    }

    public void broadcastMessage(String message) throws Exception {
        PokerCommandRequest req = new PokerCommandRequest("broadcast", 0, roomId);
        String jsonReq = gson.toJson(req);
        String encryptedReq = keyManager.encryptAes(jsonReq);
        writer.println(encryptedReq);
        writer.flush();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void sendEncryptedMessage(String message) throws IOException {
        try {
            String encrypted = keyManager.encryptAes(message);
            writer.println(encrypted);
            writer.flush();
            System.out.println("[DEBUG POKER CLIENT] Wysłano zaszyfrowaną wiadomość");
        } catch (Exception e) {
            throw new IOException("Błąd podczas szyfrowania/wysyłania wiadomości: " + e.getMessage());
        }
    }

    public String readEncryptedMessage(int timeoutMillis) throws IOException {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (reader.ready()) {
                String encryptedMessage = reader.readLine();
                if (encryptedMessage == null) {
                    throw new IOException("Połączenie zamknięte przez serwer");
                }

                try {
                    return keyManager.decryptAes(encryptedMessage);
                } catch (Exception e) {
                    throw new IOException("Błąd deszyfrowania: " + e.getMessage());
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Przerwano oczekiwanie na wiadomość");
            }
        }

        throw new IOException("Timeout: brak odpowiedzi w ciągu " + timeoutMillis + "ms");
    }
    private String readMessage() throws IOException {
        try {
            String message = reader.readLine();
            if (message == null) {
                throw new IOException("Połączenie zamknięte przez serwer");
            }

            if (!message.startsWith("{")) {
                return keyManager.decryptAes(message);
            }
            return message;
        } catch (Exception e) {
            throw new IOException("Błąd podczas odczytu wiadomości: " + e.getMessage());
        }
    }



    public void connect() throws IOException {
        this.socket = new Socket(ServerConfig.getGameServerHost(), ServerConfig.getGameServerPort());
        this.socket.setSoTimeout(5000);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        InitRequest initRequest = new InitRequest(token, "poker", roomId);
        String initJson = gson.toJson(initRequest);
        writer.println(initJson);
        writer.flush();
        System.out.println("[DEBUG POKER CLIENT] Połączono z serwerem i wysłano initRequest: " + initJson);
    }

    private PokerDTO sendCommand(String command, int amount) throws Exception {
        if (!isConnected()) {
            throw new IOException("Brak połączenia z serwerem");
        }

        PokerCommandRequest req = new PokerCommandRequest(command, amount, roomId);
        String jsonReq = gson.toJson(req);
        String encryptedReq = keyManager.encryptAes(jsonReq);
        writer.println(encryptedReq);
        writer.flush();
        System.out.println("[DEBUG POKER CLIENT] Wysłano komendę: " + command);

        String encryptedResp = reader.readLine();
        System.out.println("[DEBUG POKER CLIENT] Odebrano odpowiedź: " + encryptedResp);

        if (encryptedResp == null) {
            throw new IOException("Połączenie zamknięte przez serwer");
        }

        // Sprawdzamy czy to plain JSON (nie base64)
        if (encryptedResp.strip().startsWith("{")) {
            JsonObject errorObj = JsonParser.parseString(encryptedResp).getAsJsonObject();
            if (errorObj.has("error")) {
                throw new IOException(errorObj.get("error").getAsString());
            }
            return gson.fromJson(encryptedResp, PokerDTO.class);
        }

        // Deszyfrujemy odpowiedź base64
        String jsonResp = keyManager.decryptAes(encryptedResp);
        System.out.println("[DEBUG POKER CLIENT] Odszyfrowana odpowiedź: " + jsonResp);
        return gson.fromJson(jsonResp, PokerDTO.class);
    }


    public PokerDTO check() throws Exception {
        return sendCommand("check", 0);
    }

    public PokerDTO call(int amount) throws Exception {
        return sendCommand("call", amount);
    }

    public PokerDTO raise(int amount) throws Exception {
        return sendCommand("raise", amount);
    }

    public PokerDTO fold() throws Exception {
        return sendCommand("fold", 0);
    }

    public PokerDTO allIn(int amount) throws Exception {
        return sendCommand("allin", amount);
    }

    public void close() {
        try {
            if (writer != null) {
                sendEncryptedMessage("{\"command\":\"exit\"}");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("[DEBUG POKER CLIENT] Zamknięto połączenie");
        } catch (Exception e) {
            System.out.println("[DEBUG POKER CLIENT] Błąd przy zamykaniu połączenia: " + e.getMessage());
        }
    }

    private static class InitRequest {
        String token;
        String game;
        String roomId;

        InitRequest(String token, String game, String roomId) {
            this.token = token;
            this.game = game;
            this.roomId = roomId;
        }
    }

    private static class PokerCommandRequest {
        String command;
        int amount;
        String roomId;

        PokerCommandRequest(String command, int amount, String roomId) {
            this.command = command;
            this.amount = amount;
            this.roomId = roomId;
        }
    }

    private static class ErrorResponse {
        String error;
    }
}