package com.argusrazor.friends.command;

import com.argusrazor.friends.adapter.VersionAdapter;
import com.argusrazor.friends.config.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;

    public AdminCommand(ConfigManager config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("argusfriends.admin")) {
            sender.sendMessage(config.getPrefix() + config.getMessage("General.NoPermission"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.AQUA + "=== ArgusRazorFriends Administracion ===");
            sender.sendMessage(ChatColor.YELLOW + "/af reload " + ChatColor.GRAY + "- Recargar configuracion y mensajes.");
            sender.sendMessage(ChatColor.YELLOW + "/af info " + ChatColor.GRAY + "- Ver estado de adaptacion de version.");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            config.reload();
            sender.sendMessage(config.getPrefix() + config.getMessage("General.PluginReloaded"));
            return true;
        }

        if (sub.equals("info") || sub.equals("version")) {
            sender.sendMessage(ChatColor.AQUA + "=== Estado de Compatibilidad de ArgusRazorFriends ===");
            sender.sendMessage(ChatColor.GRAY + "Version del Plugin: " + ChatColor.WHITE + "1.0.0-MC26.3");
            sender.sendMessage(ChatColor.GRAY + "Version de Servidor Detectada: " + ChatColor.GREEN + VersionAdapter.getServerVersion());
            sender.sendMessage(ChatColor.GRAY + "Soporte MC 26+: " + (VersionAdapter.isMinecraft26OrHigher() ? ChatColor.GREEN + "SI" : ChatColor.YELLOW + "Retrocompatible (" + VersionAdapter.getMajor() + "." + VersionAdapter.getMinor() + ")"));
            sender.sendMessage(ChatColor.GRAY + "Soporte Item Models (1.21.4+/26+): " + (VersionAdapter.hasItemModelSupport() ? ChatColor.GREEN + "ACTIVO" : ChatColor.RED + "INACTIVO"));
            sender.sendMessage(ChatColor.GRAY + "Soporte PlayerProfile Oficial: " + (VersionAdapter.hasNativePlayerProfile() ? ChatColor.GREEN + "ACTIVO" : ChatColor.RED + "INACTIVO"));
            return true;
        }

        sender.sendMessage(config.getPrefix() + ChatColor.RED + "Subcomando desconocido. Usa /af help");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "info", "version", "help");
        }
        return new ArrayList<>();
    }
}
