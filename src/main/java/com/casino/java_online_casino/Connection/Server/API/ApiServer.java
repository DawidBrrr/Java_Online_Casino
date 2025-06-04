package com.casino.java_online_casino.Connection.Server.API;

import com.casino.java_online_casino.Connection.Server.API.Handlers.*;
import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.Experimental;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

@Experimental
public class ApiServer {
    static boolean isActive = false;

    public static void start() {
        if (isActive) {
            System.out.println("Server jest już aktywny");
        } else {
            init();
        }
    }

    private static void init() {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(ServerConfig.getApiServerHost(), ServerConfig.getApiServerPort()), 0);
            // Endpointy
            server.createContext("/login", new LoginHandler());
            server.createContext("/register", new RegisterHandler());
            server.createContext("/logout", new LogoutHandler());
            server.createContext("/user", new UserHandler());
            server.createContext("/stat", new StatHandler());
            server.createContext("/key", new KeyHandler());
            server.createContext("/api/stats/ranking", new StatHandler());

            // Dynamiczne zarządzanie wątkami (bez limitu)
            server.setExecutor(Executors.newCachedThreadPool());

            server.start();
            isActive = true;
            System.out.println("API działa na " + ServerConfig.getApiServerHost() + ":" + ServerConfig.getApiServerHost());
        } catch (IOException e) {
            isActive = false;
            throw new RuntimeException("Błąd startu servera");
        }
    }

    public static void main(String[] args) {
        start();
    }

}
