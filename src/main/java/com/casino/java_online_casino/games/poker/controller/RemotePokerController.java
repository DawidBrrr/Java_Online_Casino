package com.casino.java_online_casino.games.poker.controller;

import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.casino.java_online_casino.games.poker.model.Card;
import com.casino.java_online_casino.games.poker.model.PokerGame;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemotePokerController {

    private final PokerTcpClient tcpClient;
    private PokerDTO lastState;


    public RemotePokerController(PokerTcpClient tcpClient) {
        this.tcpClient = tcpClient;
        try {
            tcpClient.connect();
            lastState = tcpClient.reconnect(); // automatyczny reconnect po połączeniu
            System.out.println("[DEBUG POKER_CONTROLLER] Nawiązano połączenie z serwerem i wykonano reconnect");
            LogManager.logToFile("[DEBUG POKER_CONTROLLER] Nawiązano połączenie z serwerem i wykonano reconnect");
        } catch (Exception e) {
            System.err.println("[ERROR POKER_CONTROLLER] Nie można połączyć z serwerem: " + e.getMessage());
            LogManager.logToFile("[ERROR POKER_CONTROLLER] Nie można połączyć z serwerem: " + e.getMessage());
        }
    }

    // Rozpoczyna nową grę (dołączenie do stołu)
    public void join() throws Exception {
        lastState = tcpClient.join();
    }

    // Opuszcza stół
    public void leave() throws Exception {
        lastState = tcpClient.leave();
    }

    // Akcje gracza
    public void fold() throws Exception {
        lastState = tcpClient.fold();
    }

    public void call() throws Exception {
        lastState = tcpClient.call();
    }

    public void raise(int amount) throws Exception {
        lastState = tcpClient.raise(amount);
    }

    public void check() throws Exception {
        lastState = tcpClient.check();
    }

    public void allIn() throws Exception {
        lastState = tcpClient.allIn();
    }

    // Pobierz najnowszy stan gry z serwera (np. do odświeżenia GUI)
    public void updateState() throws Exception {
        lastState = tcpClient.getState();
    }

    // Pobierz pełny stan po zakończeniu partii (showdown)
    public void updateFinalState() throws Exception {
        lastState = tcpClient.getFinalState();
    }

    // Reconnect (np. po utracie połączenia)
    public void reconnect() throws Exception {
        lastState = tcpClient.reconnect();
    }

    // --- GETTERY STANU GRY ---

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
                .filter(dto -> dto.suit != null && dto.rank != null)
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

    public boolean isMyTurn() {
        return lastState != null && lastState.playerActions != null && !lastState.playerActions.isEmpty();
    }

    public List<String> getAvailableActions() {
        if (lastState == null) return Collections.emptyList();
        return lastState.playerActions != null ? lastState.playerActions : Collections.emptyList();
    }

    public int getSmallBlind() {
        return lastState != null ? lastState.smallBlind : 0;
    }

    public int getBigBlind() {
        return lastState != null ? lastState.bigBlind : 0;
    }

    public String getDealerId() {
        return lastState != null ? lastState.dealerId : "";
    }

    public String getSmallBlindId() {
        return lastState != null ? lastState.smallBlindId : "";
    }

    public String getBigBlindId() {
        return lastState != null ? lastState.bigBlindId : "";
    }


    public PokerDTO.PokerPlayerDTO getMyPlayer() {
        if (lastState == null || lastState.players == null) return null;
        // Znajdź gracza, który ma pełne dane (czyli ma niepustą listę hand lub canAct==true)
        for (PokerDTO.PokerPlayerDTO player : lastState.players.values()) {
            // Pełne dane tylko dla klienta: hand != null && (hand.size() == 2 lub canAct == true)
            if (player.hand != null && player.hand.size() > 0 && (player.canAct || !player.hand.isEmpty())) {
                return player;
            }
        }
        return null;
    }

    public float getMyBalance() {
        PokerDTO.PokerPlayerDTO me = getMyPlayer();
        return me != null ? me.balance : 0;
    }

    public List<Card> getMyCards() {
        PokerDTO.PokerPlayerDTO me = getMyPlayer();
        if (me == null || me.hand == null) return Collections.emptyList();
        return me.hand.stream()
                .filter(dto -> dto.suit != null && dto.rank != null)
                .map(dto -> new Card(dto.suit, dto.rank))
                .collect(Collectors.toList());
    }

    public String getMyStatus() {
        PokerDTO.PokerPlayerDTO me = getMyPlayer();
        return me != null ? me.status : "";
    }

    public void close() {
        tcpClient.close();
    }
}
