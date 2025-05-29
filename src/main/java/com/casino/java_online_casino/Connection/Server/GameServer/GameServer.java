package com.casino.java_online_casino.Connection.Server.GameServer;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
import com.casino.java_online_casino.games.blackjack.controller.BlackJackController;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final KeySessionManager keySessionManager = KeySessionManager.getInstance();
    private final Gson gson = new Gson();

    public void start() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(ServerConfig.getGameServerPort())) {
            System.out.println("[INFO] GameServer nasłuchuje na porcie " + ServerConfig.getGameServerPort());
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            processClient(reader, writer, clientSocket);
        } catch (Exception e) {
            System.out.println("[ERROR] Wyjątek obsługi klienta: " + e.getMessage());
            e.printStackTrace();
            closeSocketQuietly(clientSocket);
        }
    }

    private void processClient(BufferedReader reader, PrintWriter writer, Socket clientSocket) {
        String initRequest = readInitRequest(reader, writer, clientSocket);
        if (initRequest == null) return;

        InitRequest request = parseInitRequest(initRequest, writer, clientSocket);
        if (request == null) return;

        UUID playerUUID = validateAndExtractUUID(request.token, writer, clientSocket);
        if (playerUUID == null) return;

        KeyManager keyManager = getKeyManagerForUUID(playerUUID, writer, clientSocket);
        if (keyManager == null) return;

        Runnable gameHandler = selectGameHandler(request.game, clientSocket, keyManager, writer);
        if (gameHandler == null) return;

        System.out.println("[DEBUG] Uruchamiam handler run()...");
        try {
            gameHandler.run();
        } catch (Exception e) {
            System.out.println("[ERROR] Wyjątek w handlerze gry: " + e.getMessage());
            sendEncryptedJson(writer, keyManager, ServerJsonMessage.internalServerError());
            closeSocketQuietly(clientSocket);
        }
    }

    private String readInitRequest(BufferedReader reader, PrintWriter writer, Socket clientSocket) {
        try {
            String initRequest = reader.readLine();
            System.out.println("[DEBUG] Otrzymano initRequest: " + initRequest);
            if (initRequest == null || initRequest.isBlank()) {
                sendEncryptedJson(writer, null, ServerJsonMessage.badRequest("Empty initial request"));
                closeSocketQuietly(clientSocket);
                return null;
            }
            return initRequest;
        } catch (IOException e) {
            sendEncryptedJson(writer, null, ServerJsonMessage.internalServerError());
            closeSocketQuietly(clientSocket);
            return null;
        }
    }

    private InitRequest parseInitRequest(String initRequest, PrintWriter writer, Socket clientSocket) {
        try {
            InitRequest request = gson.fromJson(initRequest, InitRequest.class);
            if (request == null) {
                sendEncryptedJson(writer, null, ServerJsonMessage.badRequest("Parsed request is null"));
                closeSocketQuietly(clientSocket);
                return null;
            }
            if (request.token == null || request.game == null) {
                sendEncryptedJson(writer, null, ServerJsonMessage.badRequest("Missing token or game"));
                closeSocketQuietly(clientSocket);
                return null;
            }
            return request;
        } catch (Exception e) {
            sendEncryptedJson(writer, null, ServerJsonMessage.badRequest("Invalid JSON"));
            closeSocketQuietly(clientSocket);
            return null;
        }
    }

    private UUID validateAndExtractUUID(String token, PrintWriter writer, Socket clientSocket) {
        try {
            Object uuidObj = ServerTokenManager.validateJwt(token).get("UUID");
            if (uuidObj == null) {
                sendEncryptedJson(writer, null, ServerJsonMessage.invalidToken());
                closeSocketQuietly(clientSocket);
                return null;
            }
            return UUID.fromString(uuidObj.toString());
        } catch (Exception e) {
            sendEncryptedJson(writer, null, ServerJsonMessage.invalidToken());
            closeSocketQuietly(clientSocket);
            return null;
        }
    }

    private KeyManager getKeyManagerForUUID(UUID playerUUID, PrintWriter writer, Socket clientSocket) {
        try {
            return keySessionManager.getOrCreateSession(playerUUID).getKeyManager();
        } catch (Exception e) {
            sendEncryptedJson(writer, null, ServerJsonMessage.internalServerError());
            closeSocketQuietly(clientSocket);
            return null;
        }
    }

    private Runnable selectGameHandler(String game, Socket clientSocket, KeyManager keyManager, PrintWriter writer) {
        switch (game.toLowerCase()) {
            case "blackjack":
                BlackJackController controller = new BlackJackController();
                return new BlackjackTcpHandler(clientSocket, controller, keyManager);
            case "poker":
                sendEncryptedJson(writer, keyManager, ServerJsonMessage.badRequest("Poker not implemented yet"));
                closeSocketQuietly(clientSocket);
                return null;
            case "slots":
                sendEncryptedJson(writer, keyManager, ServerJsonMessage.badRequest("Slots not implemented yet"));
                closeSocketQuietly(clientSocket);
                return null;
            default:
                sendEncryptedJson(writer, keyManager, ServerJsonMessage.badRequest("Unknown game type"));
                closeSocketQuietly(clientSocket);
                return null;
        }
    }

    private void sendEncryptedJson(PrintWriter writer, KeyManager keyManager, JsonObject json) {
        try {
            String jsonString = json.toString();
            String toSend = (keyManager != null)
                    ? keyManager.encryptAes(jsonString)
                    : jsonString; // jeśli nie mamy KeyManagera, wysyłamy plain JSON
            writer.println(toSend);
        } catch (Exception e) {
            writer.println("{\"status\":\"error\",\"code\":500,\"message\":\"Encryption failed\"}");
        }
    }

    private void closeSocketQuietly(Socket socket) {
        try {
            socket.close();
        } catch (Exception ignored) {}
    }

    private static class InitRequest {
        String token;
        String game;
    }

    public static void main(String[] args) throws Exception {
        new GameServer().start();
    }
}