package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.User.Gamer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RegisterService extends Service {
    Gamer gamer;
    public RegisterService(Gamer gamer) {
        super();
        this.gamer = gamer;
    }
    @Override
    public boolean perform() {
        try {
            isStillWorking = true;
            if (keyManager.getAesKey() == null) {
                System.err.println("❌ Brak ustalonego klucza AES. Najpierw wykonaj wymianę kluczy.");
                throw new Exception();
            }
            String registerJsonString = toJson().toString();
            System.out.println("[DEBUG] Dane rejestracji JSON: " + registerJsonString);

            String encryptedRegisterBase64 = keyManager.encryptAes(registerJsonString);
            String requestJson = encode(registerJsonString);
            URL url = new URL(ServerConfig.getApiPath() + "register");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", token); // nagłówek z JWT

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestJson.getBytes(StandardCharsets.UTF_8));
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
                        isStillWorking = false;
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
            System.err.println("❌ Wyjątek podczas rejestracji:");
            return false;
        }
    }


    @Override
    public JsonObject toJson() {
        JsonObject registerJson = new JsonObject();
        registerJson.addProperty("first_name", gamer.getName());
        registerJson.addProperty("last_name", gamer.getLastName());
        registerJson.addProperty("nickname", gamer.getNickName());
        registerJson.addProperty("email", gamer.getEmail());
        registerJson.addProperty("password", gamer.getPassword());
        registerJson.addProperty("birth_date", gamer.getDateOfBirth().toString());
        registerJson.addProperty("credits", gamer.getDateOfBirth().toString());
        return registerJson;
    }
}
