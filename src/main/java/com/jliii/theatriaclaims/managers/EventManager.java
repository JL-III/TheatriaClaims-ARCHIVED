package com.jliii.theatriaclaims.managers;

import org.bukkit.Bukkit;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.listeners.BlockEventHandler;
import com.jliii.theatriaclaims.listeners.EconomyHandler;
import com.jliii.theatriaclaims.listeners.EntityEventHandler;
import com.jliii.theatriaclaims.listeners.PlayerEventHandler;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.DataStore;

public class EventManager {

    public static void registerEvents(TheatriaClaims plugin, DataStore dataStore, ConfigManager configManager) {
        //player events
        Bukkit.getPluginManager().registerEvents(new PlayerEventHandler(plugin, dataStore, configManager), plugin);
        //block events
        Bukkit.getPluginManager().registerEvents(new BlockEventHandler(dataStore, configManager), plugin);
        //entity events
        EntityEventHandler entityEventHandler = new EntityEventHandler(dataStore, plugin, configManager);
        plugin.entityEventHandler = entityEventHandler;
        Bukkit.getPluginManager().registerEvents(entityEventHandler, plugin);
        //vault-based economy integration
        Bukkit.getPluginManager().registerEvents(new EconomyHandler(plugin, configManager), plugin);

        CustomLogger.log("Events registered!");
    }
}
