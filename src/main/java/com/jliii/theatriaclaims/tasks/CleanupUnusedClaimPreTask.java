package com.jliii.theatriaclaims.tasks;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.managers.ConfigManager;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

//asynchronously loads player data without caching it in the datastore, then
//passes those data to a claim cleanup task which might decide to delete a claim for inactivity

class CleanupUnusedClaimPreTask implements Runnable {
    private final UUID ownerID;
    private final ConfigManager configManager;

    public CleanupUnusedClaimPreTask(UUID uuid, ConfigManager configManager) {
        this.ownerID = uuid;
        this.configManager = configManager;
    }

    @Override
    public void run() {
        //get the data
        PlayerData ownerData = TheatriaClaims.instance.dataStore.getPlayerDataFromStorage(ownerID);
        OfflinePlayer ownerInfo = Bukkit.getServer().getOfflinePlayer(ownerID);
        CustomLogger.log("Looking for expired claims.  Checking data for " + ownerID.toString());
        //expiration code uses last logout timestamp to decide whether to expire claims
        //don't expire claims for online players
        if (ownerInfo.isOnline()) {
            CustomLogger.log("Player is online. Ignoring.");
            return;
        }
        if (ownerInfo.getLastPlayed() <= 0) {
            CustomLogger.log("Player is new or not in the server's cached userdata. Ignoring. getLastPlayed = " + ownerInfo.getLastPlayed());
            return;
        }
        //skip claims belonging to exempted players based on block totals in config
        int bonusBlocks = ownerData.getBonusClaimBlocks();
        if (bonusBlocks >= configManager.getSystemConfig().expirationExemptionBonusBlocks || bonusBlocks + ownerData.getAccruedClaimBlocks() >= configManager.getSystemConfig().expirationExemptionTotalBlocks) {
            CustomLogger.log("Player exempt from claim expiration based on claim block counts vs. config file settings.");
            return;
        }
        Claim claimToExpire = null;
        for (Claim claim : TheatriaClaims.instance.dataStore.getClaims()) {
            if (ownerID.equals(claim.ownerID))
            {
                claimToExpire = claim;
                break;
            }
        }
        if (claimToExpire == null) {
            CustomLogger.log("Unable to find a claim to expire for " + ownerID.toString());
            return;
        }
        //pass it back to the main server thread, where it's safe to delete a claim if needed
        Bukkit.getScheduler().scheduleSyncDelayedTask(TheatriaClaims.instance, new CleanupUnusedClaimTask(claimToExpire, ownerData, ownerInfo, configManager), 1L);
    }
}