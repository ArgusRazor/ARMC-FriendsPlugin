package com.argusrazor.friends.command;

import com.argusrazor.friends.config.ConfigManager;
import com.argusrazor.friends.gui.PartyGUI;
import com.argusrazor.friends.model.Party;
import com.argusrazor.friends.service.PartyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final PartyService partyService;
    private final ConfigManager config;

    public PartyCommand(PartyService partyService, ConfigManager config) {
        this.partyService = partyService;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por jugadores en el servidor.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            PartyGUI.open(player, partyService, config);
            return true;
        }

        // Si usan /p <mensaje directo> y no coincide con subcomando
        List<String> subcommands = Arrays.asList("help", "gui", "invite", "accept", "deny", "kick", "leave", "leader", "chat");
        if (label.equalsIgnoreCase("p") && !subcommands.contains(args[0].toLowerCase())) {
            StringBuilder sb = new StringBuilder();
            for (String arg : args) {
                sb.append(arg).append(" ");
            }
            partyService.sendPartyChat(player, sb.toString().trim());
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                for (String line : config.getConfig().getStringList("Party.Help")) {
                    player.sendMessage(config.color(line));
                }
                break;

            case "invite":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /party invite <jugador>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(config.getPrefix() + config.getMessage("Friend.PlayerNotFound", "PLAYER", args[1]));
                    return true;
                }
                partyService.invitePlayer(player, target);
                break;

            case "accept":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /party accept <lider>");
                    return true;
                }
                partyService.acceptInvite(player, args[1]);
                break;

            case "deny":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /party deny <lider>");
                    return true;
                }
                partyService.denyInvite(player, args[1]);
                break;

            case "kick":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /party kick <jugador>");
                    return true;
                }
                partyService.kickPlayer(player, args[1]);
                break;

            case "leave":
                partyService.leaveParty(player);
                break;

            case "leader":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /party leader <jugador>");
                    return true;
                }
                Party party = partyService.getParty(player.getUniqueId());
                if (party == null || !party.isLeader(player.getUniqueId())) {
                    player.sendMessage(config.getPrefix() + config.getMessage("Party.NotLeader"));
                    return true;
                }
                Player newLeader = Bukkit.getPlayer(args[1]);
                if (newLeader == null || !party.isMember(newLeader.getUniqueId())) {
                    player.sendMessage(config.getPrefix() + config.getMessage("Friend.PlayerNotFound", "PLAYER", args[1]));
                    return true;
                }
                party.setLeader(newLeader.getUniqueId());
                partyService.broadcastParty(party, config.getMessage("Party.LeaderTransferred", "PLAYER", newLeader.getName()));
                break;

            case "chat":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /party chat <mensaje>");
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                partyService.sendPartyChat(player, sb.toString().trim());
                break;

            default:
                player.sendMessage(config.getPrefix() + ChatColor.RED + "Subcomando desconocido. Usa /party help");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("gui", "invite", "accept", "deny", "kick", "leave", "leader", "chat", "help"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("leader"))) {
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            return filter(playerNames, args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> list, String token) {
        List<String> res = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(token.toLowerCase())) {
                res.add(s);
            }
        }
        return res;
    }
}
