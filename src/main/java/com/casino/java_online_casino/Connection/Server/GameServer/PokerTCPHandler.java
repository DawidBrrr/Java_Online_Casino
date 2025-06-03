package com.casino.java_online_casino.Connection.Server.GameServer;

import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.Connection.Server.Rooms.PokerRoom;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.games.poker.controller.RemotePokerController;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PokerTCPHandler implements Runnable {
    private final Socket clientSocket;
    private final PokerRoom room;
    private final KeyManager keyManager;
    private final Gson gson = new Gson();
    private final RemotePokerController controller;

    public PokerTCPHandler(Socket clientSocket, PokerRoom room, KeyManager keyManager) {
        this.clientSocket = clientSocket;
        this.room = room;
        this.keyManager = keyManager;
        this.controller = room.getController();
    }

    @Override
    public void run() {
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String base64Request;

            while ((base64Request = reader.readLine()) != null) {
                if (base64Request.isBlank()) {
                    sendError(writer, "Empty request");
                    continue;
                }

                String jsonRequest;
                try {
                    jsonRequest = keyManager.decryptAes(base64Request);
                } catch (Exception e) {
                    sendError(writer, "Decryption failed");
                    continue;
                }

                PokerCommandRequest commandRequest;
                try {
                    commandRequest = gson.fromJson(jsonRequest, PokerCommandRequest.class);
                } catch (Exception e) {
                    sendError(writer, "Invalid JSON format");
                    continue;
                }

                if (commandRequest == null || commandRequest.command == null) {
                    sendError(writer, "Missing command");
                    continue;
                }

                String command = commandRequest.command.trim().toLowerCase();

                if ("exit".equals(command) || "quit".equals(command)) {
                    room.removePlayer(commandRequest.playerId);
                    break;
                }

                synchronized(controller) {
                    try {
                        switch (command) {
                            case "fold":
                                controller.fold();
                                break;
                            case "call":
                                controller.call(commandRequest.amount);
                                break;
                            case "raise":
                                controller.raise(commandRequest.amount);
                                break;
                            case "check":
                                controller.check();
                                break;
                            case "allin":
                                controller.allIn(commandRequest.amount);
                                break;
                            default:
                                sendError(writer, "Unknown command");
                                continue;
                        }
                    } catch (Exception e) {
                        sendError(writer, "Action failed: " + e.getMessage());
                        continue;
                    }
                }

                String jsonResponse = gson.toJson(new PokerResponse(false, null, controller));

                String encryptedResponse;
                try {
                    encryptedResponse = keyManager.encryptAes(jsonResponse);
                } catch (Exception e) {
                    sendError(writer, "Encryption failed");
                    continue;
                }

                writer.println(encryptedResponse);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (Exception ignored) {}
        }
    }

    private void sendError(PrintWriter writer, String message) {
        writer.println("{\"error\":\"" + message + "\"}");
    }

    private static class PokerCommandRequest {
        String command;
        int playerId;
        int amount;
    }

    private static class PokerResponse {
        boolean error;
        String errorMessage;
        PokerDTO gameState;

        public PokerResponse(boolean error, String errorMessage, RemotePokerController controller) {
            this.error = error;
            this.errorMessage = errorMessage;
            if (!error && controller != null) {
                this.gameState = new PokerDTO();
                // wypełnienie stanu gry z kontrolera
                this.gameState.gameState = controller.getGameStatus();
                this.gameState.pot = controller.getPot();
                this.gameState.currentBet = controller.getCurrentBet();
                this.gameState.players = controller.getPlayers();
                this.gameState.activePlayerId = controller.getActivePlayerId();
                this.gameState.isGameOver = controller.isGameOver();
                this.gameState.winnerId = controller.getWinnerId();
                this.gameState.minimumBet = controller.getMinimumBet();
            }
        }
    }
}