package com.casino.java_online_casino.games.slots.controller;

import com.casino.java_online_casino.controllers.DashboardController;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;

public class SlotsController {
    @FXML private Label resultLabel;
    @FXML private ImageView slot1;
    @FXML private ImageView slot2;
    @FXML private ImageView slot3;
    @FXML private Label balanceLabel;
    @FXML private ImageView winEffectImage;
    @FXML private HBox slotsContainer;
    @FXML private StackPane effectsPane;
    @FXML private Button playButton;
    @FXML private ComboBox<Integer> betComboBox;



    private final Map<List<String>, WinCondition> winConditions = Map.of(
            List.of("seven.png", "seven.png", "seven.png"), new WinCondition(20, "jackpot.png", 3),
            List.of("orange.png", "orange.png", "orange.png"), new WinCondition(8, "big_win.png", 2),
            List.of("lemon.png", "lemon.png", "lemon.png"), new WinCondition(5, "small_win.png", 1),
            List.of("cherry.png", "cherry.png", "any"), new WinCondition(1, "mini_win.png", 0),
            List.of("seven.png", "seven.png", "any"), new WinCondition(2, "small_win.png", 0),
            List.of("any", "any", "any"), new WinCondition(0, "lose.png", -1) // Domyślna przegrana
    );

    private double balance = DashboardController.getBalance();  // Początkowy balans gracza
    private Random random = new Random();
    private Map<Image, String> symbolNames = new HashMap<>();
    private Timeline timeline;

    private Image[] symbols;
    private final int FRAMES_PER_SECOND = 60;
    private final int SPIN_DURATION = 2000; // 2 sekundy
    private RotateTransition[] rotateTransitions;

    private int currentBet = 50;

    private static class WinCondition {
        double payout;
        String imageName;
        int priority;

        WinCondition(double payout, String imageName, int priority) {
            this.payout = payout;
            this.imageName = imageName;
            this.priority = priority;
        }
    }

    @FXML
    public void initialize() {
        // Ustawienia ComboBox
        betComboBox.getItems().addAll(10,20,50, 100, 200, 500, 1000);
        betComboBox.setValue(50);
        betComboBox.setOnAction(e -> currentBet = betComboBox.getValue());
        // Preload images
        symbols = new Image[] {
                loadImage("cherry.png"),
                loadImage("lemon.png"),
                loadImage("orange.png"),
                loadImage("seven.png")
        };
        symbolNames.put(symbols[0], "cherry.png");
        symbolNames.put(symbols[1], "lemon.png");
        symbolNames.put(symbols[2], "orange.png");
        symbolNames.put(symbols[3], "seven.png");

        // Ustawienie początkowych obrazków
        slot1.setImage(symbols[0]);
        slot2.setImage(symbols[1]);
        slot3.setImage(symbols[2]);

        setupAnimations();
        updateBalance();
    }
    private Image loadImage(String filename) {
        return new Image(getClass().getResourceAsStream("/com/casino/assets/" + filename));
    }

    private void setupAnimations() {
        rotateTransitions = new RotateTransition[3];
        for (int i = 0; i < 3; i++) {
            rotateTransitions[i] = new RotateTransition(Duration.millis(SPIN_DURATION),
                    i == 0 ? slot1 : i == 1 ? slot2 : slot3);
            rotateTransitions[i].setByAngle(360 * 4);
            rotateTransitions[i].setInterpolator(Interpolator.LINEAR);
        }
    }

    // Aktualizacja salda gracza
    private void updateBalance() {
        balanceLabel.setText(String.format("$%.2f", balance));
    }

    private String[] icons = { "cherry.png", "lemon.png", "orange.png", "seven.png" };
    private boolean isSpinning = false;

