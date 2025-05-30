package com.casino.java_online_casino.Connection.Server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ServerConfig {

    private static final String GAME_SERVER_HOST = "serverHost";
    private static final String GAME_SERVER_PORT = "serverPort";
    private static final String API_SERVER_HOST = "ApiHost";
    private static final String API_SERVER_PORT = "ApiPort";
    private static final String CONFIG_PATH = "src/main/resources/com/casino/Connection/server_connections_config.properties";
    private static final  Properties properties = new Properties();
    static {
        try(InputStream inputStream = Files.newInputStream(Path.of(CONFIG_PATH))) {
            properties.load(inputStream);
        }catch (IOException e) {
            throw new RuntimeException("Problem z ładowaniem konfiguracji servera");
        }
    }

    public static String getGameServerHost() {
        return properties.getProperty(GAME_SERVER_HOST);
    }

    public static int getGameServerPort() {
         return Integer.parseInt(properties.getProperty(GAME_SERVER_PORT));
    }

    public static String getApiServerHost() {
         return properties.getProperty(API_SERVER_HOST);
    }

    public static int getApiServerPort() {
         return Integer.parseInt(properties.getProperty(API_SERVER_PORT));
    }
    public static String getApiPath() {
        return "http://"+getApiServerHost()+":"+getApiServerPort()+"/";
    }
    public static String getDbUrl() {
        return properties.getProperty("db.url");
    }
    public static String getDbUser() {
        return properties.getProperty("db.user");
    }
    public static String getDbPassword() {
        return properties.getProperty("db.password");
    }
}

