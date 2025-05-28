package com.casino.java_online_casino.Connection.Server;

import com.casino.java_online_casino.Connection.Client.KeyExchangeService;
import com.casino.java_online_casino.Connection.Client.LoginService;
import com.casino.java_online_casino.Connection.Client.Service;
import com.casino.java_online_casino.Connection.Server.API.ApiServer;
import com.casino.java_online_casino.Connection.Server.GameServer.GameServer;
import com.casino.java_online_casino.Main;
import com.casino.java_online_casino.games.blackjack.controller.BlackjackTcpClient;
import com.casino.java_online_casino.games.blackjack.controller.RemoteBlackJackController;

import java.io.IOException;

public class Test {
    public static void main(String[] args) {
        //Api i serwre używaja wspołnych kluczy - muszą być uruchamiane w jednej instancji razem
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

        // Klient: wymiana kluczy + rozgrywka -- można zostawić bez zmian
        Service keyService = new KeyExchangeService();
        Thread keyExchangeThread = new Thread(keyService, "KeyExchange-Thread");
        keyExchangeThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        LoginService login = new LoginService("java@j", "admin");
        Thread loginThread = new Thread(login);
        loginThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(Service.getToken());
    }
}
