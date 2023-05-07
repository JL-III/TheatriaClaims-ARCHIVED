package com.jliii.theatriaclaims.config;

import com.jliii.theatriaclaims.enums.PistonMode;
import com.jliii.theatriaclaims.util.CustomLogger;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;

public class ClaimsConfig {

    private FileConfiguration config;
    private CustomLogger customLogger;

    public Material claims_investigationTool;                //which material will be used to investigate claims with a right click
    public Material claims_modificationTool;                    //which material will be used to create/resize claims with a right click
    public boolean claims_preventGlobalMonsterEggs;
    public boolean claims_preventTheft;
    public boolean claims_protectCreatures;
    public boolean claims_protectHorses;
    public boolean claims_protectDonkeys;
    public boolean claims_protectLlamas;
    public boolean claims_preventButtonsSwitches;
    public boolean claims_lockWoodenDoors;
    public boolean claims_lockTrapDoors;
    public boolean claims_lockFenceGates;
    public boolean claims_preventNonPlayerCreatedPortals;    // whether portals where we cannot determine the creating player should be prevented from creation in claims
    public boolean claims_enderPearlsRequireAccessTrust;        //whether teleporting into a claim with a pearl requires access trust
    public boolean claims_raidTriggersRequireBuildTrust;      //whether raids are triggered by a player that doesn't have build permission in that claim
    public boolean claims_villagerTradingRequiresTrust;      //whether trading with a claimed villager requires permission
    public int claims_initialBlocks;                            //the number of claim blocks a new player starts with
    public double claims_abandonReturnRatio;                 //the portion of claim blocks returned to a player when a claim is abandoned
    public int claims_blocksAccruedPerHour_default;            //how many additional blocks players get each hour of play (can be zero) without any special permissions
    public int claims_maxAccruedBlocks_default;                //the limit on accrued blocks (over time) for players without any special permissions.  doesn't limit purchased or admin-gifted blocks
    public int claims_accruedIdleThreshold;                    //how far (in blocks) a player must move in order to not be considered afk/idle when determining accrued claim blocks
    public int claims_accruedIdlePercent;                    //how much percentage of claim block accruals should idle players get
    public int claims_expirationDays;                        //how many days of inactivity before a player loses his claims
    public int claims_expirationExemptionTotalBlocks;        //total claim blocks amount which will exempt a player from claim expiration
    public int claims_expirationExemptionBonusBlocks;        //bonus claim blocks amount which will exempt a player from claim expiration
    public int claims_automaticClaimsForNewPlayersRadius;    //how big automatic new player claims (when they place a chest) should be.  -1 to disable
    public int claims_automaticClaimsForNewPlayersRadiusMin; //how big automatic new player claims must be. 0 to disable
    public int claims_minWidth;                                //minimum width for non-admin claims
    public int claims_minArea;                               //minimum area for non-admin claims
    public int claims_chestClaimExpirationDays;                //number of days of inactivity before an automatic chest claim will be deleted
    public ArrayList<String> claims_commandsRequiringAccessTrust; //the list of slash commands requiring access trust when in a claim
    public boolean claims_supplyPlayerManual;                //whether to give new players a book with land claim help in it
    public int claims_manualDeliveryDelaySeconds;            //how long to wait before giving a book to a new player
    public boolean claims_firespreads;                        //whether fire will spread in claims
    public boolean claims_firedamages;                        //whether fire will damage in claims
    public boolean claims_lecternReadingRequiresAccessTrust;                    //reading lecterns requires access trust
    public boolean blockClaimExplosions;                     //whether explosions may destroy claimed blocks
    public boolean claims_endermenMoveBlocks;                        //whether or not endermen may move blocks around
    public boolean claims_ravagersBreakBlocks;                //whether or not ravagers may break blocks in claims
    public boolean silverfishBreakBlocks;                    //whether silverfish may break blocks
    public boolean creaturesTrampleCrops;                    //whether or not non-player entities may trample crops
    public boolean rabbitsEatCrops;                          //whether or not rabbits may eat crops
    public boolean zombiesBreakDoors;                        //whether or not hard-mode zombies may break down wooden doors
    public boolean limitTreeGrowth;                          //whether trees should be prevented from growing into a claim from outside

    public ClaimsConfig(FileConfiguration fileConfiguration, CustomLogger customLogger) {
        config = fileConfiguration;
        loadConfigurationValues();
    }

