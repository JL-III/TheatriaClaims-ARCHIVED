/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jliii.theatriaclaims.util;

import com.google.common.io.Files;
import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.CreateClaimResult;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.events.*;
import com.jliii.theatriaclaims.managers.ConfigManager;
import com.jliii.theatriaclaims.visualization.BoundaryVisualization;
import com.jliii.theatriaclaims.visualization.VisualizationType;
import org.bukkit.*;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.InventoryHolder;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

//singleton class which manages all GriefPrevention data (except for config options)
public abstract class DataStore {

    private ConfigManager configManager;

    public DataStore(ConfigManager configManager) {
        this.configManager = configManager;
    }

    //in-memory cache for player data
    protected ConcurrentHashMap<UUID, PlayerData> playerNameToPlayerDataMap = new ConcurrentHashMap<>();

    //in-memory cache for group (permission-based) data
    protected ConcurrentHashMap<String, Integer> permissionToBonusBlocksMap = new ConcurrentHashMap<>();

    //in-memory cache for claim data
    public ArrayList<Claim> claims = new ArrayList<>();
    public ConcurrentHashMap<Long, ArrayList<Claim>> chunksToClaimsMap = new ConcurrentHashMap<>();

    //pattern for unique user identifiers (UUIDs)
    protected final static Pattern uuidpattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    //next claim ID
    Long nextClaimID = (long) 0;

    //path information, for where stuff stored on disk is well...  stored
    protected final static String dataLayerFolderPath = "plugins" + File.separator + "GriefPreventionData";
    public final static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";
    public final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";

    //the latest version of the data schema implemented here
    protected static final int latestSchemaVersion = 3;

    //reading and writing the schema version to the data store
    abstract int getSchemaVersionFromStorage();

    abstract void updateSchemaVersionInStorage(int versionToSet);

    //current version of the schema of data in secondary storage
    private int currentSchemaVersion = -1;  //-1 means not determined yet

    //video links
    //TODO change these video links, or remove the feature altogether
    public static final String SURVIVAL_VIDEO_URL = "" + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + "bit.ly/mcgpuser" + ChatColor.RESET;
    public static final String CREATIVE_VIDEO_URL = "" + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + "bit.ly/mcgpcrea" + ChatColor.RESET;
    public static final String SUBDIVISION_VIDEO_URL = "" + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + "bit.ly/mcgpsub" + ChatColor.RESET;

    //list of UUIDs which are soft-muted
    ConcurrentHashMap<UUID, Boolean> softMuteMap = new ConcurrentHashMap<>();

    //world guard reference, if available
    private WorldGuardWrapper worldGuard = null;

    protected int getSchemaVersion() {
        if (this.currentSchemaVersion >= 0) {
            return this.currentSchemaVersion;
        }
        else {
            this.currentSchemaVersion = this.getSchemaVersionFromStorage();
            return this.currentSchemaVersion;
        }
    }

    protected void setSchemaVersion(int versionToSet) {
        this.currentSchemaVersion = versionToSet;
        this.updateSchemaVersionInStorage(versionToSet);
    }

    //initialization!
    void initialize() throws Exception {
        CustomLogger.log(this.claims.size() + " total claims loaded.");

        //RoboMWM: ensure the nextClaimID is greater than any other claim ID. If not, data corruption occurred (out of storage space, usually).
        for (Claim claim : this.claims)
        {
            if (claim.id >= nextClaimID)
            {
                TheatriaClaims.instance.getLogger().severe("nextClaimID was lesser or equal to an already-existing claim ID!\n" +
                        "This usually happens if you ran out of storage space.");
                CustomLogger.log("Changing nextClaimID from " + nextClaimID + " to " + claim.id);
                nextClaimID = claim.id + 1;
            }
        }

        //ensure data folders exist
        File playerDataFolder = new File(playerDataFolderPath);
        if (!playerDataFolder.exists())
        {
            playerDataFolder.mkdirs();
        }

        //if converting up from an earlier schema version, write all claims back to storage using the latest format
        if (this.getSchemaVersion() < latestSchemaVersion)
        {
            CustomLogger.log("Please wait.  Updating data format.");

            for (Claim claim : this.claims)
            {
                this.saveClaim(claim);

                for (Claim subClaim : claim.children)
                {
                    this.saveClaim(subClaim);
                }
            }

            //clean up any UUID conversion work
            if (UUIDFetcher.lookupCache != null)
            {
                UUIDFetcher.lookupCache.clear();
                UUIDFetcher.correctedNames.clear();
            }

            CustomLogger.log("Update finished.");
        }

        //make a note of the data store schema version
        this.setSchemaVersion(latestSchemaVersion);

        //try to hook into world guard
        try
        {
            this.worldGuard = new WorldGuardWrapper();
            CustomLogger.log("Successfully hooked into WorldGuard.");
        }
        //if failed, world guard compat features will just be disabled.
        catch (IllegalStateException | IllegalArgumentException | ClassCastException | NoClassDefFoundError ignored) { }
    }

