package com.jliii.theatriaclaims.util;

import org.bukkit.Bukkit;

public class CustomLogger {

    private static final String pluginPrefix = "[TheatriaClaims] ";

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
