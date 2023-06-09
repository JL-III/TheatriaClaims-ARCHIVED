package com.jliii.theatriaclaims.player;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.database.DataStore;
import com.jliii.theatriaclaims.enums.ShovelMode;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.GeneralUtils;
import com.jliii.theatriaclaims.visualization.BoundaryVisualization;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

//holds all of GriefPrevention's player-tied data
public class PlayerData {

    private ConfigManager configManager;
    // the player's ID
    public UUID playerID;
    // the player's claims
    private Vector<Claim> claims = null;
    // how many claim blocks the player has earned via play time
    private Integer accruedClaimBlocks = null;
    // temporary holding area to avoid opening data files too early
    private int newlyAccruedClaimBlocks = 0;
    // where this player was the last time we checked on him for earning claim
    // blocks
    public Location lastAfkCheckLocation = null;
    // how many claim blocks the player has been gifted by admins, or purchased via
    // economy integration
    private Integer bonusClaimBlocks = null;
    // what "mode" the shovel is in determines what it will do when it's used
    public ShovelMode shovelMode = ShovelMode.Basic;
    // radius for restore nature fill mode
    public int fillRadius = 0;
    // last place the player used the shovel, useful in creating and resizing
    // claims,
    // because the player must use the shovel twice in those instances
    public Location lastShovelLocation = null;
    // the claim this player is currently resizing
    public Claim claimResizing = null;
    // the claim this player is currently subdividing
    public Claim claimSubdividing = null;
    // whether or not the player has a pending /trapped rescue
    public boolean pendingTrapped = false;
    // whether this player was recently warned about building outside land claims
    public boolean warnedAboutBuildingOutsideClaims = false;
    // whether the player was kicked (set and used during logout)
    public boolean wasKicked = false;

    // visualization
    private transient @Nullable BoundaryVisualization visibleBoundaries = null;

    // anti-camping pvp protection
    public boolean pvpImmune = false;

    public long lastSpawn = 0;

    // ignore claims mode
    public boolean ignoreClaims = false;

    // the last claim this player was in, that we know of
    public Claim lastClaim = null;

    // pvp
    public long lastPvpTimestamp = 0;
    public String lastPvpPlayer = "";

    // safety confirmation for deleting multi-subdivision claims
    public boolean warnedAboutMajorDeletion = false;

    public InetAddress ipAddress;

    // for addons to set per-player claim limits. Any negative value will use
    // config's value
    private int AccruedClaimBlocksLimit = -1;

    // whether or not this player has received a message about unlocking death drops
    // since his last death
    public boolean receivedDropUnlockAdvertisement = false;

    // whether or not this player's dropped items (on death) are unlocked for other
    // players to pick up
    public boolean dropsAreUnlocked = false;

    // message to send to player after he respawns
    public String messageOnRespawn = null;

    // player which a pet will be given to when it's right-clicked
    public OfflinePlayer petGiveawayRecipient = null;

    // timestamp for last "you're building outside your land claims" message
    public Long buildWarningTimestamp = null;

    // spot where a player can't talk, used to mute new players until they've moved
    // a little
    // this is an anti-bot strategy.
    public Location noChatLocation = null;

    // ignore list
    // true means invisible (admin-forced ignore), false means player-created ignore
    public ConcurrentHashMap<UUID, Boolean> ignoredPlayers = new ConcurrentHashMap<>();
    public boolean ignoreListChanged = false;

    // profanity warning, once per play session
    public boolean profanityWarned = false;

    public PlayerData(ConfigManager configManager) {
        this.configManager = configManager;
    }

    // the number of claim blocks a player has available for claiming land
    public int getRemainingClaimBlocks() {
        int remainingBlocks = this.getAccruedClaimBlocks() + this.getBonusClaimBlocks()
                + TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getGroupBonusBlocks(this.playerID);
        for (int i = 0; i < this.getClaims().size(); i++) {
            Claim claim = this.getClaims().get(i);
            remainingBlocks -= claim.getArea();
        }
        return remainingBlocks;
    }

    // don't load data from secondary storage until it's needed
    public synchronized int getAccruedClaimBlocks() {
        if (this.accruedClaimBlocks == null)
            this.loadDataFromSecondaryStorage();
        // update claim blocks with any he has accrued during his current play session
        if (this.newlyAccruedClaimBlocks > 0) {
            int accruedLimit = this.getAccruedClaimBlocksLimit();

            // if over the limit before adding blocks, leave it as-is, because the limit may
            // have changed AFTER he accrued the blocks
            if (this.accruedClaimBlocks < accruedLimit) {
                // move any in the holding area
                int newTotal = this.accruedClaimBlocks + this.newlyAccruedClaimBlocks;
                // respect limits
                this.accruedClaimBlocks = Math.min(newTotal, accruedLimit);
            }
            this.newlyAccruedClaimBlocks = 0;
            return this.accruedClaimBlocks;
        }
        return accruedClaimBlocks;
    }

    public void setAccruedClaimBlocks(Integer accruedClaimBlocks) {
        this.accruedClaimBlocks = accruedClaimBlocks;
        this.newlyAccruedClaimBlocks = 0;
    }

    public int getBonusClaimBlocks() {
        if (this.bonusClaimBlocks == null)
            this.loadDataFromSecondaryStorage();
        return bonusClaimBlocks;
    }

    public void setBonusClaimBlocks(Integer bonusClaimBlocks) {
        this.bonusClaimBlocks = bonusClaimBlocks;
    }

