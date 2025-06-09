package com.casino.java_online_casino.Connection.Server.Rooms;

import com.casino.java_online_casino.Connection.Games.Game;
import com.casino.java_online_casino.Connection.Server.DTO.PokerDTO;
import com.casino.java_online_casino.Database.GamerDAO;
import com.casino.java_online_casino.games.poker.model.PokerGame;
import com.casino.java_online_casino.games.poker.model.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class Room implements Game {
    private final String roomId;
    private final int maxPlayers;
    private final LinkedHashMap<String, Player> players = new LinkedHashMap<>();
    private final PokerGame pokerGame;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService periodicNotifier = Executors.newSingleThreadScheduledExecutor();
    private final Lock lock = new ReentrantLock();
    private volatile boolean active = true;
    private volatile boolean gameInProgress = false;
    private final Map<String, CompletableFuture<Player.playerAction>> actionFutures = new ConcurrentHashMap<>();
    private RoomEventListener eventListener;
    private volatile boolean notifierRunning = false;
    private Iterator<String> turnIterator;
    private String currentPlayerId;
    private String dealerId;

    public Room(String roomId, int maxPlayers) {
        this.roomId = roomId;
        this.maxPlayers = maxPlayers;
        this.pokerGame = new PokerGame(maxPlayers);
        System.out.println("[DEBUG ROOM " + roomId + "] Utworzono nowy pokój z maxPlayers=" + maxPlayers);
    }

    // --- Game interface ---
    @Override
    public void onPlayerJoin(String userId) {
        lock.lock();
        try {
            if (players.containsKey(userId)) {
                System.out.println("[DEBUG ROOM " + roomId + "] Gracz RECONNECT id=" + userId);
                // Można pobrać dane z GamerDAO i zaktualizować Player
            } else if (canJoin(userId)) {
                // Pobierz dane gracza z GamerDAO jeśli potrzebujesz
                Player player = new Player(userId);
                players.put(userId, player);
                pokerGame.addPlayer(player);
                System.out.println("[DEBUG ROOM " + roomId + "] Gracz DOŁĄCZYŁ id=" + userId + ", obecnych=" + players.size());
            } else {
                System.out.println("[DEBUG ROOM " + roomId + "] NIE MOŻNA DOŁĄCZYĆ gracza id=" + userId + " (pełny pokój lub gra w toku)");
                return;
            }

            if (players.size() >= 2 && !gameInProgress) {
                System.out.println("[DEBUG ROOM " + roomId + "] Wystarczająca liczba graczy, START gry.");
                startGame();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onPlayerLeave(String userId) {
        lock.lock();
        try {
            removePlayer(userId);
            System.out.println("[DEBUG ROOM " + roomId + "] Gracz OPUŚCIŁ pokój id=" + userId + ", obecnych=" + players.size());
            if (players.size() < 2) {
                System.out.println("[DEBUG ROOM " + roomId + "] Za mało graczy, STOP gry.");
                stopGame();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isInProgress() {
        lock.lock();
        try {
            System.out.println("[DEBUG ROOM " + roomId + "] Sprawdzam czy gra w toku: " + gameInProgress);
            return gameInProgress;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean canJoin(String userId) {
        lock.lock();
        try {
            boolean can = !players.containsKey(userId) && players.size() < maxPlayers && !gameInProgress;
            System.out.println("[DEBUG ROOM " + roomId + "] canJoin(" + userId + "): " + can);
            return can;
        } finally {
            lock.unlock();
        }
    }

    public boolean addPlayer(Player player) {
        lock.lock();
        try {
            if (players.size() >= maxPlayers) {
                System.out.println("[DEBUG ROOM " + roomId + "] NIE dodano gracza " + player.getId() + " - pokój pełny.");
                return false;
            }
            if (players.containsKey(player.getId())) {
                System.out.println("[DEBUG ROOM " + roomId + "] NIE dodano gracza " + player.getId() + " - już w pokoju.");
                return false;
            }
            players.put(player.getId(), player);
            pokerGame.addPlayer(player);
            System.out.println("[DEBUG ROOM " + roomId + "] Dodano gracza " + player.getId() + ", obecnych=" + players.size());
            if (players.size() >= 2 && !gameInProgress) {
                startGame();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void removePlayer(String playerId) {
        lock.lock();
        try {
            players.remove(playerId);
            pokerGame.removePlayer(playerId);
            System.out.println("[DEBUG ROOM " + roomId + "] Usunięto gracza " + playerId + ", obecnych=" + players.size());
            if (players.isEmpty()) {
                active = false;
                gameInProgress = false;
                System.out.println("[DEBUG ROOM " + roomId + "] Pokój pusty, dezaktywacja.");
            }
            if (Objects.equals(currentPlayerId, playerId)) {
                nextTurn();
            }
        } finally {
            lock.unlock();
        }
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players.values());
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public Player getCurrentPlayer() {
        return currentPlayerId != null ? getPlayer(currentPlayerId) : null;
    }

    public boolean isPlayerTurn(String playerId) {
        boolean turn = Objects.equals(getCurrentPlayerId(), playerId);
        System.out.println("[DEBUG ROOM " + roomId + "] isPlayerTurn(" + playerId + "): " + turn);
        return turn;
    }

    public void nextTurn() {
        lock.lock();
        try {
            if (players.isEmpty()) {
                currentPlayerId = null;
                return;
            }
            if (turnIterator == null || !turnIterator.hasNext()) {
                turnIterator = players.keySet().iterator();
            }
            if (turnIterator.hasNext()) {
                currentPlayerId = turnIterator.next();
            } else {
                turnIterator = players.keySet().iterator();
                currentPlayerId = turnIterator.hasNext() ? turnIterator.next() : null;
            }
            System.out.println("[DEBUG ROOM " + roomId + "] Nowy aktywny gracz: " + currentPlayerId);
            scheduleNextTurn();
            notifyPlayerTurn();
        } finally {
            lock.unlock();
        }
    }

    public void startGame() {
        lock.lock();
        try {
            if (!gameInProgress && players.size() >= 2) {
                gameInProgress = true;
                pokerGame.startNewHand();
                System.out.println("[DEBUG ROOM " + roomId + "] Gra rozpoczęta!");
                dealerId = players.keySet().iterator().next(); // pierwszy gracz jako dealer
                currentPlayerId = dealerId;
                turnIterator = players.keySet().iterator();
                broadcastGameState();
                scheduleNextTurn();
                startPeriodicBroadcast();
            }
        } finally {
            lock.unlock();
        }
    }

    public void stopGame() {
        lock.lock();
        try {
            gameInProgress = false;
            System.out.println("[DEBUG ROOM " + roomId + "] Gra zatrzymana.");
            broadcastGameState();
        } finally {
            lock.unlock();
        }
    }

    public void startPeriodicBroadcast() {
        if (!notifierRunning) {
            notifierRunning = true;
            periodicNotifier.scheduleAtFixedRate(() -> {
                try {
                    lock.lock();
                    try {
                        if (gameInProgress && eventListener != null) {
                            System.out.println("[DEBUG ROOM " + roomId + "] [PERIODIC] Wysyłam DTO do wszystkich klientów (co 30s)");
                            eventListener.onGameStateChanged(roomId);
                        }
                    } finally {
                        lock.unlock();
                    }
                } catch (Exception e) {
                    System.out.println("[DEBUG ROOM " + roomId + "] [PERIODIC] Błąd podczas broadcastu: " + e.getMessage());
                }
            }, 30, 30, TimeUnit.SECONDS);
        }
    }

    private void scheduleNextTurn() {
        Player current = getCurrentPlayer();
        if (current == null) {
            System.out.println("[DEBUG ROOM " + roomId + "] Brak aktywnego gracza do schedulera.");
            return;
        }

        System.out.println("[DEBUG ROOM " + roomId + "] Scheduler: tura gracza " + current.getId());
        CompletableFuture<Player.playerAction> future = new CompletableFuture<>();
        actionFutures.put(current.getId(), future);

        scheduler.schedule(() -> {
            if (!future.isDone()) {
                System.out.println("[DEBUG ROOM " + roomId + "] TIMEOUT gracza " + current.getId());
                future.completeExceptionally(new TimeoutException("Timeout na ruch"));
            }
        }, 30, TimeUnit.SECONDS);

        future.whenComplete((action, ex) -> {
            lock.lock();
            try {
                if (ex != null) {
                    System.out.println("[DEBUG ROOM " + roomId + "] Timeout obsłużony dla gracza " + current.getId());
                    handlePlayerTimeout(current.getId());
                } else {
                    System.out.println("[DEBUG ROOM " + roomId + "] Akcja gracza " + current.getId() + ": " + action);
                    boolean success = processPlayerAction(current.getId(), action, 0);
                    broadcastGameState();
                    if (success) {
                        nextTurn();
                    }
                }
            } finally {
                lock.unlock();
            }
        });

        if (eventListener != null) {
            eventListener.onPlayerTurn(current.getId(), roomId);
            System.out.println("[DEBUG ROOM " + roomId + "] Powiadomiono handlera o turze gracza " + current.getId());
        }
    }

    private void handlePlayerTimeout(String playerId) {
        System.out.println("[DEBUG ROOM " + roomId + "] Usuwam gracza po TIMEOUT id=" + playerId);
        removePlayer(playerId);
        broadcastGameState();
        if (players.size() >= 2) {
            startGame();
        } else {
            stopGame();
        }
    }

    public boolean handlePlayerAction(String playerId, Player.playerAction action) {
        CompletableFuture<Player.playerAction> future = actionFutures.get(playerId);
        if (future != null && !future.isDone()) {
            System.out.println("[DEBUG ROOM " + roomId + "] Otrzymano akcję od gracza " + playerId + ": " + action);
            future.complete(action);
            return true;
        }
        System.out.println("[DEBUG ROOM " + roomId + "] Ignoruję akcję od gracza " + playerId + ": " + action + " (nie jego tura lub już wykonano)");
        return false;
    }

    public boolean processPlayerAction(String playerId, Player.playerAction action, int amount) {
        lock.lock();
        try {
            if (!isPlayerTurn(playerId)) {
                System.out.println("[DEBUG ROOM " + roomId + "] ODRZUCONO akcję gracza " + playerId + ": nie jego tura.");
                return false;
            }
            boolean result = pokerGame.processPlayerAction(playerId, action, amount);
            System.out.println("[DEBUG ROOM " + roomId + "] Wykonano akcję " + action + " dla gracza " + playerId + ", result=" + result);
            if (result) {
                nextTurn();
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    // --- DTO, eventListener, gettery ---

    public PokerGame getPokerGame() {
        return pokerGame;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isGameInProgress() {
        return gameInProgress;
    }

    public boolean isActive() {
        return active;
    }

    public void setEventListener(RoomEventListener listener) {
        this.eventListener = listener;
    }

    public PokerDTO createPokerDTOByUserId(String userId) {
        PokerDTO dto = PokerDTO.fromRoomForPlayer(this, userId);
        dto.yourUserId = userId; // Ustawiamy jawnie pole yourUserId (jeśli nie robi tego już fromRoomForPlayer)
        System.out.println("[DEBUG ROOM " + roomId + "] [DTO] Wygenerowano PokerDTO dla userId=" + userId);
        return dto;
    }

    public PokerDTO createPokerDTOAll() {
        PokerDTO dto = PokerDTO.fromRoomForAdmin(this);
        dto.yourUserId = null; // lub ustaw na admina, jeśli chcesz
        System.out.println("[DEBUG ROOM " + roomId + "] [DTO] Wygenerowano PokerDTO dla ADMIN/SHOWDOWN");
        return dto;
    }

    public void notifyPlayerTurn() {
        if (eventListener != null) {
            eventListener.onPlayerTurn(getCurrentPlayerId(), roomId);
        }
    }

    public void broadcastGameState() {
        if (eventListener != null) {
            System.out.println("[DEBUG ROOM " + roomId + "] [BROADCAST] Wysyłam DTO do handlerów/klientów.");
            eventListener.onGameStateChanged(roomId);
        }
    }

    public interface RoomEventListener {
        void onPlayerTurn(String playerId, String roomId);
        void onGameStateChanged(String roomId);
    }

    public String getRoomId() {
        return roomId;
    }

    public String getDealerId() {
        return dealerId;
    }
}
