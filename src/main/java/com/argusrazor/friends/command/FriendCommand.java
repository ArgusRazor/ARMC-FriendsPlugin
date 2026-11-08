package com.argusrazor.friends.command;

import com.argusrazor.friends.config.ConfigManager;
import com.argusrazor.friends.database.DatabaseManager;
import com.argusrazor.friends.gui.FriendsGUI;
import com.argusrazor.friends.model.FriendPlayer;
import com.argusrazor.friends.model.PlayerSettings;
import com.argusrazor.friends.service.FriendService;
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

public class FriendCommand implements CommandExecutor, TabCompleter {

    private final FriendService friendService;
    private final DatabaseManager db;
    private final ConfigManager config;

    public FriendCommand(FriendService friendService, DatabaseManager db, ConfigManager config) {
        this.friendService = friendService;
        this.db = db;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por jugadores en el servidor.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            FriendsGUI.open(player, friendService, config, 1);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                for (String line : config.getConfig().getStringList("Friend.Help")) {
                    player.sendMessage(config.color(line));
                }
                break;

            case "add":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /friend add <jugador>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(config.getPrefix() + config.getMessage("Friend.PlayerNotFound", "PLAYER", args[1]));
                    return true;
                }
                friendService.sendFriendRequest(player, target);
                break;

            case "accept":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /friend accept <jugador>");
                    return true;
                }
                friendService.acceptFriendRequest(player, args[1]);
                break;

            case "deny":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /friend deny <jugador>");
                    return true;
                }
                friendService.denyFriendRequest(player, args[1]);
                break;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /friend remove <jugador>");
                    return true;
                }
                friendService.removeFriend(player, args[1]);
                break;

            case "list":
                sendFriendList(player);
                break;

            case "msg":
                if (args.length < 3) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /friend msg <jugador> <mensaje>");
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                friendService.sendPrivateMessage(player, args[1], sb.toString().trim());
                break;

            case "jump":
                player.sendMessage(config.getPrefix() + config.getMessage("Friend.JumpDisabled"));
                break;

            case "toggle":
                if (args.length < 2) {
                    player.sendMessage(config.getPrefix() + ChatColor.RED + "Uso: /friend toggle <requests|msg|notify>");
                    return true;
                }
                handleToggle(player, args[1]);
                break;

            default:
                player.sendMessage(config.getPrefix() + ChatColor.RED + "Subcomando desconocido. Usa /friend help");
                break;
        }

        return true;
    }

    private void sendFriendList(Player player) {
        List<FriendPlayer> friends = friendService.getFriends(player.getUniqueId());
        if (friends.isEmpty()) {
            player.sendMessage(config.getPrefix() + config.getMessage("Friend.ListEmpty"));
            return;
        }

        long onlineCount = friends.stream().filter(FriendPlayer::isOnline).count();
        player.sendMessage(config.getMessage("Friend.ListHeader", "ONLINE", onlineCount, "TOTAL", friends.size()));

        for (FriendPlayer fp : friends) {
            if (fp.isOnline()) {
                player.sendMessage(config.getMessage("Friend.ListEntryOnline", "PLAYER", fp.getName()));
            } else {
                player.sendMessage(config.getMessage("Friend.ListEntryOffline", "PLAYER", fp.getName()));
            }
        }
    }

    private void handleToggle(Player player, String type) {
        PlayerSettings s = db.getSettings(player.getUniqueId());
        if (type.equalsIgnoreCase("requests")) {
            s.setAllowRequests(!s.isAllowRequests());
            player.sendMessage(config.getPrefix() + ChatColor.YELLOW + "Solicitudes: " + (s.isAllowRequests() ? ChatColor.GREEN + "Activadas" : ChatColor.RED + "Desactivadas"));
        } else if (type.equalsIgnoreCase("msg")) {
            s.setAllowMessages(!s.isAllowMessages());
            player.sendMessage(config.getPrefix() + ChatColor.YELLOW + "Mensajes privados: " + (s.isAllowMessages() ? ChatColor.GREEN + "Activados" : ChatColor.RED + "Desactivados"));
        } else if (type.equalsIgnoreCase("notify")) {
            s.setNotifyJoinQuit(!s.isNotifyJoinQuit());
            player.sendMessage(config.getPrefix() + ChatColor.YELLOW + "Notificaciones: " + (s.isNotifyJoinQuit() ? ChatColor.GREEN + "Activadas" : ChatColor.RED + "Desactivadas"));
        }
        db.saveSettings(player.getUniqueId(), s);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("gui", "list", "add", "remove", "accept", "deny", "msg", "toggle", "help"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("msg") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            return filter(playerNames, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            return filter(Arrays.asList("requests", "msg", "notify"), args[1]);
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
