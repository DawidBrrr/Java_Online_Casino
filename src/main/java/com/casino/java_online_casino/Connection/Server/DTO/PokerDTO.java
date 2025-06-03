package com.casino.java_online_casino.Connection.Server.DTO;

import com.casino.java_online_casino.games.poker.controller.PokerController;
import com.casino.java_online_casino.games.poker.model.PokerGame;
import com.casino.java_online_casino.games.poker.model.Card;

import java.util.List;
import java.util.Map;
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

    public static class CardDTO {
        public Card.Suit suit;
        public Card.Rank rank;
        public String imagePath;
    }

    public static class PokerPlayerDTO {
        public String id;
        public String name;
        public int balance;
        public int currentBet;
        public List<CardDTO> hand;
        public String status;  // zmienione na String
        public boolean isDealer;
        public boolean isSmallBlind;
        public boolean isBigBlind;
    }

    public static PokerDTO fromController(PokerController controller) {
        PokerDTO dto = new PokerDTO();
        dto.gameState = controller.getGameState();
        dto.pot = controller.getPot();
        dto.currentBet = controller.getGame().getCurrentBet();
        dto.communityCards = controller.getCommunityCards().stream()
            .map(card -> {
                CardDTO cardDTO = new CardDTO();
                cardDTO.suit = card.getSuit();
                cardDTO.rank = card.getRank();
                //cardDTO.imagePath = card.getImagePath(); CZEMU DO CHOLERY NIE WIDZI FUNKCJI
                return cardDTO;
            }).toList();
        dto.players = controller.getPlayers().stream()
            .collect(Collectors.toMap(
                player -> player.getId(),
                player -> {
                    PokerPlayerDTO playerDTO = new PokerPlayerDTO();
                    playerDTO.id = player.getId();
                    playerDTO.name = player.getName();
                    playerDTO.balance = player.getBalance();
                    playerDTO.currentBet = player.getCurrentBet();
                    playerDTO.hand = player.getHand().stream()
                        .map(card -> {
                            CardDTO cardDTO = new CardDTO();
                            cardDTO.suit = card.getSuit();
                            cardDTO.rank = card.getRank();
                            //cardDTO.imagePath = card.getImagePath(); CZEMU DO CHOLERY NIE WIDZI FUNKCJI
                            return cardDTO;
                        }).toList();
                    playerDTO.status = player.isFolded() ? "folded" : (player.isAllIn() ? "all_in" : "active");
                    //playerDTO.isDealer = controller.isDealer(player);
                   // playerDTO.isSmallBlind = controller.isSmallBlind(player);
                   // playerDTO.isBigBlind = controller.isBigBlind(player); CZAT PODPOWIADA NIE MAM POJĘCIA CO TO NIE ZNAM ZASAD POKERA
                    return playerDTO;
                }
            ));
        dto.activePlayerId = controller.getCurrentPlayerId();
        dto.minimumBet = controller.getMinRaise();
        dto.isGameOver = controller.getGameState() == PokerGame.GameState.GAME_OVER;
        dto.winnerId = controller.getGame().getActivePlayers().stream()
            .filter(player -> !player.isFolded())
            .findFirst()
            .map(player -> player.getId())
            .orElse(null);


        return dto;
    }
    public static PokerDTO createGameStartDTO(String roomId) {
        PokerDTO dto = new PokerDTO();
        dto.roomId = roomId;
        dto.gameState = PokerGame.GameState.DEALING;
        dto.isStarted = true;
        dto.isInProgress = true;
        dto.pot = 0;
        dto.currentBet = 0;
        dto.minimumBet = 10; // Domyślna wartość małej ciemnej
        dto.communityCards = List.of();
        dto.players = Map.of();
        return dto;
    }

    // Metoda do aktualizacji statusu gracza
    public void updatePlayerStatus(String playerId, String status) {
        if (players != null && players.containsKey(playerId)) {
            players.get(playerId).status = status;
        }
    }
}