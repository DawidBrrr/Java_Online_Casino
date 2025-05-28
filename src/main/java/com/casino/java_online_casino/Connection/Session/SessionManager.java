package com.casino.java_online_casino.Connection.Session;

import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Experimental;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Experimental
public class    SessionManager {
    private static volatile SessionManager instance;
    private final Map<UUID, SessionToken> sessionMap = new ConcurrentHashMap<>();

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
    public SessionToken createSession(String userId) {
        SessionToken sessionToken = new SessionToken(userId);
        UUID uuid = sessionToken.getUuid();
        sessionMap.put(uuid, sessionToken);
        return sessionToken;
    }
    public SessionToken createSession(){
        return createSession(null);
    }

    public SessionToken getUnregisteredSession() {
        return new SessionToken();
    }

    // Pobiera sesję użytkownika
    public SessionToken getSessionByUUID(UUID userId) {
        return sessionMap.get(userId);
    }
    public SessionToken getSessionByUserId(String userId) {
        return sessionMap.values().stream().filter(session -> session.getUserId().equals(userId)).findFirst().orElse(null);
    }

    // Sprawdza, czy użytkownik ma aktywną sesję
    public boolean hasSessionByUUID(UUID userId) {
        return sessionMap.containsKey(userId);
    }
    public boolean hasSessionByUserId(String userId) {
        return sessionMap.values().stream().anyMatch(session -> session.getUserId().equals(userId));
    }
    public void deleteSessionByUUID(UUID userId) {
        sessionMap.remove(userId);
    }
    public void deleteSessionByUserId(String userId) {
        sessionMap.forEach((uuid, session) -> {
            if (session.getUserId().equals(userId)) {
                sessionMap.remove(uuid);
                return;
            }
        });
    }

    // Aktualizuje dane sesji (np. nowy stan gry)
    public void updateSessionData(UUID userId, SessionToken session) {
        if (session != null) {
            session.updateLastAccess();
            sessionMap.put(userId, session);
        }
    }

    public void cleanupExpiredSessions() {
        sessionMap.values().removeIf(SessionToken::isTimeToExpire);
    }
    public class SessionToken {
        private final UUID uuid;
        private String userId;
        private final KeyManager keyManager;
        private LocalDateTime lastAccess;
        private Map<String,String> userData;
        private boolean duringTheGame;

        public SessionToken(){
            this(null);
        }

        public SessionToken(String userId) {
            this.uuid = UUID.randomUUID();
            this.keyManager = new KeyManager();
            this.userId = userId;
            this.lastAccess = LocalDateTime.now();
            this.userData = new HashMap<>();
            userData.put("UUID", uuid.toString());
            this.duringTheGame = false;
        }

        public String getNewToken(){
            return ServerTokenManager.createJwt(userData);
        }
        public boolean isExpiredToken(String token){
            try{
                ServerTokenManager.validateJwt(token);
                return false;
            }catch(Exception e) {
                return true;
            }
        }

        public boolean isTimeToExpire(){
            if(userId!=null){
                return Duration.between(lastAccess, LocalDateTime.now()).toMinutes() >= 15;
            }
            return Duration.between(lastAccess, LocalDateTime.now()).toMinutes() >= 5;

        }
        public String getUserId() {
            return userId;
        }
        public KeyManager getKeyManager() {
            lastAccess = LocalDateTime.now();
            return keyManager;
        }
        public UUID getUuid() {
            return uuid;
        }
        public String getUuidString() {
            return uuid.toString();
        }
        public LocalDateTime getLastAccess() {
            return lastAccess;
        }
        public void updateLastAccess() {
            lastAccess = LocalDateTime.now();
        }
        public boolean isDuringTheGame() {
            return duringTheGame;
        }
        public void setDuringTheGame(boolean duringTheGame) {
            this.duringTheGame = duringTheGame;
        }
        public void setUserId(String userId) {
            if(userId!=null){
                return;
            }
            this.userId = userId;
        }
    }
}
