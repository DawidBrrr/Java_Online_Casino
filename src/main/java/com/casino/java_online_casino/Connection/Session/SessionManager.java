package com.casino.java_online_casino.Connection.Session;

import com.casino.java_online_casino.Experimental;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Experimental
public class SessionManager {
    private static volatile SessionManager instance;
    private final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

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

    // Tworzy nową sesję i przechowuje dowolny obiekt powiązany z użytkownikiem (np. BlackJackController)
    public String createSession(String userId, Object sessionData) {
        String sessionKey = UUID.randomUUID().toString();
        Session session = new Session(sessionKey, userId, sessionData, Instant.now());
        sessionMap.put(userId, session);
        return sessionKey;
    }

    // Pobiera sesję użytkownika
    public Session getSession(String userId) {
        return sessionMap.get(userId);
    }

    // Sprawdza, czy użytkownik ma aktywną sesję
    public boolean hasSession(String userId) {
        return sessionMap.containsKey(userId);
    }

    // Aktualizuje dane sesji (np. nowy stan gry)
    public void updateSessionData(String userId, Object sessionData) {
        Session session = sessionMap.get(userId);
        if (session != null) {
            session.setSessionData(sessionData);
            session.setLastAccess(Instant.now());
        }
    }

    // Usuwa sesję użytkownika
    public void removeSession(String userId) {
        sessionMap.remove(userId);
    }

    // (Opcjonalnie) Usuwa nieaktywne sesje po określonym czasie
    public void cleanupExpiredSessions(long maxInactiveSeconds) {
        Instant now = Instant.now();
        sessionMap.values().removeIf(session ->
                now.minusSeconds(maxInactiveSeconds).isAfter(session.getLastAccess()));
    }

    // Klasa wewnętrzna reprezentująca sesję
    public static class Session {
        private final String sessionKey;
        private final String userId;
        private Object sessionData; // np. BlackJackController
        private Instant lastAccess;

        public Session(String sessionKey, String userId, Object sessionData, Instant lastAccess) {
            this.sessionKey = sessionKey;
            this.userId = userId;
            this.sessionData = sessionData;
            this.lastAccess = lastAccess;
        }

        public String getSessionKey() { return sessionKey; }
        public String getUserId() { return userId; }
        public Object getSessionData() { return sessionData; }
        public void setSessionData(Object sessionData) { this.sessionData = sessionData; }
        public Instant getLastAccess() { return lastAccess; }
        public void setLastAccess(Instant lastAccess) { this.lastAccess = lastAccess; }
    }
}
