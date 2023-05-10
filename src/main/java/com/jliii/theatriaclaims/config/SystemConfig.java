package com.jliii.theatriaclaims.config;

import com.jliii.theatriaclaims.util.CustomLogger;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class SystemConfig {

    private FileConfiguration config;
    private CustomLogger customLogger;

    public Material investigationTool;                //which material will be used to investigate claims with a right click
    public Material modificationTool;                    //which material will be used to create/resize claims with a right click
    public boolean visualizationAntiCheatCompat;              // whether to engage compatibility mode for anti-cheat plugins
    public boolean advanced_fixNegativeClaimblockAmounts;    //whether to attempt to fix negative claim block amounts (some addons cause/assume players can go into negative amounts)
    public int advanced_claim_expiration_check_rate;            //How often GP should check for expired claims, amount in seconds
    public int advanced_offlineplayer_cache_days;            //Cache players who have logged in within the last x number of days
    public int initialBlocks;                            //the number of claim blocks a new player starts with
    public double abandonReturnRatio;                 //the portion of claim blocks returned to a player when a claim is abandoned
    public int blocksAccruedPerHour_default;            //how many additional blocks players get each hour of play (can be zero) without any special permissions
    public int maxAccruedBlocks_default;                //the limit on accrued blocks (over time) for players without any special permissions.  doesn't limit purchased or admin-gifted blocks
    public int accruedIdleThreshold;                    //how far (in blocks) a player must move in order to not be considered afk/idle when determining accrued claim blocks
    public int accruedIdlePercent;                    //how much percentage of claim block accruals should idle players get
    public int expirationDays;                        //how many days of inactivity before a player loses his claims
    public int expirationExemptionTotalBlocks;        //total claim blocks amount which will exempt a player from claim expiration
    public int expirationExemptionBonusBlocks;        //bonus claim blocks amount which will exempt a player from claim expiration
    public int automaticClaimsForNewPlayersRadius;    //how big automatic new player claims (when they place a chest) should be.  -1 to disable
    public int automaticClaimsForNewPlayersRadiusMin; //how big automatic new player claims must be. 0 to disable
    public int minWidth;                                //minimum width for non-admin claims
    public int minArea;                               //minimum area for non-admin claims
    public int chestClaimExpirationDays;                //number of days of inactivity before an automatic chest claim will be deleted
    public ArrayList<String> commandsRequiringAccessTrust; //the list of slash commands requiring access trust when in a claim
    public boolean supplyPlayerManual;                //whether to give new players a book with land claim help in it
    public int manualDeliveryDelaySeconds;            //how long to wait before giving a book to a new player
    //ATTENTION: The following config options have been added.
    public String plugin_prefix;
    public List<String> claimWorldNames;

    public SystemConfig(FileConfiguration config, CustomLogger customLogger) {
        this.config = config;
        this.customLogger = customLogger;
    }

    public void loadConfigurationValues() {

        visualizationAntiCheatCompat = config.getBoolean("GriefPrevention.VisualizationAntiCheatCompatMode", false);
        advanced_fixNegativeClaimblockAmounts = config.getBoolean("GriefPrevention.Advanced.fixNegativeClaimblockAmounts", true);
        advanced_claim_expiration_check_rate = config.getInt("GriefPrevention.Advanced.ClaimExpirationCheckRate", 60);
        advanced_offlineplayer_cache_days = config.getInt("GriefPrevention.Advanced.OfflinePlayer_cache_days", 90);
        plugin_prefix = config.getString("GriefPrevention.PluginPrefix", "GriefPrevention");
        initialBlocks = config.getInt("GriefPrevention.Claims.InitialBlocks", 100);
        blocksAccruedPerHour_default = config.getInt("GriefPrevention.Claims.BlocksAccruedPerHour", 100);
        blocksAccruedPerHour_default = config.getInt("GriefPrevention.Claims.Claim Blocks Accrued Per Hour.Default", blocksAccruedPerHour_default);
        maxAccruedBlocks_default = config.getInt("GriefPrevention.Claims.MaxAccruedBlocks", 80000);
        maxAccruedBlocks_default = config.getInt("GriefPrevention.Claims.Max Accrued Claim Blocks.Default", maxAccruedBlocks_default);
        accruedIdleThreshold = config.getInt("GriefPrevention.Claims.AccruedIdleThreshold", 0);
        accruedIdleThreshold = config.getInt("GriefPrevention.Claims.Accrued Idle Threshold", accruedIdleThreshold);
        accruedIdlePercent = config.getInt("GriefPrevention.Claims.AccruedIdlePercent", 0);
        abandonReturnRatio = config.getDouble("GriefPrevention.Claims.AbandonReturnRatio", 1.0D);
        automaticClaimsForNewPlayersRadius = config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", 4);
        automaticClaimsForNewPlayersRadiusMin = Math.max(0, Math.min(automaticClaimsForNewPlayersRadius, config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadiusMinimum", 0)));
        minWidth = config.getInt("GriefPrevention.Claims.MinimumWidth", 5);
        minArea = config.getInt("GriefPrevention.Claims.MinimumArea", 100);
        expirationDays = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.DaysInactive", 60);
        expirationExemptionTotalBlocks = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasTotalClaimBlocks", 10000);
        expirationExemptionBonusBlocks = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasBonusClaimBlocks", 5000);
        supplyPlayerManual = config.getBoolean("GriefPrevention.Claims.DeliverManuals", true);
        manualDeliveryDelaySeconds = config.getInt("GriefPrevention.Claims.ManualDeliveryDelaySeconds", 30);
        chestClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.ChestClaimDays", 7);
        commandsRequiringAccessTrust = new ArrayList<>();
        claimWorldNames = config.getStringList("GriefPrevention.worldnames");

        String accessTrustSlashCommands = config.getString("GriefPrevention.Claims.CommandsRequiringAccessTrust", "/sethome");

        String[] commands = accessTrustSlashCommands.split(";");
        for (String command : commands) {
            if (!command.isEmpty()) {
                commandsRequiringAccessTrust.add(command.trim().toLowerCase());
            }
        }
        //default for claim investigation tool
        String investigationToolMaterialName = Material.STICK.name();

        //get investigation tool from config
        investigationToolMaterialName = config.getString("GriefPrevention.Claims.InvestigationTool", investigationToolMaterialName);

        //default for claim creation/modification tool
        String modificationToolMaterialName = Material.GOLDEN_SHOVEL.name();

        //get modification tool from config
        modificationToolMaterialName = config.getString("GriefPrevention.Claims.ModificationTool", modificationToolMaterialName);
        //validate investigation tool
        investigationTool = Material.getMaterial(investigationToolMaterialName);
        if (investigationTool == null) {
            customLogger.log("ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.yml.");
            investigationTool = Material.STICK;
        }

        //validate modification tool
        modificationTool = Material.getMaterial(modificationToolMaterialName);
        if (modificationTool == null) {
            customLogger.log("ERROR: Material " + modificationToolMaterialName + " not found.  Defaulting to the golden shovel.  Please update your config.yml.");
            modificationTool = Material.GOLDEN_SHOVEL;
        }
    }
    

    public String getPluginPrefix() {
        return plugin_prefix;
    }

    //checks whether players can create claims in a world
    public boolean claimsEnabledForWorld(World world) {
        return claimWorldNames.contains(world.getName());
    }

}
