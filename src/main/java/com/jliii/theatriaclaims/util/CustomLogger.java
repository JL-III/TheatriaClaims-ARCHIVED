package com.jliii.theatriaclaims.util;


import com.jliii.theatriaclaims.managers.ConfigManager;
import org.bukkit.Bukkit;

public class CustomLogger {

    private final ConfigManager configManager;

    private String pluginPrefix() {
        return configManager.config_plugin_prefix;
    }

    public CustomLogger(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void log(String message) {
        Bukkit.getConsoleSender().sendMessage(pluginPrefix() + message);
    }
}
