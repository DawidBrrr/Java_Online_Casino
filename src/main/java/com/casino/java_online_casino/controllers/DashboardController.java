package com.casino.java_online_casino.controllers;

import com.casino.java_online_casino.Connection.Client.KeyExchangeService;
import com.casino.java_online_casino.Connection.Client.Service;
import com.casino.java_online_casino.games.poker.controller.PokerController;
import com.casino.java_online_casino.games.poker.gui.PokerView;
import com.casino.java_online_casino.games.poker.model.Player;
import com.casino.java_online_casino.games.poker.model.PokerGame;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;


import com.casino.java_online_casino.Database.GamerDAO;
import com.casino.java_online_casino.User.Gamer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class DashboardController {
    @FXML private Label usernameLabel;
    @FXML private Label balanceLabel;

    private String currentUserEmail;
    private static double balance = 0.0; // Synchronizowany z bazą
    private int currentUserId; // potrzebny do aktualizacji


    public void initialize(String email) {
        this.currentUserEmail = email;
        GamerDAO dao = GamerDAO.getInstance();
        Gamer gamer = dao.findByEmail(email);

        if (gamer != null) {
            this.currentUserId = gamer.getUserId();
            balance = gamer.getCredits();
            usernameLabel.setText("Witaj, " + gamer.getNickName() + "!");
        } else {
            usernameLabel.setText("Witaj, użytkowniku!");
            balance = 0.0;
        }

        updateBalance();
    }

    public void updateBalance() {
        balanceLabel.setText(String.format("$%.2f", balance));
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/views/auth.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sigma Kasyno - Logowanie");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeposit() {
        balance += 500;
        updateBalance();
        //GamerDAO.updateCredits(currentUserId, (float) balance);
        GamerDAO dao = GamerDAO.getInstance();
        boolean updated = dao.updateCredits(currentUserId, (float) balance);
    }

    @FXML
    private void handleWithdraw() {
        if (balance >= 500) {
            balance -= 500;
            updateBalance();
          //  GamerDAO.updateCredits(currentUserId, (float) balance);
            GamerDAO dao = GamerDAO.getInstance();
            boolean updated = dao.updateCredits(currentUserId, (float) balance);
        }
    }

    @FXML
    private void playSlots() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/slots.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setResizable(true);
            stage.setTitle("Sigma Kasyno - Gra w Sloty");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void playBlackjack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/blackjack.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setResizable(true);
            stage.setTitle("Sigma Kasyno - Gra w Blackjacka");
            scene.getStylesheets().add(getClass().getResource("/com/casino/styles/blackjack.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void playPoker() {
        try {
            System.out.println("[DEBUG DASHBOARD] Rozpoczynam inicjalizację widoku pokera");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/poker.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setResizable(true);
            stage.setTitle("Sigma Kasyno - Texas Hold'em Poker");
            scene.getStylesheets().add(getClass().getResource("/com/casino/styles/poker.css").toExternalForm());
            System.out.println("[DEBUG DASHBOARD] Widok pokera załadowany pomyślnie");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getBalance() {
        return (int) balance;
    }

    public static void setBalance(double newBalance) {
        balance = newBalance;
    }


}
