package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.GeneralUtils;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class DeleteClaim implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //deleteclaim
        else if (cmd.getName().equalsIgnoreCase("deleteclaim") && player != null)
        {
            //determine which claim the player is standing in
            com.jliii.theatriaclaims.claim.Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

            if (claim == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.DeleteClaimMissing);
            }
            else
            {
                //deleting an admin claim additionally requires the adminclaims permission
                if (!claim.isAdminClaim() || player.hasPermission("TheatriaClaims.adminclaims")) {
                    PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
                    if (claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion) {
                        Messages.sendMessage(player, configManager, TextMode.Warn.getColor(), MessageType.DeletionSubdivisionWarning);
                        playerData.warnedAboutMajorDeletion = true;
                    }
                    else {
                        // claim.removeSurfaceFluids(null);
                        TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaim(claim, true, true);
                        Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.DeleteSuccess);
                        CustomLogger.log(player.getName() + " deleted " + claim.getOwnerName() + "'s claim at " + GeneralUtils.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                        //revert any current visualization
                        playerData.setVisibleBoundaries(null);
                        playerData.warnedAboutMajorDeletion = false;
                    }
                }
                else
                {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.CantDeleteAdminClaim);
                }
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("claimexplosions") && player != null)
        {
            //determine which claim the player is standing in
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

            if (claim == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.DeleteClaimMissing);
            }
            else
            {
                Supplier<String> noBuildReason = claim.checkPermission(player, ClaimPermission.Build, null);
                if (noBuildReason != null)
                {
                    Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason.get());
                    return true;
                }

                if (claim.areExplosivesAllowed)
                {
                    claim.areExplosivesAllowed = false;
                    Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.ExplosivesDisabled);
                }
                else
                {
                    claim.areExplosivesAllowed = true;
                    Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.ExplosivesEnabled);
                }
            }

            return true;
        }
        return false;
    }
}
