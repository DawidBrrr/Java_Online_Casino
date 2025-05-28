module com.casino.java_online_casino {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires jdk.httpserver;
    requires jjwt.api;
    requires com.google.gson;
    requires java.rmi;
    requires java.sql;

    opens com.casino.java_online_casino.controllers to javafx.fxml;
    opens com.casino.java_online_casino to javafx.fxml;
    opens com.casino.java_online_casino.User to com.google.gson;


    exports com.casino.java_online_casino;
    exports com.casino.java_online_casino.controllers;
    exports com.casino.java_online_casino.User;
    exports com.casino.java_online_casino.games.slots.controller;
    exports com.casino.java_online_casino.games.blackjack.gui;

    opens com.casino.java_online_casino.games.slots.controller to javafx.fxml;
    opens com.casino.java_online_casino.games.blackjack.controller;
    opens com.casino.java_online_casino.games.blackjack.gui to javafx.fxml;
    opens com.casino.java_online_casino.Connection.Server.GameServer to com.google.gson;
    opens com.casino.java_online_casino.Connection.Server.DTO to com.google.gson;
}