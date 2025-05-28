package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Serializable;

abstract public class Service implements Runnable , ServiceHelper{
    public boolean isStillWorking;
    public static  String token;
    public static  KeyManager keyManager = new KeyManager();

    public static KeyManager getKeyManager() {
        return keyManager;
    }

    public static  String getToken() {
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

    public String encode(String jsonToEncode) throws IOException{
        String encodedJson = keyManager.encryptAes(jsonToEncode);
        JsonObject json = new JsonObject();
        json.addProperty("data", encodedJson);
        return json.toString();
    }

    public static void handleResponse(JsonObject response) throws IOException {
        if (response == null || !response.has("code")) {
            System.err.println("Invalid response object: missing 'code'.");
            return;
        }

        int code = response.get("code").getAsInt();

        switch (code) {
            case 200:
                // OK - Sukces
                System.out.println("Success: " + response.get("message").getAsString());
                break;

            case 400:
                // Błąd zapytania
                System.out.println("Bad request: " + response.get("message").getAsString());
                break;

            case 401:
                new KeyExchangeService().perform();
                System.out.println("Unauthorized: " + response.get("message").getAsString());
                break;

            case 403:
                System.out.println("Access denied: " + response.get("message").getAsString());
                break;

            case 404:
                System.out.println("Not found: " + response.get("message").getAsString());
                break;

            case 405:
                System.out.println("Method not allowed: " + response.get("message").getAsString());
                break;

            case 415:
                System.out.println("Unsupported media type: " + response.get("message").getAsString());
                break;

            case 500:
                System.out.println("Internal server error: " + response.get("message").getAsString());
                break;

            case 503:
                System.out.println("Database error: " + response.get("message").getAsString());
                break;

            default:
                System.out.println("Unhandled response code: " + code + " - " + response.get("message").getAsString());
                break;
        }
    }

}
