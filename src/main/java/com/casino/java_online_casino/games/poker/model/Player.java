package com.casino.java_online_casino.games.poker.model;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private int balance;
    private int currentBet;
    private List<Card> hand;
    private boolean folded;
    private boolean allIn;
    private playerAction lastAction;
    private String id;

    public enum playerAction {
        CHECK, CALL, RAISE, FOLD, ALL_IN
    }

    public Player(String id, String name, int initialBalance) {
        this.id = id;
        this.name = name;
        this.balance = initialBalance;
        this.currentBet = 0;
        this.hand = new ArrayList<>();
        this.folded = false;
        this.allIn = false;
        this.lastAction = null;
    }

    public void receiveCard(Card card) {
        hand.add(card);
    }

    public void clearHand() {
        hand.clear();
        folded = false;
        allIn = false;
        currentBet = 0;
        lastAction = null;
    }

    public boolean canBet(int amount) {
        return balance >= amount && !folded && !allIn;
    }

    public void placeBet(int amount) {
        if(amount >= balance){
            currentBet += amount;
            balance = 0;
            allIn = true;
            lastAction = playerAction.ALL_IN;
        }
        else{
            balance -= amount;
            currentBet += amount;
        }
    }

    public void fold() {
        folded = true;
        lastAction = playerAction.FOLD;
    }

    public void winPot(int amount) {
        balance += amount;
    }
    // Gettery i settery
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }

    public int getCurrentBet() { return currentBet; }
    public void setCurrentBet(int currentBet) { this.currentBet = currentBet; }

    public List<Card> getHand() { return new ArrayList<>(hand); }

    public boolean isFolded() { return folded; }
    public boolean isAllIn() { return allIn; }

    public playerAction getLastAction() { return lastAction; }
    public void setLastAction(playerAction lastAction) { this.lastAction = lastAction; }

    @Override
    public String toString() {
        return name + " (Balance: $" + balance + ", Bet: $" + currentBet + ")";
    }

}
