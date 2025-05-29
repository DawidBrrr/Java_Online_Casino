package com.casino.java_online_casino.Connection.Session;

import com.casino.java_online_casino.Connection.Games.Game;
import com.casino.java_online_casino.Connection.Tokens.KeyManager;
import com.casino.java_online_casino.Connection.Tokens.ServerTokenManager;
import com.casino.java_online_casino.Experimental;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Experimental
public class SessionManager {
    private static volatile SessionManager instance;
    private final Map<UUID, SessionToken> sessionMap = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock mapLock = new ReentrantReadWriteLock();

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
        mapLock.writeLock().lock();
        try {
            sessionMap.put(uuid, sessionToken);
        } finally {
            mapLock.writeLock().unlock();
        }
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
        mapLock.readLock().lock();
        try {
            return sessionMap.get(userId);
        } finally {
            mapLock.readLock().unlock();
        }
    }
    public SessionToken getSessionByUserId(String userId) {
        mapLock.readLock().lock();
        try {
            return sessionMap.values().stream()
                    .filter(session -> userId != null && userId.equals(session.getUserId()))
                    .findFirst().orElse(null);
        } finally {
            mapLock.readLock().unlock();
        }
    }

    // Sprawdza, czy użytkownik ma aktywną sesję
    public boolean hasSessionByUUID(UUID userId) {
        mapLock.readLock().lock();
        try {
            return sessionMap.containsKey(userId);
        } finally {
            mapLock.readLock().unlock();
        }
    }
    public boolean hasSessionByUserId(String userId) {
        mapLock.readLock().lock();
        try {
            return sessionMap.values().stream().anyMatch(session -> userId != null && userId.equals(session.getUserId()));
        } finally {
            mapLock.readLock().unlock();
        }
    }
    public void deleteSessionByUUID(UUID userId) {
        mapLock.writeLock().lock();
        try {
            sessionMap.remove(userId);
        } finally {
            mapLock.writeLock().unlock();
        }
    }
    public void deleteSessionByUserId(String userId) {
        mapLock.writeLock().lock();
        try {
            sessionMap.entrySet().removeIf(entry -> userId != null && userId.equals(entry.getValue().getUserId()));
        } finally {
            mapLock.writeLock().unlock();
        }
    }

    // Aktualizuje dane sesji (np. nowy stan gry)
    public void updateSessionData(UUID userId, SessionToken session) {
        if (session != null) {
            session.updateLastAccess();
            mapLock.writeLock().lock();
            try {
                sessionMap.put(userId, session);
            } finally {
                mapLock.writeLock().unlock();
            }
        }
    }

    public void cleanupExpiredSessions() {
        mapLock.writeLock().lock();
        try {
            sessionMap.values().removeIf(SessionToken::isTimeToExpire);
        } finally {
            mapLock.writeLock().unlock();
        }
    }

    public class SessionToken {
        private final UUID uuid;
        private String userId;
        private final KeyManager keyManager;
        private LocalDateTime lastAccess;
        private Map<String,String> userData;
        private boolean duringTheGame;
        private Game game;
        private final ReentrantLock gameLock = new ReentrantLock();

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
            this.game = null;
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

        // --- GAME MANAGEMENT ---

        public Game getGame() {
            return this.game;
        }

        public void setGame(Game game) {
            this.game = game;
        }

        /**
         * Próbuje zarezerwować kontroler gry dla klienta.
         * Zwraca true jeśli rezerwacja się powiodła, false jeśli już jest zajęty.
         */
        public boolean tryLockGame() {
            return gameLock.tryLock();
        }

        /**
         * Zwalnia rezerwację kontrolera gry.
         */
        public void unlockGame() {
            if (gameLock.isHeldByCurrentThread()) {
                gameLock.unlock();
            }
        }

        public boolean isGameLocked() {
            return gameLock.isLocked();
        }
    }
}
