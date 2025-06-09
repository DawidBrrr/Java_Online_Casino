package com.casino.java_online_casino.games.poker.controller;

import com.casino.java_online_casino.Connection.Client.Service;
import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.casino.java_online_casino.Server;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PokerTcpClient {
    private final String token;
    private final KeyManager keyManager;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final Gson gson = new Gson();
    private String playerId;
    private String playerName;
    private int initialBalance;
    private Thread listenerThread;
    private volatile boolean running = false;

    public PokerTcpClient() {
        this.token = Service.getToken();
        this.keyManager = Service.getKeyManager();
    }

    private GameStateListener gameStateListener;

    public void setGameStateListener(GameStateListener listener) {
        this.gameStateListener = listener;
    }

    public void connect() throws IOException {
        this.socket = new Socket(ServerConfig.getGameServerHost(), ServerConfig.getGameServerPort());
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        startListenerThread();
        InitRequest initRequest = new InitRequest(token, "poker");
        String initJson = gson.toJson(initRequest);
        writer.println(initJson);
        writer.flush();
        socket.setSoTimeout(8000);
        System.out.println("[DEBUG POKER CLIENT] Połączono z serwerem i wysłano initRequest.");
        LogManager.logToFile("[DEBUG POKER CLIENT] Połączono z serwerem i wysłano initRequest: " + initJson);
    }

    private PokerDTO sendCommand(PokerCommand cmd) throws Exception {
        String jsonReq = gson.toJson(cmd);
        String encryptedReq = keyManager.encryptAes(jsonReq);
        writer.println(encryptedReq);

        String encryptedResp = reader.readLine();
        System.out.println("[DEBUG POKER CLIENT] Odebrano odpowiedź: " + encryptedResp);
        LogManager.logToFile("[DEBUG POKER CLIENT] Odebrano odpowiedź: " + encryptedResp);
        if (encryptedResp == null) {
            throw new IOException("[DEBUG POKER CLIENT] Połączenie zamknięte przez serwer.");
        }

        // Obsługa błędów w formacie JSON (niezaszyfrowanych)
        if (encryptedResp.strip().startsWith("{")) {
            JsonObject errorObj = JsonParser.parseString(encryptedResp).getAsJsonObject();
            if (errorObj.has("error")) {
                throw new IOException("[DEBUG POKER CLIENT] Serwer zwrócił błąd: " + errorObj.get("error").getAsString());
            }
            throw new IOException("[DEBUG POKER CLIENT] Serwer zwrócił nieoczekiwany plain JSON: " + encryptedResp);
        }

        String jsonResp = keyManager.decryptAes(encryptedResp);
        System.out.println("[DEBUG POKER CLIENT] Odszyfrowana odpowiedź: " + jsonResp);
        LogManager.logToFile("[DEBUG POKER CLIENT] Odszyfrowana odpowiedź: " + jsonResp);
        JsonObject allData = gson.fromJson(jsonResp, JsonObject.class);
        String playerData = allData.get("data").getAsString();
        System.out.println("[DEBUG POKER CLIENY - DATA]" + playerData);


        return gson.fromJson(playerData, PokerDTO.class);
    }

    public PokerDTO join() throws Exception {
        PokerCommand cmd = new PokerCommand("join", playerId, playerName, initialBalance, 0);
        return sendCommand(cmd);
    }

    public PokerDTO leave() throws Exception {
        PokerCommand cmd = new PokerCommand("leave", playerId, playerName, initialBalance, 0);
        return sendCommand(cmd);
    }

    public PokerDTO fold() throws Exception {
        PokerCommand cmd = new PokerCommand("fold", playerId, playerName, initialBalance, 0);
        return sendCommand(cmd);
    }

    public PokerDTO call() throws Exception {
        PokerCommand cmd = new PokerCommand("call", playerId, playerName, initialBalance, 0);
        return sendCommand(cmd);
    }

    public PokerDTO raise(int amount) throws Exception {
        PokerCommand cmd = new PokerCommand("raise", playerId, playerName, initialBalance, amount);
        return sendCommand(cmd);
    }

    public PokerDTO check() throws Exception {
        PokerCommand cmd = new PokerCommand("check", playerId, playerName, initialBalance, 0);
        return sendCommand(cmd);
    }

    public PokerDTO allIn() throws Exception {
        PokerCommand cmd = new PokerCommand("allin", playerId, playerName, initialBalance, 0);
        return sendCommand(cmd);
    }

    public PokerDTO getState() throws Exception {
        PokerCommand cmd = new PokerCommand("get_state", playerId, playerName, initialBalance, 0);
        return sendCommand(cmd);
    }

    public PokerDTO getFinalState() throws Exception {
        PokerCommand cmd = new PokerCommand("get_final_state", playerId, playerName, initialBalance, 0);
        return sendCommand(cmd);
    }

    public PokerDTO reconnect() throws Exception {
        PokerCommand cmd = new PokerCommand("reconnect", playerId, playerName, initialBalance, 0);
        return sendCommand(cmd);
    }

    public void close() {
        running = false;
        try {
            if (writer != null) {
                PokerCommand cmd = new PokerCommand("leave", playerId, playerName, initialBalance, 0);
                String json = gson.toJson(cmd);
                String encrypted = keyManager.encryptAes(json);
                writer.println(encrypted);
                writer.flush();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }
            System.out.println("[DEBUG POKER CLIENT] Zamknięto połączenie.");
            LogManager.logToFile("[DEBUG POKER CLIENT] Zamknięto połączenie.");
        } catch (Exception e) {
            System.out.println("[DEBUG POKER CLIENT] Błąd przy zamykaniu połączenia: " + e.getMessage());
            LogManager.logToFile("[DEBUG POKER CLIENT] Błąd przy zamykaniu połączenia: " + e.getMessage());
        }
    }

    private void startListenerThread() {
        running = true;
        listenerThread = new Thread(() -> {
            try {
                while (running && socket != null && !socket.isClosed()) {
                    String encryptedResp = reader.readLine();
                    if (encryptedResp == null) {
                        System.out.println("[DEBUG POKER CLIENT] Połączenie zamknięte przez serwer (listener).");
                        break;
                    }
                    if (encryptedResp.strip().startsWith("{")) {
                        JsonObject errorObj = JsonParser.parseString(encryptedResp).getAsJsonObject();
                        if (errorObj.has("error")) {
                            System.out.println("[DEBUG POKER CLIENT] Błąd z serwera: " + errorObj.get("error").getAsString());
                            continue;
                        }
                    }
                    String jsonResp = keyManager.decryptAes(encryptedResp);
                    System.out.println("[DEBUG POKER CLIENT] [LISTENER] Odszyfrowana odpowiedź: " + jsonResp);
                    LogManager.logToFile("[DEBUG POKER CLIENT] [LISTENER] Odszyfrowana odpowiedź: " + jsonResp);

                    JsonObject allData = gson.fromJson(jsonResp, JsonObject.class);
                    String playerData = allData.get("data").getAsString();
                    PokerDTO dto = gson.fromJson(playerData, PokerDTO.class);

                    // Powiadom GUI przez listener
                    if (gameStateListener != null && dto != null) {
                        gameStateListener.onGameStateReceived(dto);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    System.out.println("[DEBUG POKER CLIENT] [LISTENER] Błąd wątku nasłuchującego: " + e.getMessage());
                    LogManager.logToFile("[DEBUG POKER CLIENT] [LISTENER] Błąd wątku nasłuchującego: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }


    // --- Pomocnicze klasy do komunikacji ---
    private static class InitRequest {
        String token;
        String game;

        InitRequest(String token, String game) {
            this.token = token;
            this.game = game;
        }
    }

    private static class PokerCommand {
        String command;
        String playerId;
        String playerName;
        int initialBalance;
        int amount;

        PokerCommand(String command, String playerId, String playerName, int initialBalance, int amount) {
            this.command = command;
            this.playerId = playerId;
            this.playerName = playerName;
            this.initialBalance = initialBalance;
            this.amount = amount;
        }
    }
}