    @FXML
    private void spinSlots() {
        if (isSpinning) return;
        isSpinning = true;
        playButton.setDisable(true);

        if (balance < currentBet) {
            resultLabel.setText("Za mało środków!");
            isSpinning = false;
            playButton.setDisable(false);
            return;
        }

        balance -= currentBet;
        updateBalance();
        resultLabel.setText("");

        // Losowanie symboli
        Image[] results = {
                symbols[random.nextInt(symbols.length)],
                symbols[random.nextInt(symbols.length)],
                symbols[random.nextInt(symbols.length)]
        };

        // Tworzenie taska
        Task<Void> spinTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> animateSpin(results));
                Thread.sleep(SPIN_DURATION + 500); // Poczekaj aż animacje się zakończą (spin + efekty)
                return null;
            }
        };

        // Po zakończeniu taska
        spinTask.setOnSucceeded(event -> {
            isSpinning = false;
            playButton.setDisable(false);
        });

        Thread spinThread = new Thread(spinTask);
        spinThread.setDaemon(true);
        spinThread.start();
    }

    private void animateSpin(Image[] results) {
        playButton.setDisable(true);
        ImageView[] slots = {slot1, slot2, slot3};

        // Reset wszystkich slotów
        for (ImageView slot : slots) {
            slot.setEffect(new GaussianBlur(0));
            slot.setScaleX(1.0);
            slot.setScaleY(1.0);
        }

        ParallelTransition allSpinsParallel = new ParallelTransition();

        // Tworzenie animacji dla każdego slotu
        for (int i = 0; i < 3; i++) {
            final int slotIndex = i;
            Timeline spinTimeline = createSpinningAnimation(slots[i], results[i], SPIN_DURATION + (i * 1000));

            // Animacja obrotu
            RotateTransition rotate = new RotateTransition(Duration.millis(SPIN_DURATION + (i * 1000)), slots[i]);
            rotate.setByAngle(360 * 8);
            rotate.setInterpolator(Interpolator.EASE_OUT);

            // Sekwencja skalowania
            SequentialTransition scaleSequence = new SequentialTransition();

            // Skalowanie w górę
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), slots[i]);
            scaleUp.setToX(1.2);
            scaleUp.setToY(1.2);

            // Skalowanie w dół
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), slots[i]);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);

            scaleSequence.getChildren().addAll(scaleUp, scaleDown);
            scaleSequence.setCycleCount(3);

            // Łączymy animacje dla pojedynczego slotu
            ParallelTransition slotAnimation = new ParallelTransition(
                    rotate,
                    scaleSequence,
                    spinTimeline
            );

            allSpinsParallel.getChildren().add(slotAnimation);
        }

        allSpinsParallel.setOnFinished(e -> {
            checkResult(results);
            playButton.setDisable(false);
            // Reset skali wszystkich slotów
            for (ImageView slot : slots) {
                slot.setScaleX(1.0);
                slot.setScaleY(1.0);
            }
        });

        allSpinsParallel.play();
    }
    private Timeline createSpinningAnimation(ImageView slot, Image finalImage, double duration) {
        Timeline timeline = new Timeline();

        // Szybkie zmiany obrazków podczas kręcenia
        for (int i = 0; i < 20; i++) {
            timeline.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis((duration / 20.0) * i),
                            event -> slot.setImage(symbols[random.nextInt(symbols.length)])
                    )
            );
        }

        // Końcowy obrazek
        timeline.getKeyFrames().add(
                new KeyFrame(
                        Duration.millis(duration),
                        event -> slot.setImage(finalImage)
                )
        );

        return timeline;
    }

    private void checkResult(Image[] results) {
        String[] symbols = Arrays.stream(results)
                .map(img -> symbolNames.get(img))
                .toArray(String[]::new);

        List<WinCondition> matchedConditions = new ArrayList<>();

        winConditions.forEach((pattern, condition) -> {
            if (matchesPattern(symbols, pattern)) {
                matchedConditions.add(condition);
            }
        });

        WinCondition bestWin = matchedConditions.stream()
                .max(Comparator.comparingInt(c -> c.priority))
                .orElse(winConditions.get(List.of("any", "any", "any")));

        balance += (bestWin.payout * currentBet);
        Platform.runLater(() -> showWinEffect(bestWin));
        updateBalance();
    }
    private boolean matchesPattern(String[] symbols, List<String> pattern) {
        for (int i = 0; i < 3; i++) {
            String p = pattern.get(i);
            if (!p.equals("any") && !p.equals(symbols[i])) {
                return false;
            }
        }
        return true;
    }

    private void showWinEffect(WinCondition condition) {
        try {
            // Załaduj obraz efektu
            Image effectImage = new Image(getClass().getResourceAsStream(
                    "/com/casino/effects/" + condition.imageName));

            // Utwórz dynamiczny ImageView
            ImageView effect = new ImageView(effectImage);
            effect.setFitWidth(300);
            effect.setFitHeight(300);
            effect.setPreserveRatio(true);
            effect.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(255,215,0,0.9), 30, 0.5, 0, 0);");

            // Pozycjonuj efekt względem slotów
            effect.setTranslateY(-slotsContainer.getHeight()/2);
            effectsPane.getChildren().add(effect);

            // Animacja wejścia
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(500), effect);
            scaleIn.setFromX(0.1);
            scaleIn.setFromY(0.1);
            scaleIn.setToX(1.2);
            scaleIn.setToY(1.2);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), effect);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            // Animacja wyjścia
            ScaleTransition scaleOut = new ScaleTransition(Duration.millis(500), effect);
            scaleOut.setToX(0.5);
            scaleOut.setToY(0.5);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), effect);
            fadeOut.setToValue(0.0);

            // Sekwencja animacji
            SequentialTransition sequence = new SequentialTransition(
                    new ParallelTransition(scaleIn, fadeIn),
                    new PauseTransition(Duration.millis(1000)),
                    new ParallelTransition(scaleOut, fadeOut),
                    new PauseTransition(Duration.millis(500))
            );
            sequence.play();

            resultLabel.setText("WYGRANA! $" + (condition.payout * currentBet));

        } catch (Exception e) {
            resultLabel.setText("wygrana $" + (condition.payout * currentBet));
        }
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/dashboard.fxml"));
            Parent root = loader.load();

            // Przekazanie balansu do DashboardController
            DashboardController dashboardController = loader.getController();
            DashboardController.setBalance(balance);
            dashboardController.updateBalance();

            Stage stage = (Stage) resultLabel.getScene().getWindow();
            // Pobierz wymiary ekranu
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