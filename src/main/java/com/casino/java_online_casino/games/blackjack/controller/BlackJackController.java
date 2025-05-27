package com.casino.java_online_casino.games.blackjack.controller;

import com.casino.java_online_casino.games.blackjack.model.Card;
import com.casino.java_online_casino.games.blackjack.model.Hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlackJackController {

    private List<Card> deck;
    private Hand playerHand;
    private Hand dealerHand;
    private boolean gameOver;

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

        if (playerScore > 21) return "Przegrałeś! Masz powyżej 21.";
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


}
