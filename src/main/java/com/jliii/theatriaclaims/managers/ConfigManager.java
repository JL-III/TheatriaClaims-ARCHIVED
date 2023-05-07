package com.jliii.theatriaclaims.managers;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.ClaimsMode;
import com.jliii.theatriaclaims.enums.PistonMode;
import com.jliii.theatriaclaims.listeners.EconomyHandler;
import com.jliii.theatriaclaims.util.CustomLogger;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private TheatriaClaims theatriaClaims;
    private CustomLogger customLogger;
    FileConfiguration config;
    private EconomyHandler economyHandler;
    public ConcurrentHashMap<World, ClaimsMode> claims_worldModes;
    public boolean creativeWorldsExist;                     //note on whether there are any creative mode worlds, to save cpu cycles on a common hash lookup

    public boolean claims_preventGlobalMonsterEggs; //whether monster eggs can be placed regardless of trust.
    public boolean claims_preventTheft;                        //whether containers and crafting blocks are protectable
    public boolean claims_protectCreatures;                    //whether claimed animals may be injured by players without permission

    //TODO this config option can be converted to an array list instead of individual booleans
    public boolean claims_protectHorses;                        //whether horses on a claim should be protected by that claim's rules
    public boolean claims_protectDonkeys;                    //whether donkeys on a claim should be protected by that claim's rules
    public boolean claims_protectLlamas;                        //whether llamas on a claim should be protected by that claim's rules
    public boolean claims_preventButtonsSwitches;            //whether buttons and switches are protectable

    //TODO this config option can be converted to an array list instead of individual booleans
    public boolean claims_lockWoodenDoors;                    //whether wooden doors should be locked by default (require /accesstrust)
    public boolean claims_lockTrapDoors;                        //whether trap doors should be locked by default (require /accesstrust)
    public boolean claims_lockFenceGates;                    //whether fence gates should be locked by default (require /accesstrust)

    public boolean claims_preventNonPlayerCreatedPortals;    // whether portals where we cannot determine the creating player should be prevented from creation in claims
    public boolean claims_enderPearlsRequireAccessTrust;        //whether teleporting into a claim with a pearl requires access trust
    public boolean claims_raidTriggersRequireBuildTrust;      //whether raids are triggered by a player that doesn't have build permission in that claim
    public boolean claims_respectWorldGuard;                 //whether claim creations requires WG build permission in creation area
    public boolean claims_villagerTradingRequiresTrust;      //whether trading with a claimed villager requires permission

    public int claims_initialBlocks;                            //the number of claim blocks a new player starts with
    public double claims_abandonReturnRatio;                 //the portion of claim blocks returned to a player when a claim is abandoned
    public int claims_blocksAccruedPerHour_default;            //how many additional blocks players get each hour of play (can be zero) without any special permissions
    public int claims_maxAccruedBlocks_default;                //the limit on accrued blocks (over time) for players without any special permissions.  doesn't limit purchased or admin-gifted blocks
    public int claims_accruedIdleThreshold;                    //how far (in blocks) a player must move in order to not be considered afk/idle when determining accrued claim blocks
    public int claims_accruedIdlePercent;                    //how much percentage of claim block accruals should idle players get
    public int claims_maxDepth;                                //limit on how deep claims can go
    public int claims_expirationDays;                        //how many days of inactivity before a player loses his claims
    public int claims_expirationExemptionTotalBlocks;        //total claim blocks amount which will exempt a player from claim expiration
    public int claims_expirationExemptionBonusBlocks;        //bonus claim blocks amount which will exempt a player from claim expiration

    public int claims_automaticClaimsForNewPlayersRadius;    //how big automatic new player claims (when they place a chest) should be.  -1 to disable
    public int claims_automaticClaimsForNewPlayersRadiusMin; //how big automatic new player claims must be. 0 to disable
    public int claims_claimsExtendIntoGroundDistance;        //how far below the shoveled block a new claim will reach
    public int claims_minWidth;                                //minimum width for non-admin claims
    public int claims_minArea;                               //minimum area for non-admin claims

    public int claims_chestClaimExpirationDays;                //number of days of inactivity before an automatic chest claim will be deleted
    public boolean claims_survivalAutoNatureRestoration;        //whether survival claims will be automatically restored to nature when auto-deleted
    public boolean claims_allowTrappedInAdminClaims;            //whether it should be allowed to use /trapped in adminclaims.
    public Material claims_investigationTool;                //which material will be used to investigate claims with a right click
    public Material claims_modificationTool;                    //which material will be used to create/resize claims with a right click
    public ArrayList<String> claims_commandsRequiringAccessTrust; //the list of slash commands requiring access trust when in a claim
    //TODO replace this book with one we make
    public boolean claims_supplyPlayerManual;                //whether to give new players a book with land claim help in it
    public int claims_manualDeliveryDelaySeconds;            //how long to wait before giving a book to a new player
    public boolean claims_firespreads;                        //whether fire will spread in claims
    public boolean claims_firedamages;                        //whether fire will damage in claims
    public boolean claims_lecternReadingRequiresAccessTrust;                    //reading lecterns requires access trust

    public int economy_claimBlocksMaxBonus;                  //max "bonus" blocks a player can buy.  set to zero for no limit.
    public double economy_claimBlocksPurchaseCost;            //cost to purchase a claim block.  set to zero to disable purchase.
    public double economy_claimBlocksSellValue;                //return on a sold claim block.  set to zero to disable sale.

    public boolean blockClaimExplosions;                     //whether explosions may destroy claimed blocks
    public boolean blockSurfaceCreeperExplosions;            //whether creeper explosions near or above the surface destroy blocks
    public boolean blockSurfaceOtherExplosions;                //whether non-creeper explosions near or above the surface destroy blocks

    //TODO i think worldguard is doing this already.
    public boolean fireSpreads;                                //whether fire spreads outside of claims
    public boolean fireDestroys;                                //whether fire destroys blocks outside of claims

    public boolean visualizationAntiCheatCompat;              // whether to engage compatibility mode for anti-cheat plugins

    public boolean claims_endermenMoveBlocks;                        //whether or not endermen may move blocks around
    public boolean claims_ravagersBreakBlocks;                //whether or not ravagers may break blocks in claims
    public boolean silverfishBreakBlocks;                    //whether silverfish may break blocks
    public boolean creaturesTrampleCrops;                    //whether or not non-player entities may trample crops
    public boolean rabbitsEatCrops;                          //whether or not rabbits may eat crops
    public boolean zombiesBreakDoors;                        //whether or not hard-mode zombies may break down wooden doors

    public boolean limitTreeGrowth;                          //whether trees should be prevented from growing into a claim from outside
    public PistonMode pistonMovement;                            //Setting for piston check options
    public boolean pistonExplosionSound;                     //whether pistons make an explosion sound when they get removed

    public boolean advanced_fixNegativeClaimblockAmounts;    //whether to attempt to fix negative claim block amounts (some addons cause/assume players can go into negative amounts)
    public int advanced_claim_expiration_check_rate;            //How often GP should check for expired claims, amount in seconds
    public int advanced_offlineplayer_cache_days;            //Cache players who have logged in within the last x number of days

    //ATTENTION: The following config options have been added.
    public String plugin_prefix;

    //TODO this needs to be looked at, the dev highly advises using the flat file, im not sure why so we need to look at it.
    public String databaseUrl;
    public String databaseUserName;
    public String databasePassword;

    public ConfigManager(TheatriaClaims theatriaClaims, CustomLogger customLogger) {
        this.theatriaClaims = theatriaClaims;
        this.customLogger = customLogger;
        config = theatriaClaims.getConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void loadConfig() {
        //read configuration settings (note defaults)
        int configVersion = config.getInt("GriefPrevention.ConfigVersion", 0);

        //get (deprecated node) claims world names from the config file
        List<World> worlds = TheatriaClaims.instance.getServer().getWorlds();
        List<String> deprecated_claimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.Worlds");

        //validate that list
        for (int i = 0; i < deprecated_claimsEnabledWorldNames.size(); i++) {
            String worldName = deprecated_claimsEnabledWorldNames.get(i);
            World world = TheatriaClaims.instance.getServer().getWorld(worldName);
            if (world == null) {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }

        //get (deprecated node) creative world names from the config file
        List<String> deprecated_creativeClaimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.CreativeRulesWorlds");

        //validate that list
        for (int i = 0; i < deprecated_creativeClaimsEnabledWorldNames.size(); i++) {
            String worldName = deprecated_creativeClaimsEnabledWorldNames.get(i);
            World world = TheatriaClaims.instance.getServer().getWorld(worldName);
            if (world == null) {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }

        //decide claim mode for each world
        claims_worldModes = new ConcurrentHashMap<>();
        creativeWorldsExist = false;
        for (World world : worlds) {
            //is it specified in the config file?
            String configSetting = config.getString("GriefPrevention.Claims.Mode." + world.getName());
            if (configSetting != null) {
                ClaimsMode claimsMode = configStringToClaimsMode(configSetting);
                if (claimsMode != null) {
                    claims_worldModes.put(world, claimsMode);
                    if (claimsMode == ClaimsMode.Creative) creativeWorldsExist = true;
                    continue;
                }
                else {
                    customLogger.log("Error: Invalid claim mode \"" + configSetting + "\".  Options are Survival, Creative, and Disabled.");
                    claims_worldModes.put(world, ClaimsMode.Creative);
                    creativeWorldsExist = true;
                }
            }

            //was it specified in a deprecated config node?
            if (deprecated_creativeClaimsEnabledWorldNames.contains(world.getName())) {
                claims_worldModes.put(world, ClaimsMode.Creative);
                creativeWorldsExist = true;
            }
            else if (deprecated_claimsEnabledWorldNames.contains(world.getName())) {
                claims_worldModes.put(world, ClaimsMode.Survival);
            }

            //does the world's name indicate its purpose?
            else if (world.getName().toLowerCase().contains("survival")) {
                claims_worldModes.put(world, ClaimsMode.Survival);
            }
            else if (world.getName().toLowerCase().contains("creative")) {
                claims_worldModes.put(world, ClaimsMode.Creative);
                creativeWorldsExist = true;
            }

            //decide a default based on server type and world type
            else if (TheatriaClaims.instance.getServer().getDefaultGameMode() == GameMode.CREATIVE) {
                claims_worldModes.put(world, ClaimsMode.Creative);
                creativeWorldsExist = true;
            }
            else if (world.getEnvironment() == Environment.NORMAL) {
                claims_worldModes.put(world, ClaimsMode.Survival);
            }
            else {
                claims_worldModes.put(world, ClaimsMode.Disabled);
            }

            //if the setting WOULD be disabled but this is a server upgrading from the old config format,
            //then default to survival mode for safety's sake (to protect any admin claims which may 
            //have been created there)
            if (claims_worldModes.get(world) == ClaimsMode.Disabled && deprecated_claimsEnabledWorldNames.size() > 0) {
                claims_worldModes.put(world, ClaimsMode.Survival);
            }
        }

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
        claims_claimsExtendIntoGroundDistance = Math.abs(config.getInt("GriefPrevention.Claims.ExtendIntoGroundDistance", 5));
        claims_minWidth = config.getInt("GriefPrevention.Claims.MinimumWidth", 5);
        claims_minArea = config.getInt("GriefPrevention.Claims.MinimumArea", 100);
        claims_maxDepth = config.getInt("GriefPrevention.Claims.MaximumDepth", Integer.MIN_VALUE);
        if (configVersion < 1 && claims_maxDepth == 0) {
            // If MaximumDepth is untouched in an older configuration, correct it.
            claims_maxDepth = Integer.MIN_VALUE;
            customLogger.log("Updated default value for GriefPrevention.Claims.MaximumDepth to " + Integer.MIN_VALUE);
        }
        claims_chestClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.ChestClaimDays", 7);
        claims_expirationDays = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.DaysInactive", 60);
        claims_expirationExemptionTotalBlocks = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasTotalClaimBlocks", 10000);
        claims_expirationExemptionBonusBlocks = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasBonusClaimBlocks", 5000);
        claims_survivalAutoNatureRestoration = config.getBoolean("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration.SurvivalWorlds", false);
        claims_allowTrappedInAdminClaims = config.getBoolean("GriefPrevention.Claims.AllowTrappedInAdminClaims", false);

        claims_respectWorldGuard = config.getBoolean("GriefPrevention.Claims.CreationRequiresWorldGuardBuildPermission", true);
        claims_villagerTradingRequiresTrust = config.getBoolean("GriefPrevention.Claims.VillagerTradingRequiresPermission", true);
        String accessTrustSlashCommands = config.getString("GriefPrevention.Claims.CommandsRequiringAccessTrust", "/sethome");
        claims_supplyPlayerManual = config.getBoolean("GriefPrevention.Claims.DeliverManuals", true);
        claims_manualDeliveryDelaySeconds = config.getInt("GriefPrevention.Claims.ManualDeliveryDelaySeconds", 30);
        claims_ravagersBreakBlocks = config.getBoolean("GriefPrevention.Claims.RavagersBreakBlocks", true);
        claims_firespreads = config.getBoolean("GriefPrevention.Claims.FireSpreadsInClaims", false);
        claims_firedamages = config.getBoolean("GriefPrevention.Claims.FireDamagesInClaims", false);
        claims_lecternReadingRequiresAccessTrust = config.getBoolean("GriefPrevention.Claims.LecternReadingRequiresAccessTrust", true);

        economy_claimBlocksMaxBonus = config.getInt("GriefPrevention.Economy.ClaimBlocksMaxBonus", 0);
        economy_claimBlocksPurchaseCost = config.getDouble("GriefPrevention.Economy.ClaimBlocksPurchaseCost", 0);
        economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);

        blockClaimExplosions = config.getBoolean("GriefPrevention.BlockLandClaimExplosions", true);
        blockSurfaceCreeperExplosions = config.getBoolean("GriefPrevention.BlockSurfaceCreeperExplosions", true);
        blockSurfaceOtherExplosions = config.getBoolean("GriefPrevention.BlockSurfaceOtherExplosions", true);
        limitTreeGrowth = config.getBoolean("GriefPrevention.LimitTreeGrowth", false);
        pistonExplosionSound = config.getBoolean("GriefPrevention.PistonExplosionSound", true);
        pistonMovement = PistonMode.of(config.getString("GriefPrevention.PistonMovement", "CLAIMS_ONLY"));
        if (config.isBoolean("GriefPrevention.LimitPistonsToLandClaims") && !config.getBoolean("GriefPrevention.LimitPistonsToLandClaims"))
            pistonMovement = PistonMode.EVERYWHERE_SIMPLE;
        if (config.isBoolean("GriefPrevention.CheckPistonMovement") && !config.getBoolean("GriefPrevention.CheckPistonMovement"))
            pistonMovement = PistonMode.IGNORED;

        fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
        fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);

        visualizationAntiCheatCompat = config.getBoolean("GriefPrevention.VisualizationAntiCheatCompatMode", false);

        claims_endermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
        silverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
        creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
        rabbitsEatCrops = config.getBoolean("GriefPrevention.RabbitsEatCrops", true);
        zombiesBreakDoors = config.getBoolean("GriefPrevention.HardModeZombiesBreakDoors", false);

        //default for claim investigation tool
        String investigationToolMaterialName = Material.STICK.name();

        //get investigation tool from config
        investigationToolMaterialName = config.getString("GriefPrevention.Claims.InvestigationTool", investigationToolMaterialName);

        //validate investigation tool
        claims_investigationTool = Material.getMaterial(investigationToolMaterialName);
        if (claims_investigationTool == null) {
            customLogger.log("ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.yml.");
            claims_investigationTool = Material.STICK;
        }

        //default for claim creation/modification tool
        String modificationToolMaterialName = Material.GOLDEN_SHOVEL.name();

        //get modification tool from config
        modificationToolMaterialName = config.getString("GriefPrevention.Claims.ModificationTool", modificationToolMaterialName);

        //validate modification tool
        claims_modificationTool = Material.getMaterial(modificationToolMaterialName);
        if (claims_modificationTool == null) {
            customLogger.log("ERROR: Material " + modificationToolMaterialName + " not found.  Defaulting to the golden shovel.  Please update your config.yml.");
            claims_modificationTool = Material.GOLDEN_SHOVEL;
        }

        List<String> breakableBlocksList;

        //optional database settings
        databaseUrl = config.getString("GriefPrevention.Database.URL", "");
        databaseUserName = config.getString("GriefPrevention.Database.UserName", "");
        databasePassword = config.getString("GriefPrevention.Database.Password", "");

        advanced_fixNegativeClaimblockAmounts = config.getBoolean("GriefPrevention.Advanced.fixNegativeClaimblockAmounts", true);
        advanced_claim_expiration_check_rate = config.getInt("GriefPrevention.Advanced.ClaimExpirationCheckRate", 60);
        advanced_offlineplayer_cache_days = config.getInt("GriefPrevention.Advanced.OfflinePlayer_cache_days", 90);


        //try to parse the list of commands requiring access trust in land claims
        claims_commandsRequiringAccessTrust = new ArrayList<>();
        String[] commands = accessTrustSlashCommands.split(";");
        for (String command : commands) {
            if (!command.isEmpty()) {
                claims_commandsRequiringAccessTrust.add(command.trim().toLowerCase());
            }
        }

    }

    //TODO looks like this could be an enum with value of string? i forget the method.
    public ClaimsMode configStringToClaimsMode(String configSetting) {
        if (configSetting.equalsIgnoreCase("Survival")) {
            return ClaimsMode.Survival;
        }
        else if (configSetting.equalsIgnoreCase("Creative")) {
            return ClaimsMode.Creative;
        }
        else if (configSetting.equalsIgnoreCase("Disabled")) {
            return ClaimsMode.Disabled;
        }
        else if (configSetting.equalsIgnoreCase("SurvivalRequiringClaims")) {
            return ClaimsMode.SurvivalRequiringClaims;
        }
        else {
            return null;
        }
    }

    public String getPluginPrefix() {
        return plugin_prefix;
    }
}
