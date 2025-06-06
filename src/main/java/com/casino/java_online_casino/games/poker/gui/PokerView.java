package com.casino.java_online_casino.games.poker.gui;
import com.casino.java_online_casino.Connection.Client.Service;
import com.casino.java_online_casino.Connection.Server.Rooms.PokerRoom;
import com.casino.java_online_casino.Connection.Server.Rooms.PokerRoomManager;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.casino.java_online_casino.controllers.DashboardController;
import com.casino.java_online_casino.games.poker.controller.PokerController;
import com.casino.java_online_casino.games.poker.controller.PokerTCPClient;
import com.casino.java_online_casino.games.poker.model.Card;
import com.casino.java_online_casino.games.poker.model.Player;
import com.casino.java_online_casino.games.poker.model.PokerGame;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PokerView {
    @FXML private HBox communityCardsArea;
    @FXML private HBox playerCardsArea;
    @FXML private HBox actionButtons;
    @FXML private VBox playersListArea;
    @FXML private Label potLabel;
    @FXML private Label currentBetLabel;
    @FXML private Label balanceLabel;
    @FXML private Label gameStateLabel;
    @FXML private Label currentPlayerLabel;
    @FXML private Slider raiseSlider;
    @FXML private Label raiseValueLabel;
    @FXML private Button foldButton;
    @FXML private Button checkButton;
    @FXML private Button callButton;
    @FXML private Button raiseButton;
    @FXML private Button allInButton;
    @FXML private Button startGameButton;
    @FXML private TextField playerNameField;
    @FXML private Label messageLabel;

    private PokerController controller;
    private PokerTCPClient tcpClient;
    private String currentPlayerId;

    @FXML
    public void initialize() {
        try {
            System.out.println("[DEBUG POKER_VIEW] Inicjalizacja widoku pokera");
            LogManager.logToFile("[DEBUG POKER_VIEW] Inicjalizacja widoku pokera");

            // Inicjalizacja klienta TCP z Service
            tcpClient = new PokerTCPClient(Service.getToken(), Service.getKeyManager());
            tcpClient.connect();
            System.out.println("[DEBUG POKER_VIEW] Połączono z serwerem TCP");
            LogManager.logToFile("[DEBUG POKER_VIEW] Połączono z serwerem TCP");

            // Inicjalizacja kontrolera
            controller = new PokerController();
            controller.setView(this);
            System.out.println("[DEBUG POKER_VIEW] Kontroler zainicjalizowany");
            LogManager.logToFile("[DEBUG POKER_VIEW] Kontroler zainicjalizowany");

            // Przygotowanie żądania dołączenia do pokera
            JsonObject request = new JsonObject();
            request.addProperty("type", "poker");
            request.addProperty("action", "join");
            request.addProperty("token", Service.getToken());

            // Wysłanie żądania
            tcpClient.sendEncryptedMessage(request.toString());
            System.out.println("[DEBUG POKER_VIEW] Wysłano żądanie dołączenia do pokera");
            LogManager.logToFile("[DEBUG POKER_VIEW] Wysłano żądanie dołączenia do pokera");

            // Czekaj na odpowiedź
            String response = tcpClient.readEncryptedMessage(1000);
            System.out.println("[DEBUG POKER_VIEW] Otrzymano odpowiedź: " + response);
            LogManager.logToFile("[DEBUG POKER_VIEW] Otrzymano odpowiedź: " + response);

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            if (json.has("type") && json.has("roomId")) {
                String type = json.get("type").getAsString();
                String roomId = json.get("roomId").getAsString();

                System.out.println("[DEBUG POKER_VIEW] " + type + " do pokoju: " + roomId);
                LogManager.logToFile("[DEBUG POKER_VIEW] " + type + " do pokoju: " + roomId);

                // Konfiguracja UI
                setupUI();
                updatePlayerActions(false);
                messageLabel.setText("Dołączono do gry. Oczekiwanie na innych graczy...");

            } else {
                throw new RuntimeException("Nieprawidłowa odpowiedź serwera - brak typu lub roomId");
            }

        } catch (Exception e) {
            System.err.println("[ERROR POKER_VIEW] Błąd inicjalizacji: " + e.getMessage());
            LogManager.logToFile("[ERROR POKER_VIEW] Błąd inicjalizacji: " + e.getMessage());
            e.printStackTrace();

            // Obsługa UI w przypadku błędu
            setupUI();
            if (messageLabel != null) {
                messageLabel.setText("Błąd połączenia z serwerem");
            }
        }
    }

    private void setupUI() {
        // Setup raise slider
        if (raiseSlider != null) {
            raiseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (raiseValueLabel != null) {
                    raiseValueLabel.setText("$" + newVal.intValue());
                }
            });
        }

        // Style community cards area
        if (communityCardsArea != null) {
            communityCardsArea.setAlignment(Pos.CENTER);
            communityCardsArea.setSpacing(10);
            communityCardsArea.setPadding(new Insets(10));
        }

        // Style player cards area
        if (playerCardsArea != null) {
            playerCardsArea.setAlignment(Pos.CENTER);
            playerCardsArea.setSpacing(10);
            playerCardsArea.setPadding(new Insets(10));
        }

        // Style action buttons
        if (actionButtons != null) {
            actionButtons.setAlignment(Pos.CENTER);
            actionButtons.setSpacing(15);
            actionButtons.setPadding(new Insets(10));
        }

        // Initialize message label
        if (messageLabel == null) {
            messageLabel = new Label("Welcome to Poker!");
            messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
        }
    }

    public void setController(PokerController controller) {
        this.controller = controller;
        controller.setView(this);
    }

    @FXML
    public void handleJoinGame() {
        if (playerNameField != null && !playerNameField.getText().trim().isEmpty()) {
            String playerName = playerNameField.getText().trim();
            // Generate simple player ID
            currentPlayerId = "player_" + System.currentTimeMillis();

            boolean joined = controller.joinGame(currentPlayerId, playerName);
            if (joined) {
                controller.setCurrentPlayer(currentPlayerId);
                showMessage(playerName + " joined the game!");
                playerNameField.setDisable(true);
            } else {
                showMessage("Could not join game - table is full!");
            }
        } else {
            showMessage("Please enter your name first!");
        }
    }

    @FXML
    public void handleStartGame() {
        if (controller.getPlayers().size() >= 2) {
            controller.startNewHand();
            showMessage("New hand started!");
        } else {
            showMessage("Need at least 2 players to start!");
        }
    }

    @FXML
    public void handleFold() {
        controller.playerFold();
        showMessage("You folded.");
    }

    @FXML
    public void handleCheck() {
        if (controller.canCheck()) {
            controller.playerCheck();
            showMessage("You checked.");
        } else {
            showMessage("Cannot check - there's a bet to call!");
        }
    }

    @FXML
    public void handleCall() {
        if (controller.canCall()) {
            int callAmount = controller.getCallAmount();
            controller.playerCall();
            showMessage("You called $" + callAmount);
        } else {
            showMessage("Cannot call!");
        }
    }

    @FXML
    public void handleRaise() {
        if (controller.canRaise() && raiseSlider != null) {
            int amount = (int) raiseSlider.getValue();
            if (amount >= controller.getMinRaise()) {
                controller.playerRaise(amount);
                showMessage("You raised $" + amount);
            } else {
                showMessage("Raise amount too small! Minimum: $" + controller.getMinRaise());
            }
        } else {
            showMessage("Cannot raise!");
        }
    }

    @FXML
    public void handleAllIn() {
        controller.playerAllIn();
        showMessage("You went all-in!");
    }

    public void updateGameState(PokerGame game) {
        Platform.runLater(() -> {
            // Update basic game info
            if (potLabel != null) {
                potLabel.setText("Pot: $" + game.getPot());
            }
            if (currentBetLabel != null) {
                currentBetLabel.setText("Current Bet: $" + game.getCurrentBet());
            }
            if (gameStateLabel != null) {
                gameStateLabel.setText("Game State: " + game.getGameState().toString().replace('_', ' '));
            }

            // Update current player info
            Player currentPlayer = game.getCurrentPlayer();
            if (currentPlayerLabel != null && currentPlayer != null) {
                currentPlayerLabel.setText("Current Turn: " + currentPlayer.getName());
            }

            // Update balance for current player
            if (balanceLabel != null && currentPlayerId != null) {
                Player player = game.getPlayerById(currentPlayerId);
                if (player != null) {
                    balanceLabel.setText("Balance: $" + player.getBalance());
                }
            }

            // Update community cards
            updateCommunityCards(game.getCommunityCards());

            // Update player's hand
            if (currentPlayerId != null) {
                Player player = game.getPlayerById(currentPlayerId);
                if (player != null) {
                    updatePlayerCards(player.getHand());
                }
            }

            // Update players list
            updatePlayersList(game.getPlayers());

            // Update raise slider range
            updateRaiseSlider();
        });
    }

    private void updateCommunityCards(List<Card> communityCards) {
        if (communityCardsArea != null) {
            communityCardsArea.getChildren().clear();

            for (Card card : communityCards) {
                Label cardLabel = createCardLabel(card);
                communityCardsArea.getChildren().add(cardLabel);
            }

            // Add placeholders for remaining cards
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

    private void updatePlayersList(List<Player> players) {
        if (playersListArea != null) {
            playersListArea.getChildren().clear();

            for (Player player : players) {
                VBox playerBox = createPlayerBox(player);
                playersListArea.getChildren().add(playerBox);
            }
        }
    }

    private VBox createPlayerBox(Player player) {
        VBox playerBox = new VBox(5);
        playerBox.setPadding(new Insets(10));
        playerBox.setAlignment(Pos.CENTER_LEFT);

        // Player name
        Label nameLabel = new Label(player.getName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // Player balance
        Label balanceLabel = new Label("Balance: $" + player.getBalance());

        // Current bet
        Label betLabel = new Label("Bet: $" + player.getCurrentBet());

        // Player status
        Label statusLabel = new Label();
        if (player.isFolded()) {
            statusLabel.setText("FOLDED");
            statusLabel.setTextFill(Color.RED);
            playerBox.setStyle("-fx-background-color: #ffeeee; -fx-border-color: #ffcccc; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        } else if (player.isAllIn()) {
            statusLabel.setText("ALL-IN");
            statusLabel.setTextFill(Color.ORANGE);
            playerBox.setStyle("-fx-background-color: #fff3e0; -fx-border-color: #ffcc80; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        } else {
            statusLabel.setText("ACTIVE");
            statusLabel.setTextFill(Color.GREEN);
            playerBox.setStyle("-fx-background-color: #f0f8f0; -fx-border-color: #c8e6c9; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        }

        // Highlight current player
        if (controller != null && controller.getGame().getCurrentPlayer() == player) {
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
            case HEARTS: return "♥";
            case DIAMONDS: return "♦";
            case CLUBS: return "♣";
            case SPADES: return "♠";
            default: return "?";
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
            int minRaise = controller.getMinRaise();
            int maxRaise = controller.getMaxRaise();

            raiseSlider.setMin(minRaise);
            raiseSlider.setMax(Math.max(minRaise, maxRaise));
            raiseSlider.setValue(minRaise);

            if (raiseValueLabel != null) {
                raiseValueLabel.setText("$" + minRaise);
            }
        }
    }

    public void updatePlayerActions(boolean isPlayerTurn) {
        Platform.runLater(() -> {
            if (actionButtons != null) {
                actionButtons.setDisable(!isPlayerTurn);
            }

            // Update individual button states
            if (controller != null && isPlayerTurn) {
                if (checkButton != null) {
                    checkButton.setDisable(!controller.canCheck());
                }
                if (callButton != null) {
                    callButton.setDisable(!controller.canCall());
                    if (controller.canCall()) {
                        callButton.setText("Call $" + controller.getCallAmount());
                    } else {
                        callButton.setText("Call");
                    }
                }
                if (raiseButton != null) {
                    raiseButton.setDisable(!controller.canRaise());
                }
                if (allInButton != null) {
                    allInButton.setDisable(controller.getMaxRaise() <= 0);
                }
            }

            // Show game state messages
            if (controller != null) {
                PokerGame.GameState state = controller.getGameState();
                switch (state) {
                    case WAITING_FOR_PLAYERS:
                        showMessage("Waiting for more players to join...");
                        break;
                    case SHOWDOWN:
                        showMessage("Showdown! Revealing cards...");
                        break;
                    case GAME_OVER:
                        showMessage("Hand finished! Starting new hand soon...");
                        break;
                }
            }
        });
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            Platform.runLater(() -> {
                messageLabel.setText(message);
                messageLabel.setStyle("-fx-text-fill: #2196f3; -fx-font-weight: bold;");
            });
        } else {
            System.out.println("POKER: " + message); // Fallback to console
        }
    }

    @FXML
    public void handleBack() {
        try {
            // Cleanup controller
            if (controller != null) {
                controller.shutdown();
            }
            tcpClient.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/dashboard.fxml"));
            Parent root = loader.load();

            DashboardController dashboardController = loader.getController();
            if (controller != null) {
             //   DashboardController.setBalance(controller.getBalance());
               // dashboardController.
            }

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getter methods for testing
    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public boolean isPlayerInGame() {
        return currentPlayerId != null && controller != null &&
                controller.getGame().getPlayerById(currentPlayerId) != null;
    }
}