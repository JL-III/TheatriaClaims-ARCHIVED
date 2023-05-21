package com.jliii.theatriaclaims.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class CustomLogger {

    private static final String pluginPrefix = ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "TheatriaClaims" + ChatColor.GREEN + "] ";

    public static void log(String message) {
        Bukkit.getConsoleSender().sendMessage(getPluginPrefix() + message);
    }

    public static void logBare(String message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public static String getPluginPrefix() {
        return pluginPrefix;
    }
}
