package com.jliii.theatriaclaims.config;

import com.jliii.theatriaclaims.util.CustomLogger;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class SystemConfig {

    private FileConfiguration config;
    private CustomLogger customLogger;

    public Material claims_investigationTool;                //which material will be used to investigate claims with a right click
    public Material claims_modificationTool;                    //which material will be used to create/resize claims with a right click
    public boolean visualizationAntiCheatCompat;              // whether to engage compatibility mode for anti-cheat plugins
    public boolean advanced_fixNegativeClaimblockAmounts;    //whether to attempt to fix negative claim block amounts (some addons cause/assume players can go into negative amounts)
    public int advanced_claim_expiration_check_rate;            //How often GP should check for expired claims, amount in seconds
    public int advanced_offlineplayer_cache_days;            //Cache players who have logged in within the last x number of days
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
    //ATTENTION: The following config options have been added.
    public String plugin_prefix;

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
        claims_expirationDays = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.DaysInactive", 60);
        claims_expirationExemptionTotalBlocks = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasTotalClaimBlocks", 10000);
        claims_expirationExemptionBonusBlocks = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasBonusClaimBlocks", 5000);
        claims_supplyPlayerManual = config.getBoolean("GriefPrevention.Claims.DeliverManuals", true);
        claims_manualDeliveryDelaySeconds = config.getInt("GriefPrevention.Claims.ManualDeliveryDelaySeconds", 30);
        claims_chestClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.ChestClaimDays", 7);
        claims_commandsRequiringAccessTrust = new ArrayList<>();

        String accessTrustSlashCommands = config.getString("GriefPrevention.Claims.CommandsRequiringAccessTrust", "/sethome");


        String[] commands = accessTrustSlashCommands.split(";");
        for (String command : commands) {
            if (!command.isEmpty()) {
                claims_commandsRequiringAccessTrust.add(command.trim().toLowerCase());
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
    }
    

    public String getPluginPrefix() {
        return plugin_prefix;
    }
}
