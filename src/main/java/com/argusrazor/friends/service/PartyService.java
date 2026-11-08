package com.argusrazor.friends.service;

import com.argusrazor.friends.adapter.SoundAdapter;
import com.argusrazor.friends.config.ConfigManager;
import com.argusrazor.friends.model.Party;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyService {

    private final ConfigManager config;
    private final Map<UUID, Party> playerPartyMap = new ConcurrentHashMap<>();
    private final Set<Party> parties = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public PartyService(ConfigManager config) {
        this.config = config;
    }

    public Party getParty(UUID uuid) {
        return playerPartyMap.get(uuid);
    }

    public boolean isInParty(UUID uuid) {
        return playerPartyMap.containsKey(uuid);
    }

    public void invitePlayer(Player leader, Player target) {
        Party party = playerPartyMap.get(leader.getUniqueId());
        if (party == null) {
            party = new Party(leader.getUniqueId());
            parties.add(party);
            playerPartyMap.put(leader.getUniqueId(), party);
            leader.sendMessage(config.getPrefix() + config.getMessage("Party.PartyCreated"));
        } else if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(config.getPrefix() + config.getMessage("Party.NotLeader"));
            return;
        }

        int maxParty = config.getConfig().getInt("General.MaxPartySize", 6);
        if (party.getSize() >= maxParty) {
            leader.sendMessage(config.getPrefix() + config.getMessage("Party.PartyFull", "MAX", maxParty));
            return;
        }

        if (isInParty(target.getUniqueId())) {
            leader.sendMessage(config.getPrefix() + config.getMessage("Party.TargetAlreadyInParty", "PLAYER", target.getName()));
            return;
        }

        party.invite(target.getUniqueId());
        leader.sendMessage(config.getPrefix() + config.getMessage("Party.InviteSent", "PLAYER", target.getName()));
        target.sendMessage(config.getPrefix() + config.getMessage("Party.InviteReceived", "LEADER", leader.getName()));
        SoundAdapter.playSound(target, "ENTITY_PLAYER_LEVELUP");
    }

    public void acceptInvite(Player player, String leaderName) {
        if (isInParty(player.getUniqueId())) {
            player.sendMessage(config.getPrefix() + config.getMessage("Party.AlreadyInParty"));
            return;
        }

        Player leaderPlayer = Bukkit.getPlayer(leaderName);
        if (leaderPlayer == null || !leaderPlayer.isOnline()) {
            player.sendMessage(config.getPrefix() + config.getMessage("Friend.PlayerOffline", "PLAYER", leaderName));
            return;
        }

        Party party = playerPartyMap.get(leaderPlayer.getUniqueId());
        if (party == null || !party.isLeader(leaderPlayer.getUniqueId()) || !party.isInvited(player.getUniqueId())) {
            player.sendMessage(config.getPrefix() + config.getMessage("Party.InviteExpired"));
            return;
        }

        int maxParty = config.getConfig().getInt("General.MaxPartySize", 6);
        if (party.getSize() >= maxParty) {
            player.sendMessage(config.getPrefix() + config.getMessage("Party.PartyFull", "MAX", maxParty));
            return;
        }

        party.addMember(player.getUniqueId());
        playerPartyMap.put(player.getUniqueId(), party);

        broadcastParty(party, config.getMessage("Party.PlayerJoined", "PLAYER", player.getName()));
    }

    public void denyInvite(Player player, String leaderName) {
        Player leaderPlayer = Bukkit.getPlayer(leaderName);
        if (leaderPlayer != null) {
            Party party = playerPartyMap.get(leaderPlayer.getUniqueId());
            if (party != null) {
                party.removeInvite(player.getUniqueId());
            }
        }
        player.sendMessage(config.getPrefix() + config.getMessage("Friend.RequestDenied", "PLAYER", leaderName));
    }

    public void leaveParty(Player player) {
        Party party = playerPartyMap.remove(player.getUniqueId());
        if (party == null) {
            player.sendMessage(config.getPrefix() + config.getMessage("Party.NotInParty"));
            return;
        }

        party.removeMember(player.getUniqueId());
        player.sendMessage(config.getPrefix() + config.getMessage("Party.PlayerLeft", "PLAYER", player.getName()));

        if (party.isLeader(player.getUniqueId())) {
            if (party.getSize() > 0) {
                UUID newLeader = party.getMembers().iterator().next();
                party.setLeader(newLeader);
                Player nl = Bukkit.getPlayer(newLeader);
                String nlName = nl != null ? nl.getName() : "Alguien";
                broadcastParty(party, config.getMessage("Party.LeaderTransferred", "PLAYER", nlName));
            } else {
                parties.remove(party);
            }
        } else {
            broadcastParty(party, config.getMessage("Party.PlayerLeft", "PLAYER", player.getName()));
        }
    }

    public void kickPlayer(Player leader, String targetName) {
        Party party = playerPartyMap.get(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(config.getPrefix() + config.getMessage("Party.NotLeader"));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        UUID targetUuid = target != null ? target.getUniqueId() : null;

        if (targetUuid == null || !party.isMember(targetUuid) || targetUuid.equals(leader.getUniqueId())) {
            leader.sendMessage(config.getPrefix() + config.getMessage("Friend.PlayerNotFound", "PLAYER", targetName));
            return;
        }

        party.removeMember(targetUuid);
        playerPartyMap.remove(targetUuid);

        if (target != null && target.isOnline()) {
            target.sendMessage(config.getPrefix() + config.getMessage("Party.PlayerKicked", "PLAYER", target.getName()));
        }
        broadcastParty(party, config.getMessage("Party.PlayerKicked", "PLAYER", targetName));
    }

    public void sendPartyChat(Player sender, String message) {
        Party party = playerPartyMap.get(sender.getUniqueId());
        if (party == null) {
            sender.sendMessage(config.getPrefix() + config.getMessage("Party.NotInParty"));
            return;
        }

        String formatted = config.getMessage("Party.ChatFormat",
                "PLAYER", sender.getName(),
                "MESSAGE", message);

        broadcastParty(party, formatted);
    }

    public void broadcastParty(Party party, String message) {
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }
}
