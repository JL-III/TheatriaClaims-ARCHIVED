/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jliii.theatriaclaims.tasks;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.ShovelMode;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.database.DataStore;
import com.jliii.theatriaclaims.util.GeneralUtils;
import com.jliii.theatriaclaims.util.Messages;
import com.jliii.theatriaclaims.util.PlayerData;
import com.jliii.theatriaclaims.visualization.BoundaryVisualization;
import com.jliii.theatriaclaims.visualization.VisualizationType;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

//tells a player about how many claim blocks he has, etc
//implemented as a task so that it can be delayed
//otherwise, it's spammy when players mouse-wheel past the shovel in their hot bars
public class EquipShovelProcessingTask implements Runnable {
    //player data
    private final Player player;

    private ConfigManager configManager;

    public EquipShovelProcessingTask(Player player, ConfigManager configManager){
        this.player = player;
        this.configManager = configManager;
    }

    @Override
    public void run() {
        //if he's not holding the golden shovel anymore, do nothing
        if (GeneralUtils.getItemInHand(player, EquipmentSlot.HAND).getType() != configManager.getSystemConfig().modificationTool)
            return;
        PlayerData playerData = TheatriaClaims.instance.getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
        //reset any work he might have been doing
        playerData.lastShovelLocation = null;
        playerData.claimResizing = null;

        //always reset to basic claims mode
        if (playerData.shovelMode != ShovelMode.Basic) {
            playerData.shovelMode = ShovelMode.Basic;
            Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.ShovelBasicClaimMode);
        }

        //tell him how many claim blocks he has available
        int remainingBlocks = playerData.getRemainingClaimBlocks();
        Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.RemainingBlocks, String.valueOf(remainingBlocks));

        if (configManager.getSystemConfig().claimsEnabledForWorld(player.getWorld())) {
            Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
        }

        //if standing in a claim owned by the player, visualize it
        Claim claim = TheatriaClaims.instance.getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true, playerData.lastClaim);
        if (claim != null && claim.checkPermission(player, ClaimPermission.Edit, null) == null) {
            playerData.lastClaim = claim;
            BoundaryVisualization.visualizeClaim(player, claim, VisualizationType.CLAIM, configManager);
        }
    }
}
