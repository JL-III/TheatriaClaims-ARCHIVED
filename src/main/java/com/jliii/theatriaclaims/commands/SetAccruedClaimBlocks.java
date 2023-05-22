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

public class SetAccruedClaimBlocks implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //setaccruedclaimblocks <player> <amount>
        else if (cmd.getName().equalsIgnoreCase("setaccruedclaimblocks"))
        {
            //requires exactly two parameters, the other player's name and the new amount
            if (args.length != 2) return false;

            //parse the adjustment amount
            int newAmount;
            try
            {
                newAmount = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException numberFormatException)
            {
                return false;  //causes usage to be displayed
            }

            //find the specified player
            OfflinePlayer targetPlayer = PlayerName.resolvePlayerByName(args[0]);
            if (targetPlayer == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                return true;
            }

            //set player's blocks
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(targetPlayer.getUniqueId());
            playerData.setAccruedClaimBlocks(newAmount);
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().savePlayerData(targetPlayer.getUniqueId(), playerData);

            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.SetClaimBlocksSuccess);
            if (player != null)
                CustomLogger.log(player.getName() + " set " + targetPlayer.getName() + "'s accrued claim blocks to " + newAmount + ".");

            return true;
        }
        return false;
    }
}