    public void loadConfigurationValues() {
        claims_preventGlobalMonsterEggs = config.getBoolean("GriefPrevention.Claims.PreventGlobalMonsterEggs", true);
        claims_preventTheft = config.getBoolean("GriefPrevention.Claims.PreventTheft", true);
        claims_protectCreatures = config.getBoolean("GriefPrevention.Claims.ProtectCreatures", true);
        claims_protectHorses = config.getBoolean("GriefPrevention.Claims.ProtectHorses", true);
        claims_protectDonkeys = config.getBoolean("GriefPrevention.Claims.ProtectDonkeys", true);
        claims_protectLlamas = config.getBoolean("GriefPrevention.Claims.ProtectLlamas", true);
        claims_preventButtonsSwitches = config.getBoolean("GriefPrevention.Claims.PreventButtonsSwitches", true);
        claims_lockWoodenDoors = config.getBoolean("GriefPrevention.Claims.LockWoodenDoors", false);
        claims_lockTrapDoors = config.getBoolean("GriefPrevention.Claims.LockTrapDoors", false);
        claims_lockFenceGates = config.getBoolean("GriefPrevention.Claims.LockFenceGates", true);
        claims_preventNonPlayerCreatedPortals = config.getBoolean("GriefPrevention.Claims.PreventNonPlayerCreatedPortals", false);
        claims_enderPearlsRequireAccessTrust = config.getBoolean("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", true);
        claims_raidTriggersRequireBuildTrust = config.getBoolean("GriefPrevention.Claims.RaidTriggersRequireBuildTrust", true);
        claims_initialBlocks = config.getInt("GriefPrevention.Claims.InitialBlocks", 100);
        claims_blocksAccruedPerHour_default = config.getInt("GriefPrevention.Claims.BlocksAccruedPerHour", 100);
        claims_blocksAccruedPerHour_default = config.getInt("GriefPrevention.Claims.Claim Blocks Accrued Per Hour.Default", claims_blocksAccruedPerHour_default);
        claims_maxAccruedBlocks_default = config.getInt("GriefPrevention.Claims.MaxAccruedBlocks", 80000);
        claims_maxAccruedBlocks_default = config.getInt("GriefPrevention.Claims.Max Accrued Claim Blocks.Default", claims_maxAccruedBlocks_default);
        claims_accruedIdleThreshold = config.getInt("GriefPrevention.Claims.AccruedIdleThreshold", 0);
        claims_accruedIdleThreshold = config.getInt("GriefPrevention.Claims.Accrued Idle Threshold", claims_accruedIdleThreshold);
        claims_accruedIdlePercent = config.getInt("GriefPrevention.Claims.AccruedIdlePercent", 0);
        claims_abandonReturnRatio = config.getDouble("GriefPrevention.Claims.AbandonReturnRatio", 1.0D);
        claims_automaticClaimsForNewPlayersRadius = config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", 4);
        claims_automaticClaimsForNewPlayersRadiusMin = Math.max(0, Math.min(claims_automaticClaimsForNewPlayersRadius, config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadiusMinimum", 0)));
        claims_minWidth = config.getInt("GriefPrevention.Claims.MinimumWidth", 5);
        claims_minArea = config.getInt("GriefPrevention.Claims.MinimumArea", 100);
        claims_villagerTradingRequiresTrust = config.getBoolean("GriefPrevention.Claims.VillagerTradingRequiresPermission", true);
        claims_expirationDays = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.DaysInactive", 60);
        claims_expirationExemptionTotalBlocks = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasTotalClaimBlocks", 10000);
        claims_expirationExemptionBonusBlocks = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasBonusClaimBlocks", 5000);
        claims_supplyPlayerManual = config.getBoolean("GriefPrevention.Claims.DeliverManuals", true);
        claims_manualDeliveryDelaySeconds = config.getInt("GriefPrevention.Claims.ManualDeliveryDelaySeconds", 30);
        claims_ravagersBreakBlocks = config.getBoolean("GriefPrevention.Claims.RavagersBreakBlocks", true);
        claims_firespreads = config.getBoolean("GriefPrevention.Claims.FireSpreadsInClaims", false);
        claims_firedamages = config.getBoolean("GriefPrevention.Claims.FireDamagesInClaims", false);
        claims_chestClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.ChestClaimDays", 7);
        claims_endermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
        silverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
        creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
        rabbitsEatCrops = config.getBoolean("GriefPrevention.RabbitsEatCrops", true);
        zombiesBreakDoors = config.getBoolean("GriefPrevention.HardModeZombiesBreakDoors", false);
        claims_lecternReadingRequiresAccessTrust = config.getBoolean("GriefPrevention.Claims.LecternReadingRequiresAccessTrust", true);
        blockClaimExplosions = config.getBoolean("GriefPrevention.BlockLandClaimExplosions", true);
        limitTreeGrowth = config.getBoolean("GriefPrevention.LimitTreeGrowth", false);

        //default for claim investigation tool
        String investigationToolMaterialName = Material.STICK.name();

        //get investigation tool from config
        investigationToolMaterialName = config.getString("GriefPrevention.Claims.InvestigationTool", investigationToolMaterialName);

        //default for claim creation/modification tool
        String modificationToolMaterialName = Material.GOLDEN_SHOVEL.name();

        //get modification tool from config
        modificationToolMaterialName = config.getString("GriefPrevention.Claims.ModificationTool", modificationToolMaterialName);

        //validate investigation tool
        claims_investigationTool = Material.getMaterial(investigationToolMaterialName);
        if (claims_investigationTool == null) {
            customLogger.log("ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.yml.");
            claims_investigationTool = Material.STICK;
        }

        //validate modification tool
        claims_modificationTool = Material.getMaterial(modificationToolMaterialName);
        if (claims_modificationTool == null) {
            customLogger.log("ERROR: Material " + modificationToolMaterialName + " not found.  Defaulting to the golden shovel.  Please update your config.yml.");
            claims_modificationTool = Material.GOLDEN_SHOVEL;
        }
        claims_commandsRequiringAccessTrust = new ArrayList<>();

        String accessTrustSlashCommands = config.getString("GriefPrevention.Claims.CommandsRequiringAccessTrust", "/sethome");


        String[] commands = accessTrustSlashCommands.split(";");
        for (String command : commands) {
            if (!command.isEmpty()) {
                claims_commandsRequiringAccessTrust.add(command.trim().toLowerCase());
            }
        }
    }
}
