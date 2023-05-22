package com.jliii.theatriaclaims.claim;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.database.DataStore;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.Messages;
import com.jliii.theatriaclaims.visualization.BoundaryVisualization;
import com.jliii.theatriaclaims.visualization.VisualizationType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ClaimManager {

    public void createClaimFromCommand(Player player, ConfigManager configManager, Location lc, Location gc, PlayerData playerData) {

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
    }

}