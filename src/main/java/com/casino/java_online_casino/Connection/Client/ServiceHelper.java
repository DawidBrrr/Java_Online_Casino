package com.casino.java_online_casino.Connection.Client;

import com.google.gson.JsonObject;

import java.io.IOException;

public interface ServiceHelper {
    String LOGIN = "login";
    String REGISTER = "register";
    String LOGOUT = "logout";
    String USER_DATA = "user";
    String KEY = "key";
    String METHOD_GET = "GET";
    String METHOD_POST = "POST";
    String METHOD_PUT = "PUT";
    String METHOD_DELETE = "DELETE";
    String OPERATION = "operation";
    String DEPOSIT = "deposit";
    String WITHDRAW = "withdraw";
    public JsonObject toJson();
    public boolean perform() throws IOException;
}
