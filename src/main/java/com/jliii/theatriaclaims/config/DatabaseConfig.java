package com.jliii.theatriaclaims.config;

import org.bukkit.configuration.file.FileConfiguration;

public class DatabaseConfig {

    private FileConfiguration config;

    public String databaseUrl;
    public String databaseUserName;
    public String databasePassword;

    public DatabaseConfig(FileConfiguration config) {
        this.config = config;
    }

    public void loadConfigurationValues() {
        //optional database settings
        databaseUrl = config.getString("GriefPrevention.Database.URL", "");
        databaseUserName = config.getString("GriefPrevention.Database.UserName", "");
        databasePassword = config.getString("GriefPrevention.Database.Password", "");
    }
}
