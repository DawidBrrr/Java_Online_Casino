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
}
