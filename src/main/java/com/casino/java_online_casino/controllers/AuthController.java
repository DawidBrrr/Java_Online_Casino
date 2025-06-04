package com.casino.java_online_casino.controllers;

import com.casino.java_online_casino.Connection.Client.*;
import com.casino.java_online_casino.Connection.Server.DTO.GamerDTO;
import com.casino.java_online_casino.User.Gamer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;

public class AuthController {
    // Logowanie
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;

    // Rejestracja (pełny zestaw pól)
    @FXML private TextField registerFirstName;
    @FXML private TextField registerLastName;
    @FXML private TextField registerNickname;
    @FXML private TextField registerEmail;
    @FXML private PasswordField registerPassword;
    @FXML private PasswordField registerConfirm;
    @FXML private DatePicker registerBirthDate;
    @FXML private Label registerError;

    @FXML private ImageView backgroundImage;

    @FXML
    private StackPane rootPane;

    @FXML
    private VBox formContainer;


    @FXML
    public void initialize() {
        // Bindowanie rozmiaru obrazu do rozmiaru rodzica (StackPane)
        backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());

        // Nasłuchiwanie zmian wysokości i szerokości dla dynamicznych marginesów
        rootPane.widthProperty().addListener((obs, oldVal, newVal) -> updateMargins());
        rootPane.heightProperty().addListener((obs, oldVal, newVal) -> updateMargins());

        // Pierwsze ustawienie marginesów
        Platform.runLater(this::updateMargins);
    }

    private void updateMargins() {
        double width = rootPane.getWidth();
        double height = rootPane.getHeight();

        if (width > 0 && height > 0) {
            double horizontalMargin = Math.max(20, width * 0.05);  // minimum 20px
            double verticalMargin = Math.max(20, height * 0.1);    // minimum 20px, 10% wysokości

            StackPane.setMargin(formContainer, new Insets(verticalMargin, horizontalMargin, verticalMargin, horizontalMargin));
        }
    }

    @FXML
    private void handleLogin() {
        // Wykonaj wymianę kluczy przed otwarciem okien
        Service keyService = new KeyExchangeService();
        Thread keyExchangeThread = new Thread(keyService, "KeyExchange-Thread");
        keyExchangeThread.start();

        String username = loginUsername.getText();
        String password = loginPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            loginError.setText("Proszę wypełnić wszystkie pola");
            return;
        }

        // >>> DODAJ HANDSHAKE ZAWSZE PRZED logowaniem <<<
        try {
            com.casino.java_online_casino.Connection.Client.Service.keyManager =
                    new com.casino.java_online_casino.Connection.Tokens.KeyManager();
            com.casino.java_online_casino.Connection.Client.KeyExchangeService handshake =
                    new com.casino.java_online_casino.Connection.Client.KeyExchangeService();
            boolean handshakeResult = handshake.perform();
            if (!handshakeResult) {
                loginError.setText("Błąd podczas wymiany kluczy z serwerem. Spróbuj ponownie.");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            loginError.setText("Błąd podczas nawiązywania połączenia szyfrowanego.");
            return;
        }
        // <<< KONIEC: handshake

        LoginService service = new LoginService(username, password);
        boolean result = service.perform();

        if (result && service.getLoginResult()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/casino/java_online_casino/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                controller.initialize(username);

                Stage stage = (Stage) loginUsername.getScene().getWindow();
                stage.setTitle("Sigma Kasyno - Panel Użytkownika");
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.setResizable(true);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                loginError.setText("Wystąpił błąd podczas ładowania panelu użytkownika.");
            }
        } else {
            loginError.setText("Błędne dane logowania lub problem z serwerem.");
        }
        new Thread(() -> {
            GamerDTO gamer = UserDataService.updateGamerDTO();
        }).start();
    }

    @FXML
    private void handleRegister() {

        // Wykonaj wymianę kluczy przed otwarciem okien
        Service keyService = new KeyExchangeService();
        Thread keyExchangeThread = new Thread(keyService, "KeyExchange-Thread");
        keyExchangeThread.start();

        String firstName = registerFirstName.getText();
        String lastName = registerLastName.getText();
        String nickname = registerNickname.getText();
        String email = registerEmail.getText();
        String password = registerPassword.getText();
        String confirm = registerConfirm.getText();
        LocalDate birthDate = registerBirthDate.getValue();

        // Walidacja pól
        if (firstName.isEmpty() || lastName.isEmpty() || nickname.isEmpty() ||
                email.isEmpty() || password.isEmpty() || confirm.isEmpty() || birthDate == null) {
            registerError.setText("Proszę wypełnić wszystkie pola");
            return;
        }

        if (!isValidEmail(email)) {
            registerError.setText("Nieprawidłowy adres e-mail");
            return;
        }

        if (!password.equals(confirm)) {
            registerError.setText("Hasła nie pasują do siebie");
            return;
        }

        if (birthDate.isAfter(LocalDate.now().minusYears(18))) {
            registerError.setText("Musisz mieć ukończone 18 lat");
            return;
        }

        // >>> HANDSHAKE tuż przed próbą rejestracji!
        try {
            com.casino.java_online_casino.Connection.Client.Service.keyManager =
                    new com.casino.java_online_casino.Connection.Tokens.KeyManager();
            com.casino.java_online_casino.Connection.Client.KeyExchangeService handshake =
                    new com.casino.java_online_casino.Connection.Client.KeyExchangeService();
            boolean handshakeResult = handshake.perform();
            if (!handshakeResult) {
                registerError.setText("Błąd podczas wymiany kluczy z serwerem. Spróbuj ponownie.");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            registerError.setText("Błąd podczas nawiązywania połączenia szyfrowanego.");
            return;
        }
        // <<< KONIEC handshake

        Date birth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Domyślne kredyty: 0 (lub inna logika)
        Gamer gamer = new Gamer(-1, firstName, lastName, nickname, email, password, 0.0f, birth);

        RegisterService service = new RegisterService(gamer);
        boolean result = service.perform();

        if (result && service.getRegisterResult()) {
            registerError.setStyle("-fx-text-fill: green;");
            registerError.setText("Rejestracja zakończona sukcesem!");
            clearRegisterFields();
        } else {
            registerError.setStyle("-fx-text-fill: red;");
            registerError.setText("Rejestracja nie powiodła się. Sprawdź dane lub spróbuj ponownie.");
        }
    }


    private void clearRegisterFields() {
        registerFirstName.setText("");
        registerLastName.setText("");
        registerNickname.setText("");
        registerEmail.setText("");
        registerPassword.setText("");
        registerConfirm.setText("");
        registerBirthDate.setValue(null);
    }

    private boolean isValidEmail(String email) {
        // Prosty regex dla e-maila
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(emailRegex, email);
    }
}
