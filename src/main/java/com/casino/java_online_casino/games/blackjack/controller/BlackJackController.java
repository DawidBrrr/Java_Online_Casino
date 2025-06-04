
package com.casino.java_online_casino.games.blackjack.controller;

import com.casino.java_online_casino.Connection.Games.Game;
import com.casino.java_online_casino.Connection.Utils.LogManager;
import com.casino.java_online_casino.games.blackjack.model.Card;
import com.casino.java_online_casino.games.blackjack.model.Hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlackJackController implements Game {

    private List<Card> deck;
    private Hand playerHand;
    private Hand dealerHand;
    private boolean gameOver;
    private int betValue;

    private volatile String currentUserId = null;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);
    private final AtomicBoolean cleanupScheduled = new AtomicBoolean(false);
    private Timer cleanupTimer = null;

    public BlackJackController() {
        playerHand = new Hand();
        dealerHand = new Hand();
        deck = new ArrayList<>();
        gameOver = false;
    }

    public void startNewGame() {
        deck = createShuffledDeck();
        playerHand.clear();
        dealerHand.clear();
        gameOver = false;

        playerHand.addCard(drawCard());
        dealerHand.addCard(drawCard());
        playerHand.addCard(drawCard());
        dealerHand.addCard(drawCard());
    }

    public void playerHit() {
        if (gameOver) return;

        playerHand.addCard(drawCard());
        if (playerHand.calculateValue() > 21) {
            gameOver = true;
        }
    }

    public void playerStand() {
        if (gameOver) return;

        while (dealerHand.calculateValue() < 17) {
            dealerHand.addCard(drawCard());
        }
        gameOver = true;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getPlayerScore() {
        return playerHand.calculateValue();
    }

    public int getDealerScore() {
        return dealerHand.calculateValue();
    }

    public List<Card> getPlayerHand() {
        return playerHand.getCards();
    }

    public List<Card> getDealerHand() {
        return dealerHand.getCards();
    }

    public String getGameResult() {
        int playerScore = playerHand.calculateValue();
        int dealerScore = dealerHand.calculateValue();

        if (playerScore > 21) return  "Przegrałeś! Masz powyżej 21.";
        if (dealerScore > 21) return "Wygrałeś! Krupier ma powyżej 21.";
        if (playerScore > dealerScore) return "Wygrałeś!";
        if (playerScore < dealerScore) return "Przegrałeś!";
        return "Remis!";
    }

    private Card drawCard() {
        if (deck.isEmpty()) {
            deck = createShuffledDeck();
        }
        return deck.remove(0);
    }

    private List<Card> createShuffledDeck() {
        String[] suits = {"Spades", "Hearts", "Diamonds", "Clubs"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace"};
        List<Card> newDeck = new ArrayList<>();

        for (String suit : suits) {
            for (String rank : ranks) {
                int value;
                if (rank.equals("Ace")) value = 11;
                else if (rank.equals("King") || rank.equals("Queen") || rank.equals("Jack")) value = 10;
                else value = Integer.parseInt(rank);
                newDeck.add(new Card(suit, rank, value));
            }
        }

        Collections.shuffle(newDeck);
        return newDeck;
    }

    // --- Game interface implementation ---

    /**
     * Ustawia userId tylko raz, nie pozwala na zmianę po ustawieniu.
     */
    public synchronized void setCurrentUserId(String userId) {
        if (this.currentUserId != null) {
            throw new IllegalStateException("UserId can only be set once and is already set to: " + this.currentUserId);
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        this.currentUserId = userId;
    }

    @Override
    public synchronized void onPlayerJoin(String userId) {
        if (inProgress.get() && (currentUserId != null && !currentUserId.equals(userId))) {
            throw new IllegalStateException("Game already in progress for another user");
        }
        if (cleanupTimer != null) {
            cleanupTimer.cancel();
            cleanupTimer = null;
            cleanupScheduled.set(false);
        }
        if (currentUserId == null) {
            setCurrentUserId(userId); // Ustawia tylko raz!
        }
        inProgress.set(true);
        System.out.println("[DEBUG] Player joined: " + userId);
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    @Override
    public synchronized void onPlayerLeave(String userId) {
        if (!inProgress.get() || (currentUserId != null && !currentUserId.equals(userId))) {
            return;
        }
        System.out.println("[DEBUG] Player left: " + userId + ", scheduling cleanup in 5 minutes...");
        LogManager.logToFile("[DEBUG] Player left: " + userId + ", scheduling cleanup in 5 minutes...");
        inProgress.set(false);
        if (!cleanupScheduled.getAndSet(true)) {
            cleanupTimer = new Timer();
            cleanupTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (BlackJackController.this) {
                        System.out.println("[DEBUG] Cleanup: closing game for user " + userId);
                        LogManager.logToFile("[DEBUG] Cleanup: closing game for user " + userId);
                        cleanupScheduled.set(false);
                    }
                }
            }, 5 * 60 * 1000); // 5 minut
        }
    }

    @Override
    public synchronized boolean isInProgress() {
        return inProgress.get();
    }

    @Override
    public synchronized boolean canJoin(String userId) {
        return !inProgress.get() || (currentUserId != null && currentUserId.equals(userId));
    }

    public int getCurrentBalance() {
        return betValue;
    }

    public void setBetValue(int betValue) {
        this.betValue = betValue;
    }
}
