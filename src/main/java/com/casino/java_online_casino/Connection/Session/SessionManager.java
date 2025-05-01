package com.casino.java_online_casino.Connection.Session;

import com.casino.java_online_casino.Experimental;

import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;
import java.util.UUID;
@Experimental

public class SessionManager {
    private static volatile SessionManager instance;
    private final Map<String, String> sessionMap = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    // Generuje nową sesję i przypisuje użytkownika
    public synchronized String createSession(String userId) {
        String sessionKey = UUID.randomUUID().toString();
        sessionMap.put(userId, sessionKey);
        return sessionKey;
    }

    // Zwraca istniejącą sesję (lub null jeśli nie istnieje)
    public synchronized String reconnectSession(String userId) {
        return sessionMap.get(userId);
    }

    // Sprawdza czy użytkownik ma już aktywną sesję
    public boolean hasSession(String userId) {
        return sessionMap.containsKey(userId);
    }

    // (opcjonalnie) Usuwa sesję
    public synchronized void removeSession(String userId) {
        sessionMap.remove(userId);
    }
}
