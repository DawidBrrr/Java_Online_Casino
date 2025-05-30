    package com.casino.java_online_casino.games.blackjack.gui;
    import com.casino.java_online_casino.Connection.Client.KeyExchangeService;
    import com.casino.java_online_casino.Connection.Client.Service;
    import com.casino.java_online_casino.controllers.DashboardController;
    import com.casino.java_online_casino.games.blackjack.controller.BlackjackTcpClient;
    import com.casino.java_online_casino.games.blackjack.controller.RemoteBlackJackController;
    import javafx.geometry.Rectangle2D;
    import javafx.stage.Screen;
    import com.casino.java_online_casino.games.blackjack.controller.BlackJackController;
    import com.casino.java_online_casino.games.blackjack.model.Card;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Parent;
    import javafx.scene.Scene;
    import javafx.scene.control.*;
    import javafx.scene.image.Image;
    import javafx.scene.image.ImageView;
    import javafx.scene.layout.FlowPane;
    import javafx.stage.Stage;

    import java.io.IOException;

    public class BlackJackGUIController {
        private static int conCounter = 0;

        @FXML
        private FlowPane playerCards;

        @FXML
        private FlowPane dealerCards;

        @FXML
        private Label playerScoreLabel;

        @FXML
        private Label dealerScoreLabel;

        @FXML
        private Label statusLabel;
        private RemoteBlackJackController controller;
        private BlackjackTcpClient tcpClient;
        @FXML
        private ComboBox<Integer> betComboBox;

        @FXML
        private Label balanceLabel;

        private int currentBet = 50;
        private double balance = DashboardController.getBalance();


        @FXML
        public void initialize(){
            tcpClient = new BlackjackTcpClient(Service.getToken(), Service.getKeyManager());
            try {
                tcpClient.connect();
            } catch (IOException e) {
                throw new RuntimeException("Połączenie z blackjack nie powiodło się");
            }
            controller = new RemoteBlackJackController(tcpClient);

            betComboBox.getItems().addAll(10, 20, 50, 100, 200, 500, 1000);
            betComboBox.setValue(currentBet);
            betComboBox.setOnAction(e -> currentBet = betComboBox.getValue());

            updateBalance();
            if(controller.getDealerHand()!=null){
                updateUI();
                showResult();
            }
            statusLabel.setText("Kliknij 'Nowa Gra', aby rozpocząć.");
        }

        private void updateBalance() {
            balanceLabel.setText(String.format("Saldo: $%.2f", balance));
        }

        @FXML
        public void onHit(){
            try {
                controller.playerHit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            updateUI();
            if (controller.isGameOver()) {
                showResult();
            }
        }

        @FXML
        public void onStand() {
            try {
                controller.playerStand();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            updateUI();
            showResult();
        }

        @FXML
        public void onNewGame()  {
            try {
                startNewGame();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @FXML
        public void onReturnToMenu() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/dashboard.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) statusLabel.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void startNewGame() throws Exception {
            if (balance < currentBet) {
                statusLabel.setText("Za mało środków, aby rozpocząć grę!");
                return;
            }

            balance -= currentBet;
            updateBalance();

            controller.startNewGame();
            updateUI();
            statusLabel.setText("Rozdano karty.");
        }

        private void updateUI() {
            playerCards.getChildren().clear();
            dealerCards.getChildren().clear();

            for (Card card : controller.getPlayerHand()) {
                playerCards.getChildren().add(createCardImage(card));
            }

            for (Card card : controller.getDealerHand()) {
                dealerCards.getChildren().add(createCardImage(card));
            }

            updateScores();
        }

        private void updateScores() {
            playerScoreLabel.setText("Punkty: " + controller.getPlayerScore());
            dealerScoreLabel.setText("Punkty: " + controller.getDealerScore());
        }

        private void showResult() {
            String result = controller.getGameResult();

            switch (result) {
                case "Wygrałeś!":
                case "Wygrałeś! Krupier ma powyżej 21.":
                    balance += currentBet * 2; // zwrot stawki + wygrana
                    break;
                case "Remis!":
                    balance += currentBet; // tylko zwrot stawki
                    break;
                case "Przegrałeś!":
                case "Przegrałeś! Masz powyżej 21.":
                    // przegrana – brak zwrotu
                    break;
            }

            updateBalance();
            statusLabel.setText(result);
        }

        private ImageView createCardImage(Card card) {
            try {
                String path = card.getImagePath();  // np. "/com/casino/assets/cards/ace_of_spades.png"
                Image image = new Image(getClass().getResourceAsStream(path));
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(80);
                imageView.setPreserveRatio(true);
                return imageView;
            } catch (Exception e) {
                System.err.println("Nie można załadować obrazu: " + card.getImagePath());
                return new ImageView();
            }
        }

        @FXML
        public void handleBack() {
            try {
                tcpClient.close();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController dashboardController = (DashboardController) loader.getController();
                DashboardController.setBalance(balance); // przekazanie balansu z blackjacka
                dashboardController.updateBalance();

                Stage stage = (Stage) statusLabel.getScene().getWindow();
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
