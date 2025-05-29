package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

public class KeyExchangeService extends Service {
    private final String keyExchangeUrl = ServerConfig.getApiPath() + ServiceHelper.KEY;

    public KeyExchangeService() {
        super();
    }

    @Override
    public JsonObject toJson() {
        String clientPublicKeyBase64 = keyManager.exportEcPublicKey();
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty(JsonFields.CL_PUBLIC_KEY, clientPublicKeyBase64);
        return requestJson;
    }

    @Override
    public boolean perform() {
        try {
            isStillWorking = true;
            String requestJsonString = toJson().toString();
            System.out.println("[DEBUG] Key exchange JSON: " + requestJsonString);

            HttpURLConnection connection = getConnection(keyExchangeUrl, ServiceHelper.METHOD_POST);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestJsonString.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("[DEBUG] Key exchange response code: " + responseCode);

            InputStream is = (responseCode >= 200 && responseCode < 300) ? connection.getInputStream() : connection.getErrorStream();
            if (is == null) {
                System.err.println("Brak strumienia odpowiedzi.");
                return false;
            }

            JsonObject responseJson;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                responseJson = JsonUtil.parseJsonFromISReader(reader);
            }

            boolean result = handleResponse(responseJson);
            isStillWorking = false;
            return result;

        } catch (Exception e) {
            System.err.println("❌ Wyjątek podczas wymiany kluczy: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean handleResponse(JsonObject response) throws IOException {
        if (response == null || !response.has(JsonFields.HTTP_CODE)) {
            System.err.println("Invalid response object: missing 'code'.");
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
        String serverPublicKeyBase64 = response.has(JsonFields.SR_PUBLIC_KEY) ? response.get(JsonFields.SR_PUBLIC_KEY).getAsString() : null;
        String token = response.has(JsonFields.TOKEN) ? response.get(JsonFields.TOKEN).getAsString() : null;

        if (serverPublicKeyBase64 != null) {
            keyManager.importForeignKey(serverPublicKeyBase64);
        }
        if (token != null) {
            Service.token = token;
            System.out.println("[DEBUG] Token JWT: " + token);
        }
    }

    @Override
    protected void badRequest400(JsonObject response) {
        System.out.println("Bad request: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void unauthorized401(JsonObject response) throws IOException {
        System.out.println("[DEBUG] Unauthorized key exchange.");
        // Możesz tu dodać logikę ponowienia wymiany, jeśli to ma sens
    }

    @Override
    protected void denied403(JsonObject response) {
        System.out.println("Access denied: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void denied404(JsonObject response) {
        System.out.println("Not found: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void notAllowed405(JsonObject response) {
        System.out.println("Method not allowed: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void unsupported415(JsonObject response) {
        System.out.println("Unsupported media type: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void serverError500(JsonObject response) {
        System.out.println("Internal server error: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void databaseError503(JsonObject response) {
        System.out.println("Database error: " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }

    @Override
    protected void unhandledCode(JsonObject response) {
        int code = response.get(JsonFields.HTTP_CODE).getAsInt();
        System.out.println("Unhandled response code: " + code + " - " + response.get(JsonFields.HTTP_MESSAGE).getAsString());
    }
}
