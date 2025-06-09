package com.casino.java_online_casino.Connection.Server.GameServer;

import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.Connection.Server.Rooms.Room;
import com.casino.java_online_casino.Connection.Server.Rooms.RoomManager;
import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
import com.casino.java_online_casino.Database.GamerDAO;
import com.casino.java_online_casino.games.poker.model.Player;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PokerTCPHandler implements Runnable ,Room.RoomEventListener{

    private final Socket clientSocket;
    private final String roomId;
    private final SessionManager.SessionToken sessionToken;
    private final KeyManager keyManager;
    private final Gson gson = new Gson();
    private GamerDAO gamerDAO = GamerDAO.getInstance();
    private PrintWriter writer;
    private String userId;
    public PokerTCPHandler(Socket clientSocket, String roomId, SessionManager.SessionToken sessionToken) {
        this.clientSocket = clientSocket;
        this.roomId =  roomId;
        this.sessionToken = sessionToken;
        this.keyManager = sessionToken.getKeyManager();
        this.userId = String.valueOf(sessionToken.getUserId());
        RoomManager.getInstance().findRoom(roomId).setEventListener(this);
    }

    @Override
    public void run() {
        String userId = String.valueOf(sessionToken.getUserId());
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            handleClientSession(reader, writer, userId);
        } catch (Exception e) {
            System.err.println("[ERROR] Błąd w PokerTcpHandler: " + e.getMessage());
            LogManager.logToFile("[ERROR] Błąd w PokerTcpHandler: " + e.getMessage());
            if (userId != null) {
                RoomManager.getInstance().findRoom(roomId).onPlayerLeave(userId);
            }
            e.printStackTrace();
        } finally {
            if (userId != null) {
                RoomManager.getInstance().findRoom(roomId).onPlayerLeave(userId);
            }
            closeSocketQuietly();
        }
    }

    private void handleClientSession(BufferedReader reader, PrintWriter writer, String userId) throws IOException {
        String base64Request;
        while ((base64Request = reader.readLine()) != null) {
            if (base64Request.isBlank()) {
                sendError(writer, ServerJsonMessage.badRequest("Puste żądanie"));
                continue;
            }

            String jsonRequest = decryptRequest(base64Request, writer);
            if (jsonRequest == null) continue;

            CommandRequest commandRequest = parseCommandRequest(jsonRequest, writer);
            if (commandRequest == null) continue;

            String command = commandRequest.command == null ? "" : commandRequest.command.trim().toLowerCase();
            if ("exit".equals(command) || "quit".equals(command)) {
                RoomManager.getInstance().findRoom(roomId).onPlayerLeave(userId);
                break;
            }

            if (!processCommand(command, commandRequest, userId, writer)) {
                continue;
            }
        }
        RoomManager.getInstance().findRoom(roomId).onPlayerLeave(userId);
    }

    private String decryptRequest(String base64Request, PrintWriter writer) {
        try {
            return keyManager.decryptAes(base64Request);
        } catch (Exception e) {
            sendError(writer, ServerJsonMessage.badRequest("Błąd deszyfracji danych"));
            return null;
        }
    }

    private CommandRequest parseCommandRequest(String jsonRequest, PrintWriter writer) {
        try {
            CommandRequest cmd = gson.fromJson(jsonRequest, CommandRequest.class);
            if (cmd == null || cmd.command == null) {
                sendError(writer, ServerJsonMessage.badRequest("Puste dane"));
                return null;
            }
            return cmd;
        } catch (Exception e) {
            sendError(writer, ServerJsonMessage.badRequest("Nieprawidłowy format JSON"));
            return null;
        }
    }

    private boolean processCommand(String command, CommandRequest cmd, String userId, PrintWriter writer) {
        int amount = cmd.amount;
        String name = cmd.playerName != null ? cmd.playerName : "Player";
        int initialBalance = cmd.initialBalance > 0 ? cmd.initialBalance : 1000;

        System.out.println("[NEW COMMAND] " + command + " [userId: " + userId + "]");
        synchronized (RoomManager.getInstance().findRoom(roomId)) {
            switch (command) {
                case "join":
                    // Tworzymy gracza tylko jeśli nie istnieje
                    Player player =  RoomManager.getInstance().findRoom(roomId).getPlayer(userId);
                    if (player == null) {
                        System.out.println("DEBUG POKER HANDLER] Utworzono nowego gracza o id "+ userId);
                        player = new Player(userId);
                    }
                    RoomManager.getInstance().findRoom(roomId).onPlayerJoin(userId);
                    sendPlayerDTO(writer, userId);
                    break;
                case "leave":
                    RoomManager.getInstance().findRoom(roomId).onPlayerLeave(userId);
                    sendPlayerDTO(writer, userId);
                    break;
                case "fold":
                    RoomManager.getInstance().findRoom(roomId).handlePlayerAction(userId, Player.playerAction.FOLD);
                    sendPlayerDTO(writer, userId);
                    break;
                case "call":
                    RoomManager.getInstance().findRoom(roomId).handlePlayerAction(userId, Player.playerAction.CALL);
                    sendPlayerDTO(writer, userId);
                    break;
                case "raise":
                    RoomManager.getInstance().findRoom(roomId).handlePlayerAction(userId, Player.playerAction.RAISE);
                    sendPlayerDTO(writer, userId);
                    break;
                case "check":
                    RoomManager.getInstance().findRoom(roomId).handlePlayerAction(userId, Player.playerAction.CHECK);
                    sendPlayerDTO(writer, userId);
                    break;
                case "allin":
                    RoomManager.getInstance().findRoom(roomId).handlePlayerAction(userId, Player.playerAction.ALL_IN);
                    sendPlayerDTO(writer, userId);
                    break;
                case "get_state":
                case "reconnect":
                    sendPlayerDTO(writer, userId);
                    break;
                case "get_final_state":
                    sendAdminDTO(writer);
                    break;
                default:
                    sendError(writer, ServerJsonMessage.methodNotAllowed());
                    return false;
            }
        }
        return true;
    }
    @Override
    public void onGameStateChanged(String roomId) {
        if (writer != null) {
            System.out.println("[DEBUG HANDLER] [ROOM " + roomId + "] Automatyczny broadcast DTO do klienta " + userId);
            sendPlayerDTO(writer, userId);
        }
    }

    @Override
    public void onPlayerTurn(String playerId, String roomId) {
        if (writer != null && userId.equals(playerId)) {
            System.out.println("[DEBUG HANDLER] [ROOM " + roomId + "] Twoja tura! (userId=" + userId + ")");
            sendPlayerDTO(writer, userId);
        }
    }

    // Wysyła DTO z pełnymi danymi tylko dla klienta, reszta graczy tylko podstawowe info
    private void sendPlayerDTO(PrintWriter writer, String userId) {
        PokerDTO dto =  RoomManager.getInstance().findRoom(roomId).createPokerDTOByUserId(userId);
        JsonObject okMessage = ServerJsonMessage.ok("Stan gry");
        okMessage.addProperty(JsonFields.DATA, gson.toJson(dto));
        try {
            String encryptedResponse = keyManager.encryptAes(okMessage.toString());
            writer.println(encryptedResponse);
        } catch (Exception e) {
            sendError(writer, ServerJsonMessage.internalServerError());
        }
    }


    // Wysyła DTO z pełnymi danymi wszystkich graczy (np. po SHOWDOWN)
    private void sendAdminDTO(PrintWriter writer) {
        PokerDTO dto =  RoomManager.getInstance().findRoom(roomId).createPokerDTOAll();
        JsonObject okMessage = ServerJsonMessage.ok("Stan gry końcowy");
        okMessage.addProperty(JsonFields.DATA, gson.toJson(dto));
        try {
            String encryptedResponse = keyManager.encryptAes(okMessage.toString());
            writer.println(encryptedResponse);
        } catch (Exception e) {
            sendError(writer, ServerJsonMessage.internalServerError());
        }
    }

    private void sendError(PrintWriter writer, JsonObject errorJson) {
        try {
            String encrypted = keyManager.encryptAes(errorJson.toString());
            writer.println(encrypted);
        } catch (Exception e) {
            writer.println("{\"status\":\"error\",\"code\":500,\"message\":\"Encryption failed\"}");
        }
    }

    private void closeSocketQuietly() {
        try {
            clientSocket.close();
        } catch (Exception ignored) {}
    }

    private static class CommandRequest {
        String command;
        String playerName;
        int initialBalance;
        int amount;
    }
}
