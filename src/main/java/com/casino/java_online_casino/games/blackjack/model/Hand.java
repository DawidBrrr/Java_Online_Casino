package com.casino.java_online_casino.games.blackjack.model;

import java.util.ArrayList;
import java.util.List;

public class Hand {
    private List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        cards.add(card);
    }

    public int calculateValue() {
        int sum = 0;
        int aces = 0;

        for (Card card : cards) {
            if (card.getRank().equals("Ace")) {
                aces++;
            }
            sum += card.getValue();
        }

        while (sum > 21 && aces > 0) {
            sum -= 10;
            aces--;
        }

        return sum;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void clear() {
        cards.clear();
    }
}
