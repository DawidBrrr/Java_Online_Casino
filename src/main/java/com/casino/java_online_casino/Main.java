package com.casino.java_online_casino;

import com.casino.java_online_casino.Connection.Client.KeyExchangeService;
import com.casino.java_online_casino.Connection.Client.Service;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(Service.getToken());

        // Tworzymy pierwsze okno
        createClientWindow("Klient 1");

        createClientWindow("Klient 2");
        // Tworzymy drugie okno
        // Możesz dodać kolejne okna...
    }

    private void createClientWindow(String                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               title) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/casino/java_online_casino/auth.fxml"));
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args); // Tylko tutaj!
    }
}

