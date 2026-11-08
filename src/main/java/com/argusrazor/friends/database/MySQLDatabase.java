package com.argusrazor.friends.database;

import com.argusrazor.friends.model.FriendPlayer;
import com.argusrazor.friends.model.PlayerSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class MySQLDatabase implements DatabaseManager {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String tablePrefix;
    private final boolean useSSL;
    private final int maxConnections;
    private final Logger logger;
    private HikariDataSource dataSource;

    public MySQLDatabase(String host, int port, String database, String username, String password,
                         String tablePrefix, boolean useSSL, int maxConnections, Logger logger) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.tablePrefix = (tablePrefix == null || tablePrefix.isEmpty()) ? "fr_" : tablePrefix;
        this.useSSL = useSSL;
        this.maxConnections = maxConnections > 0 ? maxConnections : 10;
        this.logger = logger;
    }

    @Override
    public void init() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&characterEncoding=utf8");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxConnections);
        config.setMinimumIdle(2);
        config.setPoolName("ArgusFriendsPool");

        dataSource = new HikariDataSource(config);

        try (Connection conn = getConnection(); Statement s = conn.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "players` (" +
                    "`player_id` INT NOT NULL AUTO_INCREMENT, " +
                    "`player_name` VARCHAR(16) NOT NULL, " +
                    "`player_uuid` VARCHAR(36) NOT NULL UNIQUE, " +
                    "`last_online` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY (`player_id`));");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "friend_assignment` (" +
                    "`friend1_id` INT NOT NULL, " +
                    "`friend2_id` INT NOT NULL, " +
                    "PRIMARY KEY (`friend1_id`, `friend2_id`));");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "settings` (" +
                    "`player_id` INT NOT NULL, " +
                    "`settings_id` INT NOT NULL, " +
                    "`settings_worth` INT NOT NULL, " +
                    "PRIMARY KEY (`player_id`, `settings_id`));");
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized");
        }
        return dataSource.getConnection();
    }

    private int getPlayerId(UUID uuid) {
        String sql = "SELECT player_id FROM `" + tablePrefix + "players` WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("player_id");
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL getPlayerId error: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public void savePlayer(UUID uuid, String name) {
        String sql = "INSERT INTO `" + tablePrefix + "players` (player_name, player_uuid, last_online) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), last_online = CURRENT_TIMESTAMP";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL savePlayer error: " + e.getMessage());
        }
    }

    @Override
    public FriendPlayer getPlayer(UUID uuid) {
        String sql = "SELECT player_name, last_online FROM `" + tablePrefix + "players` WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FriendPlayer(uuid, rs.getString("player_name"), rs.getTimestamp("last_online").getTime());
                }
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL getPlayer error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public FriendPlayer getPlayerByName(String name) {
        String sql = "SELECT player_uuid, last_online FROM `" + tablePrefix + "players` WHERE LOWER(player_name) = LOWER(?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    return new FriendPlayer(uuid, name, rs.getTimestamp("last_online").getTime());
                }
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL getPlayerByName error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<FriendPlayer> getFriends(UUID uuid) {
        List<FriendPlayer> list = new ArrayList<>();
        int id = getPlayerId(uuid);
        if (id == -1) return list;

        String sql = "SELECT p.player_uuid, p.player_name, p.last_online FROM `" + tablePrefix + "friend_assignment` f " +
                "JOIN `" + tablePrefix + "players` p ON " +
                "(f.friend1_id = ? AND f.friend2_id = p.player_id) OR (f.friend2_id = ? AND f.friend1_id = p.player_id)";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID fUuid = UUID.fromString(rs.getString("player_uuid"));
                    list.add(new FriendPlayer(fUuid, rs.getString("player_name"), rs.getTimestamp("last_online").getTime()));
                }
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL getFriends error: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean areFriends(UUID u1, UUID u2) {
        int id1 = getPlayerId(u1);
        int id2 = getPlayerId(u2);
        if (id1 == -1 || id2 == -1) return false;

        String sql = "SELECT 1 FROM `" + tablePrefix + "friend_assignment` WHERE " +
                "(friend1_id = ? AND friend2_id = ?) OR (friend1_id = ? AND friend2_id = ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id1);
            ps.setInt(2, id2);
            ps.setInt(3, id2);
            ps.setInt(4, id1);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL areFriends error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void addFriend(UUID u1, UUID u2) {
        int id1 = getPlayerId(u1);
        int id2 = getPlayerId(u2);
        if (id1 == -1 || id2 == -1) return;

        int min = Math.min(id1, id2);
        int max = Math.max(id1, id2);

        String sql = "INSERT IGNORE INTO `" + tablePrefix + "friend_assignment` (friend1_id, friend2_id) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, min);
            ps.setInt(2, max);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL addFriend error: " + e.getMessage());
        }
    }

    @Override
    public void removeFriend(UUID u1, UUID u2) {
        int id1 = getPlayerId(u1);
        int id2 = getPlayerId(u2);
        if (id1 == -1 || id2 == -1) return;

        String sql = "DELETE FROM `" + tablePrefix + "friend_assignment` WHERE " +
                "(friend1_id = ? AND friend2_id = ?) OR (friend1_id = ? AND friend2_id = ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id1);
            ps.setInt(2, id2);
            ps.setInt(3, id2);
            ps.setInt(4, id1);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL removeFriend error: " + e.getMessage());
        }
    }

    @Override
    public PlayerSettings getSettings(UUID uuid) {
        PlayerSettings settings = new PlayerSettings();
        int id = getPlayerId(uuid);
        if (id == -1) return settings;

        String sql = "SELECT settings_id, settings_worth FROM `" + tablePrefix + "settings` WHERE player_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int sid = rs.getInt("settings_id");
                    boolean val = rs.getInt("settings_worth") == 1;
                    if (sid == 1) settings.setAllowRequests(val);
                    if (sid == 2) settings.setAllowMessages(val);
                    if (sid == 3) settings.setNotifyJoinQuit(val);
                }
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL getSettings error: " + e.getMessage());
        }
        return settings;
    }

    @Override
    public void saveSettings(UUID uuid, PlayerSettings settings) {
        int id = getPlayerId(uuid);
        if (id == -1) return;

        saveSettingValue(id, 1, settings.isAllowRequests() ? 1 : 0);
        saveSettingValue(id, 2, settings.isAllowMessages() ? 1 : 0);
        saveSettingValue(id, 3, settings.isNotifyJoinQuit() ? 1 : 0);
    }

    private void saveSettingValue(int playerId, int settingId, int value) {
        String sql = "INSERT INTO `" + tablePrefix + "settings` (player_id, settings_id, settings_worth) " +
                "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE settings_worth = VALUES(settings_worth)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setInt(2, settingId);
            ps.setInt(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL saveSettingValue error: " + e.getMessage());
        }
    }

    @Override
    public void updateLastOnline(UUID uuid) {
        String sql = "UPDATE `" + tablePrefix + "players` SET last_online = CURRENT_TIMESTAMP WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] MySQL updateLastOnline error: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
