package com.casino.java_online_casino.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DashboardController {
    @FXML private Label usernameLabel;
    @FXML private Label balanceLabel;

    private String currentUser;
    private double balance = 1000.0; // Tymczasowe - później z serwera

    public void initialize(String username) {
        this.currentUser = username;
        usernameLabel.setText("Witaj, " + username + "!");
        updateBalance();
    }

    private void updateBalance() {
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
        System.out.println("Starting Slots game...");
        // Tutaj przejście do gry
    }

    @FXML
    private void playBlackjack() {
        System.out.println("Starting Blackjack game...");
        // Tutaj przejście do gry
    }

    @FXML
    private void playPoker() {
        System.out.println("Starting Poker game...");
        // Tutaj przejście do gry
    }
}