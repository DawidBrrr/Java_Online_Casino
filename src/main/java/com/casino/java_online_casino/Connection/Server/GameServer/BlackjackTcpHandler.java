package com.casino.java_online_casino.Connection.Server.GameServer;

import com.casino.java_online_casino.Connection.Server.DTO.GameStateDTO;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.games.blackjack.controller.BlackJackController;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class BlackjackTcpHandler implements Runnable {

    private final Socket clientSocket;
    private final BlackJackController controller;
    private final KeyManager keyManager;
    private final Gson gson = new Gson();

    public BlackjackTcpHandler(Socket clientSocket, BlackJackController controller, KeyManager keyManager) {
        this.clientSocket = clientSocket;
        this.controller = controller;
        this.keyManager = keyManager;
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

                CommandRequest commandRequest;
                try {
                    commandRequest = gson.fromJson(jsonRequest, CommandRequest.class);
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
                    // opcjonalnie, zamykamy połączenie na prośbę klienta
                    break;
                }

                synchronized(controller) {
                    switch (command) {
                        case "newgame":
                            controller.startNewGame();
                            break;
                        case "hit":
                            controller.playerHit();
                            break;
                        case "stand":
                            controller.playerStand();
                            break;
                        default:
                            sendError(writer, "Unknown command");
                            continue;
                    }
                }

                GameStateDTO state = GameStateDTO.fromController(controller);
                String jsonResponse = gson.toJson(state);
                System.out.println(jsonResponse);

                String encryptedResponse;
                try {
                    encryptedResponse = keyManager.encryptAes(jsonResponse); // tutaj poprawiłem: wysyłamy jsonResponse, a nie jsonRequest
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
        // Możemy wysłać zwykły, nieszyfrowany JSON błędu lub zaszyfrowany - tutaj dla uproszczenia nieszyfrowany
        writer.println("{\"error\":\"" + message + "\"}");
    }

    private static class CommandRequest {
        String command;
    }
}
