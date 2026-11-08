package com.argusrazor.friends.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        if (messagesFile != null) {
            messages = YamlConfiguration.loadConfiguration(messagesFile);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getPrefix() {
        return color(messages.getString("Prefix", "&8[&bArgus&fFriends&8]&r "));
    }

    public String getMessage(String path) {
        String msg = messages.getString(path);
        if (msg == null) return "Missing message: " + path;
        return color(msg);
    }

    public String getMessage(String path, Object... replacements) {
        String msg = messages.getString(path);
        if (msg == null) return "Missing message: " + path;
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String key = String.valueOf(replacements[i]);
                String val = String.valueOf(replacements[i + 1]);
                msg = msg.replace("{" + key + "}", val);
            }
        }
        return color(msg);
    }

    public String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
