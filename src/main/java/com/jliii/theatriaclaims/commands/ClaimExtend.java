package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.database.DataStore;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ClaimExtend implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //extendclaim
        if (cmd.getName().equalsIgnoreCase("extendclaim") && player != null) {
            if (args.length < 1) {
                if (configManager.getSystemConfig().claimsEnabledForWorld(player.getLocation().getWorld())) {
                    Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
                }
                return false;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException e) {
                if (configManager.getSystemConfig().claimsEnabledForWorld(player.getLocation().getWorld())) {
                    Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
                }
                return false;
            }

            //requires claim modification tool in hand
            if (player.getGameMode() != GameMode.CREATIVE && player.getItemInHand().getType() != configManager.getSystemConfig().modificationTool) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.MustHoldModificationToolForThat);
                return true;
            }

            //must be standing in a land claim
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true, playerData.lastClaim);
            if (claim == null) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.StandInClaimToResize);
                return true;
            }

            //must have permission to edit the land claim you're in
            Supplier<String> errorMessage = claim.checkPermission(player, ClaimPermission.Edit, null);
            if (errorMessage != null) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.NotYourClaim);
                return true;
            }

            //determine new corner coordinates
            org.bukkit.util.Vector direction = player.getLocation().getDirection();
            if (direction.getY() > .75) {
                Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.ClaimsExtendToSky);
                return true;
            }

            if (direction.getY() < -.75) {
                Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.ClaimsAutoExtendDownward);
                return true;
            }

            Location lc = claim.getLesserBoundaryCorner();
            Location gc = claim.getGreaterBoundaryCorner();
            int newx1 = lc.getBlockX();
            int newx2 = gc.getBlockX();
            int newz1 = lc.getBlockZ();
            int newz2 = gc.getBlockZ();

            //if changing Z only
            if (Math.abs(direction.getX()) < .3) {
                if (direction.getZ() > 0) {
                    newz2 += amount;  //north
                }
                else {
                    newz1 -= amount;  //south
                }
            }

            //if changing X only
            else if (Math.abs(direction.getZ()) < .3) {
                if (direction.getX() > 0) {
                    newx2 += amount;  //east
                }
                else {
                    newx1 -= amount;  //west
                }
            }

            //diagonals
            else {
                if (direction.getX() > 0) {
                    newx2 += amount;
                }
                else {
                    newx1 -= amount;
                }

                if (direction.getZ() > 0) {
                    newz2 += amount;
                }
                else {
                    newz1 -= amount;
                }
            }

            //attempt resize
            playerData.claimResizing = claim;
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().resizeClaimWithChecks(player, playerData, newx1, newx2, newz1, newz2);
            playerData.claimResizing = null;

            return true;
        }
        return false;
    }
}
