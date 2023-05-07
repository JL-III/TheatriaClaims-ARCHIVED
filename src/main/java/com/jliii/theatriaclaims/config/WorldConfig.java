package com.jliii.theatriaclaims.config;

import com.jliii.theatriaclaims.enums.PistonMode;
import org.bukkit.configuration.file.FileConfiguration;

public class WorldConfig {

    private FileConfiguration config;

    public boolean fireSpreads;                                //whether fire spreads outside of claims
    public boolean fireDestroys;                                //whether fire destroys blocks outside of claims
    public PistonMode pistonMovement;                            //Setting for piston check options
    public boolean pistonExplosionSound;                     //whether pistons make an explosion sound when they get removed
    public boolean claims_respectWorldGuard;                 //whether claim creations requires WG build permission in creation area
    public boolean silverfishBreakBlocks;                    //whether silverfish may break blocks
    public boolean creaturesTrampleCrops;                    //whether or not non-player entities may trample crops
    public boolean rabbitsEatCrops;                          //whether or not rabbits may eat crops
    public boolean zombiesBreakDoors;                        //whether or not hard-mode zombies may break down wooden doors

    public WorldConfig(FileConfiguration config) {
        this.config = config;
    }

    public void loadConfigurationValues() {
        claims_respectWorldGuard = config.getBoolean("GriefPrevention.Claims.CreationRequiresWorldGuardBuildPermission", true);
        fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
        fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);
        pistonExplosionSound = config.getBoolean("GriefPrevention.PistonExplosionSound", true);
        pistonMovement = PistonMode.of(config.getString("GriefPrevention.PistonMovement", "CLAIMS_ONLY"));
        silverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
        creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
        rabbitsEatCrops = config.getBoolean("GriefPrevention.RabbitsEatCrops", true);
        zombiesBreakDoors = config.getBoolean("GriefPrevention.HardModeZombiesBreakDoors", false);
        if (config.isBoolean("GriefPrevention.LimitPistonsToLandClaims") && !config.getBoolean("GriefPrevention.LimitPistonsToLandClaims"))
            pistonMovement = PistonMode.EVERYWHERE_SIMPLE;
        if (config.isBoolean("GriefPrevention.CheckPistonMovement") && !config.getBoolean("GriefPrevention.CheckPistonMovement"))
            pistonMovement = PistonMode.IGNORED;
        
    }
}
