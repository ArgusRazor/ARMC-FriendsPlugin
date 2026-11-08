package com.argusrazor.friends.model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FriendPlayer {

    private final UUID uuid;
    private String name;
    private long lastOnline;

    public FriendPlayer(UUID uuid, String name, long lastOnline) {
        this.uuid = uuid;
        this.name = name;
        this.lastOnline = lastOnline;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }

    public boolean isOnline() {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline();
    }
}
