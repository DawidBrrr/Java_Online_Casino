package com.casino.java_online_casino.games.poker.controller;

import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Utils.LogManager;
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


    private String readMessage() throws IOException {
        try {
            String message = reader.readLine();
            if (message == null) {
                LogManager.logToFile("[BŁĄD POKER CLIENT] Połączenie zamknięte przez serwer");
                throw new IOException("Połączenie zamknięte przez serwer");
            }

            if (!message.startsWith("{")) {
                return keyManager.decryptAes(message);
            }
            return message;
        } catch (Exception e) {
            LogManager.logToFile("[BŁĄD POKER CLIENT] Błąd podczas odczytu wiadomości: " + e.getMessage());
            throw new IOException("Błąd podczas odczytu wiadomości: " + e.getMessage());
        }
    }



    public void connect() throws IOException {
        System.out.println("[DEBUG POKER CLIENT] Rozpoczynam połączenie z serwerem pokerowym...");
        this.socket = new Socket(ServerConfig.getGameServerHost(), ServerConfig.getGameServerPort());
        System.out.println("[DEBUG POKER CLIENT] Socket utworzony pomyślnie.");
        this.socket.setSoTimeout(5000);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        InitRequest initRequest = new InitRequest(token, "poker", roomId);
        String initJson = gson.toJson(initRequest);
        System.out.println("[DEBUG POKER CLIENT] Przygotowano wiadomość inicjalizacyjną: " + initJson);
        writer.println(initJson);
        writer.flush();
        System.out.println("[DEBUG POKER CLIENT] Połączono z serwerem i wysłano initRequest: " + initJson);
        LogManager.logToFile("[DEBUG POKER CLIENT] Połączono z serwerem i wysłano initRequest: " + initJson);
    }

    private PokerDTO sendCommand(String command, int amount) throws Exception {
        System.out.println("[DEBUG POKER CLIENT] Próba wysłania komendy: " + command + ", kwota: " + amount + ", roomId: " + roomId);
        if (!isConnected()) {
            System.out.println("[DEBUG POKER CLIENT] Brak połączenia z serwerem – nie można wysłać komendy.");
            LogManager.logToFile("[BŁĄD POKER CLIENT] Brak połączenia z serwerem");
            throw new IOException("Brak połączenia z serwerem");
        }

        PokerCommandRequest req = new PokerCommandRequest(command, amount, roomId);
        String jsonReq = gson.toJson(req);
        System.out.println("[DEBUG POKER CLIENT] Komenda w formacie JSON: " + jsonReq);
        String encryptedReq = keyManager.encryptAes(jsonReq);
        System.out.println("[DEBUG POKER CLIENT] Komenda zaszyfrowana: " + encryptedReq);
        writer.println(encryptedReq);
        writer.flush();
        System.out.println("[DEBUG POKER CLIENT] Komenda wysłana do serwera.");

        String encryptedResp = reader.readLine();
        System.out.println("[DEBUG POKER CLIENT] Odebrano odpowiedź (zaszyfrowaną): " + encryptedResp);
        LogManager.logToFile("[DEBUG POKER CLIENT] Odebrano odpowiedź: " + encryptedResp);

        if (encryptedResp == null) {
            System.out.println("[DEBUG POKER CLIENT] Połączenie zamknięte przez serwer podczas oczekiwania na odpowiedź.");
            LogManager.logToFile("[BŁĄD POKER CLIENT] Połączenie zamknięte przez serwer");
            throw new IOException("Połączenie zamknięte przez serwer");
        }

        if (encryptedResp.strip().startsWith("{")) {
            System.out.println("[DEBUG POKER CLIENT] Otrzymano odpowiedź w formacie JSON (niezaszyfrowana).");
            JsonObject errorObj = JsonParser.parseString(encryptedResp).getAsJsonObject();
            if (errorObj.has("error")) {
                System.out.println("[DEBUG POKER CLIENT] Otrzymano błąd od serwera: " + errorObj.get("error").getAsString());
                throw new IOException(errorObj.get("error").getAsString());
            }
            PokerDTO dto = gson.fromJson(encryptedResp, PokerDTO.class);
            System.out.println("[DEBUG POKER CLIENT] Odpowiedź zdeserializowana do PokerDTO.");
            return dto;
        }

        String jsonResp = keyManager.decryptAes(encryptedResp);
        System.out.println("[DEBUG POKER CLIENT] Odpowiedź odszyfrowana: " + jsonResp);
        LogManager.logToFile("[DEBUG POKER CLIENT] Odszyfrowana odpowiedź: " + jsonResp);
        PokerDTO dto = gson.fromJson(jsonResp, PokerDTO.class);
        System.out.println("[DEBUG POKER CLIENT] Odpowiedź zdeserializowana do PokerDTO.");
        return dto;
    }

    public void sendEncryptedMessage(String message) throws IOException {
        System.out.println("[DEBUG POKER CLIENT] Przygotowuję do wysłania wiadomość: " + message);
        try {
            String encrypted = keyManager.encryptAes(message);
            System.out.println("[DEBUG POKER CLIENT] Wiadomość zaszyfrowana: " + encrypted);
            writer.println(encrypted);
            writer.flush();
            System.out.println("[DEBUG POKER CLIENT] Zaszyfrowana wiadomość wysłana do serwera.");
            LogManager.logToFile("[DEBUG POKER CLIENT] Wysłano zaszyfrowaną wiadomość: " + message);
        } catch (Exception e) {
            System.out.println("[DEBUG POKER CLIENT] Błąd podczas szyfrowania/wysyłania wiadomości: " + e.getMessage());
            LogManager.logToFile("[BŁĄD POKER CLIENT] Błąd podczas szyfrowania/wysyłania wiadomości: " + e.getMessage());
            throw new IOException("Błąd podczas szyfrowania/wysyłania wiadomości: " + e.getMessage());
        }
    }

    public String readEncryptedMessage(int timeoutMillis) throws IOException {
        System.out.println("[DEBUG POKER CLIENT] Rozpoczynam oczekiwanie na wiadomość zaszyfrowaną (timeout: " + timeoutMillis + " ms)");
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (reader.ready()) {
                String encryptedMessage = reader.readLine();
                System.out.println("[DEBUG POKER CLIENT] Odebrano wiadomość (zaszyfrowaną): " + encryptedMessage);
                if (encryptedMessage == null) {
                    System.out.println("[DEBUG POKER CLIENT] Połączenie zamknięte przez serwer podczas odczytu wiadomości.");
                    LogManager.logToFile("[BŁĄD POKER CLIENT] Połączenie zamknięte przez serwer");
                    throw new IOException("Połączenie zamknięte przez serwer");
                }

                try {
                    String decrypted = keyManager.decryptAes(encryptedMessage);
                    System.out.println("[DEBUG POKER CLIENT] Wiadomość odszyfrowana: " + decrypted);
                    return decrypted;
                } catch (Exception e) {
                    System.out.println("[DEBUG POKER CLIENT] Błąd deszyfrowania wiadomości: " + e.getMessage());
                    LogManager.logToFile("[BŁĄD POKER CLIENT] Błąd deszyfrowania: " + e.getMessage());
                    throw new IOException("Błąd deszyfrowania: " + e.getMessage());
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("[DEBUG POKER CLIENT] Przerwano oczekiwanie na wiadomość: " + e.getMessage());
                Thread.currentThread().interrupt();
                LogManager.logToFile("[BŁĄD POKER CLIENT] Przerwano oczekiwanie na wiadomość: " + e.getMessage());
                throw new IOException("Przerwano oczekiwanie na wiadomość");
            }
        }
        System.out.println("[DEBUG POKER CLIENT] Timeout podczas oczekiwania na wiadomość.");
        LogManager.logToFile("[BŁĄD POKER CLIENT] Timeout: brak odpowiedzi w ciągu " + timeoutMillis + "ms");
        throw new IOException("Timeout: brak odpowiedzi w ciągu " + timeoutMillis + "ms");
    }

    public void close() {
        System.out.println("[DEBUG POKER CLIENT] Rozpoczynam zamykanie połączenia...");
        try {
            if (writer != null) {
                sendEncryptedMessage("{\"command\":\"exit\"}");
                System.out.println("[DEBUG POKER CLIENT] Komenda exit wysłana do serwera.");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("[DEBUG POKER CLIENT] Socket zamknięty pomyślnie.");
            }
            System.out.println("[DEBUG POKER CLIENT] Połączenie zamknięte.");
            LogManager.logToFile("[DEBUG POKER CLIENT] Zamknięto połączenie");
        } catch (Exception e) {
            System.out.println("[DEBUG POKER CLIENT] Błąd przy zamykaniu połączenia: " + e.getMessage());
            LogManager.logToFile("[BŁĄD POKER CLIENT] Błąd przy zamykaniu połączenia: " + e.getMessage());
        }
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