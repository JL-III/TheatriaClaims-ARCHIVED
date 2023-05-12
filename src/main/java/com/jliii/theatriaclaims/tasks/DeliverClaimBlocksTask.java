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
import com.jliii.theatriaclaims.events.AccrueClaimBlocksEvent;
import com.jliii.theatriaclaims.managers.ConfigManager;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.DataStore;
import com.jliii.theatriaclaims.util.PlayerData;
import org.bukkit.entity.Player;

import java.util.Collection;

//FEATURE: give players claim blocks for playing, as long as they're not away from their computer

//runs every 5 minutes in the main thread, grants blocks per hour / 12 to each online player who appears to be actively playing
public class DeliverClaimBlocksTask implements Runnable {
    private final Player player;
    private final TheatriaClaims instance;
    private final ConfigManager configManager;
    private final int idleThresholdSquared;
    private final CustomLogger customLogger;

    public DeliverClaimBlocksTask(Player player, TheatriaClaims instance, ConfigManager configManager, CustomLogger customLogger) {
        this.player = player;
        this.instance = instance;
        this.configManager = configManager;
        this.customLogger = customLogger;
        this.idleThresholdSquared = configManager.getSystemConfig().accruedIdleThreshold * configManager.getSystemConfig().accruedIdleThreshold;
    }


    //TODO wtf is going on here? why are we creating more versions of this task? where is the contructor getting its input? how are we creating things based on this constructor?
    @Override
    public void run() {
        //if no player specified, this task will create a player-specific task for each online player, scheduled one tick apart
        if (this.player == null) {
            @SuppressWarnings("unchecked")
            Collection<Player> players = (Collection<Player>) instance.getServer().getOnlinePlayers();

            long i = 0;
            for (Player onlinePlayer : players) {
                DeliverClaimBlocksTask newTask = new DeliverClaimBlocksTask(onlinePlayer, instance, configManager, customLogger);
                instance.getServer().getScheduler().scheduleSyncDelayedTask(instance, newTask, i++);
            }

            return; //tasks started for each player
        }

        //deliver claim blocks to the specified player
        if (!this.player.isOnline()) {
            return; //player is not online to receive claim blocks
        }

        DataStore dataStore = instance.dataStore;
        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());

        // check if player is idle. considered idle if
        //    in vehicle or is in water (pushed by water)
        //    or has not moved at least defined blocks since last check
        boolean isIdle = false;
        try {
            isIdle = player.isInsideVehicle() || player.getLocation().getBlock().isLiquid() ||
                    !(playerData.lastAfkCheckLocation == null || playerData.lastAfkCheckLocation.distanceSquared(player.getLocation()) > idleThresholdSquared);
        }
        //can't measure distance when to/from are different worlds
        catch (IllegalArgumentException ignore) {
        }
        //remember current location for next time
        playerData.lastAfkCheckLocation = player.getLocation();

        try {
            //determine how fast blocks accrue for this player //RoboMWM: addons determine this instead
            int accrualRate = configManager.getSystemConfig().blocksAccruedPerHour_default;

            //determine idle accrual rate when idle
            if (isIdle) {
                if (configManager.getSystemConfig().accruedIdlePercent <= 0) {
                    customLogger.log(player.getName() + " wasn't active enough to accrue claim blocks this round.");
                    return; //idle accrual percentage is disabled
                }
                accrualRate = (int) (accrualRate * (configManager.getSystemConfig().accruedIdlePercent / 100.0D));
            }

            //fire event for addons
            AccrueClaimBlocksEvent event = new AccrueClaimBlocksEvent(player, accrualRate, isIdle);
            instance.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                customLogger.log(player.getName() + " claim block delivery was canceled by another plugin.");
                return; //event was cancelled
            }

            //set actual accrual
            accrualRate = event.getBlocksToAccrue();
            if (accrualRate < 0) accrualRate = 0;
            playerData.accrueBlocks(accrualRate);
            customLogger.log("Delivering " + event.getBlocksToAccrue() + " blocks to " + player.getName());

            //intentionally NOT saving data here to reduce overall secondary storage access frequency
            //many other operations will cause this player's data to save, including his eventual logout
            //dataStore.savePlayerData(player.getUniqueIdentifier(), playerData);
        }
        catch (Exception e) {
            customLogger.log("Problem delivering claim blocks to player " + player.getName() + ":");
            e.printStackTrace();
        }
    }
}
