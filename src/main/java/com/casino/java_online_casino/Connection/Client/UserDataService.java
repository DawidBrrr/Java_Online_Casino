package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Utils.JsonFields;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.casino.java_online_casino.Connection.Server.DTO.GamerDTO;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UserDataService extends Service {
    private static GamerDTO gamerDTO;
    private String pendingOperation = null;
    private GamerDTO getGamerDTO() {
        return gamerDTO;
    }

    // Pobranie danych użytkownika
    public static GamerDTO updateGamerDTO() {
        return performCreditOperation(null);
    }

    // Wpłata 500
    public static GamerDTO depositCredits() {
        return performCreditOperation(ServiceHelper.DEPOSIT);
    }

    // Wypłata 500
    public static GamerDTO withdrawCredits() {
        return performCreditOperation(ServiceHelper.WITHDRAW);
    }

    private static GamerDTO performCreditOperation(String operation) {
        FutureTask<GamerDTO> task = new FutureTask<>(new Callable<GamerDTO>() {
            @Override
            public GamerDTO call() {
                try {
                    UserDataService service = new UserDataService();
                    service.pendingOperation = operation;
                    service.run();
                    return gamerDTO;
                } catch (Exception e) {
                    System.err.println("[DEBUG] Błąd wątku FutureTask: " + e.getMessage());
                    LogManager.logToFile("[DEBUG] Błąd wątku FutureTask: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
        });
        Thread t = new Thread(task);
        t.start();
        try {
            return task.get(15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.err.println("[DEBUG] Timeout podczas oczekiwania na odpowiedź serwera.");
            LogManager.logToFile("[DEBUG] Timeout podczas oczekiwania na odpowiedź serwera.");
            t.interrupt();
            return null;
        } catch (InterruptedException e) {
            System.err.println("[DEBUG] Wątek został przerwany: " + e.getMessage());
            LogManager.logToFile("[DEBUG] Wątek został przerwany: " + e.getMessage());
            t.interrupt();
            return null;
        } catch (ExecutionException e) {
            System.err.println("[DEBUG] Błąd wykonania zadania: " + e.getMessage());
            LogManager.logToFile("[DEBUG] Błąd wykonania zadania: " + e.getMessage());
            t.interrupt();
            return null;
        }
    }

    @Override
    public JsonObject toJson() {
        if (pendingOperation != null) {
            JsonObject json = new JsonObject();
            json.addProperty(ServiceHelper.OPERATION, pendingOperation);
            return json;
        }
        return null; // Brak operacji – pusta treść
    }

    @Override
    public boolean perform() throws IOException {
        String userUrl = ServerConfig.getApiPath() + ServiceHelper.USER_DATA;
        HttpURLConnection connection;
        try {
            connection = getConnection(userUrl, ServiceHelper.METHOD_POST);
            System.out.println("[DEBUG] Nzwiązano połączenie");
            LogManager.logToFile("[DEBUG] Nawiązano połączenie");
        } catch (IOException e) {
            System.err.println("[DEBUG] Błąd podczas nawiązywania połączenia: " + e.getMessage());
            LogManager.logToFile("[DEBUG] Błąd podczas nawiązywania połączenia: " + e.getMessage());
            throw e;
        }

        JsonObject json = toJson();
        if (json != null) {
            // Wysyłamy zaszyfrowany payload JSON
            String requestJson;
            try {
                requestJson = encode(json.toString());
                System.out.println("[DEBUG] Zaszyfrowano dane JSON: " + requestJson);
                LogManager.logToFile("[DEBUG] Zaszyfrowano dane JSON" + requestJson);
            } catch (Exception e) {
                System.err.println("[DEBUG] Błąd podczas szyfrowania payloadu: " + e.getMessage());
                LogManager.logToFile("[DEBUG] Błąd podczas szyfrowania payloadu: " + e.getMessage());
                return false;
            }
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestJson.getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException e) {
                System.err.println("[DEBUG] Błąd podczas wysyłania danych do serwera: " + e.getMessage());
                LogManager.logToFile("[DEBUG] Błąd podczas wysyłania danych do serwera: " + e.getMessage());
                return false;
            }
        } else {
            // Brak body, tylko nagłówki
            connection.setDoOutput(false);
        }

        int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            System.err.println("[DEBUG] Błąd podczas pobierania kodu odpowiedzi serwera: " + e.getMessage());
            LogManager.logToFile("[DEBUG] Błąd podczas pobierania kodu odpowiedzi serwera: " + e.getMessage());
            return false;
        }

        InputStream is;
        try {
            is = (responseCode >= 200 && responseCode < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();
        } catch (IOException e) {
            System.err.println("[DEBUG] Błąd podczas pobierania strumienia odpowiedzi: " + e.getMessage());
            LogManager.logToFile("[DEBUG] Błąd podczas pobierania strumienia odpowiedzi: " + e.getMessage());
            return false;
        }

        if (is == null) {
            System.err.println("[DEBUG] Brak strumienia odpowiedzi.");
            LogManager.logToFile("[DEBUG] Brak strumienia odpowiedzi.");
            return false;
        }

        JsonObject responseJson;
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            responseJson = JsonUtil.parseJsonFromISReader(reader);
        } catch (Exception e) {
            System.err.println("[DEBUG] Błąd podczas parsowania odpowiedzi JSON: " + e.getMessage());
            LogManager.logToFile("[DEBUG] Błąd podczas parsowania odpowiedzi JSON: " + e.getMessage());
            return false;
        }

        if (responseJson.has(JsonFields.DATA)) {
            String encryptedData = responseJson.get(JsonFields.DATA).getAsString();
            String decryptedJson;
            try {
                decryptedJson = keyManager.decryptAes(encryptedData);
                System.out.println("[DEBUG] odszyfrowana wiadomośc " + decryptedJson);
                LogManager.logToFile("[DEBUG] odszyfrowana wiadomośc " + decryptedJson);
            } catch (Exception e) {
                System.err.println("[DEBUG] Błąd podczas odszyfrowywania danych: " + e.getMessage());
                LogManager.logToFile("[DEBUG] Błąd podczas odszyfrowywania danych: " + e.getMessage());
                return false;
            }
            try {
                gamerDTO = new Gson().fromJson(decryptedJson, GamerDTO.class);
            } catch (Exception e) {
                System.err.println("[DEBUG] Błąd podczas deserializacji GamerDTO: " + e.getMessage());
                LogManager.logToFile("[DEBUG] Błąd podczas deserializacji GamerDTO: " + e.getMessage());
                return false;
            }
            return true;
        } else {
            System.err.println("[DEBUG] Brak pola 'data' w odpowiedzi serwera.");
            LogManager.logToFile("[DEBUG] Brak pola 'data' w odpowiedzi serwera.");
            return false;
        }
    }
}
