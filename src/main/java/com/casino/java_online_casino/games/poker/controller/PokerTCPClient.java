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
        String message = reader.readLine();
        if (message == null) {
            throw new IOException("Połączenie zamknięte przez serwer");
        }

        // Sprawdź czy wiadomość jest zaszyfrowana
        if (!message.startsWith("{")) {
            try {
                message = keyManager.decryptAes(message);
            } catch (Exception e) {
                throw new IOException("Błąd deszyfrowania wiadomości: " + e.getMessage());
            }
        }

        return message;
    }



    public void connect() throws IOException {
        this.socket = new Socket(ServerConfig.getGameServerHost(), ServerConfig.getGameServerPort());
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        InitRequest initRequest = new InitRequest(token, "poker", roomId);
        String initJson = gson.toJson(initRequest);
        writer.println(initJson);
    }

    private PokerDTO sendCommand(String command, int amount) throws Exception {
        PokerCommandRequest req = new PokerCommandRequest(command, amount, roomId);
        String jsonReq = gson.toJson(req);
        String encryptedReq = keyManager.encryptAes(jsonReq);
        writer.println(encryptedReq);

        String encryptedResp = reader.readLine();
        if (encryptedResp == null) {
            throw new IOException("Połączenie zamknięte przez serwer.");
        }

        if (encryptedResp.startsWith("{")) {
            ErrorResponse error = gson.fromJson(encryptedResp, ErrorResponse.class);
            if (error.error != null) {
                throw new IOException("Serwer zwrócił błąd: " + error.error);
            }
        }

        String jsonResp = keyManager.decryptAes(encryptedResp);
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