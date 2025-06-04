package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LogoutService extends Service {

    @Override
    public JsonObject toJson() {
        // Szyfrujemy komendę logout i umieszczamy w polu "data"
        String encryptedLogout = keyManager.encryptAes(JsonFields.LOGOUT);
        JsonObject json = new JsonObject();
        json.addProperty(JsonFields.DATA, encryptedLogout);
        return json;
    }

    @Override
    public boolean perform() throws IOException {
        String logoutUrl = ServerConfig.getApiPath() + JsonFields.LOGOUT;

        // Przygotowanie połączenia HTTP
        URL url = new URL(logoutUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Wysyłka żądania
        String requestBody = toJson().toString();
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Odczyt odpowiedzi
        int responseCode = conn.getResponseCode();
        InputStream is = responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();
        String responseJson;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
            responseJson = sb.toString();
        }

        // Deszyfrowanie odpowiedzi
        JsonObject responseObj = com.google.gson.JsonParser.parseString(responseJson).getAsJsonObject();
        if (!responseObj.has(JsonFields.DATA)) {
            System.out.println("Brak pola 'data' w odpowiedzi: " + responseJson);
            return false;
        }
        String encryptedResponse = responseObj.get(JsonFields.DATA).getAsString();
        String decryptedResponse = keyManager.decryptAes(encryptedResponse);

        // Analiza statusu odpowiedzi
        System.out.println("Odszyfrowana odpowiedź serwera: " + decryptedResponse);
        JsonObject resp = com.google.gson.JsonParser.parseString(decryptedResponse).getAsJsonObject();
        return "ok".equalsIgnoreCase(resp.get("status").getAsString());
    }
}
