
package com.casino.java_online_casino.games.blackjack.controller;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Server.DTO.GameStateDTO;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class BlackjackTcpClient {

    private final String token;
    private final KeyManager keyManager;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final Gson gson = new Gson();
    private int  betValue;
    public BlackjackTcpClient(String token, KeyManager keyManager) {
        this.token = token;
        this.keyManager = keyManager;
    }

    public void connect() throws IOException {
        this.socket = new Socket(ServerConfig.getGameServerHost(), ServerConfig.getGameServerPort());
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        InitRequest initRequest = new InitRequest(token, "blackjack");
        String initJson = gson.toJson(initRequest);
        writer.println(initJson);
        writer.flush();
        socket.setSoTimeout(5000);
        System.out.println("[DEBUG JACK CLIENT] Połączono z serwerem i wysłano initRequest.");
    }

    private GameStateDTO sendCommand(String command) throws Exception {
        CommandRequest req = new CommandRequest(command);
        String jsonReq = gson.toJson(req);
        String encryptedReq = keyManager.encryptAes(jsonReq);
        writer.println(encryptedReq);

        String encryptedResp = reader.readLine();
        System.out.println("[DEBUG JACK CLIENT] Odebrano odpowiedź: " + encryptedResp);
        if (encryptedResp == null) {
            throw new IOException("[DEBUG JACK CLIENT] Połączenie zamknięte przez serwer.");
        }

        // Jeśli odpowiedź wygląda na plain JSON (nie base64), nie próbuj jej deszyfrować!
        if (encryptedResp.strip().startsWith("{")) {
            JsonObject errorObj = JsonParser.parseString(encryptedResp).getAsJsonObject();
            if (errorObj.has(JsonFields.HTTP_STATUS) && errorObj.has(JsonFields.HTTP_CODE) && errorObj.has(JsonFields.HTTP_MESSAGE)) {
                throw new IOException("[DEBUG JACK CLIENT] Serwer zwrócił błąd: " + errorObj.get(JsonFields.HTTP_MESSAGE).getAsString());
            }
            throw new IOException("[DEBUG JACK CLIENT] Serwer zwrócił nieoczekiwany plain JSON: " + encryptedResp);
        }

        // Odpowiedź wygląda na base64 – deszyfruj
        String jsonResp = keyManager.decryptAes(encryptedResp);
        System.out.println("[DEBUG JACK CLIENT] Odszyfrowana odpowiedź: " + jsonResp);

        JsonObject wholeJson = JsonParser.parseString(jsonResp).getAsJsonObject();

        if (wholeJson.has(JsonFields.HTTP_STATUS) && wholeJson.has(JsonFields.HTTP_CODE) && wholeJson.has(JsonFields.HTTP_MESSAGE)) {
            String status = wholeJson.get(JsonFields.HTTP_STATUS).getAsString();
            if (status.equalsIgnoreCase(JsonFields.HTTP_ERROR)) {
                throw new IOException("[DEBUG JACK CLIENT] Serwer zwrócił błąd: " + wholeJson.get(JsonFields.HTTP_MESSAGE).getAsString());
            } else if (status.equalsIgnoreCase(JsonFields.HTTP_CHECK)) {
                // Odpowiadamy pongiem na ping/check serwera
                String pong = keyManager.encryptAes(ServerJsonMessage.pong().toString());
                writer.println(pong);
                writer.flush();
                System.out.println("[DEBUG JACK CLIENT] Odpowiedź pong na ping serwera: " + LocalDateTime.now());
                // Możesz zwrócić null lub kontynuować oczekiwanie na kolejną odpowiedź, jeśli to część flow
                return null;
            }
        }

        if (wholeJson.has(JsonFields.DATA)) {
            return gson.fromJson(wholeJson.get(JsonFields.DATA).getAsString(), GameStateDTO.class);
        }
        throw new IOException("[DEBUG JACK CLIENT] Nieprawidłowy format odpowiedzi od serwera: " + jsonResp);
    }

    public GameStateDTO newGame() throws Exception {
        return sendCommand("newgame,"+betValue);
    }

    public GameStateDTO hit() throws Exception {
        return sendCommand("hit");
    }

    public GameStateDTO stand() throws Exception {
        return sendCommand("stand");
    }

    public GameStateDTO reconnect() throws Exception {
        return sendCommand("reconnect");
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
            System.out.println("[DEBUG JACK CLIENT] Zamknięto połączenie.");
        } catch (Exception e) {
            System.out.println("[DEBUG JACK CLIENT] Błąd przy zamykaniu połączenia: " + e.getMessage());
        }
    }

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

    public String getToken() {
        return token;
    }

    public int getBetValue() {
        return betValue;
    }

    public void setBetValue(int betValue) {
        this.betValue = betValue;
    }
}
