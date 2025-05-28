package com.casino.java_online_casino.games.poker.gui;

import com.casino.java_online_casino.controllers.DashboardController;
import com.casino.java_online_casino.games.poker.controller.PokerController;
import com.casino.java_online_casino.games.poker.model.PokerGame;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class PokerView {
    @FXML private HBox communityCardsArea;
    @FXML private HBox playerCardsArea;
    @FXML private HBox actionButtons;
    @FXML private VBox playersListArea;
    @FXML private Label potLabel;
    @FXML private Label currentBetLabel;
    @FXML private Label balanceLabel;
    @FXML private Slider raiseSlider;

    private PokerController controller;

    public void setController(PokerController controller) {
        this.controller = controller;
        controller.setView(this);
    }

    @FXML
    public void handleFold() {
        controller.playerFold();
    }

    @FXML
    public void handleCheck() {
        controller.playerCheck();
    }

    @FXML
    public void handleCall() {
        controller.playerCall();
    }

    @FXML
    public void handleRaise() {
        int amount = (int) raiseSlider.getValue();
        controller.playerRaise(amount);
    }

    @FXML
    public void handleAllIn() {
        controller.playerAllIn();
    }

    public void updateGameState(PokerGame game) {
        potLabel.setText(String.valueOf(game.getPot()));
        currentBetLabel.setText(String.valueOf(game.getCurrentBet()));
        // Implementacja aktualizacji kart i stanu graczy
    }

    public void updatePlayerActions(boolean isPlayerTurn) {
        actionButtons.setDisable(!isPlayerTurn);
        // Aktualizacja dostępności przycisków w zależności od możliwych akcji
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/dashboard.fxml"));
            Parent root = loader.load();

            DashboardController dashboardController = loader.getController();
            DashboardController.setBalance(controller.getBalance());
            dashboardController.updateBalance();

            Stage stage = (Stage) balanceLabel.getScene().getWindow();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());

            scene.getStylesheets().add(getClass().getResource("/com/casino/styles/casino.css").toExternalForm());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setResizable(true);
            stage.show();
            stage.setTitle("Sigma Kasyno - Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}