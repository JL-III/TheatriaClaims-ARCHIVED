package com.jliii.theatriaclaims.util;


import com.jliii.theatriaclaims.managers.ConfigManager;
import org.bukkit.Bukkit;

public class CustomLogger {

    private final String pluginPrefix;

    public CustomLogger(ConfigManager configManager) {
        this.pluginPrefix = configManager.getSystemConfig().getPluginPrefix();
    }

    public void log(String message) {
        Bukkit.getConsoleSender().sendMessage(pluginPrefix + message);
    }

    public static void logBare(String message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }
}
