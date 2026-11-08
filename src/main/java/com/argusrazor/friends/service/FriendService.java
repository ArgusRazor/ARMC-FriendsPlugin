package com.argusrazor.friends.service;

import com.argusrazor.friends.adapter.SoundAdapter;
import com.argusrazor.friends.config.ConfigManager;
import com.argusrazor.friends.database.DatabaseManager;
import com.argusrazor.friends.model.FriendPlayer;
import com.argusrazor.friends.model.FriendRequest;
import com.argusrazor.friends.model.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FriendService {

    private final DatabaseManager db;
    private final ConfigManager config;
    private final Map<UUID, List<FriendRequest>> pendingRequests = new ConcurrentHashMap<>();

    public FriendService(DatabaseManager db, ConfigManager config) {
        this.db = db;
        this.config = config;
    }

    public List<FriendPlayer> getFriends(UUID uuid) {
        return db.getFriends(uuid);
    }

    public boolean areFriends(UUID u1, UUID u2) {
        return db.areFriends(u1, u2);
    }

    public void sendFriendRequest(Player sender, Player target) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(config.getPrefix() + config.getMessage("Friend.CannotAddSelf"));
            return;
        }

        if (areFriends(sender.getUniqueId(), target.getUniqueId())) {
            sender.sendMessage(config.getPrefix() + config.getMessage("Friend.AlreadyFriends", "PLAYER", target.getName()));
            return;
        }

        PlayerSettings targetSettings = db.getSettings(target.getUniqueId());
        if (!targetSettings.isAllowRequests()) {
            sender.sendMessage(config.getPrefix() + config.getMessage("Friend.TargetRequestsDisabled", "PLAYER", target.getName()));
            return;
        }

        int maxFriends = config.getConfig().getInt("General.MaxFriends", 50);
        if (getFriends(sender.getUniqueId()).size() >= maxFriends) {
            sender.sendMessage(config.getPrefix() + config.getMessage("Friend.MaxFriendsReached", "MAX", maxFriends));
            return;
        }
        if (getFriends(target.getUniqueId()).size() >= maxFriends) {
            sender.sendMessage(config.getPrefix() + config.getMessage("Friend.TargetMaxFriendsReached", "PLAYER", target.getName()));
            return;
        }

        List<FriendRequest> requests = pendingRequests.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>());
        for (FriendRequest r : requests) {
            if (r.getSender().equals(sender.getUniqueId())) {
                sender.sendMessage(config.getPrefix() + config.getMessage("Friend.RequestAlreadySent", "PLAYER", target.getName()));
                return;
            }
        }

        requests.add(new FriendRequest(sender.getUniqueId(), target.getUniqueId()));

        sender.sendMessage(config.getPrefix() + config.getMessage("Friend.RequestSent", "PLAYER", target.getName()));
        target.sendMessage(config.getPrefix() + config.getMessage("Friend.RequestReceived", "PLAYER", sender.getName()));

        String reqSound = config.getConfig().getString("Sounds.FriendRequest", "ENTITY_PLAYER_LEVELUP");
        SoundAdapter.playSound(target, reqSound);
    }

    public void acceptFriendRequest(Player receiver, String senderName) {
        List<FriendRequest> requests = pendingRequests.get(receiver.getUniqueId());
        if (requests == null || requests.isEmpty()) {
            receiver.sendMessage(config.getPrefix() + config.getMessage("Friend.NoPendingRequest", "PLAYER", senderName));
            return;
        }

        long timeoutMs = config.getConfig().getLong("General.RequestTimeoutSeconds", 60) * 1000L;
        FriendRequest found = null;
        for (Iterator<FriendRequest> it = requests.iterator(); it.hasNext(); ) {
            FriendRequest r = it.next();
            if (r.isExpired(timeoutMs)) {
                it.remove();
                continue;
            }
            FriendPlayer fp = db.getPlayer(r.getSender());
            if (fp != null && fp.getName().equalsIgnoreCase(senderName)) {
                found = r;
                it.remove();
                break;
            }
        }

        if (found == null) {
            receiver.sendMessage(config.getPrefix() + config.getMessage("Friend.NoPendingRequest", "PLAYER", senderName));
            return;
        }

        db.addFriend(found.getSender(), receiver.getUniqueId());

        receiver.sendMessage(config.getPrefix() + config.getMessage("Friend.RequestAcceptedReceiver", "PLAYER", senderName));
        Player senderPlayer = Bukkit.getPlayer(found.getSender());
        if (senderPlayer != null && senderPlayer.isOnline()) {
            senderPlayer.sendMessage(config.getPrefix() + config.getMessage("Friend.RequestAcceptedSender", "PLAYER", receiver.getName()));
            SoundAdapter.playSound(senderPlayer, "ENTITY_PLAYER_LEVELUP");
        }
    }

    public void denyFriendRequest(Player receiver, String senderName) {
        List<FriendRequest> requests = pendingRequests.get(receiver.getUniqueId());
        if (requests == null || requests.isEmpty()) {
            receiver.sendMessage(config.getPrefix() + config.getMessage("Friend.NoPendingRequest", "PLAYER", senderName));
            return;
        }

        boolean removed = requests.removeIf(r -> {
            FriendPlayer fp = db.getPlayer(r.getSender());
            return fp != null && fp.getName().equalsIgnoreCase(senderName);
        });

        if (removed) {
            receiver.sendMessage(config.getPrefix() + config.getMessage("Friend.RequestDenied", "PLAYER", senderName));
        } else {
            receiver.sendMessage(config.getPrefix() + config.getMessage("Friend.NoPendingRequest", "PLAYER", senderName));
        }
    }

    public void removeFriend(Player player, String targetName) {
        FriendPlayer target = db.getPlayerByName(targetName);
        if (target == null || !areFriends(player.getUniqueId(), target.getUuid())) {
            player.sendMessage(config.getPrefix() + config.getMessage("Friend.PlayerNotFound", "PLAYER", targetName));
            return;
        }

        db.removeFriend(player.getUniqueId(), target.getUuid());
        player.sendMessage(config.getPrefix() + config.getMessage("Friend.Removed", "PLAYER", target.getName()));

        Player targetPlayer = Bukkit.getPlayer(target.getUuid());
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(config.getPrefix() + config.getMessage("Friend.Removed", "PLAYER", player.getName()));
        }
    }

    public void sendPrivateMessage(Player sender, String targetName, String message) {
        FriendPlayer target = db.getPlayerByName(targetName);
        if (target == null || !areFriends(sender.getUniqueId(), target.getUuid())) {
            sender.sendMessage(config.getPrefix() + config.getMessage("Friend.PlayerNotFound", "PLAYER", targetName));
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(target.getUuid());
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage(config.getPrefix() + config.getMessage("Friend.PlayerOffline", "PLAYER", target.getName()));
            return;
        }

        PlayerSettings targetSettings = db.getSettings(target.getUuid());
        if (!targetSettings.isAllowMessages()) {
            sender.sendMessage(config.getPrefix() + config.getMessage("Friend.TargetRequestsDisabled", "PLAYER", target.getName()));
            return;
        }

        String formatted = config.getMessage("Friend.MessageFormat",
                "SENDER", sender.getName(),
                "RECEIVER", targetPlayer.getName(),
                "MESSAGE", message);

        sender.sendMessage(formatted);
        targetPlayer.sendMessage(formatted);
        SoundAdapter.playSound(targetPlayer, "ENTITY_EXPERIENCE_ORB_PICKUP");
    }

    public void cleanExpiredRequests() {
        long timeoutMs = config.getConfig().getLong("General.RequestTimeoutSeconds", 60) * 1000L;
        for (List<FriendRequest> list : pendingRequests.values()) {
            list.removeIf(r -> r.isExpired(timeoutMs));
        }
    }
}
