package com.casino.java_online_casino.Connection.Server.Rooms;

import com.casino.java_online_casino.games.poker.controller.PokerTCPClient;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PokerRoomManager {
    private static PokerRoomManager instance;
    private final Map<String, PokerRoom> rooms;

    private PokerRoomManager() {
        this.rooms = new ConcurrentHashMap<>();
    }

    public static synchronized PokerRoomManager getInstance() {
        if (instance == null) {
            instance = new PokerRoomManager();
        }
        return instance;
    }

    public PokerRoom createRoom(PokerTCPClient tcpClient) {
        PokerRoom room = new PokerRoom(tcpClient);
        rooms.put(room.getRoomId(), room);
        return room;
    }

    public Optional<PokerRoom> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public void removeRoom(String roomId) {
        PokerRoom room = rooms.remove(roomId);
        if (room != null) {
            room.closeRoom();
        }
    }

    public void cleanInactiveRooms() {
        rooms.entrySet().removeIf(entry -> !entry.getValue().isActive());
    }

    public Map<String, PokerRoom> getActiveRooms() {
        return rooms;
    }
}