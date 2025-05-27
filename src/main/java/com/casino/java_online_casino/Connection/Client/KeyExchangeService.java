package com.casino.java_online_casino.Connection.Client;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Utils.JsonUtil;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class KeyExchangeService extends Service {
    private final String keyExchangeUrl = ServerConfig.getApiPath()+"key";
    public KeyExchangeService() {
        super();
    }

    @Override
    public JsonObject toJson() {
        String clientPublicKeyBase64 = keyManager.exportEcPublicKey();
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("clientPublicKey", clientPublicKeyBase64);
        return requestJson;
    }

    /**
     * Wykonuje wymianę kluczy i pobiera token JWT.
     * @throws Exception w przypadku błędów wymiany
     */
    @Override
    public boolean perform() throws IOException {
        try{
            isStillWorking = true;

            JsonObject requestJson = toJson();
            // 2. Nawiąż połączenie i wyślij żądanie
            URL url = new URL(keyExchangeUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Błąd serwera podczas wymiany kluczy: " + responseCode);
            }

            try (InputStream is = connection.getInputStream();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                JsonObject responseJson = JsonUtil.parseJsonFromISReader(reader);
                String serverPublicKeyBase64 = responseJson.get("serverPublicKey").getAsString();
                this.token = responseJson.get("token").getAsString();
                System.out.println("[DEBUG]");

                // Import klucza serwera i wyliczenie wspólnego sekretu AES
                keyManager.importForeignKey(serverPublicKeyBase64);
                isStillWorking = false;
                return true;
            }
        }catch (IOException e){
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        new KeyExchangeService().perform();
    }
}
