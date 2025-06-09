package com.casino.java_online_casino.games.poker.controller;

import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;

public interface GameStateListener {
    void onGameStateReceived(PokerDTO dto);
}