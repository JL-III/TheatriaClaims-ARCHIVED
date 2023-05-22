package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.database.DataStore;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerName;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.GeneralUtils;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TransferClaim implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //transferclaim <player>
        else if (cmd.getName().equalsIgnoreCase("transferclaim") && player != null)
        {
            //which claim is the user in?
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true, null);
            if (claim == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.TransferClaimMissing);
                return true;
            }

            //check additional permission for admin claims
            if (claim.isAdminClaim() && !player.hasPermission("TheatriaClaims.adminclaims"))
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.TransferClaimPermission);
                return true;
            }

            UUID newOwnerID = null;  //no argument = make an admin claim
            String ownerName = "admin";

            if (args.length > 0)
            {
                OfflinePlayer targetPlayer = PlayerName.resolvePlayerByName(args[0]);
                if (targetPlayer == null)
                {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                    return true;
                }
                newOwnerID = targetPlayer.getUniqueId();
                ownerName = targetPlayer.getName();
            }

            //change ownerhsip
            try
            {
                TheatriaClaims.getInstance().getDatabaseManager().getDataStore().changeClaimOwner(claim, newOwnerID);
            }
            catch (DataStore.NoTransferException e)
            {
                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.TransferTopLevel);
                return true;
            }

            //confirm
            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.TransferSuccess);
            CustomLogger.log(player.getName() + " transferred a claim at " + GeneralUtils.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + ownerName + ".");

            return true;
        }
        return false;
    }
}
