package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.CreateClaimResult;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.database.DataStore;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.Messages;
import com.jliii.theatriaclaims.visualization.BoundaryVisualization;
import com.jliii.theatriaclaims.visualization.VisualizationType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Claim implements CommandExecutor {
    private final ConfigManager configManager;

    public Claim(ConfigManager configManager) {
        this.configManager = configManager;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) {
            CustomLogger.log("You must be a player to run this command!");
            return false;
        }

        //claim
        if (!configManager.getSystemConfig().claimsEnabledForWorld(player.getWorld())) {
            Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ClaimsDisabledWorld);
            return true;
        }

        PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());

        //default is chest claim radius, unless -1
        int radius = configManager.getSystemConfig().automaticClaimsForNewPlayersRadius;
        if (radius < 0) radius = (int) Math.ceil(Math.sqrt(configManager.getSystemConfig().minArea) / 2);

        //if player has any claims, respect claim minimum size setting
        if (playerData.getClaims().size() > 0) {
            //if player has exactly one land claim, this requires the claim modification tool to be in hand (or creative mode player)
            if (playerData.getClaims().size() == 1 && player.getGameMode() != GameMode.CREATIVE && player.getItemInHand().getType() != configManager.getSystemConfig().modificationTool) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.MustHoldModificationToolForThat);
                return true;
            }

            radius = (int) Math.ceil(Math.sqrt(configManager.getSystemConfig().minArea) / 2);
        }

        //allow for specifying the radius
        if (args.length > 0) {
            if (playerData.getClaims().size() < 2 && player.getGameMode() != GameMode.CREATIVE && player.getItemInHand().getType() != configManager.getSystemConfig().modificationTool) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.RadiusRequiresGoldenShovel);
                return true;
            }

            int specifiedRadius;
            try {
                specifiedRadius = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException e) {
                return false;
            }

            if (specifiedRadius < radius) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.MinimumRadius, String.valueOf(radius));
                return true;
            }
            else {
                radius = specifiedRadius;
            }
        }

        if (radius < 0) radius = 0;

        Location lc = player.getLocation().add(-radius, 0, -radius);
        Location gc = player.getLocation().add(radius, 0, radius);

        //player must have sufficient unused claim blocks
        int area = Math.abs((gc.getBlockX() - lc.getBlockX() + 1) * (gc.getBlockZ() - lc.getBlockZ() + 1));
        int remaining = playerData.getRemainingClaimBlocks();
        if (remaining < area) {
            Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.CreateClaimInsufficientBlocks, String.valueOf(area - remaining));
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().tryAdvertiseAdminAlternatives(player);
            return true;
        }

        CreateClaimResult result = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().createClaim(lc.getWorld(),
                lc.getBlockX(), gc.getBlockX(),
                lc.getBlockZ(), gc.getBlockZ(),
                player.getUniqueId(), null, null, player);
        if (!result.succeeded || result.claim == null) {
            if (result.claim != null) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.CreateClaimFailOverlapShort);

                BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CONFLICT_ZONE, configManager);
            }
            else {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.CreateClaimFailOverlapRegion);
            }
        }
        else {
            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.CreateClaimSuccess);

            if (configManager.getSystemConfig().claimsEnabledForWorld(player.getWorld())) {
                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
            }
            BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CLAIM, configManager);
            playerData.claimResizing = null;
            playerData.lastShovelLocation = null;

        }

        return true;
    }
}
