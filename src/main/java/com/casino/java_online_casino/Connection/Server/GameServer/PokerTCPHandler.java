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
        System.out.println("[DEBUG] Rozpoczęto obsługę klienta: " + clientSocket);

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String base64Request;

            while ((base64Request = reader.readLine()) != null) {
                System.out.println("[DEBUG] Odebrano żądanie (base64): " + base64Request);

                if (base64Request.isBlank()) {
                    System.out.println("[BŁĄD] Odebrano puste żądanie");
                    sendError(writer, "Puste żądanie");
                    continue;
                }

                String jsonRequest;
                try {
                    jsonRequest = keyManager.decryptAes(base64Request);
                    System.out.println("[DEBUG] Pomyślnie odszyfrowano żądanie: " + jsonRequest);
                } catch (Exception e) {
                    System.out.println("[BŁĄD] Nie udało się odszyfrować żądania: " + e.getMessage());
                    sendError(writer, "Błąd odszyfrowania");
                    continue;
                }

                PokerCommandRequest commandRequest;
                try {
                    commandRequest = gson.fromJson(jsonRequest, PokerCommandRequest.class);
                    System.out.println("[DEBUG] Pomyślnie sparsowano JSON do PokerCommandRequest: " + gson.toJson(commandRequest));
                } catch (Exception e) {
                    System.out.println("[BŁĄD] Błędny format JSON: " + e.getMessage());
                    sendError(writer, "Błędny format JSON");
                    continue;
                }

                if (commandRequest == null || commandRequest.command == null) {
                    System.out.println("[BŁĄD] Brak komendy w żądaniu: " + jsonRequest);
                    sendError(writer, "Brak komendy");
                    continue;
                }

                String command = commandRequest.command.trim().toLowerCase();
                System.out.println("[DEBUG] Otrzymano komendę: " + command);

                if ("exit".equals(command) || "quit".equals(command)) {
                    System.out.println("[INFO] Gracz " + commandRequest.playerId + " opuszcza pokój.");
                    room.removePlayer(commandRequest.playerId);
                    System.out.println("[DEBUG] Gracz został usunięty z pokoju.");
                    break;
                }

                synchronized(controller) {
                    try {
                        switch (command) {
                            case "fold":
                                System.out.println("[DEBUG] Wykonywana akcja: fold dla gracza " + commandRequest.playerId);
                                controller.fold();
                                System.out.println("[DEBUG] Akcja fold wykonana poprawnie.");
                                break;
                            case "call":
                                System.out.println("[DEBUG] Wykonywana akcja: call (" + commandRequest.amount + ") dla gracza " + commandRequest.playerId);
                                controller.call(commandRequest.amount);
                                System.out.println("[DEBUG] Akcja call wykonana poprawnie.");
                                break;
                            case "raise":
                                System.out.println("[DEBUG] Wykonywana akcja: raise (" + commandRequest.amount + ") dla gracza " + commandRequest.playerId);
                                controller.raise(commandRequest.amount);
                                System.out.println("[DEBUG] Akcja raise wykonana poprawnie.");
                                break;
                            case "check":
                                System.out.println("[DEBUG] Wykonywana akcja: check dla gracza " + commandRequest.playerId);
                                controller.check();
                                System.out.println("[DEBUG] Akcja check wykonana poprawnie.");
                                break;
                            case "allin":
                                System.out.println("[DEBUG] Wykonywana akcja: all-in (" + commandRequest.amount + ") dla gracza " + commandRequest.playerId);
                                controller.allIn(commandRequest.amount);
                                System.out.println("[DEBUG] Akcja all-in wykonana poprawnie.");
                                break;
                            default:
                                System.out.println("[BŁĄD] Nieznana komenda: " + command);
                                sendError(writer, "Nieznana komenda");
                                continue;
                        }
                    } catch (Exception e) {
                        System.out.println("[BŁĄD] Nie udało się wykonać akcji (" + command + "): " + e.getMessage());
                        sendError(writer, "Błąd akcji: " + e.getMessage());
                        continue;
                    }
                }

                String jsonResponse = gson.toJson(new PokerResponse(false, null, controller));
                System.out.println("[DEBUG] Wygenerowano odpowiedź JSON: " + jsonResponse);

                String encryptedResponse;
                try {
                    encryptedResponse = keyManager.encryptAes(jsonResponse);
                    System.out.println("[DEBUG] Pomyślnie zaszyfrowano odpowiedź.");
                } catch (Exception e) {
                    System.out.println("[BŁĄD] Nie udało się zaszyfrować odpowiedzi: " + e.getMessage());
                    sendError(writer, "Błąd szyfrowania");
                    continue;
                }

                writer.println(encryptedResponse);
                System.out.println("[DEBUG] Odpowiedź wysłana do klienta.");
            }

        } catch (Exception e) {
            System.out.println("[BŁĄD] Wyjątek w PokerTCPHandler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("[INFO] Zamknięto połączenie z klientem: " + clientSocket);
            } catch (Exception ignored) {
                System.out.println("[BŁĄD] Nie udało się zamknąć połączenia z klientem: " + clientSocket);
            }
        }
    }

    private void sendError(PrintWriter writer, String message) {
        System.out.println("[BŁĄD] Wysyłanie błędu do klienta: " + message);
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