    //removes cached player data from memory
    public synchronized void clearCachedPlayerData(UUID playerID)
    {
        this.playerNameToPlayerDataMap.remove(playerID);
    }

    //gets the number of bonus blocks a player has from his permissions
    //Bukkit doesn't allow for checking permissions of an offline player.
    //this will return 0 when he's offline, and the correct number when online.
    synchronized public int getGroupBonusBlocks(UUID playerID)
    {
        Player player = TheatriaClaims.instance.getServer().getPlayer(playerID);

        if (player == null) return 0;

        int bonusBlocks = 0;

        for (Map.Entry<String, Integer> groupEntry : this.permissionToBonusBlocksMap.entrySet())
        {
            if (player.hasPermission(groupEntry.getKey()))
            {
                bonusBlocks += groupEntry.getValue();
            }
        }

        return bonusBlocks;
    }

    //grants a group (players with a specific permission) bonus claim blocks as long as they're still members of the group
    synchronized public int adjustGroupBonusBlocks(String groupName, int amount)
    {
        Integer currentValue = this.permissionToBonusBlocksMap.get(groupName);
        if (currentValue == null) currentValue = 0;

        currentValue += amount;
        this.permissionToBonusBlocksMap.put(groupName, currentValue);

        //write changes to storage to ensure they don't get lost
        this.saveGroupBonusBlocks(groupName, currentValue);

        return currentValue;
    }

    abstract void saveGroupBonusBlocks(String groupName, int amount);

