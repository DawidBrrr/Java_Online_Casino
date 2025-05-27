package com.casino.java_online_casino.games.poker.model;

public class Card {
    public enum Suit {
        HEARTS, DIAMONDS, CLUBS, SPADES
    }
    public enum Rank {
        TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8),
        NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13), ACE(14);

        private final int value;
        Rank(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }
    public Suit getSuit() {
        return suit;
    }
    public Rank getRank() {
        return rank;
    }
    public String getImageName(){
        String suitName = suit.name().toLowerCase();
        String rankName = rank.name().toLowerCase();

        // Konwersja nazw dla zgodności z formatem plików
        if (rank == Rank.JACK) rankName = "jack";
        else if (rank == Rank.QUEEN) rankName = "queen";
        else if (rank == Rank.KING) rankName = "king";
        else if (rank == Rank.ACE) rankName = "ace";
        else rankName = String.valueOf(rank.getValue());

        return rankName + "_of_" + suitName + ".png";
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        Card card = (Card) obj;
        return suit == card.suit && rank == card.rank;
    }

    @Override
    public int hashCode(){
        return suit.hashCode() * 31 + rank.hashCode();
    }
}
