package com.casino.java_online_casino.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class RankingsController {
    @FXML private TableView<RankingEntry> rankingTable;
    @FXML private TableColumn<RankingEntry, String> userColumn;
    @FXML private TableColumn<RankingEntry, Integer> bjWinColumn;
    @FXML private TableColumn<RankingEntry, Integer> pokerWinColumn;

    @FXML
    public void initialize() {
        userColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        bjWinColumn.setCellValueFactory(new PropertyValueFactory<>("blackjackWins"));
        pokerWinColumn.setCellValueFactory(new PropertyValueFactory<>("pokerWins"));

        ObservableList<RankingEntry> rankings = FXCollections.observableArrayList();

        try {
            URL url = new URL("http://localhost:12346/api/stats/ranking");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            StringBuilder sb = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()) {
                sb.append(scanner.nextLine());
            }
            scanner.close();

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonArray arr = json.getAsJsonArray("ranking");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                String user = obj.has("nickname") ? obj.get("nickname").getAsString() : String.valueOf(obj.get("user_id").getAsInt());                int bjWins = obj.get("blackjack_wins").getAsInt();
                int pokerWins = obj.get("poker_wins").getAsInt();
                rankings.add(new RankingEntry(user, bjWins, pokerWins));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        rankingTable.setItems(rankings);
    }

    public static class RankingEntry {
        private final String userId;
        private final int blackjackWins;
        private final int pokerWins;

        public RankingEntry(String userId, int blackjackWins, int pokerWins) {
            this.userId = userId;
            this.blackjackWins = blackjackWins;
            this.pokerWins = pokerWins;
        }
        public String getUserId() { return userId; }
        public int getBlackjackWins() { return blackjackWins; }
        public int getPokerWins() { return pokerWins; }
    }

    @FXML
    private void handleBack() {
        rankingTable.getScene().getWindow().hide();
    }
}
