package com.casino.java_online_casino.games.poker.model;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class PokerGame {
    public enum GameState {
        WAITING_FOR_PLAYERS, PRE_FLOP, FLOP, TURN, RIVER,
        SHOWDOWN, GAME_OVER, DEALING, IN_PROGRESS
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
    private final int maxPlayers; // Zmienione z stałej na konfigurowalny parametr

    // Nowe pola dla kompatybilności z PokerController
    private Player lastWinner;
    private List<Player> currentRoundWinners = new ArrayList<>();
    private final ReentrantLock gameLock = new ReentrantLock();

    // Konstruktor domyślny (zachowuje kompatybilność wsteczną)
    public PokerGame() {
        this(4); // Domyślnie 4 graczy
    }

    // Nowy konstruktor z konfiguracją maxPlayers
    public PokerGame(int maxPlayers) {
        this.maxPlayers = maxPlayers;
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
        this.lastWinner = null;
    }

    public boolean addPlayer(Player player) {
        gameLock.lock();
        try {
            if (players.size() >= maxPlayers) {
                return false;
            }
            players.add(player);
            if (players.size() >= 2) {
                gameState = GameState.IN_PROGRESS;
            }
            return true;
        } finally {
            gameLock.unlock();
        }
    }

    public void removePlayer(String playerId) {
        gameLock.lock();
        try {
            players.removeIf(p -> p.getId().equals(playerId));
            if (players.size() < 2) {
                gameState = GameState.WAITING_FOR_PLAYERS;
            }
        } finally {
            gameLock.unlock();
        }
    }

    public void startNewHand() {
        gameLock.lock();
        try {
            if (players.size() < 2) {
                gameState = GameState.WAITING_FOR_PLAYERS;
                return;
            }

            // Reset game state
            pot = 0;
            currentBet = 0;
            communityCards.clear();
            deck.reset();
            lastWinner = null;
            currentRoundWinners.clear();

            // Clear player hands
            for (Player player : players) {
                player.clearHand();
            }

            // Post blinds
            postBlinds();

            // Deal hole cards
            dealHoleCards();

            gameState = GameState.IN_PROGRESS; // zamiast PRE_FLOP
            currentPlayerIndex = getNextActivePlayerIndex((dealerIndex + 3) % players.size());
        } finally {
            gameLock.unlock();
        }
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
        gameLock.lock();
        try {
            Player player = getPlayerById(playerId);
            if (player == null || !isPlayerTurn(playerId) || player.isFolded()) {
                return false;
            }

            switch (action) {
                case FOLD:
                    player.fold();
                    break;
                case CALL:
                    float callAmount = Math.min(currentBet - player.getCurrentBet(), player.getBalance());
                    player.placeBet(callAmount);
                    pot += callAmount;
                    break;
                case RAISE:
                    float raiseAmount = Math.min(amount, player.getBalance());
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
                    float allInAmount = player.getBalance();
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
        } finally {
            gameLock.unlock();
        }
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
        gameLock.lock();
        try {
            List<Player> activePlayers = getActivePlayers();
            currentRoundWinners.clear();

            if (activePlayers.size() == 1) {
                Player winner = activePlayers.get(0);
                winner.winPot(pot);
                lastWinner = winner;
                currentRoundWinners.add(winner);
            } else {
                // Simplified - w rzeczywistym pokerze oceniano by siłę układów
                // Na razie po prostu dzielimy pulę między aktywnych graczy
                int winAmount = pot / activePlayers.size();
                for (Player player : activePlayers) {
                    player.winPot(winAmount);
                    currentRoundWinners.add(player);
                }
                // Ustaw pierwszego z zwycięzców jako głównego winnera dla getWinner()
                lastWinner = activePlayers.get(0);
            }

            // Move dealer button
            dealerIndex = (dealerIndex + 1) % players.size();
            gameState = GameState.GAME_OVER;
        } finally {
            gameLock.unlock();
        }
    }

    // NOWE METODY WYMAGANE PRZEZ POKERCONTROLLER

    /**
     * Kończy aktualną grę i przełącza stan na GAME_OVER
     */
    public void endGame() {
        gameLock.lock();
        try {
            gameState = GameState.GAME_OVER;
            lastWinner = null;
            currentRoundWinners.clear();
        } finally {
            gameLock.unlock();
        }
    }

    /**
     * Przesuwa turę na następnego gracza
     */
    public void advanceTurn() {
        gameLock.lock();
        try {
            advanceToNextPlayer();
        } finally {
            gameLock.unlock();
        }
    }

    /**
     * Zwraca zwycięzcę ostatniej rundy lub null jeśli gra w toku
     */
    public Player getWinner() {
        gameLock.lock();
        try {
            if (gameState == GameState.GAME_OVER || gameState == GameState.SHOWDOWN) {
                return lastWinner;
            }
            return null; // Gra w toku
        } finally {
            gameLock.unlock();
        }
    }

    /**
     * Zwraca wszystkich zwycięzców ostatniej rundy (w przypadku remisu)
     */
    public List<Player> getAllWinners() {
        gameLock.lock();
        try {
            return new ArrayList<>(currentRoundWinners);
        } finally {
            gameLock.unlock();
        }
    }

    /**
     * Zwraca maksymalną liczbę graczy
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    // POMOCNICZE METODY (bez zmian)

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
        gameLock.lock();
        try {
            if (currentPlayerIndex >= players.size()) return false;
            return players.get(currentPlayerIndex).getId().equals(playerId);
        } finally {
            gameLock.unlock();
        }
    }

    // GETTERY Z SYNCHRONIZACJĄ

    public List<Player> getPlayers() {
        gameLock.lock();
        try {
            return new ArrayList<>(players);
        } finally {
            gameLock.unlock();
        }
    }

    public List<Card> getCommunityCards() {
        gameLock.lock();
        try {
            return new ArrayList<>(communityCards);
        } finally {
            gameLock.unlock();
        }
    }

    public int getPot() {
        gameLock.lock();
        try {
            return pot;
        } finally {
            gameLock.unlock();
        }
    }

    public int getCurrentBet() {
        gameLock.lock();
        try {
            return currentBet;
        } finally {
            gameLock.unlock();
        }
    }

    public GameState getGameState() {
        gameLock.lock();
        try {
            return gameState;
        } finally {
            gameLock.unlock();
        }
    }

    public Player getCurrentPlayer() {
        gameLock.lock();
        try {
            return currentPlayerIndex < players.size() ? players.get(currentPlayerIndex) : null;
        } finally {
            gameLock.unlock();
        }
    }

    public int getSmallBlind() {
        gameLock.lock();
        try {
            return smallBlind;
        } finally {
            gameLock.unlock();
        }
    }

    public int getBigBlind() {
        gameLock.lock();
        try {
            return bigBlind;
        } finally {
            gameLock.unlock();
        }
    }

    public void setSmallBlind(int smallBlind) {
        gameLock.lock();
        try {
            this.smallBlind = smallBlind;
        } finally {
            gameLock.unlock();
        }
    }

    public void setBigBlind(int bigBlind) {
        gameLock.lock();
        try {
            this.bigBlind = bigBlind;
        } finally {
            gameLock.unlock();
        }
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public void setPot(int pot) {
        this.pot = pot;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public int getDealerIndex() {
        return dealerIndex;
    }

    public void setDealerIndex(int dealerIndex) {
        this.dealerIndex = dealerIndex;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public Player getLastWinner() {
        return lastWinner;
    }

    public void setLastWinner(Player lastWinner) {
        this.lastWinner = lastWinner;
    }

    public List<Player> getCurrentRoundWinners() {
        return currentRoundWinners;
    }

    public void setCurrentRoundWinners(List<Player> currentRoundWinners) {
        this.currentRoundWinners = currentRoundWinners;
    }

    public ReentrantLock getGameLock() {
        return gameLock;
    }
}
