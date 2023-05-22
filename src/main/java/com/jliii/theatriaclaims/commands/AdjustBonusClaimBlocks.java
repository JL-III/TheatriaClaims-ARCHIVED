package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.player.PlayerName;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class AdjustBonusClaimBlocks implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //adjustbonusclaimblocks <player> <amount> or [<permission>] amount
        else if (cmd.getName().equalsIgnoreCase("adjustbonusclaimblocks"))
        {
            //requires exactly two parameters, the other player or group's name and the adjustment
            if (args.length != 2) return false;

            //parse the adjustment amount
            int adjustment;
            try
            {
                adjustment = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException numberFormatException)
            {
                return false;  //causes usage to be displayed
            }

            //if granting blocks to all players with a specific permission
            if (args[0].startsWith("[") && args[0].endsWith("]"))
            {
                String permissionIdentifier = args[0].substring(1, args[0].length() - 1);
                int newTotal = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().adjustGroupBonusBlocks(permissionIdentifier, adjustment);

                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AdjustGroupBlocksSuccess, permissionIdentifier, String.valueOf(adjustment), String.valueOf(newTotal));
                if (player != null)
                    CustomLogger.log(player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");

                return true;
            }

            //otherwise, find the specified player
            OfflinePlayer targetPlayer;
            try
            {
                UUID playerID = UUID.fromString(args[0]);
                targetPlayer = TheatriaClaims.getInstance().getServer().getOfflinePlayer(playerID);

            }
            catch (IllegalArgumentException e)
            {
                targetPlayer = PlayerName.resolvePlayerByName(args[0]);
            }

            if (targetPlayer == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                return true;
            }

            //give blocks to player
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(targetPlayer.getUniqueId());
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().savePlayerData(targetPlayer.getUniqueId(), playerData);

            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment), String.valueOf(playerData.getBonusClaimBlocks()));
            if (player != null)
                CustomLogger.log(player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".");

            return true;
        }
        return false;
    }
}
