package com.casino.java_online_casino.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class AuthController {
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;
    @FXML private TextField registerUsername;
    @FXML private PasswordField registerPassword;
    @FXML private PasswordField registerConfirm;
    @FXML private Label registerError;
    @FXML private ImageView backgroundImage;

    @FXML
    public void initialize(){
        // Bindowanie rozmiaru obrazu do rozmiaru rodzica (StackPane)
        StackPane parent = (StackPane) backgroundImage.getParent();
        backgroundImage.fitWidthProperty().bind(parent.widthProperty());
        backgroundImage.fitHeightProperty().bind(parent.heightProperty());

    }
    @FXML
    private void handleLogin() {
        String username = loginUsername.getText();
        String password = loginPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            loginError.setText("Proszę wypełnić wszystkie pola");
            return;
        }

        // Tymczasowa symulacja logowania
        if (username.equals("admin") && password.equals("admin")) {
            try {
                // Ładowanie dashboardu
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/dashboard.fxml"));
                Parent root = loader.load();

                // Przekazanie danych użytkownika
                DashboardController controller = loader.getController();
                controller.initialize(username);

                // Zmiana sceny
                Stage stage = (Stage) loginUsername.getScene().getWindow();
                stage.setTitle("Sigma Kasyno - Panel Użytkownika");
                // Pobierz wymiary ekranu
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.setResizable(true);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            loginError.setText("Błędne dane logowania");
        }
    }

    @FXML
    private void handleRegister() {
        String username = registerUsername.getText();
        String password = registerPassword.getText();
        String confirm = registerConfirm.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            registerError.setText("Proszę wypełnić wszystkie pola");
            return;
        }

        if (!password.equals(confirm)) {
            registerError.setText("Hasła nie pasują do siebie");
            return;
        }

        // Tymczasowa symulacja rejestracji
        registerError.setText("Rejestracja zakończona sukcesem!");
        registerUsername.setText("");
        registerPassword.setText("");
        registerConfirm.setText("");
    }

}