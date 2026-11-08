package com.argusrazor.friends;

import com.argusrazor.friends.adapter.VersionAdapter;
import com.argusrazor.friends.command.AdminCommand;
import com.argusrazor.friends.command.FriendCommand;
import com.argusrazor.friends.command.PartyCommand;
import com.argusrazor.friends.config.ConfigManager;
import com.argusrazor.friends.database.DatabaseManager;
import com.argusrazor.friends.database.MySQLDatabase;
import com.argusrazor.friends.database.SQLiteDatabase;
import com.argusrazor.friends.gui.FriendsGUI;
import com.argusrazor.friends.gui.PartyGUI;
import com.argusrazor.friends.listener.InventoryClickListener;
import com.argusrazor.friends.listener.PlayerConnectionListener;
import com.argusrazor.friends.placeholder.ArgusFriendsExpansion;
import com.argusrazor.friends.service.FriendService;
import com.argusrazor.friends.service.PartyService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

public class ArgusRazorFriendsPlugin extends JavaPlugin {

    private static ArgusRazorFriendsPlugin instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private FriendService friendService;
    private PartyService partyService;

    public static ArgusRazorFriendsPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("==================================================");
        getLogger().info("  Iniciando ArgusRazorFriends v1.0.0-MC26.3");
        getLogger().info("  Servidor detectado: " + VersionAdapter.getServerVersion());
        getLogger().info("  Compatibilidad 26.3+: " + (VersionAdapter.isMinecraft26OrHigher() ? "SI" : "MODO RETROCOMPATIBLE"));
        getLogger().info("==================================================");

        // 1. Configuracion
        configManager = new ConfigManager(this);
        configManager.load();

        // 2. Base de datos
        String storageType = configManager.getConfig().getString("Storage", "SQLite");
        if (storageType.equalsIgnoreCase("MySQL")) {
            String host = configManager.getConfig().getString("MySQL.Host", "localhost");
            int port = configManager.getConfig().getInt("MySQL.Port", 3306);
            String dbName = configManager.getConfig().getString("MySQL.Database", "friends");
            String user = configManager.getConfig().getString("MySQL.Username", "root");
            String pass = configManager.getConfig().getString("MySQL.Password", "password");
            String prefix = configManager.getConfig().getString("MySQL.TablePrefix", "fr_");
            boolean ssl = configManager.getConfig().getBoolean("MySQL.UseSSL", false);
            int maxPool = configManager.getConfig().getInt("MySQL.Pool.MaxConnections", 10);

            databaseManager = new MySQLDatabase(host, port, dbName, user, pass, prefix, ssl, maxPool, getLogger());
        } else {
            File dbFile = new File(getDataFolder(), configManager.getConfig().getString("SQLite.FileName", "database.db"));
            databaseManager = new SQLiteDatabase(dbFile, "fr_", getLogger());
        }

        try {
            databaseManager.init();
            getLogger().info("[ArgusRazorFriends] Base de datos (" + storageType + ") inicializada correctamente.");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "[ArgusRazorFriends] Error fatal al conectar con la base de datos", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Servicios
        friendService = new FriendService(databaseManager, configManager);
        partyService = new PartyService(configManager);

        // 4. Comandos
        FriendCommand friendCmd = new FriendCommand(friendService, databaseManager, configManager);
        PartyCommand partyCmd = new PartyCommand(partyService, configManager);
        AdminCommand adminCmd = new AdminCommand(configManager);

        registerCommand("friend", friendCmd);
        registerCommand("party", partyCmd);
        registerCommand("argusfriends", adminCmd);

        PluginCommand fgui = getCommand("friendsgui");
        if (fgui != null) {
            fgui.setExecutor((sender, command, label, args) -> {
                if (sender instanceof Player p) {
                    FriendsGUI.open(p, friendService, configManager, 1);
                }
                return true;
            });
        }

        PluginCommand pgui = getCommand("partygui");
        if (pgui != null) {
            pgui.setExecutor((sender, command, label, args) -> {
                if (sender instanceof Player p) {
                    PartyGUI.open(p, partyService, configManager);
                }
                return true;
            });
        }

        // 5. Eventos / Listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(databaseManager, friendService, partyService, configManager), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(databaseManager, friendService, partyService, configManager), this);

        // 6. Tarea recurrente de limpieza de solicitudes expiradas
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (friendService != null) {
                friendService.cleanExpiredRequests();
            }
        }, 1200L, 1200L);

        // 7. Integracion con PlaceholderAPI (si esta instalado)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ArgusFriendsExpansion(friendService, partyService).register();
            getLogger().info("[ArgusRazorFriends] Extension de PlaceholderAPI registrada exitosamente.");
        }

        getLogger().info("[ArgusRazorFriends] Plugin cargado y listo para operar!");
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            if (executor instanceof org.bukkit.command.CommandExecutor ce) {
                cmd.setExecutor(ce);
            }
            if (executor instanceof org.bukkit.command.TabCompleter tc) {
                cmd.setTabCompleter(tc);
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("[ArgusRazorFriends] Desactivando plugin...");
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("[ArgusRazorFriends] Plugin desactivado correctamente.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public FriendService getFriendService() {
        return friendService;
    }

    public PartyService getPartyService() {
        return partyService;
    }
}
