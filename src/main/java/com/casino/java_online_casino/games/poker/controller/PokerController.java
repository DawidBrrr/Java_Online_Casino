package com.casino.java_online_casino.games.poker.controller;

import com.casino.java_online_casino.Connection.Games.Game;
import com.casino.java_online_casino.Connection.Server.Rooms.Room;
import com.casino.java_online_casino.games.poker.model.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
//
///**
// * PokerController zarządza logiką gry pokera w kontekście pokoju.
// * Implementuje interfejs Game, zapewniając obsługę dołączania, opuszczania, sprawdzania statusu gry.
// * Synchronizacja zapewnia bezpieczeństwo w środowisku wielowątkowym.
// */
//public class PokerController implements Game {
//    private final Room room;
//    private final PokerGame pokerGame;
//    private final ReentrantLock lock = new ReentrantLock();
//
//    public PokerController(Room room, int maxPlayers) {
//        this.room = room;
//        this.pokerGame = new PokerGame(maxPlayers);
//    }
//
//    @Override
//    public void onPlayerJoin(String userId) {
//        lock.lock();
//        try {
//            // Sprawdzamy czy gracz jest już w grze
//            if (containsPlayer(userId)) return;
//            // Pobierz gracza z pokoju
//            Player player = room.getPlayer(userId);
//            if (player != null && canJoin(userId)) {
//                pokerGame.addPlayer(player);
//                // Automatycznie uruchom grę jeśli jest wystarczająca liczba graczy
//                if (pokerGame.getPlayers().size() >= 2 && !isInProgress()) {
//                    pokerGame.startNewHand();
//                    room.notifyGameStart(); // powiadom wszystkich graczy
//                }
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    @Override
//    public void onPlayerLeave(String userId) {
//        lock.lock();
//        try {
//            // Usuwamy gracza z gry
//            pokerGame.removePlayer(userId);
//            // Jeśli jest to aktualny gracz, to kończymy jego turę lub kończymy grę
//            if (pokerGame.isPlayerTurn(userId)) {
//                pokerGame.advanceTurn();
//            }
//            // Jeśli zostali tylko jeden lub zero graczy, kończymy grę
//            if (pokerGame.getPlayers().size() < 2) {
//                pokerGame.endGame();
//                room.notifyGameCanceled(); // powiadom wszystkich
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    @Override
//    public boolean isInProgress() {
//        lock.lock();
//        try {
//            return pokerGame.getGameState() == PokerGame.GameState.IN_PROGRESS;
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    @Override
//    public boolean canJoin(String userId) {
//        lock.lock();
//        try {
//            return (pokerGame.getGameState() == PokerGame.GameState.WAITING_FOR_PLAYERS ||
//                    pokerGame.getGameState() == PokerGame.GameState.PRE_FLOP ||
//                    pokerGame.getGameState() == PokerGame.GameState.FLOP ||
//                    pokerGame.getGameState() == PokerGame.GameState.TURN ||
//                    pokerGame.getGameState() == PokerGame.GameState.RIVER)
//                    && !containsPlayer(userId)
//                    && pokerGame.getPlayers().size() < pokerGame.getMaxPlayers();
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public boolean addPlayer(Player player) {
//        lock.lock();
//        try {
//            boolean added = pokerGame.addPlayer(player);
//            // Jeśli po dodaniu jest wystarczająca liczba graczy, rozpocznij grę
//            if (added && pokerGame.getPlayers().size() >= 2 && !isInProgress()) {
//                pokerGame.startNewHand();
//                room.notifyGameStart();
//            }
//            return added;
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public void removePlayer(String playerId) {
//        lock.lock();
//        try {
//            pokerGame.removePlayer(playerId);
//            if (pokerGame.getPlayers().size() < 2) {
//                pokerGame.endGame();
//                room.notifyGameCanceled();
//            } else if (pokerGame.isPlayerTurn(playerId)) {
//                pokerGame.advanceTurn();
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public void startNewHand() {
//        lock.lock();
//        try {
//            pokerGame.startNewHand();
//            room.notifyGameStart();
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public boolean processPlayerAction(String playerId, Player.playerAction action, int amount) {
//        lock.lock();
//        try {
//            boolean result = pokerGame.processPlayerAction(playerId, action, amount);
//            if (pokerGame.getGameState() == PokerGame.GameState.SHOWDOWN || pokerGame.getGameState() == PokerGame.GameState.GAME_OVER) {
//                // После шоудауна или окончания игры, можно запустить новую руку или завершить
//                // В зависимости od логики, например:
//                // pokerGame.startNewHand();
//            }
//            return result;
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public boolean isPlayerTurn(String playerId) {
//        lock.lock();
//        try {
//            return pokerGame.isPlayerTurn(playerId);
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public Player getCurrentPlayer() {
//        lock.lock();
//        try {
//            return pokerGame.getCurrentPlayer();
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public List<Player> getPlayers() {
//        lock.lock();
//        try {
//            return pokerGame.getPlayers();
//        } finally {
//            lock.unlock();
//        }
//    }
//    public Player getPlayer(String playerId) {
//        return getPlayers().stream().filter(player -> player.getId().equals(playerId)).findFirst().orElse(null);
//    }
//
//    public boolean containsPlayer(String userId) {
//        lock.lock();
//        try {
//            return pokerGame.getPlayers().stream()
//                    .anyMatch(p -> Objects.equals(p.getId(), userId));
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public int getMaxPlayers() {
//        return pokerGame.getMaxPlayers();
//    }
//
//    public PokerGame.GameState getGameState() {
//        lock.lock();
//        try {
//            return pokerGame.getGameState();
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public Object getCurrentShift(String userId) {
//        return isPlayerTurn(userId);
//    }
//
//    public Player getWinner() {
//        lock.lock();
//        try {
//            return pokerGame.getWinner(); // POPRAWKA: dodano .getWinner()
//        } finally {
//            lock.unlock();
//        }
//    }
//
//
//
//
//}
