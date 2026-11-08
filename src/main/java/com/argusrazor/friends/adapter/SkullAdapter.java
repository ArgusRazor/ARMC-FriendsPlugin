package com.argusrazor.friends.adapter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkullAdapter {

    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    public static ItemStack createPlayerHead(UUID uuid, String playerName) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        try {
            if (uuid != null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                meta.setOwningPlayer(offlinePlayer);
            } else if (playerName != null) {
                meta.setOwner(playerName);
            }
        } catch (Throwable ignored) {
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCustomTextureHead(String textureBase64, String displayName) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        boolean applied = false;

        if (VersionAdapter.hasNativePlayerProfile() && textureBase64 != null && !textureBase64.isEmpty()) {
            try {
                String skinUrl = extractUrlFromBase64(textureBase64);
                if (skinUrl != null) {
                    org.bukkit.profile.PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), displayName != null ? displayName : "CustomHead");
                    org.bukkit.profile.PlayerTextures textures = profile.getTextures();
                    textures.setSkin(URI.create(skinUrl).toURL());
                    profile.setTextures(textures);
                    meta.setOwnerProfile(profile);
                    applied = true;
                }
            } catch (Throwable ignored) {
            }
        }

        if (displayName != null) {
            meta.setDisplayName(displayName);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static String extractUrlFromBase64(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            String json = new String(decoded, StandardCharsets.UTF_8);
            Matcher matcher = URL_PATTERN.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
            if (base64.startsWith("http://") || base64.startsWith("https://")) {
                return base64;
            }
        } catch (Exception ignored) {
            if (base64.startsWith("http://") || base64.startsWith("https://")) {
                return base64;
            }
        }
        return null;
    }
}
