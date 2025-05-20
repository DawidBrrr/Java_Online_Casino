package com.casino.java_online_casino.Connection.Server;

import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerData {
    private static final ServerData instance = new ServerData();

    private final ConcurrentHashMap<UUID, ServerTokenManger> sessions = new ConcurrentHashMap<>();
    private final KeyManager keyManager = new KeyManager(); // zakładam, że nie jest statyczny

    private ServerData() {}

    public static ServerData getInstance() {
        return instance;
    }

    public ServerTokenManger getKeyManager(UUID uuid) {
        return sessions.get(uuid);
    }
}
