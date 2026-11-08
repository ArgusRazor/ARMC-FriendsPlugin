package com.argusrazor.friends.placeholder;

import com.argusrazor.friends.model.FriendPlayer;
import com.argusrazor.friends.model.Party;
import com.argusrazor.friends.service.FriendService;
import com.argusrazor.friends.service.PartyService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class ArgusFriendsExpansion extends PlaceholderExpansion {

    private final FriendService friendService;
    private final PartyService partyService;

    public ArgusFriendsExpansion(FriendService friendService, PartyService partyService) {
        this.friendService = friendService;
        this.partyService = partyService;
    }

    @Override
    public String getIdentifier() {
        return "argusfriends";
    }

    @Override
    public String getAuthor() {
        return "ArgusRazor";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null || params == null) return "";

        String p = params.toLowerCase();

        // %argusfriends_count% o %argusfriends_total%
        if (p.equals("count") || p.equals("total")) {
            List<FriendPlayer> friends = friendService.getFriends(offlinePlayer.getUniqueId());
            return String.valueOf(friends.size());
        }

        // %argusfriends_online%
        if (p.equals("online")) {
            List<FriendPlayer> friends = friendService.getFriends(offlinePlayer.getUniqueId());
            long online = friends.stream().filter(FriendPlayer::isOnline).count();
            return String.valueOf(online);
        }

        // %argusfriends_in_party%
        if (p.equals("in_party")) {
            return partyService.isInParty(offlinePlayer.getUniqueId()) ? "true" : "false";
        }

        // %argusfriends_party_size%
        if (p.equals("party_size")) {
            Party party = partyService.getParty(offlinePlayer.getUniqueId());
            return party != null ? String.valueOf(party.getSize()) : "0";
        }

        // %argusfriends_party_leader%
        if (p.equals("party_leader")) {
            Party party = partyService.getParty(offlinePlayer.getUniqueId());
            if (party == null) return "None";
            Player leader = Bukkit.getPlayer(party.getLeader());
            return leader != null ? leader.getName() : "Offline";
        }

        return null;
    }
}
