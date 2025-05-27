package com.casino.java_online_casino;

import com.casino.java_online_casino.Connection.Client.KeyExchangeService;
import com.casino.java_online_casino.Connection.Client.Service;
import com.casino.java_online_casino.Connection.Server.API.ApiServer;
import com.casino.java_online_casino.Connection.Server.GameServer.GameServer;
import com.casino.java_online_casino.games.blackjack.controller.BlackjackTcpClient;
import com.casino.java_online_casino.games.blackjack.controller.RemoteBlackJackController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/casino/java_online_casino/auth.fxml"));

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
        scene.getStylesheets().add(getClass().getResource("/com/casino/styles/casino.css").toExternalForm());

        primaryStage.setTitle("Sigma Kasyno - Logowanie");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        //Api i serwre używaja wspołnych kluczy - muszą być uruchamiane w jednej instancji razem
        ApiServer apiServer = new ApiServer();
        GameServer gameServer = new GameServer();

        // Uruchom ApiServer w osobnym wątku
        Thread apiThread = new Thread(() -> {
            try {
                apiServer.start();
            } catch (Exception e) {
                System.out.println("[ERROR] ApiServer: " + e.getMessage());
                e.printStackTrace();
            }
        }, "ApiServer-Thread");
        apiThread.start();

        // Uruchom GameServer w osobnym wątku
        Thread gameThread = new Thread(() -> {
            try {
                gameServer.start();
            } catch (Exception e) {
                System.out.println("[ERROR] GameServer: " + e.getMessage());
                e.printStackTrace();
            }
        }, "GameServer-Thread");
        gameThread.start();

        // Jeśli chcesz poczekać aż serwery się rozkręcą
        try {
            Thread.sleep(1000); // wystarczy na "rozgrzanie" serwerów
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Klient: wymiana kluczy + rozgrywka -- można zostawić bez zmian
        Service keyService = new KeyExchangeService();
        Thread keyExchangeThread = new Thread(keyService, "KeyExchange-Thread");
        keyExchangeThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(Service.getToken());

        BlackjackTcpClient tcpClient = new BlackjackTcpClient(Service.getToken(), Service.getKeyManager());
        try {
            tcpClient.connect();
        } catch (IOException e) {
            throw new RuntimeException("Połączenie z blackjack nie powiodło się");
        }

        RemoteBlackJackController controller = new RemoteBlackJackController(tcpClient);


        launch(args);
    }
}