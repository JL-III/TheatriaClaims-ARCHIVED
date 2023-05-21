package com.jliii.theatriaclaims;

import com.jliii.theatriaclaims.commands.ChungusCommand;
import com.jliii.theatriaclaims.database.DataStore;
import com.jliii.theatriaclaims.database.DatabaseManager;
import com.jliii.theatriaclaims.listeners.EconomyHandler;
import com.jliii.theatriaclaims.listeners.EntityEventHandler;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.events.EventManager;
import com.jliii.theatriaclaims.tasks.TaskManager;
import com.jliii.theatriaclaims.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TheatriaClaims extends JavaPlugin {
    public static TheatriaClaims instance;
    private DatabaseManager databaseManager;
    EconomyHandler economyHandler;
    ConfigManager configManager;
    //helper method to resolve a player by name
    public ConcurrentHashMap<String, UUID> playerNameToIDMap = new ConcurrentHashMap<>();

    public void onEnable() {
        instance = this;
        configManager = new ConfigManager(this);
        CustomLogger.log("Finished loading configuration.");

        databaseManager = new DatabaseManager(this, configManager);
        databaseManager.initializeDataStore();
        CustomLogger.log("Finished loading database.");

        new TaskManager(this, configManager).registerScheduledSyncRepeatingTasks();

        EventManager.registerEvents(this, getDatabaseManager().getDataStore(), configManager);

        Objects.requireNonNull(Bukkit.getPluginCommand("gp")).setExecutor(new ChungusCommand(economyHandler, configManager));

        CacheOfflinePlayerNamesThread namesThread = new CacheOfflinePlayerNamesThread(configManager, getServer().getOfflinePlayers(), this.playerNameToIDMap);
        namesThread.setPriority(Thread.MIN_PRIORITY);
        namesThread.start();

        CustomLogger.log("Boot finished.");
    }

    public void onDisable() {
        databaseManager.onDisablePlugin();
        CustomLogger.log("Plugin has disabled.");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

}