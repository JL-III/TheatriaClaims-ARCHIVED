package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class RestrictSubclaim implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //restrictsubclaim
        else if (cmd.getName().equalsIgnoreCase("restrictsubclaim") && player != null)
        {
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true, playerData.lastClaim);
            if (claim == null || claim.parent == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.StandInSubclaim);
                return true;
            }

            // If player has /ignoreclaims on, continue
            // If admin claim, fail if this user is not an admin
            // If not an admin claim, fail if this user is not the owner
            if (!playerData.ignoreClaims && (claim.isAdminClaim() ? !player.hasPermission("TheatriaClaims.adminclaims") : !player.getUniqueId().equals(claim.parent.ownerID)))
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.OnlyOwnersModifyClaims, claim.getOwnerName());
                return true;
            }

            if (claim.getSubclaimRestrictions())
            {
                claim.setSubclaimRestrictions(false);
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.SubclaimUnrestricted);
            }
            else
            {
                claim.setSubclaimRestrictions(true);
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.SubclaimRestricted);
            }
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().saveClaim(claim);
            return true;
        }
        return false;
    }
}
