package com.casino.java_online_casino.Connection.Utils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void logToFile(String message) {
        LocalDateTime now = LocalDateTime.now();
        String serverName = Thread.currentThread().getName().replace("-Thread", "");
        String fileName = String.format("%s/%s_%s.txt", LOG_DIR, serverName, DATE_FORMATTER.format(now));

        File directory = new File(LOG_DIR);
        if (!directory.exists()) {
            directory.mkdir();
        }

        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            String logEntry = String.format("[%s] %s: %s",
                    TIME_FORMATTER.format(now),
                    serverName,
                    message);

            out.println(logEntry);
        } catch (IOException e) {
            System.err.println("Błąd podczas zapisywania logu: " + e.getMessage());
        }
    }
}