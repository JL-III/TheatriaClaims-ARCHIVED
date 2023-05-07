package com.jliii.theatriaclaims.config;

import com.jliii.theatriaclaims.util.CustomLogger;
import org.bukkit.configuration.file.FileConfiguration;

public class ClaimsConfig {

    private FileConfiguration config;
    private CustomLogger customLogger;

    public boolean preventGlobalMonsterEggs;
    public boolean preventTheft;
    public boolean protectCreatures;
    public boolean protectHorses;
    public boolean protectDonkeys;
    public boolean protectLlamas;
    public boolean preventButtonsSwitches;
    public boolean lockWoodenDoors;
    public boolean lockTrapDoors;
    public boolean lockFenceGates;
    public boolean preventNonPlayerCreatedPortals;    // whether portals where we cannot determine the creating player should be prevented from creation in claims
    public boolean enderPearlsRequireAccessTrust;        //whether teleporting into a claim with a pearl requires access trust
    public boolean raidTriggersRequireBuildTrust;      //whether raids are triggered by a player that doesn't have build permission in that claim
    public boolean villagerTradingRequiresTrust;      //whether trading with a claimed villager requires permission
    public boolean firespreads;                        //whether fire will spread in claims
    public boolean firedamages;                        //whether fire will damage in claims
    public boolean lecternReadingRequiresAccessTrust;                    //reading lecterns requires access trust
    public boolean blockClaimExplosions;                     //whether explosions may destroy claimed blocks
    public boolean endermenMoveBlocks;                        //whether or not endermen may move blocks around
    public boolean ravagersBreakBlocks;                //whether or not ravagers may break blocks in claims
    public boolean limitTreeGrowth;                          //whether trees should be prevented from growing into a claim from outside

    public ClaimsConfig(FileConfiguration fileConfiguration, CustomLogger customLogger) {
        config = fileConfiguration;
        loadConfigurationValues();
    }

    public void loadConfigurationValues() {
        preventGlobalMonsterEggs = config.getBoolean("GriefPrevention.Claims.PreventGlobalMonsterEggs", true);
        preventTheft = config.getBoolean("GriefPrevention.Claims.PreventTheft", true);
        protectCreatures = config.getBoolean("GriefPrevention.Claims.ProtectCreatures", true);
        protectHorses = config.getBoolean("GriefPrevention.Claims.ProtectHorses", true);
        protectDonkeys = config.getBoolean("GriefPrevention.Claims.ProtectDonkeys", true);
        protectLlamas = config.getBoolean("GriefPrevention.Claims.ProtectLlamas", true);
        preventButtonsSwitches = config.getBoolean("GriefPrevention.Claims.PreventButtonsSwitches", true);
        lockWoodenDoors = config.getBoolean("GriefPrevention.Claims.LockWoodenDoors", false);
        lockTrapDoors = config.getBoolean("GriefPrevention.Claims.LockTrapDoors", false);
        lockFenceGates = config.getBoolean("GriefPrevention.Claims.LockFenceGates", true);
        preventNonPlayerCreatedPortals = config.getBoolean("GriefPrevention.Claims.PreventNonPlayerCreatedPortals", false);
        enderPearlsRequireAccessTrust = config.getBoolean("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", true);
        raidTriggersRequireBuildTrust = config.getBoolean("GriefPrevention.Claims.RaidTriggersRequireBuildTrust", true);
        
        villagerTradingRequiresTrust = config.getBoolean("GriefPrevention.Claims.VillagerTradingRequiresPermission", true);
        
        ravagersBreakBlocks = config.getBoolean("GriefPrevention.Claims.RavagersBreakBlocks", true);
        firespreads = config.getBoolean("GriefPrevention.Claims.FireSpreadsInClaims", false);
        firedamages = config.getBoolean("GriefPrevention.Claims.FireDamagesInClaims", false);
        endermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
        lecternReadingRequiresAccessTrust = config.getBoolean("GriefPrevention.Claims.LecternReadingRequiresAccessTrust", true);
        blockClaimExplosions = config.getBoolean("GriefPrevention.BlockLandClaimExplosions", true);
        limitTreeGrowth = config.getBoolean("GriefPrevention.LimitTreeGrowth", false);

    }
}
