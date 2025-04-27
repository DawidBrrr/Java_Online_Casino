package com.casino.java_online_casino.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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


    private final Map<List<String>, WinCondition> winConditions = Map.of(
            List.of("seven.png", "seven.png", "seven.png"), new WinCondition(1000, "jackpot.png", 3),
            List.of("orange.png", "orange.png", "orange.png"), new WinCondition(300, "big_win.png", 2),
            List.of("lemon.png", "lemon.png", "lemon.png"), new WinCondition(150, "small_win.png", 1),
            List.of("cherry.png", "cherry.png", "any"), new WinCondition(50, "mini_win.png", 0),
            List.of("seven.png", "seven.png", "any"), new WinCondition(100, "small_win.png", 0),
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
        balance -= 50; // Koszt zakładu
        updateBalance();
        if (isSpinning) return;
        isSpinning = true;
        resultLabel.setText("");

        // Losowanie wyników
        Image[] results = {
                symbols[random.nextInt(symbols.length)],
                symbols[random.nextInt(symbols.length)],
                symbols[random.nextInt(symbols.length)]
        };

        // Animacja 3D
        animateSpin(results);
    }

    private void animateSpin(Image[] results) {
        // Efekt rozmycia
        GaussianBlur blur = new GaussianBlur(0);
        slot1.setEffect(blur);
        slot2.setEffect(blur);
        slot3.setEffect(blur);

        // Animacja obrotu
        for (RotateTransition rt : rotateTransitions) {
            rt.playFromStart();
        }

        // Animacja rozmycia
        Timeline blurAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(500), new KeyValue(blur.radiusProperty(), 15)),
                new KeyFrame(Duration.millis(SPIN_DURATION), new KeyValue(blur.radiusProperty(), 0))
        );

        // Główna animacja slotów
        Timeline spinTimeline = new Timeline();
        spinTimeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(SPIN_DURATION), e -> {
                    slot1.setImage(results[0]);
                    slot2.setImage(results[1]);
                    slot3.setImage(results[2]);
                    checkResult(results);
                })
        );

        // Synchroniczne zakończenie
        ParallelTransition parallelTransition = new ParallelTransition(
                spinTimeline,
                blurAnimation,
                new SequentialTransition(
                        new PauseTransition(Duration.millis(SPIN_DURATION)),
                        new Timeline(
                                new KeyFrame(Duration.millis(100), e -> isSpinning = false)
                        )
                )
        );

        parallelTransition.play();
    }

    private void updateSlotImages() {
        slot1.setImage(getRandomImage());
        slot2.setImage(getRandomImage());
        slot3.setImage(getRandomImage());
    }

    private Image getRandomImage() {
        String icon = icons[random.nextInt(icons.length)];
        return new Image(getClass().getResourceAsStream("/com/casino/assets/" + icon));
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

        balance += bestWin.payout;
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

            resultLabel.setText("WYGRANA! $" + condition.payout);

        } catch (Exception e) {
            resultLabel.setText("wygrana $" + condition.payout);
        }
    }

    private String getSymbolName(ImageView imageView) {
        if (imageView == null || imageView.getImage() == null) {
            return "";
        }

        String url = imageView.getImage().getUrl();
        if (url == null) {
            return "";
        }

        int lastSlash = url.lastIndexOf("/");
        int lastDot = url.lastIndexOf(".");

        if (lastSlash == -1 || lastDot == -1 || lastSlash >= lastDot) {
            return "";
        }

        return url.substring(lastSlash + 1, lastDot);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/dashboard.fxml"));
            Parent root = loader.load();

            // Przekazanie balansu do DashboardController
            DashboardController dashboardController = loader.getController();
            DashboardController.setBalance(balance);
            dashboardController.updateBalance();

            Stage stage = (Stage) resultLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/casino/styles/casino.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Sigma Kasyno - Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
