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
import javafx.scene.effect.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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

    private final Map<List<String>, WinCondition> winConditions = Map.ofEntries(
            // Główne kombinacje
            Map.entry(List.of("seven.png", "seven.png", "seven.png"), new WinCondition(20, "jackpot.png", 5)),
            Map.entry(List.of("orange.png", "orange.png", "orange.png"), new WinCondition(8, "big_win.png", 4)),
            Map.entry(List.of("lemon.png", "lemon.png", "lemon.png"), new WinCondition(5, "small_win.png", 3)),
            Map.entry(List.of("cherry.png", "cherry.png", "cherry.png"), new WinCondition(3, "small_win.png", 2)),

            // Kombinacje z 2 symbolami seven
            Map.entry(List.of("seven.png", "seven.png", "cherry.png"), new WinCondition(2, "small_win.png", 1)),
            Map.entry(List.of("seven.png", "cherry.png", "seven.png"), new WinCondition(2, "small_win.png", 1)),
            Map.entry(List.of("cherry.png", "seven.png", "seven.png"), new WinCondition(2, "small_win.png", 1)),
            Map.entry(List.of("seven.png", "seven.png", "orange.png"), new WinCondition(2, "small_win.png", 1)),
            Map.entry(List.of("seven.png", "seven.png", "lemon.png"), new WinCondition(2, "small_win.png", 1)),

            // Kombinacje z 2 symbolami cherry
            Map.entry(List.of("cherry.png", "cherry.png", "orange.png"), new WinCondition(1, "mini_win.png", 0)),
            Map.entry(List.of("cherry.png", "orange.png", "cherry.png"), new WinCondition(1, "mini_win.png", 0)),
            Map.entry(List.of("orange.png", "cherry.png", "cherry.png"), new WinCondition(1, "mini_win.png", 0)),
            Map.entry(List.of("cherry.png", "cherry.png", "lemon.png"), new WinCondition(1, "mini_win.png", 0)),
            Map.entry(List.of("cherry.png", "cherry.png", "seven.png"), new WinCondition(1, "mini_win.png", 0)),

            // Domyślna kombinacja (przegrana)
            Map.entry(List.of("any", "any", "any"), new WinCondition(0, "lose.png", -1))
    );

    private double balance = DashboardController.getBalance();
    private Random random = new Random();
    private Map<Image, String> symbolNames = new HashMap<>();
    private Timeline timeline;

    private Image[] symbols;
    private final int FRAMES_PER_SECOND = 60;
    private final int SPIN_DURATION = 3000; // Wydłużone do 3 sekund dla lepszego efektu
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
        betComboBox.getItems().addAll(10,20,50, 100, 200, 500, 1000);
        betComboBox.setValue(50);
        betComboBox.setOnAction(e -> currentBet = betComboBox.getValue());

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

        slot1.setImage(symbols[0]);
        slot2.setImage(symbols[1]);
        slot3.setImage(symbols[2]);

        setupAdvancedAnimations();
        updateBalance();
    }

    private Image loadImage(String filename) {
        return new Image(getClass().getResourceAsStream("/com/casino/assets/" + filename));
    }

    private void setupAdvancedAnimations() {
        rotateTransitions = new RotateTransition[3];
        ImageView[] slots = {slot1, slot2, slot3};

        for (int i = 0; i < 3; i++) {
            // Dodaj początkowe efekty wizualne do slotów
            setupSlotEffects(slots[i]);
        }
    }

    private void setupSlotEffects(ImageView slot) {
        // Dodaj subtelny blask do slotów
        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setColor(Color.GOLD);
        innerShadow.setRadius(5);
        innerShadow.setChoke(0.3);

        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.BLACK);
        dropShadow.setRadius(10);
        dropShadow.setOffsetX(3);
        dropShadow.setOffsetY(3);

        slot.setEffect(dropShadow);
    }

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

        // Losowanie symboli z większą dramaturgią
        Image[] results = {
                symbols[random.nextInt(symbols.length)],
                symbols[random.nextInt(symbols.length)],
                symbols[random.nextInt(symbols.length)]
        };

        // Rozpocznij spektakularną animację
        startSpectacularSpin(results);
    }

    private void startSpectacularSpin(Image[] results) {
        playButton.setDisable(true);
        ImageView[] slots = {slot1, slot2, slot3};

        // Reset wszystkich efektów
        for (ImageView slot : slots) {
            slot.setEffect(null);
            slot.setScaleX(1.0);
            slot.setScaleY(1.0);
            slot.setRotate(0);
        }

        // Animacja przygotowawcza - "naładowanie" slotów
        ParallelTransition preparation = createPreparationAnimation(slots);

        preparation.setOnFinished(e -> {
            // Główna animacja spinowania
            ParallelTransition mainSpin = createMainSpinAnimation(slots, results);
            mainSpin.play();
        });

        preparation.play();
    }

    private ParallelTransition createPreparationAnimation(ImageView[] slots) {
        ParallelTransition prep = new ParallelTransition();

        for (int i = 0; i < slots.length; i++) {
            ImageView slot = slots[i];

            // Efekt "ładowania energii"
            Timeline energyBuild = new Timeline();
            for (int j = 0; j <= 10; j++) {
                final double intensity = j / 10.0;
                energyBuild.getKeyFrames().add(
                        new KeyFrame(Duration.millis(j * 50), event -> {
                            // Pulsujący blask
                            InnerShadow glow = new InnerShadow();
                            glow.setColor(Color.CYAN);
                            glow.setRadius(intensity * 15);
                            glow.setChoke(0.8);
                            slot.setEffect(glow);

                            // Subtelne wibracje
                            slot.setTranslateX(Math.sin(intensity * Math.PI * 4) * 2);
                        })
                );
            }

            // Skalowanie przygotowawcze
            ScaleTransition scalePrep = new ScaleTransition(Duration.millis(500), slot);
            scalePrep.setFromX(1.0);
            scalePrep.setFromY(1.0);
            scalePrep.setToX(1.1);
            scalePrep.setToY(1.1);
            scalePrep.setAutoReverse(true);
            scalePrep.setCycleCount(2);

            ParallelTransition slotPrep = new ParallelTransition(energyBuild, scalePrep);
            prep.getChildren().add(slotPrep);
        }

        return prep;
    }

    private ParallelTransition createMainSpinAnimation(ImageView[] slots, Image[] results) {
        ParallelTransition mainAnimation = new ParallelTransition();

        for (int i = 0; i < slots.length; i++) {
            final int slotIndex = i;
            ImageView slot = slots[i];

            // Każdy slot ma różne opóźnienie dla dramatycznego efektu
            int delay = i * 800;
            int spinDuration = SPIN_DURATION + delay;

            // Kompleksowa animacja dla każdego slotu
            SequentialTransition slotSequence = createAdvancedSlotAnimation(slot, results[i], spinDuration, delay);
            mainAnimation.getChildren().add(slotSequence);
        }

        mainAnimation.setOnFinished(e -> {
            checkResult(results);
            playButton.setDisable(false);
            isSpinning = false;

            // Reset wszystkich slotów do normalnego stanu
            for (ImageView slot : slots) {
                slot.setScaleX(1.0);
                slot.setScaleY(1.0);
                slot.setRotate(0);
                slot.setTranslateX(0);
                slot.setTranslateY(0);
                setupSlotEffects(slot);
            }
        });

        return mainAnimation;
    }

    private SequentialTransition createAdvancedSlotAnimation(ImageView slot, Image finalImage, int totalDuration, int delay) {
        SequentialTransition sequence = new SequentialTransition();

        // Opóźnienie początkowe
        if (delay > 0) {
            sequence.getChildren().add(new PauseTransition(Duration.millis(delay)));
        }

        // Faza 1: Szybkie przyspieszenie
        ParallelTransition accelerationPhase = new ParallelTransition();

        // Ultra szybka rotacja z efektami
        RotateTransition hyperSpin = new RotateTransition(Duration.millis(totalDuration * 0.7), slot);
        hyperSpin.setByAngle(360 * 12); // Bardzo szybkie obroty
        hyperSpin.setInterpolator(Interpolator.EASE_IN);

        // Zmiana obrazków z efektem blur
        Timeline imageBlitz = createImageBlitzAnimation(slot, finalImage, totalDuration * 0.7);

        // Dynamiczne skalowanie i efekty
        Timeline dynamicEffects = createDynamicEffectsAnimation(slot, totalDuration * 0.7);

        accelerationPhase.getChildren().addAll(hyperSpin, imageBlitz, dynamicEffects);

        // Faza 2: Dramatyczne spowolnienie
        ParallelTransition decelerationPhase = new ParallelTransition();

        // Spowolnienie rotacji
        RotateTransition slowDown = new RotateTransition(Duration.millis(totalDuration * 0.3), slot);
        slowDown.setByAngle(360 * 2);
        slowDown.setInterpolator(Interpolator.EASE_OUT);

        // Finalne ustawienie obrazka z efektem
        Timeline finalImageSet = new Timeline();
        finalImageSet.getKeyFrames().add(
                new KeyFrame(Duration.millis(totalDuration * 0.25), event -> {
                    slot.setImage(finalImage);
                    // Efekt błysku przy zatrzymaniu
                    createStopFlash(slot);
                })
        );

        decelerationPhase.getChildren().addAll(slowDown, finalImageSet);

        sequence.getChildren().addAll(accelerationPhase, decelerationPhase);
        return sequence;
    }

    private Timeline createImageBlitzAnimation(ImageView slot, Image finalImage, double duration) {
        Timeline timeline = new Timeline();

        // Bardzo szybka zmiana obrazków z efektami
        for (int i = 0; i < 30; i++) {
            final double progress = i / 30.0;
            timeline.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis((duration / 30.0) * i),
                            event -> {
                                slot.setImage(symbols[random.nextInt(symbols.length)]);

                                // Blur effect podczas szybkiego spinowania
                                GaussianBlur blur = new GaussianBlur();
                                blur.setRadius(Math.sin(progress * Math.PI * 2) * 5 + 2);
                                slot.setEffect(blur);
                            }
                    )
            );
        }

        return timeline;
    }

    private Timeline createDynamicEffectsAnimation(ImageView slot, double duration) {
        Timeline effects = new Timeline();

        for (int i = 0; i <= 50; i++) {
            final double progress = i / 50.0;
            effects.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis((duration / 50.0) * i),
                            event -> {
                                // Dynamiczne skalowanie
                                double scale = 1.0 + Math.sin(progress * Math.PI * 8) * 0.1;
                                slot.setScaleX(scale);
                                slot.setScaleY(scale);

                                // Subtelne przemieszczenie
                                slot.setTranslateY(Math.sin(progress * Math.PI * 6) * 3);

                                // Zmieniający się blask
                                InnerShadow glow = new InnerShadow();
                                glow.setColor(Color.ORANGE);
                                glow.setRadius(10 + Math.sin(progress * Math.PI * 4) * 5);
                                glow.setChoke(0.6);

                                Blend blend = new Blend();
                                blend.setTopInput(glow);
                                blend.setBottomInput(new GaussianBlur(2));
                                slot.setEffect(blend);
                            }
                    )
            );
        }

        return effects;
    }

    private void createStopFlash(ImageView slot) {
        // Efekt błysku przy zatrzymaniu
        Timeline flash = new Timeline();

        // Białe światło
        ColorAdjust brighten = new ColorAdjust();
        brighten.setBrightness(1.0);

        // Złoty blask
        InnerShadow goldGlow = new InnerShadow();
        goldGlow.setColor(Color.GOLD);
        goldGlow.setRadius(20);
        goldGlow.setChoke(0.8);

        Blend flashEffect = new Blend();
        flashEffect.setTopInput(brighten);
        flashEffect.setBottomInput(goldGlow);

        flash.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(slot.effectProperty(), flashEffect),
                        new KeyValue(slot.scaleXProperty(), 1.3),
                        new KeyValue(slot.scaleYProperty(), 1.3)
                ),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(slot.effectProperty(), null),
                        new KeyValue(slot.scaleXProperty(), 1.0),
                        new KeyValue(slot.scaleYProperty(), 1.0)
                )
        );

        flash.play();
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
        Platform.runLater(() -> showSpectacularWinEffect(bestWin));
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

    private void showSpectacularWinEffect(WinCondition condition) {
        try {
            Image effectImage = new Image(getClass().getResourceAsStream(
                    "/com/casino/effects/" + condition.imageName));

            ImageView effect = new ImageView(effectImage);
            effect.setFitWidth(400);
            effect.setFitHeight(400);
            effect.setPreserveRatio(true);

            // Spektakularny efekt cienia i blasku
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.GOLD);
            shadow.setRadius(30);
            shadow.setSpread(0.8);

            InnerShadow innerGlow = new InnerShadow();
            innerGlow.setColor(Color.YELLOW);
            innerGlow.setRadius(15);
            innerGlow.setChoke(0.9);

            Blend winEffect = new Blend();
            winEffect.setTopInput(shadow);
            winEffect.setBottomInput(innerGlow);
            effect.setEffect(winEffect);

            effect.setTranslateY(-slotsContainer.getHeight()/2);
            effectsPane.getChildren().add(effect);

            // Spektakularna animacja wejścia
            createSpectacularWinAnimation(effect, condition);

            String winText = condition.payout > 0 ?
                    "🎉 WYGRANA! 🎉 $" + (condition.payout * currentBet) :
                    "Spróbuj ponownie!";
            resultLabel.setText(winText);

        } catch (Exception e) {
            String winText = condition.payout > 0 ?
                    "WYGRANA! $" + (condition.payout * currentBet) :
                    "Spróbuj ponownie!";
            resultLabel.setText(winText);
        }
    }

    private void createSpectacularWinAnimation(ImageView effect, WinCondition condition) {
        // Animacja wejścia z eksplozją
        ScaleTransition explosiveEntry = new ScaleTransition(Duration.millis(300), effect);
        explosiveEntry.setFromX(0.1);
        explosiveEntry.setFromY(0.1);
        explosiveEntry.setToX(1.5);
        explosiveEntry.setToY(1.5);
        explosiveEntry.setInterpolator(Interpolator.EASE_OUT);

        // Rotacja podczas wejścia
        RotateTransition entryRotation = new RotateTransition(Duration.millis(600), effect);
        entryRotation.setByAngle(720);
        entryRotation.setInterpolator(Interpolator.EASE_OUT);

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), effect);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Pulsowanie podczas wyświetlania
        ScaleTransition pulse = new ScaleTransition(Duration.millis(500), effect);
        pulse.setFromX(1.5);
        pulse.setFromY(1.5);
        pulse.setToX(1.2);
        pulse.setToY(1.2);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(4);

        // Animacja wyjścia
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(600), effect);
        scaleOut.setToX(0.3);
        scaleOut.setToY(0.3);
        scaleOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), effect);
        fadeOut.setToValue(0.0);

        // Sekwencja wszystkich animacji
        SequentialTransition masterSequence = new SequentialTransition(
                new ParallelTransition(explosiveEntry, entryRotation, fadeIn),
                pulse,
                new PauseTransition(Duration.millis(800)),
                new ParallelTransition(scaleOut, fadeOut)
        );

        masterSequence.setOnFinished(e -> effectsPane.getChildren().remove(effect));
        masterSequence.play();
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/dashboard.fxml"));
            Parent root = loader.load();

            DashboardController dashboardController = loader.getController();
            DashboardController.setBalance(balance);
            dashboardController.updateBalance();

            Stage stage = (Stage) resultLabel.getScene().getWindow();
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