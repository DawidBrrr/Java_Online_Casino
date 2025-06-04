package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mysql.cj.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

public class LoginService extends Service {
    private final String username;
    private final String password;
    private boolean loginResult = false; // pole do przechowywania wyniku logowania

    public LoginService(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public JsonObject toJson() {
        JsonObject loginJson = new JsonObject();
        loginJson.addProperty(JsonFields.EMAIL, username);
        loginJson.addProperty(JsonFields.PASSWORD, password);
        return loginJson;
    }

    @Override
    public boolean perform() {
        if (keyManager.getAesKey() == null) {
            System.err.println("❌ Brak ustalonego klucza AES. Najpierw wykonaj wymianę kluczy.");
            LogManager.logToFile("❌ Brak ustalonego klucza AES. Najpierw wykonaj wymianę kluczy.");
            loginResult = false;
            return false;
        }

        try {
            String loginJsonString = toJson().toString();
            System.out.println("[DEBUG] Dane logowania JSON: " + loginJsonString);
            LogManager.logToFile("[DEBUG] Dane logowania JSON: " + loginJsonString);

            String requestJson = encode(loginJsonString);
            String loginUrl = ServerConfig.getApiPath() + ServiceHelper.LOGIN;

            HttpURLConnection connection = getConnection(loginUrl, ServiceHelper.METHOD_POST);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestJson.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("[DEBUG] Kod odpowiedzi logowania: " + responseCode);
            LogManager.logToFile("[DEBUG] Kod odpowiedzi logowania: " + responseCode);

            InputStream is;
            if (responseCode >= 200 && responseCode < 300) {
                is = connection.getInputStream();
            } else {
                is = connection.getErrorStream();
            }

            if (is == null) {
                System.err.println("Brak strumienia odpowiedzi.");
                LogManager.logToFile("Brak strumienia odpowiedzi.");
                loginResult = false;
                return false;
            }

            JsonObject responseJson;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                responseJson = JsonUtil.parseJsonFromISReader(reader);
            }

            if (responseJson.has(JsonFields.DATA)) {
                String encryptedResponseBase64 = responseJson.get(JsonFields.DATA).getAsString();
                String decryptedResponse = keyManager.decryptAes(encryptedResponseBase64);
                System.out.println("[DEBUG] Odszyfrowana odpowiedź logowania: " + decryptedResponse);
                LogManager.logToFile("[DEBUG] Odszyfrowana odpowiedź logowania: " + decryptedResponse);
                JsonObject decryptedJson = JsonParser.parseString(decryptedResponse).getAsJsonObject();
                loginResult = handleResponse(decryptedJson);
                new UserDataService().perform();
                return loginResult;
            } else {
                loginResult = handleResponse(responseJson);
                return loginResult;
            }

        } catch (Exception e) {
            System.err.println("❌ Wyjątek podczas logowania: " + e.getMessage());
            LogManager.logToFile("❌ Wyjątek podczas logowania: " + e.getMessage());
            loginResult = false;
            return false;
        }
    }

    @Override
    public boolean handleResponse(JsonObject response) throws IOException {
        if (response == null || !response.has(JsonFields.HTTP_CODE)) {
            System.err.println("Invalid response object: missing '" + JsonFields.HTTP_CODE + "'.");
            LogManager.logToFile("Invalid response object: missing '" + JsonFields.HTTP_CODE + "'.");
            return false;
        }

        int code = response.get(JsonFields.HTTP_CODE).getAsInt();

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
        String status = response.has(JsonFields.HTTP_STATUS) ? response.get(JsonFields.HTTP_STATUS).getAsString() : "";
        String token = response.has(JsonFields.TOKEN) ? response.get(JsonFields.TOKEN).getAsString() : null;

        if (token != null) {
            Service.token = token;
            System.out.println("[DEBUG] Token auth " + token);
            LogManager.logToFile("[DEBUG] Token auth " + token);
        }
    }

    @Override
    protected void badRequest400(JsonObject response) {
        System.out.println("Bad request: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
        LogManager.logToFile("Bad request: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void unauthorized401(JsonObject response) throws IOException {
        System.out.println("[DEBUG] Unauthorized login.");
        System.out.println("[DEBUG] Próba uzyskania nowego tokenu");
        LogManager.logToFile("[DEBUG] Unauthorized login.");
        LogManager.logToFile("[DEBUG] Próba uzyskania nowego tokenu");
        new KeyExchangeService().perform();
        System.out.println("Spróbuj ponownie");
    }

    @Override
    protected void denied403(JsonObject response) {
        System.out.println("Access denied: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
        LogManager.logToFile("Access denied: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void denied404(JsonObject response) {
        System.out.println("Not found: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
        LogManager.logToFile("Not found: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void notAllowed405(JsonObject response) {
        System.out.println("Method not allowed: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
        LogManager.logToFile("Method not allowed: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void unsupported415(JsonObject response) {
        System.out.println("Unsupported media type: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
        LogManager.logToFile("Unsupported media type: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void serverError500(JsonObject response) {
        System.out.println("Internal server error: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
        LogManager.logToFile("Internal server error: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void databaseError503(JsonObject response) {
        System.out.println("Database error: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
        LogManager.logToFile("Database error: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void unhandledCode(JsonObject response) {
        int code = response.get(JsonFields.HTTP_CODE).getAsInt();
        System.out.println("Unhandled response code: " + code + " - " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
        LogManager.logToFile("Unhandled response code: " + code + " - " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    public boolean getLoginResult() {
        return loginResult;
    }
}
