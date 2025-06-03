package com.casino.java_online_casino.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class RankingsController {
    @FXML private TableView<?> rankingTable;
    @FXML private TableColumn<?, ?> userColumn;
    @FXML private TableColumn<?, ?> bjWinColumn;
    @FXML private TableColumn<?, ?> pokerWinColumn;

    @FXML
    private void handleBack() {
        // Zamknij okno lub przełącz widok na dashboard (do uzupełnienia później)
        rankingTable.getScene().getWindow().hide();
    }
}
