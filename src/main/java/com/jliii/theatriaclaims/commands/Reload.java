package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Reload implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        else if (cmd.getName().equalsIgnoreCase("gpreload"))
        {
            configManager.loadConfig();
            configManager.getMessagesConfig().loadMessages();
            if (player != null)
            {
                Messages.sendMessage(player, TextMode.Success.getColor(), "Configuration updated.  If you have updated your Grief Prevention JAR, you still need to /reload or reboot your server.");
            }
            else
            {
                CustomLogger.log("Configuration updated.  If you have updated your Grief Prevention JAR, you still need to /reload or reboot your server.");
            }

            return true;
        }
        return false;
    }
}
