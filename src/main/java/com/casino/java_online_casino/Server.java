package com.casino.java_online_casino;

import com.casino.java_online_casino.Connection.Server.API.ApiServer;
import com.casino.java_online_casino.Connection.Server.GameServer.GameServer;
import com.casino.java_online_casino.Connection.Utils.LogManager;

public class Server {
    public static void main(String[] args) {
        ApiServer apiServer = new ApiServer();
        GameServer gameServer = new GameServer();

        // Uruchom ApiServer w osobnym wątku
        Thread apiThread = new Thread(() -> {
            try {
                apiServer.start();
            } catch (Exception e) {
                System.out.println("[ERROR] ApiServer: " + e.getMessage());
                e.printStackTrace();
            }
        }, "ApiServer-Thread");
        apiThread.start();

        // Uruchom GameServer w osobnym wątku
        Thread gameThread = new Thread(() -> {
            try {
                gameServer.start();
            } catch (Exception e) {
                System.out.println("[ERROR] GameServer: " + e.getMessage());
                e.printStackTrace();
            }
        }, "GameServer-Thread");
        gameThread.start();

        // Jeśli chcesz poczekać aż serwery się rozkręcą
        try {
            Thread.sleep(1000); // wystarczy na "rozgrzanie" serwerów
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
