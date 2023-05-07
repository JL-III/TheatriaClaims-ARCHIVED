package com.jliii.theatriaclaims.managers;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.config.ClaimsConfig;
import com.jliii.theatriaclaims.config.DatabaseConfig;
import com.jliii.theatriaclaims.config.DynmapConfig;
import com.jliii.theatriaclaims.config.EconomyConfig;
import com.jliii.theatriaclaims.config.SystemConfig;
import com.jliii.theatriaclaims.config.WorldConfig;
import com.jliii.theatriaclaims.util.CustomLogger;

import scala.languageFeature.reflectiveCalls;

import org.bukkit.configuration.file.FileConfiguration;


public class ConfigManager {

    FileConfiguration config;
    private ClaimsConfig claimsConfig;
    private DatabaseConfig databaseConfig;
    private DynmapConfig dynmapConfig;
    private EconomyConfig economyConfig;
    private SystemConfig systemConfig;
    private WorldConfig worldConfig;


    public ConfigManager(TheatriaClaims theatriaClaims, CustomLogger customLogger) {
        config = theatriaClaims.getConfig();
        claimsConfig = new ClaimsConfig(config, customLogger);
        databaseConfig = new DatabaseConfig(config);
        dynmapConfig = new DynmapConfig();
        economyConfig = new EconomyConfig(config);
        systemConfig = new SystemConfig(config, customLogger);
        worldConfig = new WorldConfig(config);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void loadConfig() {
        claimsConfig.loadConfigurationValues();
        databaseConfig.loadConfigurationValues();
        dynmapConfig.loadConfigurationValues();
        economyConfig.loadConfigurationValues();
        systemConfig.loadConfigurationValues();
        worldConfig.loadConfigurationValues();
    }

}
