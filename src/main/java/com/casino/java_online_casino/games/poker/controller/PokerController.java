package com.casino.java_online_casino.games.poker.controller;
import com.casino.java_online_casino.games.poker.model.*;
import com.casino.java_online_casino.games.poker.gui.PokerView;

import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PokerController {
    private PokerGame game;
    private PokerView view;
    private String currentPlayerId;
    private ScheduledExecutorService scheduler;

    // Komunikacja z serwerem - interfejsy do implementacji
    public interface ServerCommunication {
        void sendPlayerAction(String playerId, Player.playerAction action, int amount);
        void sendGameState(PokerGame.GameState state);
        void sendPlayerUpdate(Player player);
        void broadcastMessage(String message);
    }

    private ServerCommunication serverComm;

    public PokerController() {
        this.game = new PokerGame();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void setView(PokerView view) {
        this.view = view;
    }

    public void setServerCommunication(ServerCommunication serverComm) {
        this.serverComm = serverComm;
    }

    // Metody obsługi graczy
    public boolean joinGame(String playerId, String playerName) {
        Player newPlayer = new Player(playerId, playerName, 1000); // Starting balance
        boolean added = game.addPlayer(newPlayer);

        if (added) {
            updateView();
            if (serverComm != null) {
                serverComm.sendPlayerUpdate(newPlayer);
                serverComm.broadcastMessage(playerName + " joined the game");
            }

            // Auto-start game if enough players
            if (game.getPlayers().size() >= 2 && game.getGameState() == PokerGame.GameState.WAITING_FOR_PLAYERS) {
                startNewHand();
            }
        }

        return added;
    }

    public void leaveGame(String playerId) {
        Player player = game.getPlayerById(playerId);
        if (player != null) {
            game.removePlayer(playerId);
            updateView();

            if (serverComm != null) {
                serverComm.broadcastMessage(player.getName() + " left the game");
            }
        }
    }

    public void setCurrentPlayer(String playerId) {
        this.currentPlayerId = playerId;
        updateView();
    }

    // Akcje gracza
    public void playerFold() {
        if (canPlayerAct()) {
            game.processPlayerAction(currentPlayerId, Player.playerAction.FOLD, 0);
            updateView();
            broadcastAction("folded");
        }
    }

    public void playerCall() {
        if (canPlayerAct()) {
            Player player = game.getPlayerById(currentPlayerId);
            int callAmount = game.getCurrentBet() - player.getCurrentBet();
            game.processPlayerAction(currentPlayerId, Player.playerAction.CALL, callAmount);
            updateView();
            broadcastAction("called $" + callAmount);
        }
    }

    public void playerRaise(int amount) {
        if (canPlayerAct() && amount > 0) {
            game.processPlayerAction(currentPlayerId, Player.playerAction.RAISE, amount);
            updateView();
            broadcastAction("raised $" + amount);
        }
    }

    public void playerCheck() {
        if (canPlayerAct()) {
            Player player = game.getPlayerById(currentPlayerId);
            if (player.getCurrentBet() >= game.getCurrentBet()) {
                game.processPlayerAction(currentPlayerId, Player.playerAction.CHECK, 0);
                updateView();
                broadcastAction("checked");
            }
        }
    }

    public void playerAllIn() {
        if (canPlayerAct()) {
            Player player = game.getPlayerById(currentPlayerId);
            game.processPlayerAction(currentPlayerId, Player.playerAction.ALL_IN, player.getBalance());
            updateView();
            broadcastAction("went all-in!");
        }
    }

    private boolean canPlayerAct() {
        return currentPlayerId != null &&
                game.isPlayerTurn(currentPlayerId) &&
                game.getGameState() != PokerGame.GameState.WAITING_FOR_PLAYERS &&
                game.getGameState() != PokerGame.GameState.SHOWDOWN &&
                game.getGameState() != PokerGame.GameState.GAME_OVER;
    }

    private void broadcastAction(String action) {
        if (serverComm != null) {
            Player player = game.getPlayerById(currentPlayerId);
            if (player != null) {
                serverComm.broadcastMessage(player.getName() + " " + action);
            }
        }
    }

    // Kontrola gry
    public void startNewHand() {
        game.startNewHand();
        updateView();

        if (serverComm != null) {
            serverComm.sendGameState(game.getGameState());
            serverComm.broadcastMessage("New hand started!");
        }

        // Auto-advance if current player is AI or disconnected
        scheduleNextAction();
    }

    private void scheduleNextAction() {
        if (game.getGameState() == PokerGame.GameState.GAME_OVER) {
            // Auto-start next hand after delay
            scheduler.schedule(() -> {
                Platform.runLater(() -> {
                    if (game.getPlayers().size() >= 2) {
                        startNewHand();
                    }
                });
            }, 3, TimeUnit.SECONDS);
        }
    }

    // Pomocne metody dla view
    public boolean isCurrentPlayerTurn() {
        return currentPlayerId != null && game.isPlayerTurn(currentPlayerId);
    }

    public Player getCurrentPlayerObject() {
        return currentPlayerId != null ? game.getPlayerById(currentPlayerId) : null;
    }

    public boolean canCall() {
        Player player = getCurrentPlayerObject();
        return player != null && !player.isFolded() &&
                player.getCurrentBet() < game.getCurrentBet() &&
                player.getBalance() > 0;
    }

    public boolean canRaise() {
        Player player = getCurrentPlayerObject();
        return player != null && !player.isFolded() &&
                player.getBalance() > (game.getCurrentBet() - player.getCurrentBet());
    }

    public boolean canCheck() {
        Player player = getCurrentPlayerObject();
        return player != null && !player.isFolded() &&
                player.getCurrentBet() >= game.getCurrentBet();
    }

    public int getCallAmount() {
        Player player = getCurrentPlayerObject();
        return player != null ? Math.min(game.getCurrentBet() - player.getCurrentBet(), player.getBalance()) : 0;
    }

    public int getMinRaise() {
        return game.getCurrentBet() * 2;
    }

    public int getMaxRaise() {
        Player player = getCurrentPlayerObject();
        return player != null ? player.getBalance() : 0;
    }

    // Aktualizacja widoku
    private void updateView() {
        if (view != null) {
            Platform.runLater(() -> {
                view.updateGameState(game);
                view.updatePlayerActions(isCurrentPlayerTurn());
            });
        }

        if (serverComm != null) {
            serverComm.sendGameState(game.getGameState());
        }
    }

    // Gettery dla widoku
    public PokerGame getGame() {
        return game;
    }

    public List<Player> getPlayers() {
        return game.getPlayers();
    }

    public List<Card> getCommunityCards() {
        return game.getCommunityCards();
    }

    public int getPot() {
        return game.getPot();
    }
    public int getBalance(){
        Player player = getCurrentPlayerObject();
        return player != null ? player.getBalance() : 0;
    }

    public PokerGame.GameState getGameState() {
        return game.getGameState();
    }

    public String getCurrentPlayerId() {return currentPlayerId;}

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}