    public class NoTransferException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        NoTransferException(String message)
        {
            super(message);
        }
    }

    synchronized public void changeClaimOwner(Claim claim, UUID newOwnerID)
    {
        //if it's a subdivision, throw an exception
        if (claim.parent != null)
        {
            throw new NoTransferException("Subdivisions can't be transferred.  Only top-level claims may change owners.");
        }

        //otherwise update information

        //determine current claim owner
        PlayerData ownerData = null;
        if (!claim.isAdminClaim())
        {
            ownerData = this.getPlayerData(claim.ownerID);
        }

        //call event
        ClaimTransferEvent event = new ClaimTransferEvent(claim, newOwnerID);
        Bukkit.getPluginManager().callEvent(event);

        //return if event is cancelled
        if (event.isCancelled()) return;

        //determine new owner
        PlayerData newOwnerData = null;

        if (event.getNewOwner() != null)
        {
            newOwnerData = this.getPlayerData(event.getNewOwner());
        }

        //transfer
        claim.ownerID = event.getNewOwner();
        this.saveClaim(claim);

        //adjust blocks and other records
        if (ownerData != null)
        {
            ownerData.getClaims().remove(claim);
        }

        if (newOwnerData != null)
        {
            newOwnerData.getClaims().add(claim);
        }
    }

    //adds a claim to the datastore, making it an effective claim
    synchronized void addClaim(Claim newClaim, boolean writeToStorage)
    {
        //subdivisions are added under their parent, not directly to the hash map for direct search
        if (newClaim.parent != null)
        {
            if (!newClaim.parent.children.contains(newClaim))
            {
                newClaim.parent.children.add(newClaim);
            }
            newClaim.inDataStore = true;
            if (writeToStorage)
            {
                this.saveClaim(newClaim);
            }
            return;
        }

        //add it and mark it as added
        this.claims.add(newClaim);
        addToChunkClaimMap(newClaim);

        newClaim.inDataStore = true;

        //except for administrative claims (which have no owner), update the owner's playerData with the new claim
        if (!newClaim.isAdminClaim() && writeToStorage)
        {
            PlayerData ownerData = this.getPlayerData(newClaim.ownerID);
            ownerData.getClaims().add(newClaim);
        }

        //make sure the claim is saved to disk
        if (writeToStorage)
        {
            this.saveClaim(newClaim);
        }
    }

    private void addToChunkClaimMap(Claim claim)
    {
        // Subclaims should not be added to chunk claim map.
        if (claim.parent != null) return;

        ArrayList<Long> chunkHashes = claim.getChunkHashes();
        for (Long chunkHash : chunkHashes)
        {
            ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkHash);
            if (claimsInChunk == null)
            {
                this.chunksToClaimsMap.put(chunkHash, claimsInChunk = new ArrayList<>());
            }

            claimsInChunk.add(claim);
        }
    }

    private void removeFromChunkClaimMap(Claim claim)
    {
        ArrayList<Long> chunkHashes = claim.getChunkHashes();
        for (Long chunkHash : chunkHashes)
        {
            ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkHash);
            if (claimsInChunk != null)
            {
                for (Iterator<Claim> it = claimsInChunk.iterator(); it.hasNext(); )
                {
                    Claim c = it.next();
                    if (c.id.equals(claim.id))
                    {
                        it.remove();
                        break;
                    }
                }
                if (claimsInChunk.isEmpty())
                { // if nothing's left, remove this chunk's cache
                    this.chunksToClaimsMap.remove(chunkHash);
                }
            }
        }
    }

    //turns a location into a string, useful in data storage
    private final String locationStringDelimiter = ";";

    String locationToString(Location location)
    {
        StringBuilder stringBuilder = new StringBuilder(location.getWorld().getName());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockX());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockY());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockZ());

        return stringBuilder.toString();
    }

    //turns a location string back into a location
    Location locationFromString(String string, List<World> validWorlds) throws Exception {
        //split the input string on the space
        String[] elements = string.split(locationStringDelimiter);

        //expect four elements - world name, X, Y, and Z, respectively
        if (elements.length < 4)
        {
            throw new Exception("Expected four distinct parts to the location string: \"" + string + "\"");
        }

        String worldName = elements[0];
        String xString = elements[1];
        String yString = elements[2];
        String zString = elements[3];

        //identify world the claim is in
        World world = null;
        for (World w : validWorlds)
        {
            if (w.getName().equalsIgnoreCase(worldName))
            {
                world = w;
                break;
            }
        }

        if (world == null)
        {
            throw new Exception("World not found: \"" + worldName + "\"");
        }

        //convert those numerical strings to integer values
        int x = Integer.parseInt(xString);
        int y = Integer.parseInt(yString);
        int z = Integer.parseInt(zString);

        return new Location(world, x, y, z);
    }

    //saves any changes to a claim to secondary storage
    synchronized public void saveClaim(Claim claim)
    {
        assignClaimID(claim);

        this.writeClaimToStorage(claim);
    }

    private void assignClaimID(Claim claim)
    {
        //ensure a unique identifier for the claim which will be used to name the file on disk
        if (claim.id == null || claim.id == -1)
        {
            claim.id = this.nextClaimID;
            this.incrementNextClaimID();
        }
    }

    abstract void writeClaimToStorage(Claim claim);

    //increments the claim ID and updates secondary storage to be sure it's saved
    abstract void incrementNextClaimID();

    //retrieves player data from memory or secondary storage, as necessary
    //if the player has never been on the server before, this will return a fresh player data with default values
    synchronized public PlayerData getPlayerData(UUID playerID) {
        //first, look in memory
        PlayerData playerData = this.playerNameToPlayerDataMap.get(playerID);

        //if not there, build a fresh instance with some blanks for what may be in secondary storage
        if (playerData == null) {
            playerData = new PlayerData(configManager);
            playerData.playerID = playerID;

            //shove that new player data into the hash map cache
            this.playerNameToPlayerDataMap.put(playerID, playerData);
        }

        return playerData;
    }

    public abstract PlayerData getPlayerDataFromStorage(UUID playerID);

    //deletes a claim or subdivision
    synchronized public void deleteClaim(Claim claim)
    {
        this.deleteClaim(claim, true, false);
    }

    //deletes a claim or subdivision
    synchronized public void deleteClaim(Claim claim, boolean releasePets) {
        this.deleteClaim(claim, true, releasePets);
    }

    public synchronized void deleteClaim(Claim claim, boolean fireEvent, boolean releasePets) {
        //delete any children
        for (int j = 1; (j - 1) < claim.children.size(); j++) {
            this.deleteClaim(claim.children.get(j - 1), true);
        }

        //subdivisions must also be removed from the parent claim child list
        if (claim.parent != null) {
            Claim parentClaim = claim.parent;
            parentClaim.children.remove(claim);
        }

        //mark as deleted so any references elsewhere can be ignored
        claim.inDataStore = false;

        //remove from memory
        for (int i = 0; i < this.claims.size(); i++) {
            if (claims.get(i).id.equals(claim.id)) {
                this.claims.remove(i);
                break;
            }
        }

        removeFromChunkClaimMap(claim);

        //remove from secondary storage
        this.deleteClaimFromSecondaryStorage(claim);

        //update player data
        if (claim.ownerID != null) {
            PlayerData ownerData = this.getPlayerData(claim.ownerID);
            for (int i = 0; i < ownerData.getClaims().size(); i++) {
                if (ownerData.getClaims().get(i).id.equals(claim.id)) {
                    ownerData.getClaims().remove(i);
                    break;
                }
            }
            this.savePlayerData(claim.ownerID, ownerData);
        }

        if (fireEvent) {
            ClaimDeletedEvent ev = new ClaimDeletedEvent(claim);
            Bukkit.getPluginManager().callEvent(ev);
        }

        //optionally set any pets free which belong to the claim owner
        if (releasePets && claim.ownerID != null && claim.parent == null) {
            for (Chunk chunk : claim.getChunks()) {
                Entity[] entities = chunk.getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof Tameable) {
                        Tameable pet = (Tameable) entity;
                        if (pet.isTamed()) {
                            AnimalTamer owner = pet.getOwner();
                            if (owner != null) {
                                UUID ownerID = owner.getUniqueId();
                                if (ownerID != null) {
                                    if (ownerID.equals(claim.ownerID)) {
                                        pet.setTamed(false);
                                        pet.setOwner(null);
                                        if (pet instanceof InventoryHolder) {
                                            InventoryHolder holder = (InventoryHolder) pet;
                                            holder.getInventory().clear();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    abstract void deleteClaimFromSecondaryStorage(Claim claim);

    //gets the claim at a specific location
    //ignoreHeight = TRUE means that a location UNDER an existing claim will return the claim
    //cachedClaim can be NULL, but will help performance if you have a reasonable guess about which claim the location is in
    synchronized public Claim getClaimAt(Location location, boolean ignoreHeight, Claim cachedClaim) {
        return getClaimAt(location, ignoreHeight, false, cachedClaim);
    }

    /**
     * Get the claim at a specific location.
     *
     * <p>The cached claim may be null, but will increase performance if you have a reasonable idea
     * of which claim is correct.
     *
     * @param location the location
     * @param ignoreHeight whether or not to check containment vertically
     * @param ignoreSubclaims whether or not subclaims should be returned over claims
     * @param cachedClaim the cached claim, if any
     * @return the claim containing the location or null if no claim exists there
     */
    synchronized public Claim getClaimAt(Location location, boolean ignoreHeight, boolean ignoreSubclaims, Claim cachedClaim) {
        //check cachedClaim guess first.  if it's in the datastore and the location is inside it, we're done
        if (cachedClaim != null && cachedClaim.inDataStore && cachedClaim.contains(location, ignoreHeight, !ignoreSubclaims))
            return cachedClaim;

        //find a top level claim
        Long chunkID = getChunkHash(location);
        ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkID);
        if (claimsInChunk == null) return null;

        for (Claim claim : claimsInChunk) {
            if (claim.inDataStore && claim.contains(location, ignoreHeight, false)) {
                // If ignoring subclaims, claim is a match.
                if (ignoreSubclaims) return claim;

                //when we find a top level claim, if the location is in one of its subdivisions,
                //return the SUBDIVISION, not the top level claim
                for (int j = 0; j < claim.children.size(); j++) {
                    Claim subdivision = claim.children.get(j);
                    if (subdivision.inDataStore && subdivision.contains(location, ignoreHeight, false))
                        return subdivision;
                }

                return claim;
            }
        }

        //if no claim found, return null
        return null;
    }

    //finds a claim by ID
    public synchronized Claim getClaim(long id) {
        for (Claim claim : this.claims) {
            if (claim.inDataStore) {
                if (claim.getID() == id)
                    return claim;
                for (Claim subClaim : claim.children) {
                    if (subClaim.getID() == id)
                    return subClaim;
                }
            }
        }

        return null;
    }

    //returns a read-only access point for the list of all land claims
    //if you need to make changes, use provided methods like .deleteClaim() and .createClaim().
    //this will ensure primary memory (RAM) and secondary memory (disk, database) stay in sync
    public Collection<Claim> getClaims() {
        return Collections.unmodifiableCollection(this.claims);
    }

    public Collection<Claim> getClaims(int chunkx, int chunkz) {
        ArrayList<Claim> chunkClaims = this.chunksToClaimsMap.get(getChunkHash(chunkx, chunkz));
        if (chunkClaims != null) {
            return Collections.unmodifiableCollection(chunkClaims);
        }
        else {
            return Collections.unmodifiableCollection(new ArrayList<>());
        }
    }

    //gets an almost-unique, persistent identifier for a chunk
    public static Long getChunkHash(long chunkx, long chunkz) {
        return (chunkz ^ (chunkx << 32));
    }

    //gets an almost-unique, persistent identifier for a chunk
    public static Long getChunkHash(Location location) {
        return getChunkHash(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static ArrayList<Long> getChunkHashes(Claim claim) {
        return getChunkHashes(claim.getLesserBoundaryCorner(), claim.getGreaterBoundaryCorner());
    }

    public static ArrayList<Long> getChunkHashes(Location min, Location max) {
        ArrayList<Long> hashes = new ArrayList<>();
        int smallX = min.getBlockX() >> 4;
        int smallZ = min.getBlockZ() >> 4;
        int largeX = max.getBlockX() >> 4;
        int largeZ = max.getBlockZ() >> 4;

        for (int x = smallX; x <= largeX; x++) {
            for (int z = smallZ; z <= largeZ; z++) {
                hashes.add(getChunkHash(x, z));
            }
        }

        return hashes;
    }

    /*
     * Creates a claim and flags it as being new....throwing a create claim event;
     */
    synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int z1, int z2, UUID ownerID, Claim parent, Long id, Player creatingPlayer) {
        return createClaim(world, x1, x2, z1, z2, ownerID, parent, id, creatingPlayer, false);
    }

    //creates a claim.
    //if the new claim would overlap an existing claim, returns a failure along with a reference to the existing claim
    //if the new claim would overlap a WorldGuard region where the player doesn't have permission to build, returns a failure with NULL for claim
    //otherwise, returns a success along with a reference to the new claim
    //use ownerName == "" for administrative claims
    //for top level claims, pass parent == NULL
    //DOES adjust claim blocks available on success (players can go into negative quantity available)
    //DOES check for world guard regions where the player doesn't have permission
    //does NOT check a player has permission to create a claim, or enough claim blocks.
    //does NOT check minimum claim size constraints
    //does NOT visualize the new claim for any players
    synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int z1, int z2, UUID ownerID, Claim parent, Long id, Player creatingPlayer, boolean dryRun) {
        CreateClaimResult result = new CreateClaimResult();

        int smallx, bigx, smallz, bigz;

        //determine small versus big inputs
        if (x1 < x2) {
            smallx = x1;
            bigx = x2;
        }
        else {
            smallx = x2;
            bigx = x1;
        }

        if (z1 < z2) {
            smallz = z1;
            bigz = z2;
        }
        else {
            smallz = z2;
            bigz = z1;
        }

        if (parent != null) {
            Location lesser = parent.getLesserBoundaryCorner();
            Location greater = parent.getGreaterBoundaryCorner();
            if (smallx < lesser.getX() || smallz < lesser.getZ() || bigx > greater.getX() || bigz > greater.getZ()) {
                result.succeeded = false;
                result.claim = parent;
                return result;
            }
        }

        //create a new claim instance (but don't save it, yet)
        Claim newClaim = new Claim(
                new Location(world, smallx, world.getMinHeight(), smallz),
                new Location(world, bigx, world.getMaxHeight(), bigz),
                ownerID,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                id);

        newClaim.parent = parent;

        //ensure this new claim won't overlap any existing claims
        ArrayList<Claim> claimsToCheck;
        if (newClaim.parent != null) {
            claimsToCheck = newClaim.parent.children;
        }
        else {
            claimsToCheck = this.claims;
        }

        for (Claim otherClaim : claimsToCheck) {
            //if we find an existing claim which will be overlapped
            if (otherClaim.id != newClaim.id && otherClaim.inDataStore && otherClaim.overlaps(newClaim)) {
                //result = fail, return conflicting claim
                result.succeeded = false;
                result.claim = otherClaim;
                return result;
            }
        }

        //if worldguard is installed, also prevent claims from overlapping any worldguard regions
        if (configManager.getWorldConfig().respectWorldGuard && this.worldGuard != null && creatingPlayer != null) {
            if (!this.worldGuard.canBuild(newClaim.lesserBoundaryCorner, newClaim.getGreaterBoundaryCorner(), creatingPlayer)) {
                result.succeeded = false;
                result.claim = null;
                return result;
            }
        }
        if (dryRun) {
            // since this is a dry run, just return the unsaved claim as is.
            result.succeeded = true;
            result.claim = newClaim;
            return result;
        }
        assignClaimID(newClaim); // assign a claim ID before calling event, in case a plugin wants to know the ID.
        ClaimCreatedEvent event = new ClaimCreatedEvent(newClaim, creatingPlayer);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            result.succeeded = false;
            result.claim = null;
            return result;

        }
        //otherwise add this new claim to the data store to make it effective
        this.addClaim(newClaim, true);

        //then return success along with reference to new claim
        result.succeeded = true;
        result.claim = newClaim;
        return result;
    }

    //saves changes to player data to secondary storage.  MUST be called after you're done making changes, otherwise a reload will lose them
    public void savePlayerDataSync(UUID playerID, PlayerData playerData) {
        //ensure player data is already read from file before trying to save
        playerData.getAccruedClaimBlocks();
        playerData.getClaims();

        this.asyncSavePlayerData(playerID, playerData);
    }

    //saves changes to player data to secondary storage.  MUST be called after you're done making changes, otherwise a reload will lose them
    public void savePlayerData(UUID playerID, PlayerData playerData) {
        new SavePlayerDataThread(playerID, playerData).start();
    }

    public void asyncSavePlayerData(UUID playerID, PlayerData playerData) {
        //save everything except the ignore list
        this.overrideSavePlayerData(playerID, playerData);

        //save the ignore list
        if (playerData.ignoreListChanged) {
            StringBuilder fileContent = new StringBuilder();
            try {
                for (UUID uuidKey : playerData.ignoredPlayers.keySet()) {
                    Boolean value = playerData.ignoredPlayers.get(uuidKey);
                    if (value == null) continue;

                    //admin-enforced ignores begin with an asterisk
                    if (value) {
                        fileContent.append("*");
                    }

                    fileContent.append(uuidKey);
                    fileContent.append("\n");
                }

                //write data to file
                File playerDataFile = new File(playerDataFolderPath + File.separator + playerID + ".ignore");
                Files.write(fileContent.toString().trim().getBytes("UTF-8"), playerDataFile);
            }

            //if any problem, log it
            catch (Exception e) {
                CustomLogger.log("GriefPrevention: Unexpected exception saving data for player \"" + playerID.toString() + "\": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    abstract void overrideSavePlayerData(UUID playerID, PlayerData playerData);

    //deletes all claims owned by a player
    synchronized public void deleteClaimsForPlayer(UUID playerID, boolean releasePets) {
        //make a list of the player's claims
        ArrayList<Claim> claimsToDelete = new ArrayList<>();
        for (Claim claim : this.claims) {
            if ((playerID == claim.ownerID || (playerID != null && playerID.equals(claim.ownerID))))
                claimsToDelete.add(claim);
        }

        //delete them one by one
        for (Claim claim : claimsToDelete) {
            this.deleteClaim(claim, releasePets);

        }
    }

    //tries to resize a claim
    //see CreateClaim() for details on return value
    synchronized public CreateClaimResult resizeClaim(Claim claim, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2, Player resizingPlayer) {
        //try to create this new claim, ignoring the original when checking for overlap
        CreateClaimResult result = this.createClaim(claim.getLesserBoundaryCorner().getWorld(), newx1, newx2, newz1, newz2, claim.ownerID, claim.parent, claim.id, resizingPlayer, true);

        //if succeeded
        if (result.succeeded) {
            removeFromChunkClaimMap(claim); // remove the old boundary from the chunk cache
            // copy the boundary from the claim created in the dry run of createClaim() to our existing claim
            claim.lesserBoundaryCorner = result.claim.lesserBoundaryCorner;
            claim.greaterBoundaryCorner = result.claim.greaterBoundaryCorner;
            result.claim = claim;
            addToChunkClaimMap(claim); // add the new boundary to the chunk cache
        }

        return result;
    }

    public void resizeClaimWithChecks(Player player, PlayerData playerData, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2) {
        //for top level claims, apply size rules and claim blocks requirement
        if (playerData.claimResizing.parent == null) {
            //measure new claim, apply size rules
            int newWidth = (Math.abs(newx1 - newx2) + 1);
            int newHeight = (Math.abs(newz1 - newz2) + 1);
            boolean smaller = newWidth < playerData.claimResizing.getWidth() || newHeight < playerData.claimResizing.getHeight();

            if (!player.hasPermission("griefprevention.adminclaims") && !playerData.claimResizing.isAdminClaim() && smaller) {
                if (newWidth < configManager.getSystemConfig().minWidth || newHeight < configManager.getSystemConfig().minWidth) {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ResizeClaimTooNarrow, String.valueOf(configManager.getSystemConfig().minWidth));
                    return;
                }

                int newArea = newWidth * newHeight;
                if (newArea < configManager.getSystemConfig().minArea) {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ResizeClaimInsufficientArea, String.valueOf(configManager.getSystemConfig().minArea));
                    return;
                }
            }

            //make sure player has enough blocks to make up the difference
            if (!playerData.claimResizing.isAdminClaim() && player.getName().equals(playerData.claimResizing.getOwnerName())) {
                int newArea = newWidth * newHeight;
                int blocksRemainingAfter = playerData.getRemainingClaimBlocks() + playerData.claimResizing.getArea() - newArea;

                if (blocksRemainingAfter < 0) {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ResizeNeedMoreBlocks, String.valueOf(Math.abs(blocksRemainingAfter)));
                    this.tryAdvertiseAdminAlternatives(player);
                    return;
                }
            }
        }

        Claim oldClaim = playerData.claimResizing;
        Claim newClaim = new Claim(oldClaim);
        World world = newClaim.getLesserBoundaryCorner().getWorld();
        newClaim.lesserBoundaryCorner = new Location(world, newx1, newy1, newz1);
        newClaim.greaterBoundaryCorner = new Location(world, newx2, newy2, newz2);

        //call event here to check if it has been cancelled
        ClaimResizeEvent event = new ClaimModifiedEvent(oldClaim, newClaim, player); // Swap to ClaimResizeEvent when ClaimModifiedEvent is removed
        Bukkit.getPluginManager().callEvent(event);

        //return here if event is cancelled
        if (event.isCancelled()) return;

        //special rule for making a top-level claim smaller.  to check this, verifying the old claim's corners are inside the new claim's boundaries.
        //rule: in any mode, shrinking a claim removes any surface fluids
        boolean smaller = false;
        if (oldClaim.parent == null) {
            //if the new claim is smaller
            if (!newClaim.contains(oldClaim.getLesserBoundaryCorner(), true, false) || !newClaim.contains(oldClaim.getGreaterBoundaryCorner(), true, false)) {
                smaller = true;
            }
        }

        //ask the datastore to try and resize the claim, this checks for conflicts with other claims
        CreateClaimResult result = TheatriaClaims.instance.dataStore.resizeClaim(
                playerData.claimResizing,
                newClaim.getLesserBoundaryCorner().getBlockX(),
                newClaim.getGreaterBoundaryCorner().getBlockX(),
                newClaim.getLesserBoundaryCorner().getBlockY(),
                newClaim.getGreaterBoundaryCorner().getBlockY(),
                newClaim.getLesserBoundaryCorner().getBlockZ(),
                newClaim.getGreaterBoundaryCorner().getBlockZ(),
                player);

        if (result.succeeded && result.claim != null) {
            //decide how many claim blocks are available for more resizing
            int claimBlocksRemaining = 0;
            if (!playerData.claimResizing.isAdminClaim()) {
                UUID ownerID = playerData.claimResizing.ownerID;
                if (playerData.claimResizing.parent != null) {
                    ownerID = playerData.claimResizing.parent.ownerID;
                }
                if (ownerID == player.getUniqueId()) {
                    claimBlocksRemaining = playerData.getRemainingClaimBlocks();
                }
                else {
                    PlayerData ownerData = this.getPlayerData(ownerID);
                    claimBlocksRemaining = ownerData.getRemainingClaimBlocks();
                    OfflinePlayer owner = TheatriaClaims.instance.getServer().getOfflinePlayer(ownerID);
                    if (!owner.isOnline()) {
                        this.clearCachedPlayerData(ownerID);
                    }
                }
            }

            //inform about success, visualize, communicate remaining blocks available
            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.ClaimResizeSuccess, String.valueOf(claimBlocksRemaining));
            BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CLAIM, configManager);

            //if resizing someone else's claim, make a log entry
            if (!player.getUniqueId().equals(playerData.claimResizing.ownerID) && playerData.claimResizing.parent == null) {
                CustomLogger.log(player.getName() + " resized " + playerData.claimResizing.getOwnerName() + "'s claim at " + GeneralUtils.getfriendlyLocationString(playerData.claimResizing.lesserBoundaryCorner) + ".");
            }


            //if increased to a sufficiently large size and no subdivisions yet, send subdivision instructions
            if (oldClaim.getArea() < 1000 && result.claim.getArea() >= 1000 && result.claim.children.size() == 0 && !player.hasPermission("griefprevention.adminclaims")) {
                Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.BecomeMayor, 200L);
                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SubdivisionVideo2, 201L, DataStore.SUBDIVISION_VIDEO_URL);
            }

            //clean up
            playerData.claimResizing = null;
            playerData.lastShovelLocation = null;
        }
        else {
            if (result.claim != null) {
                //inform player
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ResizeFailOverlap);

                //show the player the conflicting claim
                BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CONFLICT_ZONE, configManager);
            }
            else {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ResizeFailOverlapRegion);
            }
        }
    }

    //educates a player about /adminclaims and /acb, if he can use them 
    public void tryAdvertiseAdminAlternatives(Player player)
    {
        if (player.hasPermission("griefprevention.adminclaims") && player.hasPermission("griefprevention.adjustclaimblocks"))
        {
            Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.AdvertiseACandACB);
        }
        else if (player.hasPermission("griefprevention.adminclaims"))
        {
            Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.AdvertiseAdminClaims);
        }
        else if (player.hasPermission("griefprevention.adjustclaimblocks"))
        {
            Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.AdvertiseACB);
        }
    }

    //used in updating the data schema from 0 to 1.
    //converts player names in a list to uuids
    protected List<String> convertNameListToUUIDList(List<String> names) {
        //doesn't apply after schema has been updated to version 1
        if (this.getSchemaVersion() >= 1) return names;

        //list to build results
        List<String> resultNames = new ArrayList<>();

        for (String name : names) {
            //skip non-player-names (groups and "public"), leave them as-is
            if (name.startsWith("[") || name.equals("public")) {
                resultNames.add(name);
                continue;
            }

            //otherwise try to convert to a UUID
            UUID playerID = null;
            try {
                playerID = UUIDFetcher.getUUIDOf(name);
            }
            catch (Exception ex) { }

            //if successful, replace player name with corresponding UUID
            if (playerID != null) {
                resultNames.add(playerID.toString());
            }
        }

        return resultNames;
    }

    public abstract void close();

    private class SavePlayerDataThread extends Thread {
        private final UUID playerID;
        private final PlayerData playerData;

        SavePlayerDataThread(UUID playerID, PlayerData playerData) {
            this.playerID = playerID;
            this.playerData = playerData;
        }

        public void run() {
            //ensure player data is already read from file before trying to save
            playerData.getAccruedClaimBlocks();
            playerData.getClaims();
            asyncSavePlayerData(this.playerID, this.playerData);
        }
    }

    //gets all the claims "near" a location
    public Set<Claim> getNearbyClaims(Location location) {
        Set<Claim> claims = new HashSet<>();

        Chunk lesserChunk = location.getWorld().getChunkAt(location.subtract(150, 0, 150));
        Chunk greaterChunk = location.getWorld().getChunkAt(location.add(300, 0, 300));

        for (int chunk_x = lesserChunk.getX(); chunk_x <= greaterChunk.getX(); chunk_x++) {
            for (int chunk_z = lesserChunk.getZ(); chunk_z <= greaterChunk.getZ(); chunk_z++) {
                Chunk chunk = location.getWorld().getChunkAt(chunk_x, chunk_z);
                Long chunkID = getChunkHash(chunk.getBlock(0, 0, 0).getLocation());
                ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkID);
                if (claimsInChunk != null)
                {
                    for (Claim claim : claimsInChunk)
                    {
                        if (claim.inDataStore && claim.getLesserBoundaryCorner().getWorld().equals(location.getWorld()))
                        {
                            claims.add(claim);
                        }
                    }
                }
            }
        }

        return claims;
    }

    //deletes all the land claims in a specified world
    public void deleteClaimsInWorld(World world, boolean deleteAdminClaims) {
        for (int i = 0; i < claims.size(); i++) {
            Claim claim = claims.get(i);
            if (claim.getLesserBoundaryCorner().getWorld().equals(world)) {
                if (!deleteAdminClaims && claim.isAdminClaim()) continue;
                this.deleteClaim(claim, false, false);
                i--;
            }
        }
    }
}
