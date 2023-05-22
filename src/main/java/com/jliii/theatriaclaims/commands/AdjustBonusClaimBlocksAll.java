package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public class AdjustBonusClaimBlocksAll implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //adjustbonusclaimblocksall <amount>
        else if (cmd.getName().equalsIgnoreCase("adjustbonusclaimblocksall"))
        {
            //requires exactly one parameter, the amount of adjustment
            if (args.length != 1) return false;

            //parse the adjustment amount
            int adjustment;
            try
            {
                adjustment = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException numberFormatException)
            {
                return false;  //causes usage to be displayed
            }

            //for each online player
            @SuppressWarnings("unchecked")
            Collection<Player> players = (Collection<Player>) TheatriaClaims.getInstance().getServer().getOnlinePlayers();
            StringBuilder builder = new StringBuilder();
            for (Player onlinePlayer : players)
            {
                UUID playerID = onlinePlayer.getUniqueId();
                PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(playerID);
                playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
                TheatriaClaims.getInstance().getDatabaseManager().getDataStore().savePlayerData(playerID, playerData);
                builder.append(onlinePlayer.getName()).append(' ');
            }

            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AdjustBlocksAllSuccess, String.valueOf(adjustment));
            CustomLogger.log("Adjusted all " + players.size() + "players' bonus claim blocks by " + adjustment + ".  " + builder.toString());

            return true;
        }
        return false;
    }
}
