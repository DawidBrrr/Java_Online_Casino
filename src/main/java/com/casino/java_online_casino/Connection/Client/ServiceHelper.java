package com.casino.java_online_casino.Connection.Client;

import com.google.gson.JsonObject;

import java.io.IOException;

public interface ServiceHelper {
    public JsonObject toJson();
    public boolean perform() throws IOException;
}
