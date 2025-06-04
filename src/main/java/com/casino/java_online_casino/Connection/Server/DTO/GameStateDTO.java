package com.casino.java_online_casino.Connection.Server.DTO;

import com.casino.java_online_casino.games.blackjack.controller.BlackJackController;
import com.casino.java_online_casino.games.blackjack.model.Card;

import java.util.List;

public class GameStateDTO {
    public List<CardDTO> playerHand;
    public List<CardDTO> dealerHand;
    public int playerScore;
    public int dealerScore;
    public boolean gameOver;
    public String result;
    public int balance;

    public static class CardDTO {
        public String suit;
        public String rank;
        public int value;
        public String imagePath;

        public CardDTO(Card card) {
            this.suit = card.getSuit();
            this.rank = card.getRank();
            this.value = card.getValue();
            this.imagePath = card.getImagePath();
        }
    }

    public static GameStateDTO fromController(BlackJackController controller) {
        GameStateDTO dto = new GameStateDTO();
        dto.playerHand = controller.getPlayerHand().stream()
                .map(c -> new CardDTO(new Card(c.getSuit(), c.getRank(), c.getValue())))
                .toList();
        dto.dealerHand = controller.getDealerHand().stream()
                .map(c -> new CardDTO(new Card(c.getSuit(), c.getRank(), c.getValue())))
                .toList();
        dto.playerScore = controller.getPlayerScore();
        dto.dealerScore = controller.getDealerScore();
        dto.gameOver = controller.isGameOver();
        dto.result = controller.getGameResult();
        dto.balance = controller.getCurrentBalance();
        return dto;
    }
}
