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
        System.out.println("[DEBUG POKER ROOM MANAGER] Utworzono nową instancję managera");
    }

    public static synchronized PokerRoomManager getInstance() {
        if (instance == null) {
            instance = new PokerRoomManager();
        }
        return instance;
    }

    public PokerRoom createRoom(PokerTCPClient tcpClient) {
        System.out.println("[DEBUG POKER ROOM MANAGER] Próba utworzenia nowego pokoju dla klienta: " + tcpClient);
        if (tcpClient == null) {
            throw new IllegalArgumentException("PokerTCPClient nie może być null");
        }
        PokerRoom room = new PokerRoom(tcpClient);
        rooms.put(room.getRoomId(), room);
        System.out.println("[DEBUG POKER ROOM MANAGER] Utworzono pokój o ID: " + room.getRoomId() + ". Aktywne pokoje: " + rooms.size());
        return room;
    }

    public Optional<PokerRoom> getRoom(String roomId) {
        System.out.println("[DEBUG POKER ROOM MANAGER] Szukam pokoju o ID: " + roomId);
        PokerRoom room = rooms.get(roomId);
        System.out.println("[DEBUG POKER ROOM MANAGER] " + (room != null ? "Znaleziono pokój" : "Nie znaleziono pokoju"));
        return Optional.ofNullable(room);
    }

    public void removeRoom(String roomId) {
        System.out.println("[DEBUG POKER ROOM MANAGER] Próba usunięcia pokoju o ID: " + roomId);
        PokerRoom room = rooms.remove(roomId);
        if (room != null) {
            room.closeRoom();
            System.out.println("[DEBUG POKER ROOM MANAGER] Usunięto pokój o ID: " + roomId);
        }
    }

    public void cleanInactiveRooms() {
        int beforeSize = rooms.size();
        System.out.println("[DEBUG POKER ROOM MANAGER] Rozpoczynam czyszczenie nieaktywnych pokoi. Przed: " + beforeSize);
        rooms.entrySet().removeIf(entry -> {
            boolean inactive = !entry.getValue().isActive();
            if (inactive) {
                System.out.println("[DEBUG POKER ROOM MANAGER] Znaleziono nieaktywny pokój: " + entry.getKey());
            }
            return inactive;
        });
        System.out.println("[DEBUG POKER ROOM MANAGER] Zakończono czyszczenie. Po: " + rooms.size());
    }

    public Map<String, PokerRoom> getActiveRooms() {
        System.out.println("[DEBUG POKER ROOM MANAGER] Liczba aktywnych pokoi: " + rooms.size());
        return rooms;
    }
    public Optional<PokerRoom> getAvailableRoom() {
        return rooms.values().stream()
                .filter(room -> room.getPlayerCount() < room.getMaxPlayers())
                .findFirst();
    }

}