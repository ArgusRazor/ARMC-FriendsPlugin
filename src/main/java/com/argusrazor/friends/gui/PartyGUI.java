package com.argusrazor.friends.gui;

import com.argusrazor.friends.adapter.SkullAdapter;
import com.argusrazor.friends.adapter.SoundAdapter;
import com.argusrazor.friends.config.ConfigManager;
import com.argusrazor.friends.model.Party;
import com.argusrazor.friends.service.PartyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyGUI {

    public static void open(Player player, PartyService partyService, ConfigManager config) {
        Party party = partyService.getParty(player.getUniqueId());

        String title = config.color(config.getConfig().getString("GUI.PartyMenu.Title", "&8Menu de Grupo"));
        Inventory inv = Bukkit.createInventory(null, 36, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, filler);
        }

        if (party == null) {
            ItemStack noParty = createItem(Material.BARRIER, ChatColor.RED + "No estas en ningun grupo");
            ItemMeta meta = noParty.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Usa " + ChatColor.YELLOW + "/party invite <jugador>" + ChatColor.GRAY + " para crear uno.");
                meta.setLore(lore);
                noParty.setItemMeta(meta);
            }
            inv.setItem(13, noParty);
            player.openInventory(inv);
            return;
        }

        int slot = 10;
        for (UUID memberUuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(memberUuid);
            String name = p != null ? p.getName() : "Desconectado";
            boolean isLeader = party.isLeader(memberUuid);

            ItemStack head = SkullAdapter.createPlayerHead(memberUuid, name);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((isLeader ? ChatColor.GOLD + "★ " : ChatColor.YELLOW) + name);
                List<String> lore = new ArrayList<>();
                lore.add(isLeader ? ChatColor.GOLD + "Lider del Grupo" : ChatColor.GRAY + "Miembro");
                if (party.isLeader(player.getUniqueId()) && !isLeader) {
                    lore.add("");
                    lore.add(ChatColor.RED + "Clic para expulsar");
                }
                meta.setLore(lore);
                head.setItemMeta(meta);
            }

            inv.setItem(slot++, head);
            if (slot == 17) slot = 19;
        }

        ItemStack leaveBtn = createItem(Material.RED_BED, ChatColor.RED + "Abandonar Grupo");
        inv.setItem(31, leaveBtn);

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
