package com.casino.java_online_casino.games.poker.model;

import java.util.*;

public class PokerGame {
    public enum GameState {
        WAITING_FOR_PLAYERS, PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN, GAME_OVER, DEALING
    }

    private List<Player> players;
    private List<Card> communityCards;
    private Deck deck;
    private int pot;
    private int currentBet;
    private int dealerIndex;
    private int currentPlayerIndex;
    private GameState gameState;
    private int smallBlind;
    private int bigBlind;
    private final int maxPlayers = 4;

    public PokerGame() {
        this.players = new ArrayList<>();
        this.communityCards = new ArrayList<>();
        this.deck = new Deck();
        this.pot = 0;
        this.currentBet = 0;
        this.dealerIndex = 0;
        this.currentPlayerIndex = 0;
        this.gameState = GameState.WAITING_FOR_PLAYERS;
        this.smallBlind = 10;
        this.bigBlind = 20;
    }

    public boolean addPlayer(Player player) {
        if (players.size() >= maxPlayers) {
            return false;
        }
        players.add(player);
        if (players.size() >= 2) {
            gameState = GameState.PRE_FLOP;
        }
        return true;
    }

    public void removePlayer(String playerId) {
        players.removeIf(p -> p.getId().equals(playerId));
        if (players.size() < 2) {
            gameState = GameState.WAITING_FOR_PLAYERS;
        }
    }

    public void startNewHand() {
        if (players.size() < 2) {
            gameState = GameState.WAITING_FOR_PLAYERS;
            return;
        }

        // Reset game state
        pot = 0;
        currentBet = 0;
        communityCards.clear();
        deck.reset();

        // Clear player hands
        for (Player player : players) {
            player.clearHand();
        }

        // Post blinds
        postBlinds();

        // Deal hole cards
        dealHoleCards();

        gameState = GameState.PRE_FLOP;
        currentPlayerIndex = getNextActivePlayerIndex((dealerIndex + 3) % players.size());
    }

    private void postBlinds() {
        if (players.size() < 2) return;

        int smallBlindIndex = (dealerIndex + 1) % players.size();
        int bigBlindIndex = (dealerIndex + 2) % players.size();

        Player smallBlindPlayer = players.get(smallBlindIndex);
        Player bigBlindPlayer = players.get(bigBlindIndex);

        smallBlindPlayer.placeBet(Math.min(smallBlind, smallBlindPlayer.getBalance()));
        bigBlindPlayer.placeBet(Math.min(bigBlind, bigBlindPlayer.getBalance()));

        currentBet = bigBlind;
        pot += smallBlindPlayer.getCurrentBet() + bigBlindPlayer.getCurrentBet();
    }

    private void dealHoleCards() {
        for (int i = 0; i < 2; i++) {
            for (Player player : players) {
                if (!player.isFolded()) {
                    player.receiveCard(deck.dealCard());
                }
            }
        }
    }

    public boolean processPlayerAction(String playerId, Player.playerAction action, int amount) {
        Player player = getPlayerById(playerId);
        if (player == null || !isPlayerTurn(playerId) || player.isFolded()) {
            return false;
        }

        switch (action) {
            case FOLD:
                player.fold();
                break;
            case CALL:
                int callAmount = Math.min(currentBet - player.getCurrentBet(), player.getBalance());
                player.placeBet(callAmount);
                pot += callAmount;
                break;
            case RAISE:
                int raiseAmount = Math.min(amount, player.getBalance());
                player.placeBet(raiseAmount);
                pot += raiseAmount;
                currentBet = Math.max(currentBet, player.getCurrentBet());
                break;
            case CHECK:
                if (player.getCurrentBet() < currentBet) {
                    return false; // Can't check if there's a bet to call
                }
                break;
            case ALL_IN:
                int allInAmount = player.getBalance();
                player.placeBet(allInAmount);
                pot += allInAmount;
                currentBet = Math.max(currentBet, player.getCurrentBet());
                break;
        }

        player.setLastAction(action);
        advanceToNextPlayer();

        if (isBettingRoundComplete()) {
            advanceGameState();
        }

        return true;
    }

