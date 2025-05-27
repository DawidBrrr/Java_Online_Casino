package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginService extends Service {
    String username, password;
    public LoginService(String username, String password) {
        this.username = username;
        this.password = password;
    }
    @Override
    public JsonObject toJson() {
        JsonObject loginJson = new JsonObject();
        loginJson.addProperty("email", username);
        loginJson.addProperty("password", password);
        return loginJson;
    }

    @Override
    public boolean perform() {
        try {
            if (keyManager.getAesKey() == null) {
                System.err.println("❌ Brak ustalonego klucza AES. Najpierw wykonaj wymianę kluczy.");
                return false;
            }

           String loginJsonString = toJson().toString();

            System.out.println("[DEBUG] Dane logowania JSON: " + loginJsonString);

            String requestJson = encode(loginJsonString);

            String loginUrl = ServerConfig.getApiPath() + "login";
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
                    System.out.println("[DEBUG] Token auth " + token);

                    if ("ok".equalsIgnoreCase(status)) {
                        System.out.println("✅ Logowanie powiodło się.");
                        return true;
                    } else {
                        System.err.println("❌ Logowanie nie powiodło się: " + loginResponse.get("message").getAsString());
                        return false;
                    }
                }
            }else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                System.out.println("[DEBUG] Unauthorized login.");
                System.out.println("[DEBUG] Próba uzyskania nowego tokenu" );
                new KeyExchangeService().perform();
                System.out.println("Spróbuj ponownie");
                return false;
            }
            else {
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
            return false;
        }
    }
}
