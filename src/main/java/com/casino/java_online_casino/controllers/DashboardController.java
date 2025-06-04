package com.casino.java_online_casino.controllers;

import com.casino.java_online_casino.Connection.Client.LogoutService;
import com.casino.java_online_casino.Connection.Client.Service;
import com.casino.java_online_casino.Connection.Client.UserDataService;
import com.casino.java_online_casino.Connection.Server.DTO.GamerDTO;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.casino.java_online_casino.games.blackjack.gui.BlackJackGUIController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class DashboardController {
    @FXML private Label usernameLabel;
    @FXML private Label balanceLabel;

    private GamerDTO gamerDTO;
    private String currentUserEmail;

    public void initialize(String email) {
        this.currentUserEmail = email;
        refreshUserData();
    }

    private void refreshUserData() {
        new Thread(() -> {
            GamerDTO dto = UserDataService.updateGamerDTO();
            Platform.runLater(() -> {
                if (dto != null) {
                    this.gamerDTO = dto;
                    updateDashboardFields();
                } else {
                    showError("Nie udało się pobrać danych użytkownika.");
                }
            });
        }).start();
    }

    private void updateDashboardFields() {
        if (gamerDTO != null) {
            usernameLabel.setText("Witaj, " + gamerDTO.getNickName() + "!");
            balanceLabel.setText(String.format("$%.2f", gamerDTO.getCredits()));
        } else {
            usernameLabel.setText("Witaj, użytkowniku!");
            balanceLabel.setText("$0.00");
        }
    }

    @FXML
    private void handleLogout() {
        new Thread(() -> {
            boolean logoutOk = false;
            try {
                LogoutService logoutService = new LogoutService();
                logoutOk = logoutService.perform();
                System.out.println("[DEBUG LOGOUT] Wynik wylogowania: " + logoutOk);
                LogManager.logToFile("[DEBUG LOGOUT] Wynik wylogowania: " + logoutOk);
            } catch (Exception e) {
                System.err.println("[DEBUG LOGOUT] Błąd podczas wylogowywania: " + e.getMessage());
                LogManager.logToFile("[DEBUG LOGOUT] Błąd podczas wylogowywania: " + e.getMessage());
                e.printStackTrace();
            } finally {
                Platform.runLater(() -> {
                    Service.token = null;
                    Service.keyManager = null;
                    currentUserEmail = null;
                    gamerDTO = null;
                    usernameLabel.setText("");
                    balanceLabel.setText("");
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/auth.fxml"));
                        Parent root = loader.load();
                        Stage stage = (Stage) usernameLabel.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.setTitle("Sigma Kasyno - Logowanie");
                    } catch (Exception e) {
                        showError("Błąd podczas wylogowywania: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }).start();
    }

    @FXML
    private void handleDeposit() {
        setButtonsEnabled(false);
        new Thread(() -> {
            GamerDTO dto = UserDataService.depositCredits();
            Platform.runLater(() -> {
                if (dto != null) {
                    this.gamerDTO = dto;
                    updateDashboardFields();
                } else {
                    showError("Błąd podczas wpłaty.");
                }
                setButtonsEnabled(true);
            });
        }).start();
    }

    @FXML
    private void handleWithdraw() {
        setButtonsEnabled(false);
        new Thread(() -> {
            GamerDTO dto = UserDataService.withdrawCredits();
            Platform.runLater(() -> {
                if (dto != null) {
                    this.gamerDTO = dto;
                    updateDashboardFields();
                } else {
                    showError("Błąd podczas wypłaty.");
                }
                setButtonsEnabled(true);
            });
        }).start();
    }

    @FXML
    private void playSlots() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/slots.fxml"));
            Parent root = loader.load();
            com.casino.java_online_casino.games.slots.controller.SlotsController slotsController = loader.getController();
            slotsController.initWithUser(gamerDTO != null ? gamerDTO.getEmail() : null);
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setResizable(true);
            stage.setTitle("Sigma Kasyno - Gra w Sloty");
        } catch (Exception e) {
            showError("Błąd podczas uruchamiania gry sloty: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void playBlackjack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/blackjack.fxml"));
            Parent root = loader.load();
            BlackJackGUIController controller = loader.getController();
            controller.initWithUser(gamerDTO != null ? gamerDTO.getEmail() : null);
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            scene.getStylesheets().add(getClass().getResource("/com/casino/styles/blackjack.css").toExternalForm());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setResizable(true);
            stage.setTitle("Sigma Kasyno - Gra w Blackjacka");
        } catch (Exception e) {
            showError("Błąd podczas uruchamiania gry blackjack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void playPoker() {
        try {
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
        } catch (Exception e) {
            showError("Błąd podczas uruchamiania gry poker: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void showRankings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/rankings.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Rankingi graczy");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            showError("Błąd podczas wyświetlania rankingów: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Pomocnicze do blokowania przycisków podczas operacji
    private void setButtonsEnabled(boolean enabled) {}

    private void showError(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}
