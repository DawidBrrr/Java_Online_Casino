package com.casino.java_online_casino.games.poker.gui;

import com.casino.java_online_casino.Connection.Client.Service;
import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.casino.java_online_casino.controllers.DashboardController;
import com.casino.java_online_casino.games.poker.controller.PokerTcpClient;
import com.casino.java_online_casino.games.poker.controller.RemotePokerController;
import com.casino.java_online_casino.games.poker.model.Card;
import com.casino.java_online_casino.games.poker.model.PokerGame;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PokerView{
    @FXML
    private HBox communityCardsArea;
    @FXML
    private HBox playerCardsArea;
    @FXML
    private HBox actionButtons;
    @FXML
    private VBox playersListArea;
    @FXML
    private Label potLabel;
    @FXML
    private Label currentBetLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private Label gameStateLabel;
    @FXML
    private Label currentPlayerLabel;
    @FXML
    private Slider raiseSlider;
    @FXML
    private Label raiseValueLabel;
    @FXML
    private Button foldButton;
    @FXML
    private Button checkButton;
    @FXML
    private Button callButton;
    @FXML
    private Button raiseButton;
    @FXML
    private Button allInButton;
    @FXML
    private Button startGameButton;
    @FXML
    private TextField playerNameField;
    @FXML
    private Label messageLabel;

    private RemotePokerController controller;
    private PokerTcpClient tcpClient;
    private PokerDTO pokerDTO;
    @FXML
    public void initialize() {
        try {
            System.out.println("[DEBUG POKER_VIEW] Inicjalizacja widoku pokera");
            LogManager.logToFile("[DEBUG POKER_VIEW] Inicjalizacja widoku pokera");

            // Inicjalizacja klienta TCP i kontrolera
            tcpClient = new PokerTcpClient();
            controller = new RemotePokerController(tcpClient);

            setupUI();
            updateUIFromState();
            showMessage("Dołączono do gry. Oczekiwanie na innych graczy...");
        } catch (Exception e) {
            showMessage("Błąd połączenia z serwerem: " + e.getMessage());
            LogManager.logToFile("[ERROR POKER_VIEW] Błąd inicjalizacji: " + e.getMessage());
        }
    }

    private void setupUI() {
        if (raiseSlider != null) {
            raiseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (raiseValueLabel != null) {
                    raiseValueLabel.setText("$" + newVal.intValue());
                }
            });
        }
        if (communityCardsArea != null) {
            communityCardsArea.setAlignment(Pos.CENTER);
            communityCardsArea.setSpacing(10);
            communityCardsArea.setPadding(new Insets(10));
        }
        if (playerCardsArea != null) {
            playerCardsArea.setAlignment(Pos.CENTER);
            playerCardsArea.setSpacing(10);
            playerCardsArea.setPadding(new Insets(10));
        }
        if (actionButtons != null) {
            actionButtons.setAlignment(Pos.CENTER);
            actionButtons.setSpacing(15);
            actionButtons.setPadding(new Insets(10));
        }
        if (messageLabel == null) {
            messageLabel = new Label("Welcome to Poker!");
            messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
        }
    }

    public void handleJoinGame() {
        if (playerNameField != null && !playerNameField.getText().trim().isEmpty()) {
            try {
                controller.join();
                showMessage("Dołączono do gry!");
                playerNameField.setDisable(true);
                updateUIFromState();
            } catch (Exception e) {
                showMessage("Nie można dołączyć: " + e.getMessage());
            }
        } else {
            showMessage("Please enter your name first!");
        }
    }

    public void handleStartGame() {
        try {
            controller.updateState();
            updateUIFromState();
        } catch (Exception e) {
            showMessage("Błąd podczas startu gry: " + e.getMessage());
        }
    }

    @FXML
    public void handleFold() {
        try {
            controller.fold();
            updateUIFromState();
            showMessage("You folded.");
        } catch (Exception e) {
            showMessage("Błąd: " + e.getMessage());
        }
    }

    @FXML
    public void handleCheck() {
        try {
            controller.check();
            updateUIFromState();
            showMessage("You checked.");
        } catch (Exception e) {
            showMessage("Błąd: " + e.getMessage());
        }
    }

    @FXML
    public void handleCall() {
        try {
            controller.call();
            updateUIFromState();
            showMessage("You called.");
        } catch (Exception e) {
            showMessage("Błąd: " + e.getMessage());
        }
    }

    @FXML
    public void handleRaise() {
        try {
            int amount = (int) raiseSlider.getValue();
            controller.raise(amount);
            updateUIFromState();
            showMessage("You raised $" + amount);
        } catch (Exception e) {
            showMessage("Błąd: " + e.getMessage());
        }
    }

    @FXML
    public void handleAllIn() {
        try {
            controller.allIn();
            updateUIFromState();
            showMessage("You went all-in!");
        } catch (Exception e) {
            showMessage("Błąd: " + e.getMessage());
        }
    }

    public void updateUIFromState() {
        Platform.runLater(() -> {
            try {
                controller.updateState();
                PokerDTO.PokerPlayerDTO me = controller.getMyPlayer();

                if (potLabel != null) potLabel.setText("Pot: $" + controller.getPot());
                if (currentBetLabel != null) currentBetLabel.setText("Current Bet: $" + controller.getCurrentBet());
                if (balanceLabel != null && me != null) balanceLabel.setText("Balance: $" + me.balance);
                if (gameStateLabel != null) gameStateLabel.setText("Game State: " + controller.getGameStatus());

                if (currentPlayerLabel != null) {
                    String activeId = controller.getActivePlayerId();
                    Map<String, PokerDTO.PokerPlayerDTO> players = controller.getPlayers();
                    currentPlayerLabel.setText("Current Turn: " +
                            (players.containsKey(activeId) ? players.get(activeId).name : "N/A"));
                }
                if(!me.canAct){
                    allInButton.setDisable(true);
                    callButton.setDisable(true);
                    checkButton.setDisable(true);
                    raiseButton.setDisable(true);
                    foldButton.setDisable(true);
                }
               else if(me.canAct){
                    allInButton.setDisable(false);
                    callButton.setDisable(false);
                    checkButton.setDisable(false);
                    raiseButton.setDisable(false);
                    foldButton.setDisable(false);
                }

                updateCommunityCards(controller.getCommunityCards());
                updatePlayerCards(controller.getMyCards());
                updatePlayersList(controller.getPlayers());
                updateRaiseSlider();

                // Przyciski aktywne tylko jeśli to tura gracza
                boolean myTurn = controller.isMyTurn();
                foldButton.setDisable(!myTurn);
                checkButton.setDisable(!myTurn || !controller.getAvailableActions().contains("CHECK"));
                callButton.setDisable(!myTurn || !controller.getAvailableActions().contains("CALL"));
                raiseButton.setDisable(!myTurn || !controller.getAvailableActions().contains("RAISE"));
                allInButton.setDisable(!myTurn || !controller.getAvailableActions().contains("ALL_IN"));
                actionButtons.setDisable(!myTurn);
            } catch (Exception e) {
                showMessage("Błąd aktualizacji stanu: " + e.getMessage());
            }
        });
    }

    private void updateCommunityCards(List<Card> communityCards) {
        if (communityCardsArea != null) {
            communityCardsArea.getChildren().clear();
            for (Card card : communityCards) {
                Label cardLabel = createCardLabel(card);
                communityCardsArea.getChildren().add(cardLabel);
            }
            int remaining = 5 - communityCards.size();
            for (int i = 0; i < remaining; i++) {
                Label placeholder = createPlaceholderCard();
                communityCardsArea.getChildren().add(placeholder);
            }
        }
    }

    private void updatePlayerCards(List<Card> playerCards) {
        if (playerCardsArea != null) {
            playerCardsArea.getChildren().clear();
            for (Card card : playerCards) {
                Label cardLabel = createCardLabel(card);
                cardLabel.setStyle(cardLabel.getStyle() + "; -fx-border-color: gold; -fx-border-width: 2px;");
                playerCardsArea.getChildren().add(cardLabel);
            }
        }
    }

    private void updatePlayersList(Map<String, PokerDTO.PokerPlayerDTO> players) {
        if (playersListArea != null) {
            playersListArea.getChildren().clear();
            for (PokerDTO.PokerPlayerDTO player : players.values()) {
                VBox playerBox = createPlayerBox(player);
                playersListArea.getChildren().add(playerBox);
            }
        }
    }

    private VBox createPlayerBox(PokerDTO.PokerPlayerDTO player) {
        VBox playerBox = new VBox(5);
        playerBox.setPadding(new Insets(10));
        playerBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(player.name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        Label balanceLabel = new Label("Balance: $" + player.balance);
        Label betLabel = new Label("Bet: $" + player.currentBet);

        Label statusLabel = new Label();
        if ("folded".equals(player.status)) {
            statusLabel.setText("FOLDED");
            statusLabel.setTextFill(Color.RED);
            playerBox.setStyle("-fx-background-color: #ffeeee; -fx-border-color: #ffcccc; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        } else if ("all_in".equals(player.status)) {
            statusLabel.setText("ALL-IN");
            statusLabel.setTextFill(Color.ORANGE);
            playerBox.setStyle("-fx-background-color: #fff3e0; -fx-border-color: #ffcc80; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        } else {
            statusLabel.setText("ACTIVE");
            statusLabel.setTextFill(Color.GREEN);
            playerBox.setStyle("-fx-background-color: #f0f8f0; -fx-border-color: #c8e6c9; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        }

        if (controller != null && controller.getActivePlayerId().equals(player.id)) {
            playerBox.setStyle(playerBox.getStyle() + "; -fx-border-width: 3px; -fx-border-color: #2196f3;");
        }

        playerBox.getChildren().addAll(nameLabel, balanceLabel, betLabel, statusLabel);
        return playerBox;
    }

    private Label createCardLabel(Card card) {
        Label cardLabel = new Label(formatCard(card));
        cardLabel.setPrefSize(60, 90);
        cardLabel.setAlignment(Pos.CENTER);
        cardLabel.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: black; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        getCardColor(card)
        );
        return cardLabel;
    }

    private Label createPlaceholderCard() {
        Label placeholder = new Label("?");
        placeholder.setPrefSize(60, 90);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setStyle(
                "-fx-background-color: #f0f0f0; " +
                        "-fx-border-color: #ccc; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-font-size: 16px; " +
                        "-fx-text-fill: #999;"
        );
        return placeholder;
    }

    private String formatCard(Card card) {
        String rank = card.getRank().toString();
        String suit = getSuitSymbol(card.getSuit());
        return rank + "\n" + suit;
    }

    private String getSuitSymbol(Card.Suit suit) {
        switch (suit) {
            case HEARTS:
                return "♥";
            case DIAMONDS:
                return "♦";
            case CLUBS:
                return "♣";
            case SPADES:
                return "♠";
            default:
                return "?";
        }
    }

    private String getCardColor(Card card) {
        if (card.getSuit() == Card.Suit.HEARTS || card.getSuit() == Card.Suit.DIAMONDS) {
            return "-fx-text-fill: red;";
        } else {
            return "-fx-text-fill: black;";
        }
    }

    private void updateRaiseSlider() {
        if (raiseSlider != null && controller != null) {
            float minRaise = controller.getMinimumBet();
            float maxRaise = controller.getMyBalance();
            raiseSlider.setMin(minRaise);
            raiseSlider.setMax(Math.max(minRaise, maxRaise));
            raiseSlider.setValue(minRaise);
            if (raiseValueLabel != null) {
                raiseValueLabel.setText("$" + minRaise);
            }
        }
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            Platform.runLater(() -> {
                messageLabel.setText(message);
                messageLabel.setStyle("-fx-text-fill: #2196f3; -fx-font-weight: bold;");
            });
        } else {
            System.out.println("POKER: " + message);
        }
    }

    @FXML
    public void handleBack() {
        try {
            if (controller != null) {
                controller.close();
            }
            if (tcpClient != null) {
                tcpClient.close();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/dashboard.fxml"));
            Parent root = loader.load();

            DashboardController dashboardController = loader.getController();
            Stage stage = (Stage) (balanceLabel != null ? balanceLabel.getScene().getWindow() :
                    (potLabel != null ? potLabel.getScene().getWindow() : null));

            if (stage != null) {
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());

                scene.getStylesheets().add(getClass().getResource("/com/casino/styles/casino.css").toExternalForm());
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.setResizable(true);
                stage.show();
                stage.setTitle("Sigma Kasyno - Dashboard");
            }

            // ... tutaj pozostawiasz swój kod do powrotu do dashboardu ...
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}