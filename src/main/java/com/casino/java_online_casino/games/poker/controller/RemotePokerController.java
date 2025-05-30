package com.casino.java_online_casino.games.poker.controller;

import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.games.poker.model.Card;
import com.casino.java_online_casino.games.poker.model.PokerGame;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemotePokerController {
    private final PokerTCPClient tcpClient;
    private PokerDTO lastState;

    public RemotePokerController(PokerTCPClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    // Akcje gracza
    public void fold() throws Exception {
        lastState = tcpClient.fold();
    }

    public void call(int amount) throws Exception {
        lastState = tcpClient.call(amount);
    }

    public void raise(int amount) throws Exception {
        lastState = tcpClient.raise(amount);
    }

    public void check() throws Exception {
        lastState = tcpClient.check();
    }

    public void allIn(int amount) throws Exception {
        lastState = tcpClient.allIn(amount);
    }

    // Gettery stanu gry
    public PokerGame.GameState getGameStatus() {
        return lastState != null ? lastState.gameState : PokerGame.GameState.WAITING_FOR_PLAYERS;
    }

    public int getPot() {
        return lastState != null ? lastState.pot : 0;
    }

    public int getCurrentBet() {
        return lastState != null ? lastState.currentBet : 0;
    }

    public List<Card> getCommunityCards() {
        if (lastState == null || lastState.communityCards == null) {
            return Collections.emptyList();
        }
        return lastState.communityCards.stream()
                .map(dto -> new Card(dto.suit, dto.rank))
                .collect(Collectors.toList());
    }

    public Map<String, PokerDTO.PokerPlayerDTO> getPlayers() {
        return lastState != null ? lastState.players : Collections.emptyMap();
    }

    public String getActivePlayerId() {
        return lastState != null ? lastState.activePlayerId : "";
    }

    public boolean isGameOver() {
        return lastState != null && lastState.isGameOver;
    }

    public String getWinnerId() {
        return lastState != null ? lastState.winnerId : "";
    }

    public int getMinimumBet() {
        return lastState != null ? lastState.minimumBet : 0;
    }
}