package com.argusrazor.friends.listener;

import com.argusrazor.friends.adapter.SoundAdapter;
import com.argusrazor.friends.config.ConfigManager;
import com.argusrazor.friends.database.DatabaseManager;
import com.argusrazor.friends.gui.FriendsGUI;
import com.argusrazor.friends.gui.PartyGUI;
import com.argusrazor.friends.gui.SettingsGUI;
import com.argusrazor.friends.model.PlayerSettings;
import com.argusrazor.friends.service.FriendService;
import com.argusrazor.friends.service.PartyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {

    private final DatabaseManager db;
    private final FriendService friendService;
    private final PartyService partyService;
    private final ConfigManager config;

    public InventoryClickListener(DatabaseManager db, FriendService friendService, PartyService partyService, ConfigManager config) {
        this.db = db;
        this.friendService = friendService;
        this.partyService = partyService;
        this.config = config;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        String friendsPrefix = config.color(config.getConfig().getString("GUI.FriendsMenu.Title", "&8Menu de Amigos"));
        int parenIndex = friendsPrefix.indexOf('(');
        if (parenIndex > 0) friendsPrefix = friendsPrefix.substring(0, parenIndex).trim();

        String partyTitle = config.color(config.getConfig().getString("GUI.PartyMenu.Title", "&8Menu de Grupo"));
        String settingsTitle = config.color(config.getConfig().getString("GUI.SettingsMenu.Title", "&8Ajustes de Privacidad"));

        boolean isFriendsGUI = title.startsWith(friendsPrefix);
        boolean isPartyGUI = title.equals(partyTitle);
        boolean isSettingsGUI = title.equals(settingsTitle);

        if (!isFriendsGUI && !isPartyGUI && !isSettingsGUI) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        int slot = event.getRawSlot();

        if (isFriendsGUI) {
            handleFriendsGUIClick(player, clicked, slot, event.getClick(), title);
        } else if (isPartyGUI) {
            handlePartyGUIClick(player, clicked, slot);
        } else {
            handleSettingsGUIClick(player, slot);
        }
    }

    private void handleFriendsGUIClick(Player player, ItemStack clicked, int slot, ClickType click, String title) {
        if (slot == 45) { // Pagina anterior
            int curPage = parseCurrentPage(title);
            FriendsGUI.open(player, friendService, config, Math.max(1, curPage - 1));
            return;
        }
        if (slot == 49) { // Ajustes
            SettingsGUI.open(player, db, config);
            return;
        }
        if (slot == 53) { // Pagina siguiente
            int curPage = parseCurrentPage(title);
            FriendsGUI.open(player, friendService, config, curPage + 1);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String targetName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                player.closeInventory();
                friendService.removeFriend(player, targetName);
            } else if (click.isRightClick()) {
                Player target = Bukkit.getPlayer(targetName);
                if (target != null && target.isOnline()) {
                    partyService.invitePlayer(player, target);
                } else {
                    player.sendMessage(config.getPrefix() + config.getMessage("Friend.PlayerOffline", "PLAYER", targetName));
                }
            } else if (click.isLeftClick()) {
                player.closeInventory();
                player.sendMessage(config.getPrefix() + ChatColor.YELLOW + "Escribe " + ChatColor.AQUA + "/f msg " + targetName + " <mensaje>" + ChatColor.YELLOW + " para hablar.");
            }
        }
    }

    private void handlePartyGUIClick(Player player, ItemStack clicked, int slot) {
        if (slot == 31) { // Salir del grupo
            player.closeInventory();
            partyService.leaveParty(player);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String targetName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).replace("★", "").trim();
            partyService.kickPlayer(player, targetName);
            PartyGUI.open(player, partyService, config);
        }
    }

    private void handleSettingsGUIClick(Player player, int slot) {
        PlayerSettings settings = db.getSettings(player.getUniqueId());

        if (slot == 11) { // Solicitudes
            settings.setAllowRequests(!settings.isAllowRequests());
            db.saveSettings(player.getUniqueId(), settings);
            SettingsGUI.open(player, db, config);
        } else if (slot == 13) { // Mensajes
            settings.setAllowMessages(!settings.isAllowMessages());
            db.saveSettings(player.getUniqueId(), settings);
            SettingsGUI.open(player, db, config);
        } else if (slot == 15) { // Notificaciones
            settings.setNotifyJoinQuit(!settings.isNotifyJoinQuit());
            db.saveSettings(player.getUniqueId(), settings);
            SettingsGUI.open(player, db, config);
        } else if (slot == 22) { // Volver
            FriendsGUI.open(player, friendService, config, 1);
        }
    }

    private int parseCurrentPage(String title) {
        try {
            int openParen = title.lastIndexOf('(');
            int slash = title.lastIndexOf('/');
            if (openParen != -1 && slash != -1 && slash > openParen) {
                String pageStr = title.substring(openParen + 1, slash).trim();
                return Integer.parseInt(pageStr);
            }
        } catch (Exception ignored) {
        }
        return 1;
    }
}
