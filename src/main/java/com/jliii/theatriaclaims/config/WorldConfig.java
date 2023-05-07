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

    public WorldConfig(FileConfiguration config) {
        this.config = config;
    }

    public void loadConfigurationValues() {
        claims_respectWorldGuard = config.getBoolean("GriefPrevention.Claims.CreationRequiresWorldGuardBuildPermission", true);
        fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
        fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);
        pistonExplosionSound = config.getBoolean("GriefPrevention.PistonExplosionSound", true);
        pistonMovement = PistonMode.of(config.getString("GriefPrevention.PistonMovement", "CLAIMS_ONLY"));
        if (config.isBoolean("GriefPrevention.LimitPistonsToLandClaims") && !config.getBoolean("GriefPrevention.LimitPistonsToLandClaims"))
            pistonMovement = PistonMode.EVERYWHERE_SIMPLE;
        if (config.isBoolean("GriefPrevention.CheckPistonMovement") && !config.getBoolean("GriefPrevention.CheckPistonMovement"))
            pistonMovement = PistonMode.IGNORED;
    }
}
