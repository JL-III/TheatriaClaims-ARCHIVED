package com.jliii.theatriaclaims.managers;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.config.SystemConfig;
import com.jliii.theatriaclaims.util.CustomLogger;
import org.bukkit.configuration.file.FileConfiguration;


public class ConfigManager {

    private TheatriaClaims theatriaClaims;
    private CustomLogger customLogger;
    FileConfiguration config;
    public SystemConfig systemConfig;

    public ConfigManager(TheatriaClaims theatriaClaims, CustomLogger customLogger) {
        this.theatriaClaims = theatriaClaims;
        this.customLogger = customLogger;
        config = theatriaClaims.getConfig();
        systemConfig = new SystemConfig(config, customLogger);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void loadConfig() {
        systemConfig.loadConfigurationValues();
    }
}
