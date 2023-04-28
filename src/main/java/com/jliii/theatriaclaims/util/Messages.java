package com.jliii.theatriaclaims.util;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.tasks.SendPlayerMessageTask;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Messages {

    private CustomLogger customLogger;

    public Messages(CustomLogger customLogger) {
        this.customLogger = customLogger;
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, MessageType messageID, String... args) {
        sendMessage(player, color, messageID, 0, args);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, MessageType messageID, long delayInTicks, String... args) {
        String message = TheatriaClaims.instance.dataStore.getMessage(messageID, args);
        sendMessage(player, color, message, delayInTicks);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, String message) {
        if (message == null || message.length() == 0) return;

        if (player == null) {
            TheatriaClaims.AddLogEntry(color + message);
        }
        else {
            player.sendMessage(color + message);
        }
    }

    public static void sendMessage(Player player, TextMode textMode, String message) {
        if (message == null || message.length() == 0) return;

        if (player == null) {
            TheatriaClaims.AddLogEntry(textMode.getColor() + message);
        }
        else {
            player.sendMessage(textMode.getColor() + message);
        }
    }

    public static void sendMessage(Player player, ChatColor color, String message, long delayInTicks) {
        SendPlayerMessageTask task = new SendPlayerMessageTask(player, color, message);

        //Only schedule if there should be a delay. Otherwise, send the message right now, else the message will appear out of order.
        if (delayInTicks > 0) {
            TheatriaClaims.instance.getServer().getScheduler().runTaskLater(TheatriaClaims.instance, task, delayInTicks);
        }
        else {
            task.run();
        }
    }
}
