package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.User.Gamer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class RegisterService extends Service {
    private final Gamer gamer;
    private boolean registerResult = false;

    public RegisterService(Gamer gamer) {
        super();
        this.gamer = gamer;
    }

    @Override
    public JsonObject toJson() {
        JsonObject registerJson = new JsonObject();
        registerJson.addProperty(JsonFields.FIRST_NAME, gamer.getName());
        registerJson.addProperty(JsonFields.LAST_NAME, gamer.getLastName());
        registerJson.addProperty(JsonFields.NICKNAME, gamer.getNickName());
        registerJson.addProperty(JsonFields.EMAIL, gamer.getEmail());
        registerJson.addProperty(JsonFields.PASSWORD, gamer.getPassword());
        registerJson.addProperty(JsonFields.BIRTH_DATE, gamer.getDateOfBirth().getTime());
        registerJson.addProperty(JsonFields.CREDITS, gamer.getCredits());
        return registerJson;
    }

    @Override
    public boolean perform() {
        try {
            isStillWorking = true;
            if (keyManager.getAesKey() == null) {
                System.err.println("❌ Brak ustalonego klucza AES. Najpierw wykonaj wymianę kluczy.");
                registerResult = false;
                return false;
            }
            String registerJsonString = toJson().toString();
            System.out.println("[DEBUG] Dane rejestracji JSON: " + registerJsonString);

            String requestJson = encode(registerJsonString);
            String registerUrl = ServerConfig.getApiPath() + "register";

            var connection = getConnection(registerUrl, ServiceHelper.METHOD_POST);
            connection.setRequestProperty("Authorization", token);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestJson.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("[DEBUG] Kod odpowiedzi rejestracji: " + responseCode);

            InputStream is = (responseCode >= 200 && responseCode < 300) ? connection.getInputStream() : connection.getErrorStream();
            if (is == null) {
                System.err.println("Brak strumienia odpowiedzi.");
                registerResult = false;
                return false;
            }

            JsonObject responseJson;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                responseJson = JsonUtil.parseJsonFromISReader(reader);
            }

            if (responseJson.has(JsonFields.DATA)) {
                String encryptedResponseBase64 = responseJson.get(JsonFields.DATA).getAsString();
                String decryptedResponse = keyManager.decryptAes(encryptedResponseBase64);
                System.out.println("[DEBUG] Odszyfrowana odpowiedź rejestracji: " + decryptedResponse);
                JsonObject decryptedJson = JsonParser.parseString(decryptedResponse).getAsJsonObject();
                registerResult = handleResponse(decryptedJson);
                return registerResult;
            } else {
                registerResult = handleResponse(responseJson);
                return registerResult;
            }

        } catch (Exception e) {
            System.err.println("❌ Wyjątek podczas rejestracji: " + e.getMessage());
            registerResult = false;
            return false;
        } finally {
            isStillWorking = false;
        }
    }

    @Override
    public boolean handleResponse(JsonObject response) throws IOException {
        if (response == null || !response.has(JsonFields.HTTP_STATUS)) {
            System.err.println("Invalid response object: missing status.");
            return false;
        }

        String status = response.get(JsonFields.HTTP_STATUS).getAsString();
        if ("ok".equalsIgnoreCase(status)) {
            ok200(response);
            return true;
        } else {
            System.err.println("❌ Rejestracja nie powiodła się: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
            return false;
        }
    }

    @Override
    protected void ok200(JsonObject response) {
        System.out.println("✅ Rejestracja powiodła się: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    // Pozostałe metody obsługi błędów możesz zostawić jak w Service (np. badRequest400, unauthorized401 itd.)

    public boolean getRegisterResult() {
        return registerResult;
    }
}
