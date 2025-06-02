package com.casino.java_online_casino.Connection.Server.Rooms;

import com.casino.java_online_casino.games.poker.controller.RemotePokerController;
import com.casino.java_online_casino.games.poker.controller.PokerTCPClient;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PokerRoom {
    private String roomId;
    private final RemotePokerController controller;
    private final PokerTCPClient tcpClient;
    private final Set<String> players;
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

    public boolean addPlayer(String playerId) {
        System.out.println("[DEBUG POKER_ROOM] Próba dodania gracza: " + playerId + " do pokoju: " + roomId);
        if (players.size() < maxPlayers) {
            boolean added = players.add(playerId);
            System.out.println("[DEBUG POKER_ROOM] " + (added ? "Dodano gracza" : "Gracz już istnieje") +
                    ". Liczba graczy: " + players.size());
            return added;
        }
        System.out.println("[DEBUG POKER_ROOM] Nie można dodać gracza - pokój pełny");
        return false;
    }

    public boolean removePlayer(String playerId) {
        System.out.println("[DEBUG POKER_ROOM] Próba usunięcia gracza: " + playerId + " z pokoju: " + roomId);
        boolean removed = players.remove(playerId);
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