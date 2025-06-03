package com.casino.java_online_casino.games.poker.controller;

import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.games.poker.model.Card;
import com.casino.java_online_casino.games.poker.model.PokerGame;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemotePokerController {
    private final PokerTCPClient tcpClient;
    private PokerDTO lastState;

    public RemotePokerController(PokerTCPClient tcpClient) {
        this.tcpClient = tcpClient;
        try {
            tcpClient.connect();
            System.out.println("[DEBUG POKER_CONTROLLER] Nawiązano połączenie z serwerem");
        } catch (IOException e) {
            System.err.println("[ERROR POKER_CONTROLLER] Nie można połączyć z serwerem: " + e.getMessage());
        }
    }

    public void startGame() {
        try {
            if (!isConnected()) {
                tcpClient.connect();
            }
            System.out.println("[DEBUG POKER_CONTROLLER] Rozpoczynanie gry");
            lastState = tcpClient.startGame();
        } catch (Exception e) {
            System.err.println("[ERROR POKER_CONTROLLER] Błąd podczas rozpoczynania gry: " + e.getMessage());
        }
    }
    private boolean isConnected() {
        return tcpClient != null && tcpClient.isConnected();
    }

    public void broadcastMessage(String message) {
        try {
            System.out.println("[DEBUG POKER_CONTROLLER] Wysyłanie wiadomości: " + message);
            tcpClient.broadcastMessage(message);
        } catch (Exception e) {
            System.err.println("[ERROR POKER_CONTROLLER] Błąd podczas wysyłania wiadomości: " + e.getMessage());
        }
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