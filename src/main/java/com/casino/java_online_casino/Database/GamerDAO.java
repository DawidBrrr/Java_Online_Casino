package com.casino.java_online_casino.Database;

import com.casino.java_online_casino.Connection.Server.ServerConfig;
import com.casino.java_online_casino.User.Gamer;

import java.sql.*;

public class GamerDAO {
    private static GamerDAO instance;
    private static Connection connection;

    private GamerDAO() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    ServerConfig.getDbUrl(),
                    ServerConfig.getDbUser(),
                    ServerConfig.getDbPassword()
            );
            System.out.println("[GamerDAO] Połączono z bazą danych.");
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException("[GamerDAO] Błąd połączenia z bazą: " + e.getMessage());
        }
    }

    public static GamerDAO getInstance() {
        if (instance == null) {
            synchronized (GamerDAO.class) {
                if (instance == null) {
                    instance = new GamerDAO();
                }
            }
        }
        return instance;
    }

    // Rejestracja nowego gracza
    public static boolean register(Gamer gamer) {
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
            System.err.println("[GamerDAO] Błąd rejestracji gracza: " + e.getMessage());
            return false;
        }
    }

    // Logowanie gracza (po emailu i haśle)
    public static Gamer login(String email, String password) {
        String sql = "SELECT * FROM gamers WHERE email = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToGamer(rs);
            }
        } catch (SQLException e) {
            System.err.println("[GamerDAO] Błąd logowania: " + e.getMessage());
        }
        return null;
    }

    // Pobieranie gracza po emailu
    public static Gamer findByEmail(String email) {
        String sql = "SELECT * FROM gamers WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToGamer(rs);
            }
        } catch (SQLException e) {
            System.err.println("[GamerDAO] Błąd pobierania gracza po emailu: " + e.getMessage());
        }
        return null;
    }

    // Pobieranie gracza po ID
    public static Gamer findById(int userId) {
        String sql = "SELECT * FROM gamers WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToGamer(rs);
            }
        } catch (SQLException e) {
            System.err.println("[GamerDAO] Błąd pobierania gracza po ID: " + e.getMessage());
        }
        return null;
    }

    // Aktualizacja kredytów gracza
    public static boolean updateCredits(int userId, float newCredits) {
        String sql = "UPDATE gamers SET credits = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setFloat(1, newCredits);
            stmt.setInt(2, userId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[GamerDAO] Błąd przy aktualizacji kredytów: " + e.getMessage());
            return false;
        }
    }

    private static Gamer mapResultSetToGamer(ResultSet rs) throws SQLException {
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

    public static void close() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            System.err.println("[GamerDAO] Błąd podczas zamykania połączenia: " + e.getMessage());
        }
    }
}
