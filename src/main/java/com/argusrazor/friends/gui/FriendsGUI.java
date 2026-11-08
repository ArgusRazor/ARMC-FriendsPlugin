package com.argusrazor.friends.gui;

import com.argusrazor.friends.adapter.ItemModelAdapter;
import com.argusrazor.friends.adapter.SkullAdapter;
import com.argusrazor.friends.adapter.SoundAdapter;
import com.argusrazor.friends.config.ConfigManager;
import com.argusrazor.friends.model.FriendPlayer;
import com.argusrazor.friends.service.FriendService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FriendsGUI {

    private static final int ITEMS_PER_PAGE = 45;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public static void open(Player player, FriendService friendService, ConfigManager config, int page) {
        List<FriendPlayer> friends = friendService.getFriends(player.getUniqueId());
        int totalFriends = friends.size();
        int maxPages = Math.max(1, (int) Math.ceil((double) totalFriends / ITEMS_PER_PAGE));
        int currentPage = Math.min(Math.max(1, page), maxPages);

        String title = config.color(config.getConfig().getString("GUI.FriendsMenu.Title", "&8Menu de Amigos ({PAGE}/{MAX_PAGE})")
                .replace("{PAGE}", String.valueOf(currentPage))
                .replace("{MAX_PAGE}", String.valueOf(maxPages)));

        Inventory inv = Bukkit.createInventory(null, 54, title);

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalFriends);

        for (int i = startIndex; i < endIndex; i++) {
            FriendPlayer friend = friends.get(i);
            ItemStack head = SkullAdapter.createPlayerHead(friend.getUuid(), friend.getName());
            ItemMeta meta = head.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + friend.getName());
                List<String> lore = new ArrayList<>();
                if (friend.isOnline()) {
                    lore.add(ChatColor.GREEN + "● En linea");
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "Clic izquierdo: " + ChatColor.GRAY + "Enviar mensaje");
                    lore.add(ChatColor.AQUA + "Clic derecho: " + ChatColor.GRAY + "Invitar a grupo");
                } else {
                    lore.add(ChatColor.RED + "● Desconectado");
                    lore.add(ChatColor.DARK_GRAY + "Ultima vez: " + DATE_FORMAT.format(new Date(friend.getLastOnline())));
                    lore.add("");
                    lore.add(ChatColor.RED + "Shift + Clic: " + ChatColor.GRAY + "Eliminar amigo");
                }
                meta.setLore(lore);
                head.setItemMeta(meta);
            }

            inv.setItem(i - startIndex, head);
        }

        // Barra inferior (Slots 45 a 53)
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        if (currentPage > 1) {
            inv.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "◀ Pagina Anterior"));
        }

        inv.setItem(49, createItem(Material.COMPARATOR, ChatColor.AQUA + "⚙ Ajustes de Privacidad"));

        if (currentPage < maxPages) {
            inv.setItem(53, createItem(Material.ARROW, ChatColor.YELLOW + "Pagina Siguiente ▶"));
        }

        player.openInventory(inv);
        SoundAdapter.playSound(player, "UI_BUTTON_CLICK");
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
