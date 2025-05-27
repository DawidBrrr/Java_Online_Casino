package com.casino.java_online_casino.Connection.Server.GameServer;

import com.almasb.fxgl.net.Server;
import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Session.KeySessionManager;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.games.blackjack.controller.BlackJackController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final KeySessionManager keySessionManager = KeySessionManager.getInstance();

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
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            // 1. Odbierz pierwszą linię: JSON zawierający token i nazwę gry
            String initRequest = reader.readLine();
            System.out.println("[DEBUG] Otrzymano initRequest: " + initRequest);

            if (initRequest == null || initRequest.isBlank()) {
                System.out.println("[DEBUG] Empty initial request!");
                writer.println("{\"error\":\"Empty initial request\"}");
                clientSocket.close();
                return;
            }

            // Parsowanie JSON (możesz użyć Gson lub innego parsera)
            InitRequest request = null;
            try {
                request = new com.google.gson.Gson().fromJson(initRequest, InitRequest.class);
            } catch (Exception e) {
                System.out.println("[DEBUG] Błąd parsowania JSON: " + e.getMessage());
                writer.println("{\"error\":\"Invalid JSON\"}");
                clientSocket.close();
                return;
            }

            if (request == null) {
                System.out.println("[DEBUG] request == null po Gson");
                writer.println("{\"error\":\"Parsed request is null\"}");
                clientSocket.close();
                return;
            }

            System.out.println("[DEBUG] Parsed token: " + request.token);
            System.out.println("[DEBUG] Parsed game: " + request.game);

            if (request.token == null || request.game == null) {
                System.out.println("[DEBUG] token lub game null w request");
                writer.println("{\"error\":\"Missing token or game\"}");
                clientSocket.close();
                return;
            }

            // 2. Walidacja tokena i wyciągnięcie UUID
            String playerUUIDstring = null;
            try {
                Object uuidObj = ServerTokenManager.validateJwt(request.token).get("UUID");
                System.out.println("[DEBUG] UUID zwrócony przez validateJwt: " + uuidObj);
                playerUUIDstring = (uuidObj != null) ? uuidObj.toString() : null;
            } catch (Exception e) {
                System.out.println("[DEBUG] Błąd walidacji tokena lub brak UUID: " + e.getMessage());
                writer.println("{\"error\":\"Token invalid or UUID missing\"}");
                clientSocket.close();
                return;
            }

            if (playerUUIDstring == null) {
                System.out.println("[DEBUG] UUID == null po validateJwt");
                writer.println("{\"error\":\"UUID is null in token\"}");
                clientSocket.close();
                return;
            }

            UUID playerUUID = null;
            try {
                playerUUID = UUID.fromString(playerUUIDstring);
                System.out.println("[DEBUG] Sparsowany UUID: " + playerUUID);
            } catch (Exception e) {
                System.out.println("[DEBUG] Błąd konwersji UUID: " + e.getMessage());
                writer.println("{\"error\":\"Malformed UUID in token\"}");
                clientSocket.close();
                return;
            }

            // 3. Pobierz KeyManager powiązany z UUID
            KeyManager keyManager = null;
            try {
                keyManager = keySessionManager.getOrCreateKeyManager(playerUUID);
                System.out.println("[DEBUG] Pobrano KeyManager: " + keyManager);
            } catch (Exception e) {
                System.out.println("[DEBUG] Błąd pobierania KeyManager: " + e.getMessage());
                writer.println("{\"error\":\"Could not obtain session KeyManager\"}");
                clientSocket.close();
                return;
            }

            // 4. Wybierz handler na podstawie nazwy gry
            Runnable gameHandler;
            switch (request.game.toLowerCase()) {
                case "blackjack":
                    BlackJackController controller = new BlackJackController();
                    gameHandler = new BlackjackTcpHandler(clientSocket, controller, keyManager);
                    System.out.println("[INFO] GameServer blackjack started dla UUID=" + playerUUID);
                    break;
                case "poker":
                    writer.println("{\"error\":\"Poker not implemented yet\"}");
                    clientSocket.close();
                    return;
                case "slots":
                    writer.println("{\"error\":\"Slots not implemented yet\"}");
                    clientSocket.close();
                    return;
                default:
                    writer.println("{\"error\":\"Unknown game type\"}");
                    clientSocket.close();
                    return;
            }

            // 5. Uruchom handler
            System.out.println("[DEBUG] Uruchamiam handler run()...");
            gameHandler.run();

        } catch (Exception e) {
            System.out.println("[DEBUG] Wyjątek obsługi klienta: " + e.getMessage());
            e.printStackTrace();
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
