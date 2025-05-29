package com.casino.java_online_casino.games.blackjack.controller;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Server.DTO.GameStateDTO;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Klient TCP kompatybilny z GameServer i BlackjackTcpHandler.
 * Wysyła token i nazwę gry na start,
 * a następnie wykonuje komendy: newGame, hit, stand,
 * szyfrując je AES i deszyfrując odpowiedzi.
 */
public class BlackjackTcpClient {

    private final String token;
    private final KeyManager keyManager;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final Gson gson = new Gson();

    public BlackjackTcpClient(String token, KeyManager keyManager) {
        this.token = token;
        this.keyManager = keyManager;
    }

    /**
     * Nawiązuje połączenie i przesyła inicjalny JSON z tokenem i nazwą gry.
     */
    public void connect() throws IOException {
        this.socket = new Socket(ServerConfig.getGameServerHost(), ServerConfig.getGameServerPort());
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        // Wysłanie inicjalnego JSON z tokenem i nazwą gry "blackjack"
        InitRequest initRequest = new InitRequest(token, "blackjack");
        String initJson = gson.toJson(initRequest);
        writer.println(initJson);
        writer.flush();
        socket.setSoTimeout(5000);
    }

    /**
     * Wysyła komendę do serwera i zwraca obiekt GameStateDTO z odpowiedzi.
     * Komenda jest szyfrowana AES.
     */
    private GameStateDTO sendCommand(String command) throws Exception {
        CommandRequest req = new CommandRequest(command);
        String jsonReq = gson.toJson(req);
        String encryptedReq = keyManager.encryptAes(jsonReq);
        writer.println(encryptedReq);

        String encryptedResp = reader.readLine();
        if (encryptedResp == null) {
            throw new IOException("Połączenie zamknięte przez serwer.");
        }

        // Jeśli serwer zwrócił JSON z błędem (bez szyfrowania), próbujemy to wykryć
        if (encryptedResp.startsWith("{")) {
            ErrorResponse error = gson.fromJson(encryptedResp, ErrorResponse.class);
            if (error.error != null) {
                throw new IOException("Serwer zwrócił błąd: " + error.error);
            }
        }

        String jsonResp = keyManager.decryptAes(encryptedResp);
        return gson.fromJson(jsonResp, GameStateDTO.class);
    }

    // Metody odpowiadające akcjom w kontrolerze Blackjack

    public GameStateDTO newGame() throws Exception {
        return sendCommand("newgame");
    }

    public GameStateDTO hit() throws Exception {
        return sendCommand("hit");
    }

    public GameStateDTO stand() throws Exception {
        return sendCommand("stand");
    }

    /**
     * Zamknięcie połączenia.
     */
    public void close() {
        try {
            if (writer != null) {
                writer.println(keyManager.encryptAes("{\"command\":\"exit\"}")); // wysłanie exit
                writer.flush();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {}
    }

    // --- Klasy pomocnicze ---

    private static class InitRequest {
        String token;
        String game;

        InitRequest(String token, String game) {
            this.token = token;
            this.game = game;
        }
    }

    private static class CommandRequest {
        String command;

        CommandRequest(String command) {
            this.command = command;
        }
    }

    private static class ErrorResponse {
        String error;
    }
}
