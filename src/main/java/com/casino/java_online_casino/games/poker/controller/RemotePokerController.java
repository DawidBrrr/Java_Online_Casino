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

    public void join() throws Exception {
        lastState = tcpClient.join();
    }

    public void leave() throws Exception {
        lastState = tcpClient.leave();
    }

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

    public void updateState() throws Exception {
        System.out.println("[DEBUG CONTROLLER] Aktualizacja stanu...");
        PokerDTO newState = tcpClient.getState();
        if (newState == null) {
            System.out.println("[DEBUG CONTROLLER] Otrzymano null state!");
            return;
        }
        System.out.println("[DEBUG CONTROLLER] Otrzymano nowy stan:");
        System.out.println("- Liczba graczy: " + (newState.players != null ? newState.players.size() : "null"));
        System.out.println("- Stan gry: " + newState.gameState);
        lastState = newState;
    }

    public void updateFinalState() throws Exception {
        lastState = tcpClient.getFinalState();
    }

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
        if (lastState == null || lastState.players == null) return Collections.emptyMap();
        return lastState.players;
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

    // --- NOWA LOGIKA: identyfikacja własnego gracza po yourUserId ---

    public PokerDTO.PokerPlayerDTO getMyPlayer() {
        if (lastState == null || lastState.players == null || lastState.yourUserId == null) return null;
        PokerDTO.PokerPlayerDTO me = lastState.players.get(lastState.yourUserId);
        if (me == null) {
            System.out.println("[DEBUG CONTROLLER] Nie znaleziono gracza o id=" + lastState.yourUserId);
        }
        return me;
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
