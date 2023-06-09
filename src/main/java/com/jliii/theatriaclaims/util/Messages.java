package com.jliii.theatriaclaims.util;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.tasks.SendPlayerMessageTask;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;

public class Messages {

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ConfigManager configManager, NamedTextColor color, MessageType messageID, String... args) {
        sendMessage(player, configManager, color, messageID, 0, args);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ConfigManager configManager, NamedTextColor color, MessageType messageID, long delayInTicks, String... args) {
        String message =  configManager.getMessagesConfig().getMessage(messageID, args);
        sendMessage(player, color, message, delayInTicks);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, NamedTextColor color, String message) {
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

    public static void sendMessage(Player player, NamedTextColor color, String message, long delayInTicks) {
        SendPlayerMessageTask task = new SendPlayerMessageTask(player, color, message);

        //Only schedule if there should be a delay. Otherwise, send the message right now, else the message will appear out of order.
        if (delayInTicks > 0) {
            TheatriaClaims.getInstance().getServer().getScheduler().runTaskLater(TheatriaClaims.getInstance(), task, delayInTicks);
        }
        else {
            task.run();
        }
    }
}
