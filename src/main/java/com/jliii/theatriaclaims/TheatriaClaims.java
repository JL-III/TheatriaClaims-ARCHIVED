package com.jliii.theatriaclaims;

import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.commands.ChungusCommand;
import com.jliii.theatriaclaims.dynmap.DynmapIntegration;
import com.jliii.theatriaclaims.enums.ClaimsMode;
import com.jliii.theatriaclaims.enums.CustomLogEntryTypes;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.events.PreventBlockBreakEvent;
import com.jliii.theatriaclaims.events.TrustChangedEvent;
import com.jliii.theatriaclaims.listeners.BlockEventHandler;
import com.jliii.theatriaclaims.listeners.EconomyHandler;
import com.jliii.theatriaclaims.listeners.EntityEventHandler;
import com.jliii.theatriaclaims.listeners.PlayerEventHandler;
import com.jliii.theatriaclaims.managers.ConfigManager;
import com.jliii.theatriaclaims.tasks.DeliverClaimBlocksTask;
import com.jliii.theatriaclaims.tasks.EntityCleanupTask;
import com.jliii.theatriaclaims.tasks.FindUnusedClaimsTask;
import com.jliii.theatriaclaims.tasks.PvPImmunityValidationTask;
import com.jliii.theatriaclaims.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TheatriaClaims extends JavaPlugin {
    //for convenience, a reference to the instance of this plugin
    public static TheatriaClaims instance;
    //for logging to the console and log file
    private static Logger log;
    //this handles data storage, like player and region data
    public DataStore dataStore;
    // Event handlers with common functionality
    public EntityEventHandler entityEventHandler;
    //this tracks item stacks expected to drop which will need protection
    public ArrayList<PendingItemProtection> pendingItemWatchList = new ArrayList<>();
    //log entry manager for GP's custom log files
    CustomLogger customLogger;
    // Player event handler
    PlayerEventHandler playerEventHandler;

    EconomyHandler economyHandler;

    ConfigManager configManager;

    //how far away to search from a tree trunk for its branch blocks
    public static final int TREE_RADIUS = 5;

    //how long to wait before deciding a player is staying online or staying offline, for notication messages
    public static final int NOTIFICATION_SECONDS = 20;

    //initializes well...   everything
    public void onEnable() {
        //DYNMAP INTEGRATION
        Plugin dynmap = getServer().getPluginManager().getPlugin("dynmap");
        if(dynmap != null && dynmap.isEnabled()) {
            getLogger().severe("Found Dynmap!  Enabling Dynmap integration...");

            DynmapIntegration dynmapIntegration = new DynmapIntegration(this);



        }
        instance = this;
        log = instance.getLogger();
        customLogger = new CustomLogger(configManager, log);
        configManager = new ConfigManager(this, customLogger);
        configManager.loadConfig();
        customLogger.AddLogEntry("Finished loading configuration.");

        //TODO this is creating flatfile datastore in more than one case, can we cut some repeated code out?
        //when datastore initializes, it loads player and claim data, and posts some stats to the log
        if (configManager.databaseUrl.length() > 0) {
            try {
                DatabaseDataStore databaseStore = new DatabaseDataStore(configManager, customLogger);
                if (FlatFileDataStore.hasData()) {
                    customLogger.AddLogEntry("There appears to be some data on the hard drive.  Migrating those data to the database...");
                    FlatFileDataStore flatFileStore = new FlatFileDataStore(configManager, customLogger);
                    this.dataStore = flatFileStore;
                    flatFileStore.migrateData(databaseStore);
                    customLogger.AddLogEntry("Data migration process complete.");
                }
                this.dataStore = databaseStore;
            }
            catch (Exception e) {
                customLogger.AddLogEntry("Because there was a problem with the database, GriefPrevention will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.yml so that GriefPrevention can use the file system to store data.");
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
                    File claimdata = new File("plugins" + File.separator + "GriefPreventionData" + File.separator + "ClaimData");
                    oldclaimdata.renameTo(claimdata);
                    File oldplayerdata = new File(getDataFolder(), "PlayerData");
                    File playerdata = new File("plugins" + File.separator + "GriefPreventionData" + File.separator + "PlayerData");
                    oldplayerdata.renameTo(playerdata);
                }
            }
            try {
                this.dataStore = new FlatFileDataStore(configManager, customLogger);
            }
            catch (Exception e) {
                customLogger.AddLogEntry("Unable to initialize the file system data store.  Details:");
                customLogger.AddLogEntry(e.getMessage());
                e.printStackTrace();
            }
        }

        String dataMode = (this.dataStore instanceof FlatFileDataStore) ? "(File Mode)" : "(Database Mode)";
        customLogger.AddLogEntry("Finished loading data " + dataMode + ".");

        //unless claim block accrual is disabled, start the recurring per 10 minute event to give claim blocks to online players
        //20L ~ 1 second
        if (configManager.config_claims_blocksAccruedPerHour_default > 0) {
            DeliverClaimBlocksTask task = new DeliverClaimBlocksTask(null, this, configManager, customLogger);
            this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 10, 20L * 60 * 10);
        }

        //start the recurring cleanup event for entities in creative worlds
        EntityCleanupTask task = new EntityCleanupTask(configManager, customLogger, 0);
        this.getServer().getScheduler().scheduleSyncDelayedTask(TheatriaClaims.instance, task, 20L * 60 * 2);

        //start recurring cleanup scan for unused claims belonging to inactive players
        FindUnusedClaimsTask task2 = new FindUnusedClaimsTask();
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task2, 20L * 60, 20L * configManager.config_advanced_claim_expiration_check_rate);

        //register for events
        PluginManager pluginManager = this.getServer().getPluginManager();

        //player events
        playerEventHandler = new PlayerEventHandler(this, this.dataStore, configManager, customLogger);
        pluginManager.registerEvents(playerEventHandler, this);

        //block events
        BlockEventHandler blockEventHandler = new BlockEventHandler(this.dataStore);
        pluginManager.registerEvents(blockEventHandler, this);

        //entity events
        entityEventHandler = new EntityEventHandler(this.dataStore, this);
        pluginManager.registerEvents(entityEventHandler, this);

        //vault-based economy integration
        economyHandler = new EconomyHandler(this);
        pluginManager.registerEvents(economyHandler, this);

        //register commands
        Objects.requireNonNull(Bukkit.getPluginCommand("gp")).setExecutor(new ChungusCommand(economyHandler, playerEventHandler));

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

        customLogger.AddLogEntry("Boot finished.");

    }

    public void setIgnoreStatus(OfflinePlayer ignorer, OfflinePlayer ignoree, IgnoreMode mode) {
        PlayerData playerData = this.dataStore.getPlayerData(ignorer.getUniqueId());
        if (mode == IgnoreMode.None) {
            playerData.ignoredPlayers.remove(ignoree.getUniqueId());
        }
        else {
            playerData.ignoredPlayers.put(ignoree.getUniqueId(), mode == IgnoreMode.StandardIgnore ? false : true);
        }

        playerData.ignoreListChanged = true;
        if (!ignorer.isOnline()) {
            this.dataStore.savePlayerData(ignorer.getUniqueId(), playerData);
            this.dataStore.clearCachedPlayerData(ignorer.getUniqueId());
        }
    }

    //TODO wtf move this to the enums
    public enum IgnoreMode {
        None, StandardIgnore, AdminIgnore
    }

    public String trustEntryToPlayerName(String entry) {
        if (entry.startsWith("[") || entry.equals("public")) {
            return entry;
        }
        else {
            return PlayerName.lookupPlayerName(entry);
        }
    }

    //TODO theres some creative mode stuff in here that needs to be addressed, ensure that things that were cut out arent breaking it.
    public boolean abandonClaimHandler(Player player, boolean deleteTopLevelClaim) {
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        //which claim is being abandoned?
        Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
        if (claim == null) {
            Messages.sendMessage(player, TextMode.Instr.getColor(), MessageType.AbandonClaimMissing);
        }

        //verify ownership
        else if (claim.checkPermission(player, ClaimPermission.Edit, null) != null) {
            Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.NotYourClaim);
        }

        //warn if has children and we're not explicitly deleting a top level claim
        else if (claim.children.size() > 0 && !deleteTopLevelClaim) {
            Messages.sendMessage(player, TextMode.Instr.getColor(), MessageType.DeleteTopLevelClaim);
            return true;
        }
        else {
            //delete it
            claim.removeSurfaceFluids(null);
            this.dataStore.deleteClaim(claim, true, false);

            //if in a creative mode world, restore the claim area
            if (TheatriaClaims.instance.creativeRulesApply(claim.getLesserBoundaryCorner())) {
                customLogger.AddLogEntry(player.getName() + " abandoned a claim @ " + GeneralUtils.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                Messages.sendMessage(player, TextMode.Warn.getColor(), MessageType.UnclaimCleanupWarning);
//                GriefPrevention.instance.restoreClaim(claim, 20L * 60 * 2);
            }

            //adjust claim blocks when abandoning a top level claim
            if (configManager.config_claims_abandonReturnRatio != 1.0D && claim.parent == null && claim.ownerID.equals(playerData.playerID)) {
                playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - (int) Math.ceil((claim.getArea() * (1 - configManager.config_claims_abandonReturnRatio))));
            }

            //tell the player how many claim blocks he has left
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            Messages.sendMessage(player, TextMode.Success.getColor(), MessageType.AbandonSuccess, String.valueOf(remainingBlocks));

            //revert any current visualization
            playerData.setVisibleBoundaries(null);

            playerData.warnedAboutMajorDeletion = false;
        }

        return true;

    }

    //helper method keeps the trust commands consistent and eliminates duplicate code
    public void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) {
        //determine which claim the player is standing in
        Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);

        //validate player or group argument
        String permission = null;
        OfflinePlayer otherPlayer = null;
        UUID recipientID = null;
        if (recipientName.startsWith("[") && recipientName.endsWith("]")) {
            permission = recipientName.substring(1, recipientName.length() - 1);
            if (permission == null || permission.isEmpty()) {
                Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.InvalidPermissionID);
                return;
            }
        }
        else {
            otherPlayer = PlayerName.resolvePlayerByName(recipientName);
            boolean isPermissionFormat = recipientName.contains(".");
            if (otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all") && !isPermissionFormat) {
                Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                return;
            }

            if (otherPlayer == null && isPermissionFormat) {
                //player does not exist and argument has a period so this is a permission instead
                permission = recipientName;
            }
            else if (otherPlayer != null) {
                recipientName = otherPlayer.getName();
                recipientID = otherPlayer.getUniqueId();
            }
            else {
                recipientName = "public";
            }
        }

        //determine which claims should be modified
        ArrayList<Claim> targetClaims = new ArrayList<>();
        if (claim == null) {
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            targetClaims.addAll(playerData.getClaims());
        }
        else {
            //check permission here
            if (claim.checkPermission(player, ClaimPermission.Manage, null) != null) {
                Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.NoPermissionTrust, claim.getOwnerName());
                return;
            }

            //see if the player has the level of permission he's trying to grant
            Supplier<String> errorMessage;

            //permission level null indicates granting permission trust
            if (permissionLevel == null) {
                errorMessage = claim.checkPermission(player, ClaimPermission.Edit, null);
                if (errorMessage != null) {
                    errorMessage = () -> "Only " + claim.getOwnerName() + " can grant /PermissionTrust here.";
                }
            }

            //otherwise just use the ClaimPermission enum values
            else {
                errorMessage = claim.checkPermission(player, permissionLevel, null);
            }

            //error message for trying to grant a permission the player doesn't have
            if (errorMessage != null) {
                Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.CantGrantThatPermission);
                return;
            }

            targetClaims.add(claim);
        }

        //if we didn't determine which claims to modify, tell the player to be specific
        if (targetClaims.size() == 0) {
            Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.GrantPermissionNoClaim);
            return;
        }

        String identifierToAdd = recipientName;
        if (permission != null) {
            identifierToAdd = "[" + permission + "]";
            //replace recipientName as well so the success message clearly signals a permission
            recipientName = identifierToAdd;
        }
        else if (recipientID != null) {
            identifierToAdd = recipientID.toString();
        }

        //calling the event
        TrustChangedEvent event = new TrustChangedEvent(player, targetClaims, permissionLevel, true, identifierToAdd);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        //apply changes
        for (Claim currentClaim : event.getClaims()) {
            if (permissionLevel == null) {
                if (!currentClaim.managers.contains(identifierToAdd)) {
                    currentClaim.managers.add(identifierToAdd);
                }
            }
            else {
                currentClaim.setPermission(identifierToAdd, permissionLevel);
            }
            this.dataStore.saveClaim(currentClaim);
        }

        //notify player
        if (recipientName.equals("public")) recipientName = this.dataStore.getMessage(MessageType.CollectivePublic);
        String permissionDescription;
        if (permissionLevel == null) {
            permissionDescription = this.dataStore.getMessage(MessageType.PermissionsPermission);
        }
        else if (permissionLevel == ClaimPermission.Build) {
            permissionDescription = this.dataStore.getMessage(MessageType.BuildPermission);
        }
        else if (permissionLevel == ClaimPermission.Access) {
            permissionDescription = this.dataStore.getMessage(MessageType.AccessPermission);
        }
        //ClaimPermission.Inventory
        else {
            permissionDescription = this.dataStore.getMessage(MessageType.ContainersPermission);
        }

        String location;
        if (claim == null) {
            location = this.dataStore.getMessage(MessageType.LocationAllClaims);
        }
        else {
            location = this.dataStore.getMessage(MessageType.LocationCurrentClaim);
        }

        Messages.sendMessage(player, TextMode.Success.getColor(), MessageType.GrantPermissionConfirmation, recipientName, permissionDescription, location);
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
                    if (daysDiff <= configManager.config_advanced_offlineplayer_cache_days) {
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

        //dump any remaining unwritten log entries
        this.customLogger.WriteEntries();

        customLogger.AddLogEntry("GriefPrevention disabled.");
    }

    //called when a player spawns, applies protection for that player if necessary
    public void checkPvpProtectionNeeded(Player player) {
        //if anti spawn camping feature is not enabled, do nothing
        if (!configManager.config_pvp_protectFreshSpawns) return;

        //if pvp is disabled, do nothing
        if (!pvpRulesApply(player.getWorld())) return;

        //if player is in creative mode, do nothing
        if (player.getGameMode() == GameMode.CREATIVE) return;

        //if the player has the damage any player permission enabled, do nothing
        if (player.hasPermission("griefprevention.nopvpimmunity")) return;

        //check inventory for well, anything
        if (TheatriaClaims.isInventoryEmpty(player)) {
            //if empty, apply immunity
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            playerData.pvpImmune = true;

            //inform the player after he finishes respawning
            Messages.sendMessage(player, TextMode.Success.getColor(), MessageType.PvPImmunityStart, 5L);

            //start a task to re-check this player's inventory every minute until his immunity is gone
            PvPImmunityValidationTask task = new PvPImmunityValidationTask(player);
            this.getServer().getScheduler().scheduleSyncDelayedTask(this, task, 1200L);
        }
    }

    public static boolean isInventoryEmpty(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armorStacks = inventory.getArmorContents();

        //check armor slots, stop if any items are found
        for (ItemStack armorStack : armorStacks) {
            if (!(armorStack == null || armorStack.getType() == Material.AIR)) return false;
        }

        //check other slots, stop if any items are found
        ItemStack[] generalStacks = inventory.getContents();
        for (ItemStack generalStack : generalStacks) {
            if (!(generalStack == null || generalStack.getType() == Material.AIR)) return false;
        }

        return true;
    }
    //TODO lets double check if these while loops are necessary.
    //moves a player from the claim he's in to a nearby wilderness location
    public Location ejectPlayer(Player player) {
        //look for a suitable location
        Location candidateLocation = player.getLocation();
        while (true) {
            Claim claim = null;
            claim = TheatriaClaims.instance.dataStore.getClaimAt(candidateLocation, false, null);

            //if there's a claim here, keep looking
            if (claim != null) {
                candidateLocation = new Location(claim.lesserBoundaryCorner.getWorld(), claim.lesserBoundaryCorner.getBlockX() - 1, claim.lesserBoundaryCorner.getBlockY(), claim.lesserBoundaryCorner.getBlockZ() - 1);
                continue;
            }

            //otherwise find a safe place to teleport the player
            else {
                //find a safe height, a couple of blocks above the surface
                GuaranteeChunkLoaded(candidateLocation);
                Block highestBlock = candidateLocation.getWorld().getHighestBlockAt(candidateLocation.getBlockX(), candidateLocation.getBlockZ());
                Location destination = new Location(highestBlock.getWorld(), highestBlock.getX(), highestBlock.getY() + 2, highestBlock.getZ());
                player.teleport(destination);
                return destination;
            }
        }
    }

    //ensures a piece of the managed world is loaded into server memory
    //(generates the chunk if necessary)
    private static void GuaranteeChunkLoaded(Location location) {
        Chunk chunk = location.getChunk();
        while (!chunk.isLoaded() || !chunk.load(true)) ;
    }

    //checks whether players can create claims in a world
    public boolean claimsEnabledForWorld(World world) {
        ClaimsMode mode = configManager.config_claims_worldModes.get(world);
        return mode != null && mode != ClaimsMode.Disabled;
    }

    //determines whether creative anti-grief rules apply at a location
    public boolean creativeRulesApply(Location location) {
        if (!configManager.config_creativeWorldsExist) return false;
        return configManager.config_claims_worldModes.get((location.getWorld())) == ClaimsMode.Creative;
    }

    public String allowBuild(Player player, Location location) {
        // TODO check all derivatives and rework API
        return this.allowBuild(player, location, location.getBlock().getType());
    }

    public String allowBuild(Player player, Location location, Material material) {
        if (!TheatriaClaims.instance.claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        //wilderness rules
        if (claim == null) {
            //no building in the wilderness in creative mode
            if (this.creativeRulesApply(location) || configManager.config_claims_worldModes.get(location.getWorld()) == ClaimsMode.SurvivalRequiringClaims) {
                //exception: when chest claims are enabled, players who have zero land claims and are placing a chest
                if (material != Material.CHEST || playerData.getClaims().size() > 0 || configManager.config_claims_automaticClaimsForNewPlayersRadius == -1) {
                    String reason = this.dataStore.getMessage(MessageType.NoBuildOutsideClaims);
                    if (player.hasPermission("griefprevention.ignoreclaims"))
                        reason += "  " + this.dataStore.getMessage(MessageType.IgnoreClaimsAdvertisement);
                    reason += "  " + this.dataStore.getMessage(MessageType.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
                    return reason;
                }
                else {
                    return null;
                }
            }

            //but it's fine in survival mode
            else {
                return null;
            }
        }

        //if not in the wilderness, then apply claim rules (permissions, etc)
        else {
            //cache the claim for later reference
            playerData.lastClaim = claim;
            Block block = location.getBlock();

            Supplier<String> supplier = claim.checkPermission(player, ClaimPermission.Build, new BlockPlaceEvent(block, block.getState(), block, new ItemStack(material), player, true, EquipmentSlot.HAND));

            if (supplier == null) return null;

            return supplier.get();
        }
    }

    //TODO these methods do nothing currently, could be import issues though
    public String allowBreak(Player player, Block block, Location location) {
        return this.allowBreak(player, block, location, new BlockBreakEvent(block, player));
    }

    public String allowBreak(Player player, Material material, Location location, BlockBreakEvent breakEvent) {
        return this.allowBreak(player, location.getBlock(), location, breakEvent);
    }

    public String allowBreak(Player player, Block block, Location location, BlockBreakEvent breakEvent) {
        if (!TheatriaClaims.instance.claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        //wilderness rules
        if (claim == null){
            //no building in the wilderness in creative mode
            if (this.creativeRulesApply(location) || configManager.config_claims_worldModes.get(location.getWorld()) == ClaimsMode.SurvivalRequiringClaims) {
                String reason = this.dataStore.getMessage(MessageType.NoBuildOutsideClaims);
                if (player.hasPermission("griefprevention.ignoreclaims"))
                    reason += "  " + this.dataStore.getMessage(MessageType.IgnoreClaimsAdvertisement);
                reason += "  " + this.dataStore.getMessage(MessageType.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
                return reason;
            }

            //but it's fine in survival mode
            else {
                return null;
            }
        }
        else
        {
            //cache the claim for later reference
            playerData.lastClaim = claim;

            //if not in the wilderness, then apply claim rules (permissions, etc)
            Supplier<String> cancel = claim.checkPermission(player, ClaimPermission.Build, breakEvent);
            if (cancel != null && breakEvent != null) {
                PreventBlockBreakEvent preventionEvent = new PreventBlockBreakEvent(breakEvent);
                Bukkit.getPluginManager().callEvent(preventionEvent);
                if (preventionEvent.isCancelled())
                {
                    cancel = null;
                }
            }
            if (cancel == null) return null;
            return cancel.get();
        }
    }

    public Set<Material> parseMaterialListFromConfig(List<String> stringsToParse) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        //for each string in the list
        for (int i = 0; i < stringsToParse.size(); i++) {
            String string = stringsToParse.get(i);
            //defensive coding
            if (string == null) continue;
            //try to parse the string value into a material
            Material material = Material.getMaterial(string.toUpperCase());
            //null value returned indicates an error parsing the string from the config file
            if (material == null) {
                //check if string has failed validity before
                if (!string.contains("can't")) {
                    //update string, which will go out to config file to help user find the error entry
                    stringsToParse.set(i, string + "     <-- can't understand this entry, see BukkitDev documentation");
                    //warn about invalid material in log
                    Bukkit.getLogger().info(String.format("ERROR: Invalid material %s.  Please update your config.yml.", string));
                }
            }
            //otherwise material is valid, add it
            else {
                materials.add(material);
            }
        }
        return materials;
    }

    //TODO not sure if this is needed since we wont use this setting.
    public int getSeaLevel(World world) {
        Integer overrideValue = configManager.config_seaLevelOverride.get(world.getName());
        if (overrideValue == null || overrideValue == -1) {
            return world.getSeaLevel();
        }
        else {
            return overrideValue;
        }
    }

    public boolean pvpRulesApply(World world) {
        Boolean configSetting = configManager.config_pvp_specifiedWorlds.get(world);
        if (configSetting != null) return configSetting;
        return world.getPVP();
    }

    //Track scheduled "rescues" so we can cancel them if the player happens to teleport elsewhere so we can cancel it.
    public ConcurrentHashMap<UUID, BukkitTask> portalReturnTaskMap = new ConcurrentHashMap<>();

}
