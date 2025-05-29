package com.casino.java_online_casino.Connection.Games;

import com.casino.java_online_casino.Connection.Server.DTO.GameStateDTO;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.games.blackjack.controller.BlackJackController;
import com.google.gson.Gson;

import java.io.*;
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
            handleClientSession(reader, writer);
        } catch (Exception e) {
            System.err.println("[ERROR] Błąd w BlackjackTcpHandler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeSocketQuietly();
        }
    }

    private void handleClientSession(BufferedReader reader, PrintWriter writer) throws IOException {
        String base64Request;
        while ((base64Request = reader.readLine()) != null) {
            if (base64Request.isBlank()) {
                sendError(writer, "Empty request");
                continue;
            }

            String jsonRequest = decryptRequest(base64Request, writer);
            if (jsonRequest == null) continue;

            CommandRequest commandRequest = parseCommandRequest(jsonRequest, writer);
            if (commandRequest == null) continue;

            String command = commandRequest.command == null ? "" : commandRequest.command.trim().toLowerCase();
            if ("exit".equals(command) || "quit".equals(command)) {
                controller.onPlayerLeave(controller.getCurrentUserId()); // Wywołaj logikę opuszczenia gry
                break;
            }

            if (!processCommand(command, writer)) {
                continue;
            }

            sendGameState(writer);
        }
        // Po zakończeniu pętli (np. utrata połączenia) również wywołaj cleanup
        controller.onPlayerLeave(controller.getCurrentUserId());
    }

    private String decryptRequest(String base64Request, PrintWriter writer) {
        try {
            return keyManager.decryptAes(base64Request);
        } catch (Exception e) {
            sendError(writer, "Decryption failed");
            return null;
        }
    }

    private CommandRequest parseCommandRequest(String jsonRequest, PrintWriter writer) {
        try {
            CommandRequest cmd = gson.fromJson(jsonRequest, CommandRequest.class);
            if (cmd == null || cmd.command == null) {
                sendError(writer, "Missing command");
                return null;
            }
            return cmd;
        } catch (Exception e) {
            sendError(writer, "Invalid JSON format");
            return null;
        }
    }

    private boolean processCommand(String command, PrintWriter writer) {
        synchronized (controller) {
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
                    return false;
            }
        }
        return true;
    }

    private void sendGameState(PrintWriter writer) {
        GameStateDTO state = GameStateDTO.fromController(controller);
        String jsonResponse = gson.toJson(state);
        System.out.println("[DEBUG] Odpowiedź GameState: " + jsonResponse);

        try {
            String encryptedResponse = keyManager.encryptAes(jsonResponse);
            writer.println(encryptedResponse);
        } catch (Exception e) {
            sendError(writer, "Encryption failed");
        }
    }

    private void sendError(PrintWriter writer, String message) {
        writer.println("{\"error\":\"" + message + "\"}");
    }

    private void closeSocketQuietly() {
        try {
            clientSocket.close();
        } catch (Exception ignored) {}
    }

    private static class CommandRequest {
        String command;
    }
}
