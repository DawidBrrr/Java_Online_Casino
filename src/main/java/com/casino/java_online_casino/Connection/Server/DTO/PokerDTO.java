package com.casino.java_online_casino.Connection.Server.DTO;

import com.casino.java_online_casino.Connection.Server.Rooms.Room;
import com.casino.java_online_casino.games.poker.model.PokerGame;
import com.casino.java_online_casino.games.poker.model.Card;
import com.casino.java_online_casino.games.poker.model.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PokerDTO {
    public String roomId;
    public PokerGame.GameState gameState;
    public int pot;
    public int currentBet;
    public List<CardDTO> communityCards;
    public Map<String, PokerPlayerDTO> players;
    public String activePlayerId;
    public int minimumBet;
    public boolean isGameOver;
    public String winnerId;
    public boolean isStarted;
    public boolean isInProgress;
    public List<String> playerActions;
    public int smallBlind;
    public int bigBlind;
    public int dealerIndex;
    public String dealerId;
    public String smallBlindId;
    public String bigBlindId;

    // DTO pojedynczej karty
    public static class CardDTO {
        public Card.Suit suit;
        public Card.Rank rank;
        public String imagePath;
        public int value;
        public boolean isHidden;

        public static CardDTO fromCard(Card card, boolean hidden) {
            CardDTO dto = new CardDTO();
            if (hidden) {
                dto.suit = null;
                dto.rank = null;
                dto.imagePath = "card_back.png";
                dto.value = 0;
                dto.isHidden = true;
            } else {
                dto.suit = card.getSuit();
                dto.rank = card.getRank();
                dto.imagePath = card.getImageName();
                dto.value = card.getRank().getValue();
                dto.isHidden = false;
            }
            return dto;
        }
    }

    // DTO gracza
    public static class PokerPlayerDTO {
        public String id;
        public String name;
        public float balance;
        public float currentBet;
        public List<CardDTO> hand;
        public String status;
        public boolean isDealer;
        public boolean isSmallBlind;
        public boolean isBigBlind;
        public boolean isActive;
        public String lastAction;
        public boolean canAct;
        public int handSize;
        public Player playerRef;

        public static PokerPlayerDTO fromPlayer(Player player, Room room, boolean showCards) {
            PokerPlayerDTO dto = new PokerPlayerDTO();
            dto.id = player.getId();
            dto.name = player.getName();
            dto.balance = player.getBalance();
            dto.currentBet = player.getCurrentBet();
            dto.handSize = player.getHand().size();
            dto.playerRef = player;

            dto.hand = player.getHand().stream()
                    .map(card -> CardDTO.fromCard(card, !showCards))
                    .collect(Collectors.toList());

            if (player.isFolded()) {
                dto.status = "folded";
            } else if (player.isAllIn()) {
                dto.status = "all_in";
            } else {
                dto.status = "active";
            }

            PokerGame game = room.getPokerGame();
            List<Player> allPlayers = game.getPlayers();
            int dealerIdx = game.getDealerIndex();

            dto.isDealer = allPlayers.size() > 0 && allPlayers.get(dealerIdx).getId().equals(player.getId());
            dto.isSmallBlind = allPlayers.size() > 1 && allPlayers.get((dealerIdx + 1) % allPlayers.size()).getId().equals(player.getId());
            dto.isBigBlind = allPlayers.size() > 2 && allPlayers.get((dealerIdx + 2) % allPlayers.size()).getId().equals(player.getId());
            dto.isActive = room.isPlayerTurn(player.getId());
            dto.lastAction = player.getLastAction() != null ? player.getLastAction().toString() : null;
            dto.canAct = dto.isActive && !player.isFolded() && !player.isAllIn();

            return dto;
        }
    }

    /**
     * Tworzy bezpieczne DTO dla konkretnego gracza (tylko jego karty, reszta hidden)
     */
    public static PokerDTO fromRoomForPlayer(Room room, String playerId) {
        PokerDTO dto = new PokerDTO();
        PokerGame game = room.getPokerGame();
        dto.roomId = room.getRoomId();
        dto.gameState = game.getGameState();
        dto.pot = game.getPot();
        dto.currentBet = game.getCurrentBet();
        dto.smallBlind = game.getSmallBlind();
        dto.bigBlind = game.getBigBlind();
        dto.dealerIndex = game.getDealerIndex();

        dto.communityCards = game.getCommunityCards().stream()
                .map(card -> CardDTO.fromCard(card, false))
                .collect(Collectors.toList());

        boolean showAllCards = dto.gameState == PokerGame.GameState.SHOWDOWN
                || dto.gameState == PokerGame.GameState.GAME_OVER;

        dto.players = new HashMap<>();
        for (Player player : game.getPlayers()) {
            boolean showCards = showAllCards || player.getId().equals(playerId);
            dto.players.put(player.getId(), PokerPlayerDTO.fromPlayer(player, room, showCards));
        }

        Player currentPlayer = room.getCurrentPlayer();
        dto.activePlayerId = currentPlayer != null ? currentPlayer.getId() : null;
        dto.minimumBet = game.getCurrentBet();
        dto.isGameOver = dto.gameState == PokerGame.GameState.GAME_OVER;
        dto.isStarted = dto.gameState != PokerGame.GameState.WAITING_FOR_PLAYERS;
        dto.isInProgress = room.isInProgress();

        Player winner = game.getWinner();
        dto.winnerId = winner != null ? winner.getId() : null;

        List<Player> allPlayers = game.getPlayers();
        int dealerIdx = game.getDealerIndex();
        dto.dealerId = allPlayers.size() > dealerIdx ? allPlayers.get(dealerIdx).getId() : null;
        dto.smallBlindId = allPlayers.size() > 1 ? allPlayers.get((dealerIdx + 1) % allPlayers.size()).getId() : null;
        dto.bigBlindId = allPlayers.size() > 2 ? allPlayers.get((dealerIdx + 2) % allPlayers.size()).getId() : null;

        if (playerId != null && playerId.equals(dto.activePlayerId)) {
            dto.playerActions = Arrays.asList("FOLD", "CALL", "RAISE", "CHECK", "ALL_IN");
        } else {
            dto.playerActions = new ArrayList<>();
        }

        return dto;
    }

    /**
     * DTO dla admina - pokazuje wszystkie karty
     */
    public static PokerDTO fromRoomForAdmin(Room room) {
        PokerGame game = room.getPokerGame();
        PokerDTO dto = fromRoomForPlayer(room, null);
        // Ustaw showAllCards na true dla wszystkich graczy
        dto.players.values().forEach(playerDTO -> {
            playerDTO.hand = playerDTO.playerRef.getHand().stream()
                    .map(card -> CardDTO.fromCard(card, false))
                    .collect(Collectors.toList());
        });
        return dto;
    }
}
