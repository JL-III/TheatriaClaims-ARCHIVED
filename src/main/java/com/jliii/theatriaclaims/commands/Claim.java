package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.ClaimManager;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Claim implements CommandExecutor {
    private final ConfigManager configManager;
    private ClaimManager claimManager;

    public Claim(ConfigManager configManager, ClaimManager claimManager) {
        this.configManager = configManager;
        this.claimManager = claimManager;
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

        if (args.length >= 2) {
            switch (args[1]) {
                case "list" -> {
                    //handle list command here
                }
                case "extend" -> {
                    //handle extend
                }
                default -> {

                }
            }
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

            int requestedRadius;
            try {
                requestedRadius = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException e) {
                return false;
            }

            if (requestedRadius < radius) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.MinimumRadius, String.valueOf(radius));
                return true;
            }
            else {
                radius = requestedRadius;
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
        claimManager.createClaimFromCommand(player, configManager, lc, gc, playerData);
        return true;
    }
}
