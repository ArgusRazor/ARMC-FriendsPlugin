package com.argusrazor.friends.gui;

import com.argusrazor.friends.adapter.SoundAdapter;
import com.argusrazor.friends.config.ConfigManager;
import com.argusrazor.friends.database.DatabaseManager;
import com.argusrazor.friends.model.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SettingsGUI {

    public static void open(Player player, DatabaseManager db, ConfigManager config) {
        PlayerSettings settings = db.getSettings(player.getUniqueId());

        String title = config.color(config.getConfig().getString("GUI.SettingsMenu.Title", "&8Ajustes de Privacidad"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Slot 11: Solicitudes de amistad
        inv.setItem(11, createToggleItem(
                Material.WRITABLE_BOOK,
                ChatColor.YELLOW + "Solicitudes de Amistad",
                settings.isAllowRequests(),
                "Permite que otros jugadores te envien solicitudes."
        ));

        // Slot 13: Mensajes privados
        inv.setItem(13, createToggleItem(
                Material.PAPER,
                ChatColor.AQUA + "Mensajes Privados (/f msg)",
                settings.isAllowMessages(),
                "Permite recibir mensajes privados de amigos."
        ));

        // Slot 15: Notificaciones de conexion
        inv.setItem(15, createToggleItem(
                Material.BELL,
                ChatColor.GREEN + "Notificaciones de Conexion",
                settings.isNotifyJoinQuit(),
                "Recibe avisos cuando tus amigos entran o salen."
        ));

        // Slot 22: Boton volver
        inv.setItem(22, createItem(Material.ARROW, ChatColor.GRAY + "◀ Volver al Menu de Amigos"));

        player.openInventory(inv);
        SoundAdapter.playSound(player, "UI_BUTTON_CLICK");
    }

    private static ItemStack createToggleItem(Material material, String title, boolean enabled, String desc) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + desc);
            lore.add("");
            lore.add(ChatColor.WHITE + "Estado: " + (enabled ? ChatColor.GREEN + "Activado" : ChatColor.RED + "Desactivado"));
            lore.add(ChatColor.YELLOW + "Haz clic para cambiar.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
