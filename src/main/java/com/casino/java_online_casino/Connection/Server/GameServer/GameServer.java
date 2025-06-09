package com.casino.java_online_casino.Connection.Server.GameServer;

import com.casino.java_online_casino.Connection.Games.BlackjackTcpHandler;
import com.casino.java_online_casino.Connection.Games.Game;
import com.casino.java_online_casino.Connection.Server.Rooms.Room;
import com.casino.java_online_casino.Connection.Server.Rooms.RoomManager;
import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
import com.casino.java_online_casino.games.blackjack.controller.BlackJackController;
import com.casino.java_online_casino.games.poker.model.Player;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class GameServer {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final SessionManager sessionManager = SessionManager.getInstance();

    public void start() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(ServerConfig.getGameServerPort())) {
            System.out.println("[INFO] GameServer nasłuchuje na porcie " + ServerConfig.getGameServerPort());
            LogManager.logToFile("[INFO] GameServer nasłuchuje na porcie " + ServerConfig.getGameServerPort());
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        UUID playerUUID ;
        SessionManager.SessionToken session = null;
        boolean locked = false;
        final int userId;
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            System.out.println("[DEBUG GAME SERVER] Nowe połączenie: " + clientSocket.getRemoteSocketAddress());
            LogManager.logToFile("[DEBUG GAME SERVER] Nowe połączenie: " + clientSocket.getRemoteSocketAddress());

            String initRequest = reader.readLine();
            System.out.println("[DEBUG GAME SERVER] Odebrano initRequest: " + initRequest);
            LogManager.logToFile("[DEBUG GAME SERVER] Odebrano initRequest: " + initRequest);

            if (initRequest == null || initRequest.isBlank()) {
                writer.println("{\"status\":\"error\",\"code\":400,\"message\":\"Empty initial request\"}");
                return;
            }
            InitRequest request = new com.google.gson.Gson().fromJson(initRequest, InitRequest.class);
            if (request == null || request.token == null || request.game == null) {
                writer.println("{\"status\":\"error\",\"code\":400,\"message\":\"Missing token or game\"}");
                return;
            }

            Object uuidObj = ServerTokenManager.validateJwt(request.token).get(JsonFields.UUID);
            String id = (String)ServerTokenManager.validateJwt(request.token).get(JsonFields.ID);
            if (uuidObj == null || id == null || id.isBlank()) {
                writer.println("{\"status\":\"error\",\"code\":401,\"message\":\"Invalid token\"}");
                return;
            }
            playerUUID = UUID.fromString(uuidObj.toString());
            userId = Integer.parseInt(id);
            if(userId == -1){
                writer.println("{\"status\":\"error\",\"code\":401,\"message\":\"Invalid token\"}");
                return;
            }

            // Pobierz sesję po UUID (do szyfrowania)
            session = sessionManager.getSessionByUUID(playerUUID);
            if (session == null) {
                System.out.println("[DEBUG] nie uznano żadnej sesji ");
                LogManager.logToFile("[DEBUG] nie uznano żadnej sesji dla UUID: " + playerUUID);
                writer.println("{\"status\":\"error\",\"code\":404,\"message\":\"Session not found\"}");
                return;
            }

            List<SessionManager.SessionToken> candidateSessions = sessionManager.findSessionsByUserId(userId);

// Jeśli nie ma żadnej sesji z tym userId – błąd
            if (candidateSessions.isEmpty()) {
                System.out.println("[DEBUG] Session not found");
                LogManager.logToFile("[DEBUG] Session not found");
                writer.println("{\"status\":\"error\",\"code\":404,\"message\":\"No session found for this userId\"}");
                return;
            }


            SessionManager.SessionToken currentSession = sessionManager.getSessionByUUID(playerUUID);


            Game foundGame = null;
            for (SessionManager.SessionToken s : candidateSessions) {
                if (!s.getUuid().equals(playerUUID)) {
                    if (foundGame == null && s.getGame() != null) {
                        foundGame = s.getGame();
                    }
                    sessionManager.deleteSessionByUUID(s.getUuid());
                    System.out.println("[DEBUG GAME SERVER] Usunięto starą sesję: " + s.getUuid());
                    LogManager.logToFile("[DEBUG GAME SERVER] Usunięto starą sesję: " + s.getUuid());
                }
            }

            if (foundGame != null) {
                currentSession.setGame(foundGame);
            }

            KeyManager currentKeyManager = currentSession.getKeyManager();
            // --- KONIEC KLUCZOWEJ LOGIKI ---

            // Próbuj zablokować dostęp do gry
            if (!session.tryLockGame()) {
                if (tryPingPong(clientSocket,currentKeyManager)) {
                    System.out.println("[DEBUG] Game already started by another client");
                    writer.println("{\"status\":\"error\",\"code\":409,\"message\":\"Game already in use by another client\"}");
                    return;
                } else {
                    session.unlockGame();
                    if (!session.tryLockGame()) {
                        writer.println("{\"status\":\"error\",\"code\":409,\"message\":\"Game already in use by another client (lock error)\"}");
                        return;
                    }
                }
            }
            locked = true;

            Game gameInstance = session.getGame();
            Runnable gameHandler;
            if (session.getGame() == null) {
                switch (request.game.toLowerCase()) {
                    case "blackjack":
                        System.out.println("[DEBUG] Blackjack game");
                        LogManager.logToFile("[DEBUG] Blackjack game");
                        session.setGame(new BlackJackController());
                        break;
                    case "poker":
                        System.out.println("[DEBUG] Poker game");
                        LogManager.logToFile("[DEBUG] Poker game");
                        // Nie ustawiamy Room w sesji, bo RoomManager zarządza pokojami
                        break;
                    // Dodaj kolejne gry tutaj
                    default:
                        writer.println("{\"status\":\"error\",\"code\":400,\"message\":\"Unknown game type\"}");
                        return;
                }
            }
            switch (request.game.toLowerCase()) {
                case "blackjack":
                    if (session.getGame() == null) {
                        session.setGame(new BlackJackController());
                    }
                    gameHandler = new BlackjackTcpHandler(clientSocket, (BlackJackController) session.getGame(), session);
                    break;

                case "poker":
                    System.out.println("[DEBUG] Poker game");
                    LogManager.logToFile("[DEBUG] Poker game");

                    RoomManager pokerRoomManager = RoomManager.getInstance();
                    Player pokerPlayer = new Player(String.valueOf(userId));

                    // Szukaj pierwszego wolnego pokoju
                    Optional<String> freeRoom = pokerRoomManager.firstFreeRoomId(RoomManager.DEFAULT_MAX_PLAYERS);
                    Room room;
                    if (freeRoom.isPresent()) {
                        room = pokerRoomManager.findRoom(freeRoom.get());
                        room.addPlayer(pokerPlayer);
                    } else {
                        room = RoomManager.getInstance().findRoom(pokerRoomManager.createRoom(RoomManager.DEFAULT_MAX_PLAYERS));
                        room.addPlayer(pokerPlayer);
                        RoomManager.getInstance().addRoom(room);
                    }

                    if (room == null) {
                        writer.println("{\"status\":\"error\",\"code\":500,\"message\":\"Nie udało się utworzyć/znaleźć pokoju pokerowego\"}");
                        return;
                    }

                    JsonObject response = new JsonObject();
                    response.addProperty("roomId", room.getRoomId());
                    response.addProperty("type", room.getPlayers().size() == 1 ? "room_created" : "room_joined");

                    String encryptedResponse = currentKeyManager.encryptAes(response.toString());
                    writer.println(encryptedResponse);
                    writer.flush();

                    gameHandler = new PokerTCPHandler(clientSocket, room.getRoomId(), currentSession);
                    break;

                // Dodaj kolejne gry tutaj

                default:
                    writer.println("{\"error\":\"Unknown game type\"}");
                    clientSocket.close();
                    return;
            }
            gameHandler.run();

        } catch (Exception e) {
            System.err.println("[ERROR] GameServer: " + e.getMessage());
            LogManager.logToFile("[ERROR] GameServer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (locked && session != null) {
                session.unlockGame();
            }
            try {
                clientSocket.close();
            } catch (Exception ignored) {}
        }
    }

    private static class InitRequest {
        String token;
        String game;
    }

    public static void main(String[] args) throws Exception {
        new GameServer().start();
    }

    boolean tryPingPong(Socket clientSocket, KeyManager keyManager) {
        try {
            String encryptedPing = keyManager.encryptAes(ServerJsonMessage.ping().toString());
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            writer.println(encryptedPing);
            writer.flush();

            clientSocket.setSoTimeout(5000);

            String response = reader.readLine();
            if (response == null) return false;

            String decrypted;
            try {
                decrypted = keyManager.decryptAes(response);
            } catch (Exception e) {
                return false;
            }
            JsonObject respJson = JsonParser.parseString(decrypted).getAsJsonObject();
            if (respJson.has("status") && respJson.has("code")) {
                String status = respJson.get("status").getAsString();
                int code = respJson.get("code").getAsInt();
                return code == 101 && "check".equalsIgnoreCase(status);
            }
            return false;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
