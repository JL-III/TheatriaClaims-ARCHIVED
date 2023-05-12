package com.jliii.theatriaclaims.config;

import org.bukkit.configuration.file.FileConfiguration;

public class EconomyConfig {

    private FileConfiguration config;

    public int economy_claimBlocksMaxBonus;                  //max "bonus" blocks a player can buy.  set to zero for no limit.
    public double economy_claimBlocksPurchaseCost;            //cost to purchase a claim block.  set to zero to disable purchase.
    public double economy_claimBlocksSellValue;                //return on a sold claim block.  set to zero to disable sale.

    public EconomyConfig(FileConfiguration config) {
        this.config = config;
    }

    public void loadConfigurationValues() {
        economy_claimBlocksMaxBonus = config.getInt("GriefPrevention.Economy.ClaimBlocksMaxBonus", 0);
        economy_claimBlocksPurchaseCost = config.getDouble("GriefPrevention.Economy.ClaimBlocksPurchaseCost", 0);
        economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);
    }
}
