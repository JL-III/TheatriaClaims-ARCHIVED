package com.jliii.theatriaclaims;

import com.jliii.theatriaclaims.commands.ChungusCommand;
import com.jliii.theatriaclaims.database.DatabaseManager;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.events.EventManager;
import com.jliii.theatriaclaims.player.CacheOfflinePlayerNamesThread;
import com.jliii.theatriaclaims.tasks.TaskManager;
import com.jliii.theatriaclaims.util.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TheatriaClaims extends JavaPlugin {
    private static TheatriaClaims instance;
    private DatabaseManager databaseManager;
    private EventManager eventManager;
    private ConfigManager configManager;
    private ConcurrentHashMap<String, UUID> playerNameToIDMap = new ConcurrentHashMap<>();

    public void onEnable() {
        instance = this;
        setConfigManager(new ConfigManager(this));
        CustomLogger.log("Finished loading configuration.");

        setDatabaseManager(new DatabaseManager(this, configManager));
        databaseManager.initializeDataStore();
        CustomLogger.log("Finished loading database.");

        setEventManager(new EventManager(this, getDatabaseManager().getDataStore(), configManager));

        new TaskManager(this, configManager).registerScheduledSyncRepeatingTasks();

        Objects.requireNonNull(Bukkit.getPluginCommand("gp")).setExecutor(new ChungusCommand(eventManager.getEconomyHandler(), configManager));

        CacheOfflinePlayerNamesThread namesThread = new CacheOfflinePlayerNamesThread(configManager, getServer().getOfflinePlayers(), this.playerNameToIDMap);
        namesThread.setPriority(Thread.MIN_PRIORITY);
        namesThread.start();

        CustomLogger.log("Boot finished.");
    }

    public void onDisable() {
        databaseManager.onDisablePlugin();
        CustomLogger.log("Plugin has disabled.");
    }

    public static TheatriaClaims getInstance() { return instance; }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    public void setDatabaseManager(DatabaseManager databaseManager) { this.databaseManager = databaseManager; }
    public EventManager getEventManager() {
        return eventManager;
    }
    public void setEventManager(EventManager eventManager) { this.eventManager = eventManager; }
    public ConcurrentHashMap<String, UUID> getPlayerNameToIDMap() {
        return playerNameToIDMap;
    }
    public void setPlayerNameToIDMap(ConcurrentHashMap<String, UUID> concurrentHashMap) {
        this.playerNameToIDMap = concurrentHashMap;
    }
    public ConfigManager getConfigManager() { return configManager; }
    public void setConfigManager(ConfigManager configManager) { this.configManager = configManager; }

}