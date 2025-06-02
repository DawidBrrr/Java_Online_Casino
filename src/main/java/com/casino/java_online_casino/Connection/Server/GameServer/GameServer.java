package com.casino.java_online_casino.Connection.Server.GameServer;

import com.casino.java_online_casino.Connection.Games.BlackjackTcpHandler;
import com.casino.java_online_casino.Connection.Games.Game;
import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Server.Rooms.PokerRoom;
import com.casino.java_online_casino.Connection.Server.Rooms.PokerRoomManager;
import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
import com.casino.java_online_casino.games.blackjack.controller.BlackJackController;
import com.casino.java_online_casino.games.poker.controller.PokerController;
import com.casino.java_online_casino.games.poker.controller.PokerTCPClient;
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

public class GameServer {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final SessionManager sessionManager = SessionManager.getInstance();

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
        UUID playerUUID = null;
        SessionManager.SessionToken session = null;
        boolean locked = false;
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            System.out.println("[DEBUG GAME SERVER] Nowe połączenie: " + clientSocket.getRemoteSocketAddress());

            String initRequest = reader.readLine();
            System.out.println("[DEBUG GAME SERVER] Odebrano initRequest: " + initRequest);

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
            if (uuidObj == null) {
                writer.println("{\"status\":\"error\",\"code\":401,\"message\":\"Invalid token\"}");
                return;
            }
            playerUUID = UUID.fromString(uuidObj.toString());

            // Pobierz sesję po UUID (do szyfrowania)
            session = sessionManager.getSessionByUUID(playerUUID);
            if (session == null) {
                writer.println("{\"status\":\"error\",\"code\":404,\"message\":\"Session not found\"}");
                return;
            }

            int userId = session.getUserId();
            List<SessionManager.SessionToken> candidateSessions = sessionManager.findSessionsByUserId(userId);

            // Jeśli nie ma żadnej sesji z tym userId – błąd, nie tworzymy nowej!
            if (candidateSessions.isEmpty()) {
                writer.println("{\"status\":\"error\",\"code\":404,\"message\":\"No session found for this userId\"}");
                return;
            }

            // Jeśli jest jedna i to ta sama (UUID == playerUUID), użyj jej
            SessionManager.SessionToken chosenSession = null;
            if (candidateSessions.size() == 1 && candidateSessions.get(0).getUuid().equals(playerUUID)) {
                chosenSession = candidateSessions.get(0);
                System.out.println("[DEBUG GAME SERVER] To ta sama sesja po UUID i userId.");
            } else {
                // Są inne sesje - sprawdź ping-pongiem
                System.out.println("[DEBUG GAME SERVER] Wykryto " + candidateSessions.size() + " sesji dla userId, sprawdzam ping-pongiem...");
                for (SessionManager.SessionToken s : candidateSessions) {
                    if (tryPingPong(clientSocket, s.getKeyManager())) {
                        writer.println("{\"status\":\"error\",\"code\":409,\"message\":\"Game already in use by another client\"}");
                        return;
                    } else {
                        // Usuwamy nieaktywne sesje (zombie)
                        SessionManager.getInstance().deleteSessionByUUID(s.getUuid());
                        System.out.println("[DEBUG GAME SERVER] Usunięto nieaktywną sesję: " + s.getUuid());
                    }
                }
                // Żadna nie odpowiedziała – użyj pierwszej (lub możesz wybrać wg własnej logiki)
                chosenSession = candidateSessions.get(0);
                System.out.println("[DEBUG GAME SERVER] Przełączam się na istniejącą nieaktywną sesję userId: " + userId);
            }
            session = chosenSession;

            // 4. Próbuj zablokować dostęp do gry
            if (!session.tryLockGame()) {
                if (tryPingPong(clientSocket, session.getKeyManager())) {
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

            // 5. Pobierz lub utwórz obiekt gry w sesji
            Game game = session.getGame();
            if (game == null) {
                switch (request.game.toLowerCase()) {
                    case "blackjack":
                        game = new BlackJackController();
                        session.setGame(game);
                        break;
                    case "poker":
                        game = new PokerController();
                        session.setGame(game);
                        break;
                    // Dodaj kolejne gry tutaj
                    default:
                        writer.println("{\"status\":\"error\",\"code\":400,\"message\":\"Unknown game type\"}");
                        return;
                }
            }

            // 6. Uruchom handler gry, przekazując kontroler z sesji i KeyManager z wybranej sesji!
            KeyManager keyManager = session.getKeyManager();
            Runnable gameHandler;
            switch (request.game.toLowerCase()) {
                case "blackjack":
                    gameHandler = new BlackjackTcpHandler(clientSocket, (BlackJackController) game, keyManager);
                    break;
                case "poker":
                    System.out.println("[DEBUG GAME SERVER] Tworzę nowy pokój pokerowy dla gracza: " + playerUUID);
                    JsonObject response = new JsonObject();

                    // Sprawdź czy istnieje aktywny pokój z wolnym miejscem
                    Optional<PokerRoom> existingRoom = PokerRoomManager.getInstance().getAvailableRoom();

                    if (existingRoom.isPresent()) {
                        // Dołącz do istniejącego pokoju
                        response.addProperty("type", "room_joined");
                        response.addProperty("roomId", existingRoom.get().getRoomId());
                    } else {
                        // Utwórz nowy pokój
                        PokerRoom pokerRoom = PokerRoomManager.getInstance().createRoom(new PokerTCPClient(request.token, keyManager));
                        response.addProperty("type", "room_created");
                        response.addProperty("roomId", pokerRoom.getRoomId());
                    }

                    String encryptedResponse = keyManager.encryptAes(response.toString());
                    writer.println(encryptedResponse);
                    writer.flush();

                    gameHandler = new PokerTCPHandler(clientSocket, existingRoom.orElse(null), keyManager);
                    break;
                case "slots":
                    writer.println("{\"error\":\"Slots not implemented yet\"}");
                    clientSocket.close();
                    return;
                default:
                    writer.println("{\"error\":\"Unknown game type\"}");
                    clientSocket.close();
                    return;
            }
            gameHandler.run();

        } catch (Exception e) {
            System.err.println("[ERROR] GameServer: " + e.getMessage());
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

            clientSocket.setSoTimeout(3000);

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
