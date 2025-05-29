package com.casino.java_online_casino.Connection.Games;


public interface Game {
    void onPlayerJoin(String userId);
    void onPlayerLeave(String userId);
    boolean isInProgress();
    boolean canJoin(String userId);
}