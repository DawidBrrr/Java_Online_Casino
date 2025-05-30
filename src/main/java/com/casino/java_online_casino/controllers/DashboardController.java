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

public class DashboardController {
    @FXML private Label usernameLabel;
    @FXML private Label balanceLabel;

    private String currentUser;
    private static double balance = 1000.0; // Tymczasowe - później z serwera

    public void initialize(String username) {
        this.currentUser = username;
        usernameLabel.setText("Witaj, " + username + "!");
        updateBalance();
        // Jak będzie serwer to się z serwera bdz brać i bdz lepiej chodzić

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
        // Tymczasowe - później z serwera
        balance += 500;
        updateBalance();
    }

    @FXML
    private void handleWithdraw() {
        // Tymczasowe - później z serwera
        if (balance >= 500) {
            balance -= 500;
            updateBalance();
        }
    }

    @FXML
    private void playSlots() {
        try {
            // Ładowanie FXML dla gry w sloty
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/slots.fxml"));
            Parent root = loader.load();

            // Pobranie aktualnej sceny i ustawienie jej na nową
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/poker.fxml"));
            Parent root = loader.load();

            // Pobierz widok - użyj właściwego typu
            PokerView pokerView = loader.getController();

            // Utwórz nowy kontroler i przypisz go do widoku
            PokerController pokerController = new PokerController();
            pokerView.setController(pokerController);

            //SEKCJA TEEESTOWAAA
            /*
            // Dodaj testową komunikację z serwerem z botem All-In
            pokerController.setServerCommunication(new PokerController.ServerCommunication() {
                @Override
                public void sendPlayerAction(String playerId, Player.playerAction action, int amount) {
                    if (playerId.equals("player2")) { // Bot
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                Platform.runLater(() -> {
                                    pokerController.setCurrentPlayer("player2");
                                    pokerController.playerAllIn();
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }

                @Override
                public void sendGameState(PokerGame.GameState state) {
                    if (pokerController.getGame().isPlayerTurn("player2")) {
                        pokerController.playerAllIn();
                    }
                }

                @Override
                public void sendPlayerUpdate(Player player) {
                    // Tymczasowo puste
                }

                @Override
                public void broadcastMessage(String message) {
                    System.out.println(message);
                }
            });

            // Pobierz aktualny balans użytkownika
            String balanceText = balanceLabel.getText().replace("$", "").replace(",", ".");
            double currentBalance = Double.parseDouble(balanceText);

            // Dodaj graczy używając joinGame z poprawnym balansem
            pokerController.joinGame("player1", currentUser);
            pokerController.getGame().getPlayerById("player1").setBalance((int)currentBalance);
            pokerController.joinGame("player2", "Bot");
            pokerController.setCurrentPlayer("player1");




            // KONIEC SEEEKCJI TESTOWEJ
            */
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setResizable(true);
            stage.setTitle("Sigma Kasyno - Texas Hold'em Poker");
            scene.getStylesheets().add(getClass().getResource("/com/casino/styles/poker.css").toExternalForm());
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