package com.jliii.theatriaclaims.database;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.PlayerData;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.UUID;

public class DatabaseManager {
    private TheatriaClaims plugin;
    private ConfigManager configManager;
    private DataStore dataStore;

    public DatabaseManager(TheatriaClaims theatriaClaims, ConfigManager configManager) {
        this.plugin = theatriaClaims;
        this.configManager = configManager;
    }

    public void initializeDataStore() {
        //TODO this is creating flatfile datastore in more than one case, can we cut some repeated code out?
        //when datastore initializes, it loads player and claim data, and posts some stats to the log
        if (configManager.getDatabaseConfig().databaseUrl.length() > 0) {
            try {
                DatabaseDataStore databaseStore = new DatabaseDataStore(configManager);
                if (FlatFileDataStore.hasData()) {
                    CustomLogger.log("There appears to be some data on the hard drive.  Migrating those data to the database...");
                    FlatFileDataStore flatFileStore = new FlatFileDataStore(configManager);
                    setDataStore(flatFileStore);
                    flatFileStore.migrateData(databaseStore);
                    CustomLogger.log("Data migration process complete.");
                }
                setDataStore(databaseStore);
            }
            catch (Exception e) {
                CustomLogger.log("Because there was a problem with the database, TheatriaClaims will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.yml so that TheatriaClaims can use the file system to store data.");
                e.printStackTrace();
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        }

        //if not using the database because it's not configured or because there was a problem, use the file system to store data
        //this is the preferred method, as it's simpler than the database scenario
        if (this.dataStore == null) {
            File oldclaimdata = new File(plugin.getDataFolder(), "ClaimData");
            if (oldclaimdata.exists()) {
                if (!FlatFileDataStore.hasData()) {
                    File claimdata = new File("plugins" + File.separator + "TheatriaClaimsData" + File.separator + "ClaimData");
                    oldclaimdata.renameTo(claimdata);
                    File oldplayerdata = new File(plugin.getDataFolder(), "PlayerData");
                    File playerdata = new File("plugins" + File.separator + "TheatriaClaimsData" + File.separator + "PlayerData");
                    oldplayerdata.renameTo(playerdata);
                }
            }
            try {
//                this.dataStore = new FlatFileDataStore(configManager);
                setDataStore(new FlatFileDataStore(configManager));
            }
            catch (Exception e) {
                CustomLogger.log("Unable to initialize the file system data store.  Details:");
                CustomLogger.log(e.getMessage());
                e.printStackTrace();
            }
        }
        String dataMode = (this.dataStore instanceof FlatFileDataStore) ? "(File Mode)" : "(Database Mode)";
        CustomLogger.log("Finished loading data " + dataMode + ".");
    }

    public DataStore getDataStore() { return dataStore; }

    public void setDataStore(DataStore dataStore) { this.dataStore = dataStore; }

    public void onDisablePlugin() {
        //save data for any online players
        @SuppressWarnings("unchecked")
        Collection<Player> players = (Collection<Player>) plugin.getServer().getOnlinePlayers();
        for (Player player : players) {
            UUID playerID = player.getUniqueId();
            PlayerData playerData = getDataStore().getPlayerData(playerID);
            getDataStore().savePlayerDataSync(playerID, playerData);
        }

        getDataStore().close();
    }
}