    private void loadDataFromSecondaryStorage() {
        // reach out to secondary storage to get any data there
        PlayerData storageData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerDataFromStorage(this.playerID);

        if (this.accruedClaimBlocks == null) {
            if (storageData.accruedClaimBlocks != null) {
                this.accruedClaimBlocks = storageData.accruedClaimBlocks;

                // ensure at least minimum accrued are accrued (in case of settings changes to
                // increase initial amount)
                if (configManager.getSystemConfig().advanced_fixNegativeClaimblockAmounts
                        && (this.accruedClaimBlocks < configManager.getSystemConfig().initialBlocks)) {
                    this.accruedClaimBlocks = configManager.getSystemConfig().initialBlocks;
                }

            } else {
                this.accruedClaimBlocks = configManager.getSystemConfig().initialBlocks;
            }
        }

        if (this.bonusClaimBlocks == null) {
            if (storageData.bonusClaimBlocks != null) {
                this.bonusClaimBlocks = storageData.bonusClaimBlocks;
            } else {
                this.bonusClaimBlocks = 0;
            }
        }
    }

    public Vector<Claim> getClaims() {
        if (this.claims == null) {
            this.claims = new Vector<>();

            // find all the claims belonging to this player and note them for future
            // reference
            DataStore dataStore = TheatriaClaims.getInstance().getDatabaseManager().getDataStore();
            int totalClaimsArea = 0;
            for (int i = 0; i < dataStore.claims.size(); i++) {
                Claim claim = dataStore.claims.get(i);
                if (!claim.inDataStore) {
                    dataStore.claims.remove(i--);
                    continue;
                }
                if (playerID.equals(claim.ownerID)) {
                    this.claims.add(claim);
                    totalClaimsArea += claim.getArea();
                }
            }

            // ensure player has claim blocks for his claims, and at least the minimum
            // accrued
            this.loadDataFromSecondaryStorage();

            // if total claimed area is more than total blocks available
            int totalBlocks = this.accruedClaimBlocks + this.getBonusClaimBlocks()
                    + TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getGroupBonusBlocks(this.playerID);
            if (configManager.getSystemConfig().advanced_fixNegativeClaimblockAmounts && totalBlocks < totalClaimsArea) {
                OfflinePlayer player = TheatriaClaims.getInstance().getServer().getOfflinePlayer(this.playerID);
                CustomLogger
                        .log(player.getName() + " has more claimed land than blocks available.  Adding blocks to fix.");
                CustomLogger.log(player.getName() + " Accrued blocks: " + this.getAccruedClaimBlocks()
                        + " Bonus blocks: " + this.getBonusClaimBlocks());
                CustomLogger.log("Total blocks: " + totalBlocks + " Total claimed area: " + totalClaimsArea);
                for (Claim claim : this.claims) {
                    if (!claim.inDataStore)
                        continue;
                    CustomLogger.log(GeneralUtils.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " // " + GeneralUtils.getfriendlyLocationString(claim.getGreaterBoundaryCorner()) + " = " + claim.getArea());
                }

                // try to fix it by adding to accrued blocks
                this.accruedClaimBlocks = totalClaimsArea; // Set accrued blocks to equal total claims
                int accruedLimit = this.getAccruedClaimBlocksLimit();
                this.accruedClaimBlocks = Math.min(accruedLimit, this.accruedClaimBlocks); // set accrued blocks to
                                                                                           // maximum limit, if it's
                                                                                           // smaller
                CustomLogger.log("New accrued blocks: " + this.accruedClaimBlocks);

                // Recalculate total blocks (accrued + bonus + permission group bonus)
                totalBlocks = this.accruedClaimBlocks + this.getBonusClaimBlocks()
                        + TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getGroupBonusBlocks(this.playerID);
                        CustomLogger.log("New total blocks: " + totalBlocks);

                // if that didn't fix it, then make up the difference with bonus blocks
                if (totalBlocks < totalClaimsArea) {
                    int bonusBlocksToAdd = totalClaimsArea - totalBlocks;
                    this.bonusClaimBlocks += bonusBlocksToAdd;
                    CustomLogger.log(
                            "Accrued blocks weren't enough. Adding " + bonusBlocksToAdd + " bonus blocks.");
                }
                CustomLogger.log(player.getName() + " Accrued blocks: " + this.getAccruedClaimBlocks() + " Bonus blocks: " + this.getBonusClaimBlocks() + " Group Bonus Blocks: " + TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getGroupBonusBlocks(this.playerID));
                // Recalculate total blocks (accrued + bonus + permission group bonus)
                totalBlocks = this.accruedClaimBlocks + this.getBonusClaimBlocks()
                        + TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getGroupBonusBlocks(this.playerID);
                CustomLogger.log("Total blocks: " + totalBlocks + " Total claimed area: " + totalClaimsArea);
                CustomLogger.log("Remaining claim blocks to use: " + this.getRemainingClaimBlocks() + " (should be 0)");
            }
        }

        for (int i = 0; i < this.claims.size(); i++) {
            if (!claims.get(i).inDataStore) {
                claims.remove(i--);
            }
        }

        return claims;
    }

    // Limit can be changed by addons
    public int getAccruedClaimBlocksLimit() {
        if (this.AccruedClaimBlocksLimit < 0)
            return configManager.getSystemConfig().maxAccruedBlocks_default;
        return this.AccruedClaimBlocksLimit;
    }

    public void setAccruedClaimBlocksLimit(int limit) {
        this.AccruedClaimBlocksLimit = limit;
    }

    public void accrueBlocks(int howMany) {
        this.newlyAccruedClaimBlocks += howMany;
    }

    public @Nullable BoundaryVisualization getVisibleBoundaries() {
        return visibleBoundaries;
    }

    public void setVisibleBoundaries(@Nullable BoundaryVisualization visibleBoundaries) {
        if (this.visibleBoundaries != null) {
            this.visibleBoundaries.revert(Bukkit.getPlayer(playerID));
        }
        this.visibleBoundaries = visibleBoundaries;
    }

}