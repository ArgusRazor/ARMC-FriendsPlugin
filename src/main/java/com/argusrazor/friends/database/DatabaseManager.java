package com.argusrazor.friends.database;

import com.argusrazor.friends.model.FriendPlayer;
import com.argusrazor.friends.model.PlayerSettings;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface DatabaseManager {

    void init() throws SQLException;

    Connection getConnection() throws SQLException;

    void savePlayer(UUID uuid, String name);

    FriendPlayer getPlayer(UUID uuid);

    FriendPlayer getPlayerByName(String name);

    List<FriendPlayer> getFriends(UUID uuid);

    boolean areFriends(UUID u1, UUID u2);

    void addFriend(UUID u1, UUID u2);

    void removeFriend(UUID u1, UUID u2);

    PlayerSettings getSettings(UUID uuid);

    void saveSettings(UUID uuid, PlayerSettings settings);

    void updateLastOnline(UUID uuid);

    void close();
}
