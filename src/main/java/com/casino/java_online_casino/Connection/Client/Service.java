package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

abstract public class Service implements Runnable, ServiceHelper {
    public boolean isStillWorking;
    public static String token;
    public static KeyManager keyManager = new KeyManager();

    public static KeyManager getKeyManager() {
        return keyManager;
    }

    public static String getToken() {
        return token;
    }

    @Override
    public void run() {
        try {
            perform();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String encode(String jsonToEncode) throws IOException {
        String encodedJson = keyManager.encryptAes(jsonToEncode);
        JsonObject json = new JsonObject();
        json.addProperty(JsonFields.DATA, encodedJson);
        return json.toString();
    }

    public boolean handleResponse(JsonObject response) throws IOException {
        if (response == null || !response.has(JsonFields.HTTP_CODE)) {
            System.err.println("Invalid response object: missing '" + JsonFields.HTTP_CODE + "'.");
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

    // === METODY DO NADPISANIA LUB DOMYŚLNE ZACHOWANIE ===

    protected void ok200(JsonObject response) {
        System.out.println("Success: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    protected void badRequest400(JsonObject response) {
        System.out.println("Bad request: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    protected void unauthorized401(JsonObject response) throws IOException {
        new KeyExchangeService().perform();
        System.out.println("Unauthorized: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    protected void denied403(JsonObject response) {
        System.out.println("Access denied: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    protected void denied404(JsonObject response) {
        System.out.println("Not found: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    protected void notAllowed405(JsonObject response) {
        System.out.println("Method not allowed: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    protected void unsupported415(JsonObject response) {
        System.out.println("Unsupported media type: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    protected void serverError500(JsonObject response) {
        System.out.println("Internal server error: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    protected void databaseError503(JsonObject response) {
        System.out.println("Database error: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    protected void unhandledCode(JsonObject response) {
        int code = response.get(JsonFields.HTTP_CODE).getAsInt();
        System.out.println("Unhandled response code: " + code + " - " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    protected HttpURLConnection getConnection(String url, String method) throws IOException {
        URL endpoint = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();

        connection.setRequestMethod(method);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", token);
        }

        return connection;
    }
}
