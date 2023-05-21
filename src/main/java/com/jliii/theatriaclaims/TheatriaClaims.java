package com.jliii.theatriaclaims;

import com.jliii.theatriaclaims.commands.ChungusCommand;
import com.jliii.theatriaclaims.listeners.EconomyHandler;
import com.jliii.theatriaclaims.listeners.EntityEventHandler;
import com.jliii.theatriaclaims.managers.ConfigManager;
import com.jliii.theatriaclaims.managers.EventManager;
import com.jliii.theatriaclaims.tasks.DeliverClaimBlocksTask;
import com.jliii.theatriaclaims.tasks.FindUnusedClaimsTask;
import com.jliii.theatriaclaims.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TheatriaClaims extends JavaPlugin {
    //for convenience, a reference to the instance of this plugin
    public static TheatriaClaims instance;
    //this handles data storage, like player and region data
    public DataStore dataStore;
    // Event handlers with common functionality
    public EntityEventHandler entityEventHandler;
    // Player event handler
    EconomyHandler economyHandler;
    ConfigManager configManager;

    //initializes well...   everything
    public void onEnable() {
        //DYNMAP INTEGRATION
        Plugin dynmap = getServer().getPluginManager().getPlugin("dynmap");
        if(dynmap != null && dynmap.isEnabled()) {
//            getLogger().severe("Found Dynmap!  Enabling Dynmap integration...");
//            DynmapIntegration dynmapIntegration = new DynmapIntegration(this);
//
        }
        instance = this;
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        CustomLogger.log("Finished loading configuration.");

        //TODO this is creating flatfile datastore in more than one case, can we cut some repeated code out?
        //when datastore initializes, it loads player and claim data, and posts some stats to the log
        if (configManager.getDatabaseConfig().databaseUrl.length() > 0) {
            try {
                DatabaseDataStore databaseStore = new DatabaseDataStore(configManager);
                if (FlatFileDataStore.hasData()) {
                    CustomLogger.log("There appears to be some data on the hard drive.  Migrating those data to the database...");
                    FlatFileDataStore flatFileStore = new FlatFileDataStore(configManager);
                    this.dataStore = flatFileStore;
                    flatFileStore.migrateData(databaseStore);
                    CustomLogger.log("Data migration process complete.");
                }
                this.dataStore = databaseStore;
            }
            catch (Exception e) {
                CustomLogger.log("Because there was a problem with the database, TheatriaClaims will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.yml so that TheatriaClaims can use the file system to store data.");
                e.printStackTrace();
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        //if not using the database because it's not configured or because there was a problem, use the file system to store data
        //this is the preferred method, as it's simpler than the database scenario
        if (this.dataStore == null) {
            File oldclaimdata = new File(getDataFolder(), "ClaimData");
            if (oldclaimdata.exists()) {
                if (!FlatFileDataStore.hasData()) {
                    File claimdata = new File("plugins" + File.separator + "TheatriaClaimsData" + File.separator + "ClaimData");
                    oldclaimdata.renameTo(claimdata);
                    File oldplayerdata = new File(getDataFolder(), "PlayerData");
                    File playerdata = new File("plugins" + File.separator + "TheatriaClaimsData" + File.separator + "PlayerData");
                    oldplayerdata.renameTo(playerdata);
                }
            }
            try {
                this.dataStore = new FlatFileDataStore(configManager);
            }
            catch (Exception e) {
                CustomLogger.log("Unable to initialize the file system data store.  Details:");
                CustomLogger.log(e.getMessage());
                e.printStackTrace();
            }
        }

        String dataMode = (this.dataStore instanceof FlatFileDataStore) ? "(File Mode)" : "(Database Mode)";
        CustomLogger.log("Finished loading data " + dataMode + ".");

        //unless claim block accrual is disabled, start the recurring per 10 minute event to give claim blocks to online players
        //20L ~ 1 second
        if (configManager.getSystemConfig().blocksAccruedPerHour_default > 0) {
            DeliverClaimBlocksTask task = new DeliverClaimBlocksTask(null, this, configManager);
            this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 10, 20L * 60 * 10);
        }

        //start recurring cleanup scan for unused claims belonging to inactive players
        FindUnusedClaimsTask task2 = new FindUnusedClaimsTask(configManager);
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task2, 20L * 60, 20L * configManager.getSystemConfig().advanced_claim_expiration_check_rate);

        //Register events
        EventManager.registerEvents(this, dataStore, configManager);

        //register commands
        Objects.requireNonNull(Bukkit.getPluginCommand("gp")).setExecutor(new ChungusCommand(economyHandler, configManager));

        //cache offline players
        OfflinePlayer[] offlinePlayers = this.getServer().getOfflinePlayers();
        CacheOfflinePlayerNamesThread namesThread = new CacheOfflinePlayerNamesThread(offlinePlayers, this.playerNameToIDMap);
        namesThread.setPriority(Thread.MIN_PRIORITY);
        namesThread.start();

        //load ignore lists for any already-online players
        @SuppressWarnings("unchecked")
        Collection<Player> players = (Collection<Player>) TheatriaClaims.instance.getServer().getOnlinePlayers();
        for (Player player : players) {
            new IgnoreLoaderThread(player.getUniqueId(), this.dataStore.getPlayerData(player.getUniqueId()).ignoredPlayers).start();
        }

        CustomLogger.log("Boot finished.");

    }

    //helper method to resolve a player by name
    public ConcurrentHashMap<String, UUID> playerNameToIDMap = new ConcurrentHashMap<>();

    //thread to build the above cache
    private class CacheOfflinePlayerNamesThread extends Thread {
        private final OfflinePlayer[] offlinePlayers;
        private final ConcurrentHashMap<String, UUID> playerNameToIDMap;

        CacheOfflinePlayerNamesThread(OfflinePlayer[] offlinePlayers, ConcurrentHashMap<String, UUID> playerNameToIDMap) {
            this.offlinePlayers = offlinePlayers;
            this.playerNameToIDMap = playerNameToIDMap;
        }

        public void run() {
            long now = System.currentTimeMillis();
            final long millisecondsPerDay = 1000 * 60 * 60 * 24;
            for (OfflinePlayer player : offlinePlayers) {
                try {
                    UUID playerID = player.getUniqueId();
                    if (playerID == null) continue;
                    long lastSeen = player.getLastPlayed();

                    //if the player has been seen in the last 90 days, cache his name/UUID pair
                    long diff = now - lastSeen;
                    long daysDiff = diff / millisecondsPerDay;
                    if (daysDiff <= configManager.getSystemConfig().advanced_offlineplayer_cache_days) {
                        String playerName = player.getName();
                        if (playerName == null) continue;
                        this.playerNameToIDMap.put(playerName, playerID);
                        this.playerNameToIDMap.put(playerName.toLowerCase(), playerID);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onDisable() {
        //save data for any online players
        @SuppressWarnings("unchecked")
        Collection<Player> players = (Collection<Player>) this.getServer().getOnlinePlayers();
        for (Player player : players) {
            UUID playerID = player.getUniqueId();
            PlayerData playerData = this.dataStore.getPlayerData(playerID);
            this.dataStore.savePlayerDataSync(playerID, playerData);
        }

        this.dataStore.close();

        CustomLogger.log("Plugin has disabled.");
    }

}