    private void advanceToNextPlayer() {
        currentPlayerIndex = getNextActivePlayerIndex(currentPlayerIndex);
    }

    private int getNextActivePlayerIndex(int startIndex) {
        int nextIndex = (startIndex + 1) % players.size();
        int attempts = 0;

        while (attempts < players.size()) {
            Player player = players.get(nextIndex);
            if (!player.isFolded() && !player.isAllIn()) {
                return nextIndex;
            }
            nextIndex = (nextIndex + 1) % players.size();
            attempts++;
        }

        return startIndex; // Return original if no active players found
    }

    private boolean isBettingRoundComplete() {
        List<Player> activePlayers = getActivePlayers();
        if (activePlayers.size() <= 1) return true;

        int maxBet = activePlayers.stream().mapToInt(Player::getCurrentBet).max().orElse(0);

        return activePlayers.stream()
                .allMatch(p -> p.getCurrentBet() == maxBet || p.isAllIn());
    }

    private void advanceGameState() {
        switch (gameState) {
            case PRE_FLOP:
                dealFlop();
                gameState = GameState.FLOP;
                break;
            case FLOP:
                dealTurn();
                gameState = GameState.TURN;
                break;
            case TURN:
                dealRiver();
                gameState = GameState.RIVER;
                break;
            case RIVER:
                gameState = GameState.SHOWDOWN;
                determineWinner();
                break;
        }

        if (gameState != GameState.SHOWDOWN) {
            resetCurrentBets();
            currentPlayerIndex = getNextActivePlayerIndex(dealerIndex);
        }
    }

    private void dealFlop() {
        deck.dealCard(); // Burn card
        for (int i = 0; i < 3; i++) {
            communityCards.add(deck.dealCard());
        }
    }

    private void dealTurn() {
        deck.dealCard(); // Burn card
        communityCards.add(deck.dealCard());
    }

    private void dealRiver() {
        deck.dealCard(); // Burn card
        communityCards.add(deck.dealCard());
    }

    private void resetCurrentBets() {
        currentBet = 0;
        for (Player player : players) {
            player.setCurrentBet(0);
        }
    }

    private void determineWinner() {
        List<Player> activePlayers = getActivePlayers();
        if (activePlayers.size() == 1) {
            activePlayers.get(0).winPot(pot);
        } else {
            // Simplified - in real poker, you'd evaluate hand rankings
            // For now, just split the pot
            int winAmount = pot / activePlayers.size();
            for (Player player : activePlayers) {
                player.winPot(winAmount);
            }
        }

        // Move dealer button
        dealerIndex = (dealerIndex + 1) % players.size();
        gameState = GameState.GAME_OVER;
    }

    public List<Player> getActivePlayers() {
        return players.stream()
                .filter(p -> !p.isFolded())
                .toList();
    }

    public Player getPlayerById(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public boolean isPlayerTurn(String playerId) {
        if (currentPlayerIndex >= players.size()) return false;
        return players.get(currentPlayerIndex).getId().equals(playerId);
    }

    // Gettery
    public List<Player> getPlayers() { return new ArrayList<>(players); }
    public List<Card> getCommunityCards() { return new ArrayList<>(communityCards); }
    public int getPot() { return pot; }
    public int getCurrentBet() { return currentBet; }
    public GameState getGameState() { return gameState; }
    public Player getCurrentPlayer() {
        return currentPlayerIndex < players.size() ? players.get(currentPlayerIndex) : null;
    }
    public int getSmallBlind() { return smallBlind; }
    public int getBigBlind() { return bigBlind; }
    public void setSmallBlind(int smallBlind) { this.smallBlind = smallBlind; }
    public void setBigBlind(int bigBlind) { this.bigBlind = bigBlind; }
}
