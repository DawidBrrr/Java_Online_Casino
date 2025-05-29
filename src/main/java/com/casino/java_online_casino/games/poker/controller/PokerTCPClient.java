package com.casino.java_online_casino.games.poker.controller;

import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.google.gson.Gson;

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
                writer.println(keyManager.encryptAes("{\"command\":\"exit\"}"));
                writer.flush();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {}
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