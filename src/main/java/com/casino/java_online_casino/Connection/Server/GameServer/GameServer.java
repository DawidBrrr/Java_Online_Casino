package com.casino.java_online_casino.Connection.Server.GameServer;

import com.casino.java_online_casino.Connection.Games.BlackjackTcpHandler;
import com.casino.java_online_casino.Connection.Games.Game;
import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Session.SessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Utils.ServerJsonMessage;
import com.casino.java_online_casino.games.blackjack.controller.BlackJackController;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
            System.out.println("[DEBUG GAME SERVER] Nowe połączenie: " + clientSocket.getRemoteSocketAddress());

            String initRequest = reader.readLine();
            System.out.println("[DEBUG GAME SERVER] Odebrano initRequest: " + initRequest);

            if (initRequest == null || initRequest.isBlank()) {
                System.out.println("[DEBUG GAME SERVER] Brak żądania inicjalizującego.");
                writer.println("{\"status\":\"error\",\"code\":400,\"message\":\"Empty initial request\"}");
                return;
            }
            InitRequest request = new com.google.gson.Gson().fromJson(initRequest, InitRequest.class);
            if (request == null || request.token == null || request.game == null) {
                System.out.println("[DEBUG GAME SERVER] Brak tokenu lub typu gry.");
                writer.println("{\"status\":\"error\",\"code\":400,\"message\":\"Missing token or game\"}");
                return;
            }

            Object uuidObj = com.casino.java_online_casino.Connection.Tokens.ServerTokenManager.validateJwt(request.token).get("UUID");
            if (uuidObj == null) {
                System.out.println("[DEBUG GAME SERVER] Token nieprawidłowy.");
                writer.println("{\"status\":\"error\",\"code\":401,\"message\":\"Invalid token\"}");
                return;
            }
            playerUUID = UUID.fromString(uuidObj.toString());
            System.out.println("[DEBUG GAME SERVER] Odczytano UUID: " + playerUUID);

            session = sessionManager.getSessionByUUID(playerUUID);
            if (session == null) {
                System.out.println("[DEBUG GAME SERVER] Brak sesji dla UUID: " + playerUUID);
                writer.println("{\"status\":\"error\",\"code\":404,\"message\":\"Session not found\"}");
                return;
            }

            // 4. Próbuj zablokować dostęp do gry
            if (!session.tryLockGame()) {
                System.out.println("[DEBUG GAME SERVER] Gra zajęta, próbuję pingować aktywnego klienta...");
                if (tryPingPong(clientSocket, session.getKeyManager())) {
                    System.out.println("[DEBUG GAME SERVER] Aktywny klient odpowiedział na ping – odrzucam nowe połączenie.");
                    writer.println("{\"status\":\"error\",\"code\":409,\"message\":\"Game already in use by another client\"}");
                    return;
                } else {
                    System.out.println("[DEBUG GAME SERVER] Aktywny klient nie odpowiedział – zwalniam lock i przejmuję grę.");
                    session.unlockGame();
                    if (!session.tryLockGame()) {
                        System.out.println("[DEBUG GAME SERVER] Nie udało się przejąć locka po ping timeout.");
                        writer.println("{\"status\":\"error\",\"code\":409,\"message\":\"Game already in use by another client (lock error)\"}");
                        return;
                    }
                }
            }
            locked = true;

            // 5. Pobierz lub utwórz obiekt gry w sesji
            Game game = session.getGame();
            System.out.println("[DEBUG GAME SERVER] Obiekt gry w sesji: " + game);
            if (game == null) {
                switch (request.game.toLowerCase()) {
                    case "blackjack":
                        game = new BlackJackController();
                        session.setGame(game);
                        System.out.println("[DEBUG GAME SERVER] Utworzono nową grę: " + game);
                        break;
                    default:
                        System.out.println("[DEBUG GAME SERVER] Nieznany typ gry: " + request.game);
                        writer.println("{\"status\":\"error\",\"code\":400,\"message\":\"Unknown game type\"}");
                        return;
                }
            }

            // 6. Uruchom handler gry, przekazując kontroler z sesji
            KeyManager keyManager = session.getKeyManager();
            if (game instanceof BlackJackController) {
                System.out.println("[DEBUG GAME SERVER] Uruchamiam handler BlackJack dla UUID: " + playerUUID);
                new BlackjackTcpHandler(clientSocket, (BlackJackController) game, keyManager).run();
            } else {
                System.out.println("[DEBUG GAME SERVER] Brak handlera dla typu gry.");
                writer.println("{\"status\":\"error\",\"code\":500,\"message\":\"Game handler not implemented\"}");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] GameServer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (locked && session != null) {
                System.out.println("[DEBUG GAME SERVER] Zwalniam lock na sesji dla UUID: " + (playerUUID != null ? playerUUID : "?"));
                session.unlockGame();
            }
            try {
                clientSocket.close();
                System.out.println("[DEBUG GAME SERVER] Zamknięto socket: " + clientSocket.getRemoteSocketAddress());
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

            System.out.println("[DEBUG GAME SERVER] Wysyłam ping do klienta...");
            writer.println(encryptedPing);
            writer.flush();

            clientSocket.setSoTimeout(3000);

            String response = reader.readLine();
            System.out.println("[DEBUG GAME SERVER] Odpowiedź na ping: " + response);
            if (response == null) return false;

            String decrypted;
            try {
                decrypted = keyManager.decryptAes(response);
            } catch (Exception e) {
                System.out.println("[DEBUG GAME SERVER] Błąd deszyfracji odpowiedzi na ping: " + e.getMessage());
                return false;
            }
            JsonObject respJson = JsonParser.parseString(decrypted).getAsJsonObject();
            if (respJson.has("status") && respJson.has("code")) {
                String status = respJson.get("status").getAsString();
                int code = respJson.get("code").getAsInt();
                boolean result = code == 101 && "check".equalsIgnoreCase(status);
                System.out.println("[DEBUG GAME SERVER] Wynik ping: " + result);
                return result;
            }
            return false;
        } catch (SocketTimeoutException e) {
            System.out.println("[DEBUG GAME SERVER] Timeout podczas pingowania klienta.");
            return false;
        } catch (Exception e) {
            System.out.println("[DEBUG GAME SERVER] Wyjątek podczas pingowania: " + e.getMessage());
            return false;
        }
    }
}
