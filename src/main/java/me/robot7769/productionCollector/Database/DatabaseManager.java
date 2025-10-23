package me.robot7769.productionCollector.Database;

import me.robot7769.productionCollector.ProductionCollector;
import me.robot7769.productionCollector.Utils.Logger;
import org.bukkit.Material;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {
    private final ProductionCollector plugin;
    private final Logger logger;
    private Connection connection;
    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private final String jdbcUrl;

    public DatabaseManager(ProductionCollector plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.host = plugin.getConfig().getString("database_address", "mysql://172.18.0.1");
        this.database = plugin.getConfig().getString("database_name", "event_production");
        this.username = plugin.getConfig().getString("database_user", "eventer_DB");
        this.password = plugin.getConfig().getString("database_password", "");

        // Parse host URL and prepare JDBC URL with connection pooling settings
        String cleanHost = host.replace("mysql://", "");
        this.jdbcUrl = "jdbc:mysql://" + cleanHost + ":3306/" + database
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&autoReconnect=true"
            + "&maxReconnects=3"
            + "&initialTimeout=2"
            + "&testOnBorrow=true"
            + "&validationQuery=SELECT 1";
    }

    public void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            connection = DriverManager.getConnection(jdbcUrl, username, password);
            logger.info("Successfully connected to database!");

            createTables();
        } catch (SQLException e) {
            logger.error("Failed to connect to database", e);
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }

    private void createTables() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS production_logs (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_name VARCHAR(16) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                material VARCHAR(50) NOT NULL,
                amount INT NOT NULL,
                score_value INT NOT NULL,
                total_score INT NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_player_uuid (player_uuid),
                INDEX idx_timestamp (timestamp),
                INDEX idx_material (material)
            )
        """;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableSQL);
            logger.info("Database table 'production_logs' ready.");
        } catch (SQLException e) {
            logger.error("Failed to create table", e);
        }
    }

    /**
     * Zkontroluje platnost připojení a případně ho obnoví
     */
    private void ensureConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                logger.info("Database connection lost, reconnecting...");
                connection = DriverManager.getConnection(jdbcUrl, username, password);
                logger.info("Database connection restored!");
            }
        } catch (SQLException e) {
            logger.error("Failed to ensure database connection", e);
        }
    }

    public void logProduction(String playerName, String playerUUID, Material material, int amount, int scoreValue, int totalScore) {
        ensureConnection();
        String insertSQL = "INSERT INTO production_logs (player_name, player_uuid, material, amount, score_value, total_score) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            statement.setString(1, playerName);
            statement.setString(2, playerUUID);
            statement.setString(3, material.name());
            statement.setInt(4, amount);
            statement.setInt(5, scoreValue);
            statement.setInt(6, totalScore);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Failed to log production to database: " + e.getMessage());
        }
    }

    public Map<Material, Integer> getPlayerProductionCounts(String playerUUID) {
        ensureConnection();
        Map<Material, Integer> counts = new HashMap<>();
        String query = "SELECT material, SUM(amount) as total FROM production_logs WHERE player_uuid = ? GROUP BY material";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                try {
                    Material material = Material.valueOf(rs.getString("material"));
                    int total = rs.getInt("total");
                    counts.put(material, total);
                } catch (IllegalArgumentException e) {
                    // Skip invalid materials
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to get player production counts: " + e.getMessage());
        }

        return counts;
    }

    public int getPlayerTotalScore(String playerUUID) {
        ensureConnection();
        String query = "SELECT SUM(total_score) as total FROM production_logs WHERE player_uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            logger.warning("Failed to get player total score: " + e.getMessage());
        }

        return 0;
    }

    public int getPlayerRank(String playerUUID) {
        ensureConnection();
        String query = """
            SELECT COUNT(*) + 1 as rank
            FROM (
                SELECT player_uuid, SUM(total_score) as total
                FROM production_logs
                GROUP BY player_uuid
            ) as scores
            WHERE total > (
                SELECT SUM(total_score)
                FROM production_logs
                WHERE player_uuid = ?
            )
        """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return rs.getInt("rank");
            }
        } catch (SQLException e) {
            logger.warning("Failed to get player rank: " + e.getMessage());
        }

        return 0;
    }

    public String getRankRange(int rank) {
        if (rank >= 1 && rank <= 10) return "1-10";
        if (rank >= 11 && rank <= 20) return "11-20";
        if (rank >= 21 && rank <= 30) return "21-30";
        if (rank >= 31 && rank <= 40) return "31-40";
        return "40+";
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
