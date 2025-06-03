package com.casino.java_online_casino.Connection.Server.Rooms;

import com.casino.java_online_casino.games.poker.controller.RemotePokerController;
import com.casino.java_online_casino.games.poker.controller.PokerTCPClient;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PokerRoom {
    private String roomId;
    private final RemotePokerController controller;
    private final PokerTCPClient tcpClient;
    private final Set<Integer> players;
    private final int maxPlayers;
    private boolean isActive;

    public PokerRoom(PokerTCPClient tcpClient) {
        this.roomId = UUID.randomUUID().toString();
        this.tcpClient = tcpClient;
        this.controller = new RemotePokerController(tcpClient);
        this.players = new HashSet<>();
        this.maxPlayers = 4;
        this.isActive = true;
    }

    public String getRoomId() {
        return roomId;
    }

    public RemotePokerController getController() {
        return controller;
    }

    public boolean addPlayer(int userId) {
        System.out.println("[DEBUG POKER_ROOM] Próba dodania gracza ID: " + userId + " do pokoju: " + roomId);
        if (players.size() < maxPlayers) {
            boolean added = players.add(userId);
            System.out.println("[DEBUG POKER_ROOM] " + (added ? "Dodano gracza" : "Gracz już istnieje") +
                    ". Liczba graczy: " + players.size());

            if (added) {
                // Powiadom innych graczy o dołączeniu nowego gracza
                JsonObject notification = new JsonObject();
                notification.addProperty("type", "player_joined");
                notification.addProperty("playerId", userId);
                notification.addProperty("playerCount", players.size());

                // Wyślij powiadomienie przez kontroler
                controller.broadcastMessage(notification.toString());

                // Jeśli jest odpowiednia liczba graczy, rozpocznij grę
                if (players.size() >= 2) {
                    controller.startGame();
                }
            }
            return added;
        }
        System.out.println("[DEBUG POKER_ROOM] Nie można dodać gracza - pokój pełny");
        return false;
    }

    public boolean removePlayer(int userId) {  // Zmiana parametru na int
        System.out.println("[DEBUG POKER_ROOM] Próba usunięcia gracza ID: " + userId + " z pokoju: " + roomId);
        boolean removed = players.remove(userId);
        System.out.println("[DEBUG POKER_ROOM] " + (removed ? "Usunięto gracza" : "Nie znaleziono gracza") +
                ". Pozostało graczy: " + players.size());
        return removed;
    }

    public void closeRoom() {
        System.out.println("[DEBUG POKER_ROOM] Zamykanie pokoju: " + roomId);
        isActive = false;
        tcpClient.close();
        System.out.println("[DEBUG POKER_ROOM] Pokój zamknięty");
    }

    public boolean isActive() {
        return isActive;
    }

    public int getPlayerCount() {
        System.out.println("[DEBUG POKER_ROOM] Liczba graczy w pokoju " + roomId + ": " + players.size());
        return players.size();
    }
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    public int getMaxPlayers() {
        return maxPlayers;
    }

}