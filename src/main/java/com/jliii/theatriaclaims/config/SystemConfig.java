package com.jliii.theatriaclaims.config;

import com.jliii.theatriaclaims.util.CustomLogger;
import org.bukkit.configuration.file.FileConfiguration;

public class SystemConfig {

    private FileConfiguration config;
    private CustomLogger customLogger;

    public boolean visualizationAntiCheatCompat;              // whether to engage compatibility mode for anti-cheat plugins
    public boolean advanced_fixNegativeClaimblockAmounts;    //whether to attempt to fix negative claim block amounts (some addons cause/assume players can go into negative amounts)
    public int advanced_claim_expiration_check_rate;            //How often GP should check for expired claims, amount in seconds
    public int advanced_offlineplayer_cache_days;            //Cache players who have logged in within the last x number of days

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

    }

    public String getPluginPrefix() {
        return plugin_prefix;
    }
}
