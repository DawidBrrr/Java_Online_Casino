package com.casino.java_online_casino.Connection.Server.Rooms;

import com.casino.java_online_casino.games.poker.model.Player;

import java.util.*;
import java.util.concurrent.locks.*;

public class RoomManager {
    public static final int DEFAULT_MAX_PLAYERS = 4 ;
    private static final RoomManager INSTANCE = new RoomManager();
    private final Map<String, Room> rooms = new HashMap<>();
    private final Lock lock = new ReentrantLock();

    private RoomManager() {}

    public static RoomManager getInstance() {
        return INSTANCE;
    }

    public Room findOrCreateRoom(Player player, int maxPlayers) {
        lock.lock();
        try {
            for (Room room : rooms.values()) {
                if (room.isActive() && room.getPlayers().size() < maxPlayers) {
                    if (room.addPlayer(player)) {
                        return room;
                    }
                }
            }
            Room newRoom = createRoom(maxPlayers);
            newRoom.addPlayer(player);
            return newRoom;
        } finally {
            lock.unlock();
        }
    }

    public Room createRoom(int maxPlayers) {
        lock.lock();
        try {
            String roomId = UUID.randomUUID().toString();
            Room newRoom = new Room(roomId, maxPlayers);
            rooms.put(roomId, newRoom);
            return newRoom;
        } finally {
            lock.unlock();
        }
    }

    public void addRoom(Room room) {
        lock.lock();
        try {
            rooms.put(room.getRoomId(), room);
        } finally {
            lock.unlock();
        }
    }

    public void removeRoom(String roomId) {
        lock.lock();
        try {
            rooms.remove(roomId);
        } finally {
            lock.unlock();
        }
    }

    public Collection<Room> getAllRooms() {
        lock.lock();
        try {
            return new ArrayList<>(rooms.values());
        } finally {
            lock.unlock();
        }
    }

    public Optional<Room> getAnyRoomOptional() {
        lock.lock();
        try {
            return rooms.values().stream().findFirst();
        } finally {
            lock.unlock();
        }
    }

    public Optional<Collection<Room>> getAllRoomsOptional() {
        Collection<Room> allRooms = rooms.values();
        return allRooms.isEmpty() ? Optional.empty() : Optional.of(new ArrayList<>(allRooms));
    }
    public Optional<Room> firstFreeRoom(int maxPlayers) {
        lock.lock();
        try {
            return rooms.values().stream()
                    .filter(room -> room.isActive() && room.getPlayers().size() < maxPlayers)
                    .findFirst();
        } finally {
            lock.unlock();
        }
    }
}
