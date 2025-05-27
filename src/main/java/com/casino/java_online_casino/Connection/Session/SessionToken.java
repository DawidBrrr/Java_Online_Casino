package com.casino.java_online_casino.Connection.Session;

import com.casino.java_online_casino.Experimental;

import java.util.UUID;
@Experimental

public class SessionToken {
    private final String sessionKey;
    private final String userId;
    private final String gameId;

    public SessionToken(String userId, String gameId) {
        this.userId = userId;
        this.gameId = gameId;
        this.sessionKey = UUID.randomUUID().toString(); // Możesz zaszyfrować lub zakodować
    }

    public String getSessionKey() { return sessionKey; }
    public String getUserId() { return userId; }
    public String getGameId() { return gameId; }
}
