package com.argusrazor.friends.database;

import com.argusrazor.friends.model.FriendPlayer;
import com.argusrazor.friends.model.PlayerSettings;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class SQLiteDatabase implements DatabaseManager {

    private final File dbFile;
    private final String tablePrefix;
    private final Logger logger;
    private Connection connection;

    public SQLiteDatabase(File dbFile, String tablePrefix, Logger logger) {
        this.dbFile = dbFile;
        this.tablePrefix = (tablePrefix == null || tablePrefix.isEmpty()) ? "fr_" : tablePrefix;
        this.logger = logger;
    }

    @Override
    public synchronized void init() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.warning("[ArgusRazorFriends] org.sqlite.JDBC not found via standard ClassLoader, attempting direct connection.");
        }

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "players (" +
                    "player_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "player_uuid VARCHAR(36) NOT NULL UNIQUE, " +
                    "last_online TIMESTAMP DEFAULT CURRENT_TIMESTAMP);");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "friend_assignment (" +
                    "friend1_id INTEGER NOT NULL, " +
                    "friend2_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (friend1_id, friend2_id));");

            s.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "settings (" +
                    "player_id INTEGER NOT NULL, " +
                    "settings_id INTEGER NOT NULL, " +
                    "settings_worth INTEGER NOT NULL, " +
                    "PRIMARY KEY (player_id, settings_id));");
        }
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }
        return connection;
    }

    private int getPlayerId(UUID uuid) {
        String sql = "SELECT player_id FROM " + tablePrefix + "players WHERE player_uuid = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("player_id");
                }
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error getting player ID: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public synchronized void savePlayer(UUID uuid, String name) {
        String checkSql = "SELECT player_id FROM " + tablePrefix + "players WHERE player_uuid = ?";
        try (PreparedStatement check = getConnection().prepareStatement(checkSql)) {
            check.setString(1, uuid.toString());
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    String update = "UPDATE " + tablePrefix + "players SET player_name = ?, last_online = CURRENT_TIMESTAMP WHERE player_uuid = ?";
                    try (PreparedStatement u = getConnection().prepareStatement(update)) {
                        u.setString(1, name);
                        u.setString(2, uuid.toString());
                        u.executeUpdate();
                    }
                    return;
                }
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error verifying player: " + e.getMessage());
        }

        String insert = "INSERT OR IGNORE INTO " + tablePrefix + "players (player_name, player_uuid, last_online) VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = getConnection().prepareStatement(insert)) {
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error inserting player: " + e.getMessage());
        }
    }

    @Override
    public synchronized FriendPlayer getPlayer(UUID uuid) {
        String sql = "SELECT player_name, last_online FROM " + tablePrefix + "players WHERE player_uuid = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FriendPlayer(uuid, rs.getString("player_name"), rs.getTimestamp("last_online").getTime());
                }
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error getting player: " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized FriendPlayer getPlayerByName(String name) {
        String sql = "SELECT player_uuid, last_online FROM " + tablePrefix + "players WHERE LOWER(player_name) = LOWER(?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    return new FriendPlayer(uuid, name, rs.getTimestamp("last_online").getTime());
                }
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error getting player by name: " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized List<FriendPlayer> getFriends(UUID uuid) {
        List<FriendPlayer> list = new ArrayList<>();
        int id = getPlayerId(uuid);
        if (id == -1) return list;

        String sql = "SELECT p.player_uuid, p.player_name, p.last_online FROM " + tablePrefix + "friend_assignment f " +
                "JOIN " + tablePrefix + "players p ON " +
                "(f.friend1_id = ? AND f.friend2_id = p.player_id) OR (f.friend2_id = ? AND f.friend1_id = p.player_id)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID fUuid = UUID.fromString(rs.getString("player_uuid"));
                    list.add(new FriendPlayer(fUuid, rs.getString("player_name"), rs.getTimestamp("last_online").getTime()));
                }
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error listing friends: " + e.getMessage());
        }
        return list;
    }

    @Override
    public synchronized boolean areFriends(UUID u1, UUID u2) {
        int id1 = getPlayerId(u1);
        int id2 = getPlayerId(u2);
        if (id1 == -1 || id2 == -1) return false;

        String sql = "SELECT 1 FROM " + tablePrefix + "friend_assignment WHERE " +
                "(friend1_id = ? AND friend2_id = ?) OR (friend1_id = ? AND friend2_id = ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id1);
            ps.setInt(2, id2);
            ps.setInt(3, id2);
            ps.setInt(4, id1);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error checking friendship: " + e.getMessage());
        }
        return false;
    }

    @Override
    public synchronized void addFriend(UUID u1, UUID u2) {
        int id1 = getPlayerId(u1);
        int id2 = getPlayerId(u2);
        if (id1 == -1 || id2 == -1) return;

        int min = Math.min(id1, id2);
        int max = Math.max(id1, id2);

        String sql = "INSERT OR IGNORE INTO " + tablePrefix + "friend_assignment (friend1_id, friend2_id) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, min);
            ps.setInt(2, max);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error adding friendship: " + e.getMessage());
        }
    }

    @Override
    public synchronized void removeFriend(UUID u1, UUID u2) {
        int id1 = getPlayerId(u1);
        int id2 = getPlayerId(u2);
        if (id1 == -1 || id2 == -1) return;

        String sql = "DELETE FROM " + tablePrefix + "friend_assignment WHERE " +
                "(friend1_id = ? AND friend2_id = ?) OR (friend1_id = ? AND friend2_id = ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id1);
            ps.setInt(2, id2);
            ps.setInt(3, id2);
            ps.setInt(4, id1);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error removing friendship: " + e.getMessage());
        }
    }

    @Override
    public synchronized PlayerSettings getSettings(UUID uuid) {
        PlayerSettings settings = new PlayerSettings();
        int id = getPlayerId(uuid);
        if (id == -1) return settings;

        String sql = "SELECT settings_id, settings_worth FROM " + tablePrefix + "settings WHERE player_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
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
            logger.severe("[ArgusRazorFriends] Error reading settings: " + e.getMessage());
        }
        return settings;
    }

    @Override
    public synchronized void saveSettings(UUID uuid, PlayerSettings settings) {
        int id = getPlayerId(uuid);
        if (id == -1) return;

        saveSettingValue(id, 1, settings.isAllowRequests() ? 1 : 0);
        saveSettingValue(id, 2, settings.isAllowMessages() ? 1 : 0);
        saveSettingValue(id, 3, settings.isNotifyJoinQuit() ? 1 : 0);
    }

    private void saveSettingValue(int playerId, int settingId, int value) {
        String sql = "INSERT INTO " + tablePrefix + "settings (player_id, settings_id, settings_worth) " +
                "VALUES (?, ?, ?) ON CONFLICT(player_id, settings_id) DO UPDATE SET settings_worth = excluded.settings_worth";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setInt(2, settingId);
            ps.setInt(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error saving setting " + settingId + ": " + e.getMessage());
        }
    }

    @Override
    public synchronized void updateLastOnline(UUID uuid) {
        String sql = "UPDATE " + tablePrefix + "players SET last_online = CURRENT_TIMESTAMP WHERE player_uuid = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[ArgusRazorFriends] Error updating last online: " + e.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
