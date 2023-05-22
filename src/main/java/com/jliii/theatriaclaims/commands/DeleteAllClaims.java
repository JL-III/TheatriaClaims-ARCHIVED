package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerName;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DeleteAllClaims implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //deleteallclaims <player>
        else if (cmd.getName().equalsIgnoreCase("deleteallclaims"))
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //try to find that player
            OfflinePlayer otherPlayer = PlayerName.resolvePlayerByName(args[0]);
            if (otherPlayer == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                return true;
            }

            //delete all that player's claims
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaimsForPlayer(otherPlayer.getUniqueId(), true);

            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.DeleteAllSuccess, otherPlayer.getName());
            if (player != null)
            {
                CustomLogger.log(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".");

                //revert any current visualization
                if (player.isOnline())
                {
                    TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId()).setVisibleBoundaries(null);
                }
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("deleteclaimsinworld"))
        {
            //must be executed at the console
            if (player != null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ConsoleOnlyCommand);
                return true;
            }

            //requires exactly one parameter, the world name
            if (args.length != 1) return false;

            //try to find the specified world
            World world = Bukkit.getServer().getWorld(args[0]);
            if (world == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.WorldNotFound);
                return true;
            }

            //delete all claims in that world
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaimsInWorld(world, true);
            CustomLogger.log("Deleted all claims in world: " + world.getName() + ".");
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("deleteuserclaimsinworld"))
        {
            //must be executed at the console
            if (player != null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ConsoleOnlyCommand);
                return true;
            }

            //requires exactly one parameter, the world name
            if (args.length != 1) return false;

            //try to find the specified world
            World world = Bukkit.getServer().getWorld(args[0]);
            if (world == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.WorldNotFound);
                return true;
            }

            //delete all USER claims in that world
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaimsInWorld(world, false);
            CustomLogger.log("Deleted all user claims in world: " + world.getName() + ".");
            return true;
        }
        return false;
    }
}
