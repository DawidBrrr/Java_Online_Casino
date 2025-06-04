package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.google.gson.JsonObject;
import com.mysql.cj.log.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LogoutService extends Service {

    @Override
    public JsonObject toJson() {
        String encryptedLogout = keyManager.encryptAes(JsonFields.LOGOUT);
        JsonObject json = new JsonObject();
        json.addProperty(JsonFields.DATA, encryptedLogout);
        System.out.println("[DEBUG LOGOUT] Wygenerowano zaszyfrowany JSON logout: " + json);
        LogManager.logToFile("[DEBUG LOGOUT] Wygenerowano zaszyfrowany JSON logout: " + json);
        return json;
    }

    @Override
    public boolean perform() throws IOException {
        String logoutUrl = ServerConfig.getApiPath() + JsonFields.LOGOUT;
        System.out.println("[DEBUG LOGOUT] Rozpoczynam wylogowanie pod URL: " + logoutUrl);
        LogManager.logToFile("[DEBUG LOGOUT] Rozpoczynam wylogowanie pod URL: " + logoutUrl);

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
            System.out.println("[DEBUG LOGOUT] Wysłano żądanie logout: " + requestBody);
            LogManager.logToFile("[DEBUG LOGOUT] Wysłano żądanie logout: " + requestBody);
        } catch (Exception e) {
            System.err.println("[DEBUG LOGOUT] Błąd podczas wysyłania żądania: " + e.getMessage());
            LogManager.logToFile("[DEBUG LOGOUT] Błąd podczas wysyłania żądania: " + e.getMessage());
            return false;
        }

        // Odczyt odpowiedzi
        int responseCode = conn.getResponseCode();
        System.out.println("[DEBUG LOGOUT] Kod odpowiedzi serwera: " + responseCode);
        LogManager.logToFile("[DEBUG LOGOUT] Kod odpowiedzi serwera: " + responseCode);

        InputStream is;
        try {
            is = responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) {
                System.err.println("[DEBUG LOGOUT] Brak strumienia odpowiedzi od serwera.");
                LogManager.logToFile("[DEBUG LOGOUT] Brak strumienia odpowiedzi od serwera.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[DEBUG LOGOUT] Błąd przy pobieraniu strumienia odpowiedzi: " + e.getMessage());
            LogManager.logToFile("[DEBUG LOGOUT] Błąd przy pobieraniu strumienia odpowiedzi: " + e.getMessage());
            return false;
        }

        String responseJson;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
            responseJson = sb.toString();
            System.out.println("[DEBUG LOGOUT] Otrzymano odpowiedź JSON: " + responseJson);
            LogManager.logToFile("[DEBUG LOGOUT] Otrzymano odpowiedź JSON: " + responseJson);
        } catch (Exception e) {
            System.err.println("[DEBUG LOGOUT] Błąd podczas odczytu odpowiedzi: " + e.getMessage());
            LogManager.logToFile("[DEBUG LOGOUT] Błąd podczas odczytu odpowiedzi: " + e.getMessage());
            return false;
        }

        // Deszyfrowanie odpowiedzi
        JsonObject responseObj;
        try {
            responseObj = com.google.gson.JsonParser.parseString(responseJson).getAsJsonObject();
        } catch (Exception e) {
            System.err.println("[DEBUG LOGOUT] Błąd parsowania odpowiedzi JSON: " + e.getMessage());
            LogManager.logToFile("[DEBUG LOGOUT] Błąd parsowania odpowiedzi JSON: " + e.getMessage());
            return false;
        }

        if (!responseObj.has(JsonFields.DATA)) {
            System.err.println("[DEBUG LOGOUT] Brak pola 'data' w odpowiedzi: " + responseJson);
            LogManager.logToFile("[DEBUG LOGOUT] Brak pola 'data' w odpowiedzi: " + responseJson);
            return false;
        }

        String encryptedResponse = responseObj.get(JsonFields.DATA).getAsString();
        String decryptedResponse;
        try {
            decryptedResponse = keyManager.decryptAes(encryptedResponse);
            System.out.println("[DEBUG LOGOUT] Odszyfrowana odpowiedź serwera: " + decryptedResponse);
            LogManager.logToFile("[DEBUG LOGOUT] Odszyfrowana odpowiedź serwera: " + decryptedResponse);
        } catch (Exception e) {
            System.err.println("[DEBUG LOGOUT] Błąd deszyfrowania odpowiedzi: " + e.getMessage());
            LogManager.logToFile("[DEBUG LOGOUT] Błąd deszyfrowania odpowiedzi: " + e.getMessage());
            return false;
        }

        // Analiza statusu odpowiedzi
        JsonObject resp;
        try {
            resp = com.google.gson.JsonParser.parseString(decryptedResponse).getAsJsonObject();
        } catch (Exception e) {
            System.err.println("[DEBUG LOGOUT] Błąd parsowania odszyfrowanej odpowiedzi: " + e.getMessage());
            LogManager.logToFile("[DEBUG LOGOUT] Błąd parsowania odszyfrowanej odpowiedzi: " + e.getMessage());
            return false;
        }

        boolean result = false;
        if (resp.has("status")) {
            String status = resp.get("status").getAsString();
            if ("ok".equalsIgnoreCase(status)) {
                System.out.println("[DEBUG LOGOUT] Wylogowanie zakończone sukcesem.");
                LogManager.logToFile("[DEBUG LOGOUT] Wylogowanie zakończone sukcesem.");
                result = true;
            } else {
                System.err.println("[DEBUG LOGOUT] Wylogowanie nie powiodło się. Status: " + status);
                LogManager.logToFile("[DEBUG LOGOUT] Wylogowanie nie powiodło się. Status: " + status);
            }
        } else {
            System.err.println("[DEBUG LOGOUT] Brak pola 'status' w odszyfrowanej odpowiedzi: " + decryptedResponse);
            LogManager.logToFile("[DEBUG LOGOUT] Brak pola 'status' w odszyfrowanej odpowiedzi: " + decryptedResponse);
        }
        return result;
    }
}
