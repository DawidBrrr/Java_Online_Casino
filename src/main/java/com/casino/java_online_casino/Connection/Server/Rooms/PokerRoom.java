package com.casino.java_online_casino.Connection.Server.Rooms;

import com.casino.java_online_casino.games.poker.controller.RemotePokerController;
import com.casino.java_online_casino.games.poker.controller.PokerTCPClient;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PokerRoom {
    private final String roomId;
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
        if (players.size() < maxPlayers) {
            return players.add(playerId);
        }
        return false;
    }

    public boolean removePlayer(String playerId) {
        return players.remove(playerId);
    }

    public void closeRoom() {
        isActive = false;
        tcpClient.close();
    }

    public boolean isActive() {
        return isActive;
    }

    public int getPlayerCount() {
        return players.size();
    }
}