package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DeleteAllAdminClaims implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //deletealladminclaims
        else if (player != null && cmd.getName().equalsIgnoreCase("deletealladminclaims"))
        {
            if (!player.hasPermission("TheatriaClaims.deleteclaims"))
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.NoDeletePermission);
                return true;
            }

            //delete all admin claims
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaimsForPlayer(null, true);  //null for owner id indicates an administrative claim

            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AllAdminDeleted);
            if (player != null)
            {
                CustomLogger.log(player.getName() + " deleted all administrative claims.");

                //revert any current visualization
                TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId()).setVisibleBoundaries(null);
            }

            return true;
        }
        return false;
    }
}
