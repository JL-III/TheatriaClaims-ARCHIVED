package com.jliii.theatriaclaims.util;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.managers.ConfigManager;
import com.jliii.theatriaclaims.tasks.SendPlayerMessageTask;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Messages {
    
    public Messages() {
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, MessageType messageID, CustomLogger customLogger, String... args) {
        sendMessage(player, color, messageID, 0, customLogger, args);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, MessageType messageID, long delayInTicks, CustomLogger customLogger, String... args) {
        String message = TheatriaClaims.instance.dataStore.getMessage(messageID, args);
        sendMessage(player, color, message, delayInTicks, customLogger);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, String message) {
        if (message == null || message.length() == 0) return;

        if (player == null) {
            CustomLogger.logBare(color + message);
        }
        else {
            player.sendMessage(color + message);
        }
    }

    public static void sendMessage(Player player, TextMode textMode, String message) {
        if (message == null || message.length() == 0) return;

        if (player == null) {
            CustomLogger.logBare(textMode.getColor() + message);
        }
        else {
            player.sendMessage(textMode.getColor() + message);
        }
    }

    public static void sendMessage(Player player, ChatColor color, String message, long delayInTicks, CustomLogger customLogger) {
        SendPlayerMessageTask task = new SendPlayerMessageTask(player, color, message, customLogger);

        //Only schedule if there should be a delay. Otherwise, send the message right now, else the message will appear out of order.
        if (delayInTicks > 0) {
            TheatriaClaims.instance.getServer().getScheduler().runTaskLater(TheatriaClaims.instance, task, delayInTicks);
        }
        else {
            task.run();
        }
    }
}
