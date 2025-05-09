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

    opens com.casino.java_online_casino.controllers to javafx.fxml;
    opens com.casino.java_online_casino to javafx.fxml;

    exports com.casino.java_online_casino;
    exports com.casino.java_online_casino.controllers;
}