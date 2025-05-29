package com.casino.java_online_casino.Connection.Client;

import com.google.gson.JsonObject;

import java.io.IOException;

public interface ServiceHelper {
    String LOGIN = "login";
    String REGISTER = "register";
    String LOGOUT = "logout";
    String KEY = "key";
    String METHOD_GET = "GET";
    String METHOD_POST = "POST";
    String METHOD_PUT = "PUT";
    String METHOD_DELETE = "DELETE";
    public JsonObject toJson();
    public boolean perform() throws IOException;
}
