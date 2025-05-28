package com.casino.java_online_casino.games.poker.model;

import java.util.*;

public class Deck {
    private List<Card> cards;
    private Random random;

    public Deck() {
        this.random = new Random();
        initializeDeck();
    }

    private void initializeDeck() {
        cards = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards, random);
    }

    public Card dealCard() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("No Card no Deal bro!");
        }
        return cards.remove(cards.size() - 1);
    }

    public void reset() {
        initializeDeck();
        shuffle();
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
