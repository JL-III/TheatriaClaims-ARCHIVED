package com.jliii.theatriaclaims.events;

import com.jliii.theatriaclaims.config.ConfigManager;
import org.bukkit.Bukkit;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.listeners.BlockEventHandler;
import com.jliii.theatriaclaims.listeners.EconomyHandler;
import com.jliii.theatriaclaims.listeners.EntityEventHandler;
import com.jliii.theatriaclaims.listeners.PlayerEventHandler;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.database.DataStore;
//TODO this is not instantiated yet, lets make on in main class
public class EventManager {
    private EntityEventHandler entityEventHandler;

    private EconomyHandler economyHandler;


    public EventManager(TheatriaClaims plugin, DataStore dataStore, ConfigManager configManager) {
        registerEvents(plugin, dataStore, configManager);
    }

    public void registerEvents(TheatriaClaims plugin, DataStore dataStore, ConfigManager configManager) {
        //player events
        Bukkit.getPluginManager().registerEvents(new PlayerEventHandler(plugin, dataStore, configManager), plugin);
        //block events
        Bukkit.getPluginManager().registerEvents(new BlockEventHandler(dataStore, configManager), plugin);
        //entity events
        EntityEventHandler entityEventHandler = new EntityEventHandler(dataStore, plugin, configManager);
        setEntityEventHandler(entityEventHandler);
        Bukkit.getPluginManager().registerEvents(entityEventHandler, plugin);
        //vault-based economy integration
        EconomyHandler economyHandler = new EconomyHandler(plugin, configManager);
        Bukkit.getPluginManager().registerEvents(economyHandler, plugin);
        setEconomyHandler(economyHandler);
        CustomLogger.log("Events registered!");
    }

    public EntityEventHandler getEntityEventHandler() {
        return entityEventHandler;
    }

    public void setEntityEventHandler(EntityEventHandler entityEventHandler) { this.entityEventHandler = entityEventHandler; }

    public EconomyHandler getEconomyHandler() { return economyHandler; }

    public void setEconomyHandler(EconomyHandler economyHandler) { this.economyHandler = economyHandler; }
}
