package com.jliii.theatriaclaims.util;


import com.jliii.theatriaclaims.managers.ConfigManager;
import org.bukkit.Bukkit;

public class CustomLogger {

    private final ConfigManager configManager;

    private final String pluginPrefix;

    public CustomLogger(ConfigManager configManager) {
        this.configManager = configManager;
        this.pluginPrefix = configManager.getPluginPrefix();
    }

    public void log(String message) {
        Bukkit.getConsoleSender().sendMessage(pluginPrefix + message);
    }
}
