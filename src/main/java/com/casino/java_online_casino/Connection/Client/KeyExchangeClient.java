package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class KeyExchangeClient {

    private final KeyManager keyManager;
    private final String serverUrl;
    private  String token;

    public KeyExchangeClient(String serverUrl) {
        this.serverUrl = serverUrl; // np. http://localhost:12346/key
        this.keyManager = new KeyManager();
    }

    public void performKeyExchange() {
        try {
            // 1. Przygotuj własny klucz publiczny w Base64
            String clientPublicKeyBase64 = keyManager.exportEcPublicKey();
            System.out.println("[DEBUG] Klucz publiczny klienta (Base64): " + clientPublicKeyBase64);

            // 2. Przygotuj żądanie JSON
            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("clientPublicKey", clientPublicKeyBase64);

            // 3. Otwórz połączenie HTTP
            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            System.out.println("[DEBUG] Wysyłam żądanie POST do " + serverUrl);

            // 4. Wyślij dane
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestJson.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input);
                os.flush();
            }

            // 5. Odczytaj odpowiedź
            int responseCode = connection.getResponseCode();
            System.out.println("[DEBUG] Otrzymano kod odpowiedzi HTTP: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream is = connection.getInputStream();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                    JsonObject responseJson = JsonUtil.parseJsonFromISReader(reader);

                    String serverPublicKeyBase64 = responseJson.get("serverPublicKey").getAsString();
                    token = responseJson.get("token").getAsString();

                    System.out.println("[DEBUG] Odebrano klucz publiczny serwera (Base64): " + serverPublicKeyBase64);
                    System.out.println("[DEBUG] Odebrano token JWT: " + token);

                    // 6. Importuj klucz publiczny serwera - wywołuje deriveSharedSecret
                    keyManager.importForeignKey(serverPublicKeyBase64);

                    // 7. Pokaż wspólny sekret AES (Base64)
                    byte[] aesKeyBytes = keyManager.getAesKey().getEncoded();
                    String aesKeyBase64 = Base64.getEncoder().encodeToString(aesKeyBytes);
                    System.out.println("[DEBUG] Wspólny sekret AES (Base64): " + aesKeyBase64);

                    System.out.println("✅ Klucz publiczny serwera i token JWT pomyślnie odebrane.");
                    System.out.println("🔐 Wspólny klucz AES ustawiony.");

                }
            } else {
                System.err.println("❌ Błąd po stronie serwera: " + responseCode);
                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        System.err.println("🧾 Treść błędu: " + errorResponse);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Wyjątek podczas wymiany kluczy: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public boolean login(String username, String password) {
        try {
            if (keyManager.getAesKey() == null) {
                System.err.println("❌ Brak ustalonego klucza AES. Najpierw wykonaj wymianę kluczy.");
                return false;
            }

            JsonObject loginJson = new JsonObject();
            loginJson.addProperty("email", username);
            loginJson.addProperty("password", password);

            String loginJsonString = loginJson.toString();
            System.out.println("[DEBUG] Dane logowania JSON: " + loginJsonString);

            // Szyfrujemy jawny tekst i otrzymujemy Base64
            String encryptedLoginBase64 = keyManager.encryptAes(loginJsonString);

            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("data", encryptedLoginBase64);

            String loginUrl = serverUrl.replace("/key", "/login");
            URL url = new URL(loginUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", token);  // <-- dodany nagłówek


            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("[DEBUG] Kod odpowiedzi logowania: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream is = connection.getInputStream();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                    JsonObject responseJson = JsonUtil.parseJsonFromISReader(reader);
                    String encryptedResponseBase64 = responseJson.get("data").getAsString();

                    // Tu poprawnie odszyfrowujemy Base64 zakodowany tekst
                    String decryptedResponse = keyManager.decryptAes(encryptedResponseBase64);

                    System.out.println("[DEBUG] Odszyfrowana odpowiedź logowania: " + decryptedResponse);

                    JsonObject loginResponse = JsonParser.parseString(decryptedResponse).getAsJsonObject();
                    String status = loginResponse.get("status").getAsString();
                    String token = loginResponse.get("token").getAsString();
                    System.out.println("[DEBUG] Token auth "+ token);

                    if ("ok".equalsIgnoreCase(status)) {
                        System.out.println("✅ Logowanie powiodło się.");
                        return true;
                    } else {
                        System.err.println("❌ Logowanie nie powiodło się: " + loginResponse.get("message").getAsString());
                        return false;
                    }
                }
            } else {
                System.err.println("❌ Błąd serwera przy logowaniu: " + responseCode);
                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        System.err.println("🧾 Treść błędu: " + errorResponse);
                    }
                }
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Wyjątek podczas logowania: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public boolean register(String firstName, String lastName, String email, String password, String birthDate) {
        try {
            if (keyManager.getAesKey() == null) {
                System.err.println("❌ Brak ustalonego klucza AES. Najpierw wykonaj wymianę kluczy.");
                return false;
            }

            JsonObject registerJson = new JsonObject();
            registerJson.addProperty("first_name", firstName);
            registerJson.addProperty("last_name", lastName);
            registerJson.addProperty("email", email);
            registerJson.addProperty("password", password);
            registerJson.addProperty("birth_date", birthDate);

            String registerJsonString = registerJson.toString();
            System.out.println("[DEBUG] Dane rejestracji JSON: " + registerJsonString);

            String encryptedRegisterBase64 = keyManager.encryptAes(registerJsonString);

            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("data", encryptedRegisterBase64);

            String registerUrl = serverUrl.replace("/key", "/register");
            URL url = new URL(registerUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", token); // nagłówek z JWT

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("[DEBUG] Kod odpowiedzi rejestracji: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream is = connection.getInputStream();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                    JsonObject responseJson = JsonUtil.parseJsonFromISReader(reader);
                    String encryptedResponseBase64 = responseJson.get("data").getAsString();

                    String decryptedResponse = keyManager.decryptAes(encryptedResponseBase64);
                    System.out.println("[DEBUG] Odszyfrowana odpowiedź rejestracji: " + decryptedResponse);

                    JsonObject registerResponse = JsonParser.parseString(decryptedResponse).getAsJsonObject();
                    String status = registerResponse.get("status").getAsString();

                    if ("ok".equalsIgnoreCase(status)) {
                        System.out.println("✅ Rejestracja powiodła się.");
                        return true;
                    } else {
                        System.err.println("❌ Rejestracja nie powiodła się: " + registerResponse.get("message").getAsString());
                        return false;
                    }
                }
            } else {
                System.err.println("❌ Błąd serwera przy rejestracji: " + responseCode);
                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        System.err.println("🧾 Treść błędu: " + errorResponse);
                    }
                }
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Wyjątek podczas rejestracji: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public KeyManager getKeyManager() {
        return keyManager;
    }
    public static void main(String[] args) {
        KeyExchangeClient client = new KeyExchangeClient("http://localhost:12346/key");
        client.performKeyExchange();
        client.register("Dominiik","Koralik","java@java.com", "admin", "26-09-2004");
        client.login("admin", "admin");
    }
}
