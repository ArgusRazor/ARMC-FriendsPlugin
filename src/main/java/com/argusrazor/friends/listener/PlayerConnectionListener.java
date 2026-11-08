package com.argusrazor.friends.listener;

import com.argusrazor.friends.adapter.SoundAdapter;
import com.argusrazor.friends.config.ConfigManager;
import com.argusrazor.friends.database.DatabaseManager;
import com.argusrazor.friends.model.FriendPlayer;
import com.argusrazor.friends.model.PlayerSettings;
import com.argusrazor.friends.service.FriendService;
import com.argusrazor.friends.service.PartyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class PlayerConnectionListener implements Listener {

    private final DatabaseManager db;
    private final FriendService friendService;
    private final PartyService partyService;
    private final ConfigManager config;

    public PlayerConnectionListener(DatabaseManager db, FriendService friendService, PartyService partyService, ConfigManager config) {
        this.db = db;
        this.friendService = friendService;
        this.partyService = partyService;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("ArgusRazorFriends"), () -> {
            db.savePlayer(player.getUniqueId(), player.getName());

            if (config.getConfig().getBoolean("General.NotifyFriendJoin", true)) {
                List<FriendPlayer> friends = friendService.getFriends(player.getUniqueId());
                for (FriendPlayer f : friends) {
                    Player friendOnline = Bukkit.getPlayer(f.getUuid());
                    if (friendOnline != null && friendOnline.isOnline()) {
                        PlayerSettings s = db.getSettings(friendOnline.getUniqueId());
                        if (s.isNotifyJoinQuit()) {
                            friendOnline.sendMessage(config.getPrefix() + config.getMessage("Friend.JoinNotify", "PLAYER", player.getName()));
                            SoundAdapter.playSound(friendOnline, config.getConfig().getString("Sounds.FriendJoin", "BLOCK_NOTE_BLOCK_PLING"));
                        }
                    }
                }
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("ArgusRazorFriends"), () -> {
            db.updateLastOnline(player.getUniqueId());

            if (config.getConfig().getBoolean("General.NotifyFriendQuit", true)) {
                List<FriendPlayer> friends = friendService.getFriends(player.getUniqueId());
                for (FriendPlayer f : friends) {
                    Player friendOnline = Bukkit.getPlayer(f.getUuid());
                    if (friendOnline != null && friendOnline.isOnline()) {
                        PlayerSettings s = db.getSettings(friendOnline.getUniqueId());
                        if (s.isNotifyJoinQuit()) {
                            friendOnline.sendMessage(config.getPrefix() + config.getMessage("Friend.QuitNotify", "PLAYER", player.getName()));
                            SoundAdapter.playSound(friendOnline, config.getConfig().getString("Sounds.FriendQuit", "BLOCK_NOTE_BLOCK_BASS"));
                        }
                    }
                }
            }
        });
    }
}
