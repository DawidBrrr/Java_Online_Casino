package com.casino.java_online_casino.Connection.Server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerConfig {

    private static final String GAME_SERVER_HOST = "serverHost";
    private static final String GAME_SERVER_PORT = "serverPort";
    private static final String API_SERVER_HOST = "ApiHost";
    private static final String API_SERVER_PORT = "ApiPort";
    private static final String CONFIG_PATH = "/server_connections_config.properties";
    private static final  Properties properties = new Properties();
    static {
        try (InputStream input = new FileInputStream("server_connections_config.properties")) {
            properties.load(input);
            System.out.println("Loaded config from file.");
        } catch (IOException e) {
            System.err.println("Could not load config.properties: " + e.getMessage());
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

