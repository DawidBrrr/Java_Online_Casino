package com.casino.java_online_casino.Database;

import com.casino.java_online_casino.User.Gamer;

import java.sql.*;
import java.util.Date;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/casino"; // <-- dostosuj
    private static final String USER = "root"; // <-- dostosuj
    private static final String PASSWORD = ""; // <-- dostosuj

    private Connection connection;

    public DatabaseManager() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        System.out.println("Połączenie z bazą danych nawiązane.");
    }

    // Tworzenie nowego użytkownika
    public boolean createGamer(Gamer gamer) {
        String sql = "INSERT INTO gamers (name, last_name, nickname, email, password, date_of_birth, credits) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, gamer.getName());
            stmt.setString(2, gamer.getLastName());
            stmt.setString(3, gamer.getNickName());
            stmt.setString(4, gamer.getEmail());
            stmt.setString(5, gamer.getPassword());
            stmt.setDate(6, new java.sql.Date(gamer.getDateOfBirth().getTime()));
            stmt.setFloat(7, gamer.getCredits());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Błąd przy dodawaniu gracza: " + e.getMessage());
            return false;
        }
    }

    // Pobieranie gracza po e-mailu i haśle (logowanie)
    public Gamer getGamer(String email, String password) {
        String sql = "SELECT * FROM gamers WHERE email = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Gamer(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("last_name"),
                        rs.getString("nickname"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getFloat("credits"),
                        rs.getDate("date_of_birth")
                );
            }
        } catch (SQLException e) {
            System.err.println("Błąd podczas pobierania gracza: " + e.getMessage());
        }
        return null;
    }

    // Aktualizacja kredytów
    public boolean updateCredits(int userId, float newCredits) {
        String sql = "UPDATE gamers SET credits = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setFloat(1, newCredits);
            stmt.setInt(2, userId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Błąd przy aktualizacji kredytów: " + e.getMessage());
            return false;
        }
    }

    // Zamknięcie połączenia
    public void close() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            System.err.println("Błąd podczas zamykania połączenia: " + e.getMessage());
        }
    }
}
