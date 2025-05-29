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
import java.nio.charset.StandardCharsets;

public class RegisterService extends Service {
    private final Gamer gamer;

    public RegisterService(Gamer gamer) {
        super();
        this.gamer = gamer;
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
        registerJson.addProperty("credits", gamer.getCredits());
        return registerJson;
    }

    @Override
    public boolean perform() {
        try {
            isStillWorking = true;
            if (keyManager.getAesKey() == null) {
                System.err.println("❌ Brak ustalonego klucza AES. Najpierw wykonaj wymianę kluczy.");
                return false;
            }
            String registerJsonString = toJson().toString();
            System.out.println("[DEBUG] Dane rejestracji JSON: " + registerJsonString);

            String requestJson = encode(registerJsonString);
            String registerUrl = ServerConfig.getApiPath() + "register";

            // Używamy metody z klasy bazowej, która ustawia Content-Type
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
                return false;
            }

            JsonObject responseJson;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                responseJson = JsonUtil.parseJsonFromISReader(reader);
            }

            // Jeśli odpowiedź zawiera pole "data", odszyfruj
            if (responseJson.has("data")) {
                String encryptedResponseBase64 = responseJson.get("data").getAsString();
                String decryptedResponse = keyManager.decryptAes(encryptedResponseBase64);
                System.out.println("[DEBUG] Odszyfrowana odpowiedź rejestracji: " + decryptedResponse);
                JsonObject decryptedJson = JsonParser.parseString(decryptedResponse).getAsJsonObject();
                return handleResponse(decryptedJson);
            } else {
                return handleResponse(responseJson);
            }

        } catch (Exception e) {
            System.err.println("❌ Wyjątek podczas rejestracji: " + e.getMessage());
            return false;
        } finally {
            isStillWorking = false;
        }
    }

    @Override
    public boolean handleResponse(JsonObject response) throws IOException {
        if (response == null || !response.has("code")) {
            System.err.println("Invalid response object: missing 'code'.");
            return false;
        }

        int code = response.get("code").getAsInt();

        switch (code) {
            case 200:
                ok200(response);
                return true;
            case 400:
                badRequest400(response);
                break;
            case 401:
                unauthorized401(response);
                break;
            case 403:
                denied403(response);
                break;
            case 404:
                denied404(response);
                break;
            case 405:
                notAllowed405(response);
                break;
            case 415:
                unsupported415(response);
                break;
            case 500:
                serverError500(response);
                break;
            case 503:
                databaseError503(response);
                break;
            default:
                unhandledCode(response);
                break;
        }
        return false;
    }

    @Override
    protected void ok200(JsonObject response) {
        String status = response.has("status") ? response.get("status").getAsString() : "";
        if ("ok".equalsIgnoreCase(status)) {
            System.out.println("✅ Rejestracja powiodła się.");
        } else {
            System.err.println("❌ Rejestracja nie powiodła się: " + response.get("message").getAsString());
        }
    }

    @Override
    protected void badRequest400(JsonObject response) {
        System.out.println("Bad request: " + response.get("message").getAsString());
    }

    @Override
    protected void unauthorized401(JsonObject response) throws IOException {
        System.out.println("[DEBUG] Unauthorized registration.");
        System.out.println("[DEBUG] Próba uzyskania nowego tokenu");
        new KeyExchangeService().perform();
        System.out.println("Spróbuj ponownie");
    }

    @Override
    protected void denied403(JsonObject response) {
        System.out.println("Access denied: " + response.get("message").getAsString());
    }

    @Override
    protected void denied404(JsonObject response) {
        System.out.println("Not found: " + response.get("message").getAsString());
    }

    @Override
    protected void notAllowed405(JsonObject response) {
        System.out.println("Method not allowed: " + response.get("message").getAsString());
    }

    @Override
    protected void unsupported415(JsonObject response) {
        System.out.println("Unsupported media type: " + response.get("message").getAsString());
    }

    @Override
    protected void serverError500(JsonObject response) {
        System.out.println("Internal server error: " + response.get("message").getAsString());
    }

    @Override
    protected void databaseError503(JsonObject response) {
        System.out.println("Database error: " + response.get("message").getAsString());
    }

    @Override
    protected void unhandledCode(JsonObject response) {
        int code = response.get("code").getAsInt();
        System.out.println("Unhandled response code: " + code + " - " + response.get("message").getAsString());
    }
}
