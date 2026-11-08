package com.argusrazor.friends.adapter;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundAdapter {

    public static void playSound(Player player, String soundName) {
        if (player == null || soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            try {
                player.playSound(player.getLocation(), soundName.toLowerCase().replace('_', '.'), 1.0f, 1.0f);
            } catch (Throwable ignored) {
            }
        }
    }
}
