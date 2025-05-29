package com.casino.java_online_casino.Connection.Server.GameServer;

import com.casino.java_online_casino.Connection.Games.BlackjackTcpHandler;
import com.casino.java_online_casino.Connection.Games.Game;
import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.games.blackjack.controller.BlackJackController;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
            // 1. Odbierz żądanie inicjalizujące (token, game)
            System.out.println("[INFO] " + clientSocket.getRemoteSocketAddress());
            String initRequest = reader.readLine();
            if (initRequest == null || initRequest.isBlank()) {
                System.out.println("{\"status\":\"error\",\"code\":400,\"message\":\"Empty initial request\"}");
                writer.println("{\"status\":\"error\",\"code\":400,\"message\":\"Empty initial request\"}");
                return;
            }
            InitRequest request = new com.google.gson.Gson().fromJson(initRequest, InitRequest.class);
            if (request == null || request.token == null || request.game == null) {
                System.out.println("{\"status\":\"error\",\"code\":400,\"message\":\"Missing token or game\"}");
                writer.println("{\"status\":\"error\",\"code\":400,\"message\":\"Missing token or game\"}");
                return;
            }

            // 2. Walidacja tokena, pobranie UUID
            Object uuidObj = com.casino.java_online_casino.Connection.Tokens.ServerTokenManager.validateJwt(request.token).get("UUID");
            if (uuidObj == null) {
                System.out.println("{\"status\":\"error\",\"code\":401,\"message\":\"Invalid token\"}");
                writer.println("{\"status\":\"error\",\"code\":401,\"message\":\"Invalid token\"}");
                return;
            }
            playerUUID = UUID.fromString(uuidObj.toString());

            // 3. Pobierz sesję użytkownika
            session = sessionManager.getSessionByUUID(playerUUID);
            if (session == null) {
                System.out.println("{\"status\":\"error\",\"code\":404,\"message\":\"Session not found\"}");
                writer.println("{\"status\":\"error\",\"code\":404,\"message\":\"Session not found\"}");

                return;
            }

            // 4. Próbuj zablokować dostęp do gry
            if (!session.tryLockGame()) {
                writer.println("{\"status\":\"error\",\"code\":409,\"message\":\"Game already in use by another client\"}");
                System.out.println("{\"status\":\"error\",\"code\":409,\"message\":\"Game already in use by another client\"}");
                return;
            }
            locked = true;

            // 5. Pobierz lub utwórz obiekt gry w sesji
            Game game = session.getGame();
            System.out.println(game);
            if (game == null) {
                switch (request.game.toLowerCase()) {
                    case "blackjack":
                        game = new BlackJackController();
                        session.setGame(game);
                        System.out.println(game);
                        break;
                    // Dodaj inne gry tutaj
                    default:
                        writer.println("{\"status\":\"error\",\"code\":400,\"message\":\"Unknown game type\"}");
                        return;
                }
            }

            // 6. Uruchom handler gry, przekazując kontroler z sesji
            KeyManager keyManager = session.getKeyManager();
            if (game instanceof BlackJackController) {
                System.out.println("Uruchamiam handler black jack");
                new BlackjackTcpHandler(clientSocket, (BlackJackController) game, keyManager).run();
            } else {
                writer.println("{\"status\":\"error\",\"code\":500,\"message\":\"Game handler not implemented\"}");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] GameServer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 7. ZAWSZE zwalniaj lock po zakończeniu obsługi klienta!
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
}
