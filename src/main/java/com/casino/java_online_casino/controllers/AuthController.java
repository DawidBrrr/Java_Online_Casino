package com.casino.java_online_casino.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class AuthController {
    // Elementy logowania
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;

    // Elementy rejestracji
    @FXML private TextField registerUsername;
    @FXML private PasswordField registerPassword;
    @FXML private PasswordField registerConfirm;
    @FXML private Label registerError;

    // Kontener gier
    @FXML private VBox gamesContainer;

    @FXML
    private void handleLogin() {
        String username = loginUsername.getText();
        String password = loginPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            loginError.setText("Please fill all fields");
            return;
        }

        // Tymczasowa symulacja logowania
        if (username.equals("admin") && password.equals("admin")) {
            loginError.setText("");
            showGames();
        } else {
            loginError.setText("Invalid credentials");
        }
    }

    @FXML
    private void handleRegister() {
        String username = registerUsername.getText();
        String password = registerPassword.getText();
        String confirm = registerConfirm.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            registerError.setText("Please fill all fields");
            return;
        }

        if (!password.equals(confirm)) {
            registerError.setText("Passwords don't match");
            return;
        }

        // Tymczasowa symulacja rejestracji
        registerError.setText("Registration successful! Please login.");
        registerUsername.setText("");
        registerPassword.setText("");
        registerConfirm.setText("");
    }

    private void showGames() {
        gamesContainer.setVisible(true);
    }

    @FXML
    private void selectSlots() {
        System.out.println("Slots selected");
        // Tutaj przejście do gry
    }

    @FXML
    private void selectBlackjack() {
        System.out.println("Blackjack selected");
        // Tutaj przejście do gry
    }

    @FXML
    private void selectPoker() {
        System.out.println("Poker selected");
        // Tutaj przejście do gry
    }
}