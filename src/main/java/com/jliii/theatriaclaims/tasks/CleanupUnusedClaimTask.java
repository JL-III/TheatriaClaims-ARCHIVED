package com.jliii.theatriaclaims.tasks;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.events.ClaimExpirationEvent;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.util.Calendar;
import java.util.Date;

public class CleanupUnusedClaimTask implements Runnable {
    Claim claim;
    PlayerData ownerData;
    OfflinePlayer ownerInfo;
    ConfigManager configManager;

    public CleanupUnusedClaimTask(Claim claim, PlayerData ownerData, OfflinePlayer ownerInfo, ConfigManager configManager) {
        this.claim = claim;
        this.ownerData = ownerData;
        this.ownerInfo = ownerInfo;
        this.configManager = configManager;
    }

    @Override
    public void run() {
        //determine area of the default chest claim
        int areaOfDefaultClaim = 0;
        if (configManager.getSystemConfig().automaticClaimsForNewPlayersRadius >= 0) {
            areaOfDefaultClaim = (int) Math.pow(configManager.getSystemConfig().automaticClaimsForNewPlayersRadius * 2 + 1, 2);
        }

        //if this claim is a chest claim and those are set to expire
        if (ownerData.getClaims().size() == 1 && claim.getArea() <= areaOfDefaultClaim && configManager.getSystemConfig().chestClaimExpirationDays > 0) {
            //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
            Calendar sevenDaysAgo = Calendar.getInstance();
            sevenDaysAgo.add(Calendar.DATE, -configManager.getSystemConfig().chestClaimExpirationDays);
            if (sevenDaysAgo.getTime().after(new Date(ownerInfo.getLastPlayed()))) {
                if (expireEventCanceled())
                    return;
                TheatriaClaims.instance.getDatabaseManager().getDataStore().deleteClaim(claim, true, true);
                CustomLogger.log(" " + claim.getOwnerName() + "'s new player claim expired.");
            }
        }

        //if configured to always remove claims after some inactivity period without exceptions...
        else if (configManager.getSystemConfig().expirationDays > 0) {
            Calendar earliestPermissibleLastLogin = Calendar.getInstance();
            earliestPermissibleLastLogin.add(Calendar.DATE, -configManager.getSystemConfig().expirationDays);

            if (earliestPermissibleLastLogin.getTime().after(new Date(ownerInfo.getLastPlayed()))) {
                if (expireEventCanceled())
                    return;
                //delete them
                TheatriaClaims.instance.getDatabaseManager().getDataStore().deleteClaimsForPlayer(claim.ownerID, true);
                CustomLogger.log(" All of " + claim.getOwnerName() + "'s claims have expired.");
                CustomLogger.log("earliestPermissibleLastLogin#getTime: " + earliestPermissibleLastLogin.getTime());
                CustomLogger.log("ownerInfo#getLastPlayed: " + ownerInfo.getLastPlayed());
            }
        }
    }

    public boolean expireEventCanceled() {
        //see if any other plugins don't want this claim deleted
        ClaimExpirationEvent event = new ClaimExpirationEvent(this.claim);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}
