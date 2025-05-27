package com.casino.java_online_casino.games.blackjack.controller;

import com.casino.java_online_casino.Connection.Server.DTO.GameStateDTO;
import com.casino.java_online_casino.games.blackjack.model.Card;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteBlackJackController {

    private final BlackjackTcpClient tcpClient;

    // Ostatni znany stan, aktualizujesz po każdej akcji!
    private GameStateDTO lastState;

    public RemoteBlackJackController(BlackjackTcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public void startNewGame() throws Exception {
        lastState = tcpClient.newGame();
    }

    public void playerHit() throws Exception {
        lastState = tcpClient.hit();
    }

    public void playerStand() throws Exception {
        lastState = tcpClient.stand();
    }

    public boolean isGameOver() {
        return lastState != null && lastState.gameOver;
    }

    public int getPlayerScore() {
        return lastState != null ? lastState.playerScore : 0;
    }

    public int getDealerScore() {
        return lastState != null ? lastState.dealerScore : 0;
    }

    public List<Card> getPlayerHand() {
        if (lastState == null || lastState.playerHand == null) return Collections.emptyList();
        return lastState.playerHand.stream()
            .map(dto -> new Card(dto.suit, dto.rank, dto.value))
            .collect(Collectors.toList());
    }

    public List<Card> getDealerHand() {
        if (lastState == null || lastState.dealerHand == null) return Collections.emptyList();
        return lastState.dealerHand.stream()
            .map(dto -> new Card(dto.suit, dto.rank, dto.value))
            .collect(Collectors.toList());
    }

    public String getGameResult() {
        return (lastState != null && lastState.gameOver) ? lastState.result : "";
    }
}
