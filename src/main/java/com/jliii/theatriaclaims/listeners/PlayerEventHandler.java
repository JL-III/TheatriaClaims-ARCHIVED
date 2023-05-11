/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

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

package com.jliii.theatriaclaims.listeners;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.chat.SpamDetector;
import com.jliii.theatriaclaims.chat.WordFinder;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.claim.CreateClaimResult;
import com.jliii.theatriaclaims.enums.*;
import com.jliii.theatriaclaims.events.ClaimInspectionEvent;
import com.jliii.theatriaclaims.managers.ConfigManager;
import com.jliii.theatriaclaims.tasks.AutoExtendClaimTask;
import com.jliii.theatriaclaims.tasks.BroadcastMessageTask;
import com.jliii.theatriaclaims.tasks.EquipShovelProcessingTask;
import com.jliii.theatriaclaims.tasks.WelcomeTask;
import com.jliii.theatriaclaims.util.*;
import com.jliii.theatriaclaims.visualization.BoundaryVisualization;
import com.jliii.theatriaclaims.visualization.VisualizationType;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.Command;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class PlayerEventHandler implements Listener {

    private final TheatriaClaims instance;
    private final ConfigManager configManager;
    private final CustomLogger customLogger;
    private final DataStore dataStore;

    //number of milliseconds in a day
    private final long MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;

    //timestamps of login and logout notifications in the last minute
    private final ArrayList<Long> recentLoginLogoutNotifications = new ArrayList<>();

    //regex pattern for the "how do i claim land?" scanner
    private Pattern howToClaimPattern = null;

    //matcher for banned words
    private final WordFinder bannedWordFinder;

    //spam tracker
    SpamDetector spamDetector = new SpamDetector();

    public PlayerEventHandler(TheatriaClaims plugin, DataStore dataStore, ConfigManager configManager, CustomLogger customLogger) {
        this.instance = plugin;
        this.dataStore = dataStore;
        this.configManager = configManager;
        this.customLogger = customLogger;
        bannedWordFinder = new WordFinder(instance.dataStore.loadBannedWords());
    }

    public void resetPattern()
    {
        this.howToClaimPattern = null;
    }

    //when a player uses a slash command...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    synchronized void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String[] args = message.split(" ");

        String command = args[0].toLowerCase();

        Player player = event.getPlayer();
        PlayerData playerData = null;


        //if the slash command used is in the list of monitored commands, treat it like a chat message (see above)
        boolean isMonitoredCommand;

        //if requires access trust, check for permission
        isMonitoredCommand = false;
        String lowerCaseMessage = message.toLowerCase();
        for (String monitoredCommand : configManager.getSystemConfig().commandsRequiringAccessTrust) {
            if (lowerCaseMessage.startsWith(monitoredCommand)) {
                isMonitoredCommand = true;
                break;
            }
        }

        if (isMonitoredCommand) {
            Claim claim = dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
            if (claim != null)
            {
                playerData.lastClaim = claim;
                Supplier<String> reason = claim.checkPermission(player, ClaimPermission.Access, event);
                if (reason != null)
                {
                    Messages.sendMessage(player, TextMode.Err.getColor(), reason.get());
                    event.setCancelled(true);
                }
            }
        }
    }

    private final ConcurrentHashMap<String, CommandCategory> commandCategoryMap = new ConcurrentHashMap<>();

    static int longestNameLength = 10;

    private final ConcurrentHashMap<UUID, Date> lastLoginThisServerSessionMap = new ConcurrentHashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();

        //note login time
        Date nowDate = new Date();
        long now = nowDate.getTime();
        PlayerData playerData = this.dataStore.getPlayerData(playerID);
        playerData.lastSpawn = now;
        this.lastLoginThisServerSessionMap.put(playerID, nowDate);

        //if newish, prevent chat until he's moved a bit to prove he's not a bot
        if (isNewToServer(player) && !player.hasPermission("griefprevention.premovementchat"))
        {
            playerData.noChatLocation = player.getLocation();
        }

        //in case player has changed his name, on successful login, update UUID > Name mapping
        PlayerName.cacheUUIDNamePair(player.getUniqueId(), player.getName());

    }

    //when a player teleports
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        //FEATURE: prevent players from using ender pearls to gain access to secured claims
        TeleportCause cause = event.getCause();
        if (cause == TeleportCause.CHORUS_FRUIT || (cause == TeleportCause.ENDER_PEARL && configManager.getClaimsConfig().enderPearlsRequireAccessTrust)) {
            Claim toClaim = this.dataStore.getClaimAt(event.getTo(), false, playerData.lastClaim);
            if (toClaim != null) {
                playerData.lastClaim = toClaim;
                Supplier<String> noAccessReason = toClaim.checkPermission(player, ClaimPermission.Access, event);
                if (noAccessReason != null) {
                    Messages.sendMessage(player, TextMode.Err.getColor(), noAccessReason.get());
                    event.setCancelled(true);
                    if (cause == TeleportCause.ENDER_PEARL)
                        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
                }
            }
        }

    }

    //when a player triggers a raid (in a claim)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTriggerRaid(RaidTriggerEvent event) {
        if (!configManager.getClaimsConfig().raidTriggersRequireBuildTrust)
            return;

        Player player = event.getPlayer();
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        Claim claim = this.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
        if (claim == null)
            return;

        playerData.lastClaim = claim;
        if (claim.checkPermission(player, ClaimPermission.Build, event) == null)
            return;

        event.setCancelled(true);
    }

    //when a player interacts with a specific part of entity...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        //treat it the same as interacting with an entity in general
        if (event.getRightClicked().getType() == EntityType.ARMOR_STAND) {
            this.onPlayerInteractEntity((PlayerInteractEntityEvent) event);
        }
    }

    //when a player interacts with an entity...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (!configManager.getSystemConfig().claimsEnabledForWorld(entity.getWorld())) return;

        //allow horse protection to be overridden to allow management from other plugins
        if (!configManager.getClaimsConfig().protectHorses && entity instanceof AbstractHorse) return;
        if (!configManager.getClaimsConfig().protectDonkeys && entity instanceof Donkey) return;
        if (!configManager.getClaimsConfig().protectDonkeys && entity instanceof Mule) return;
        if (!configManager.getClaimsConfig().protectLlamas && entity instanceof Llama) return;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        //if entity is tameable and has an owner, apply special rules
        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            if (tameable.isTamed()) {
                if (tameable.getOwner() != null) {
                    UUID ownerID = tameable.getOwner().getUniqueId();

                    //if the player interacting is the owner or an admin in ignore claims mode, always allow
                    if (player.getUniqueId().equals(ownerID) || playerData.ignoreClaims) {
                        //if giving away pet, do that instead
                        if (playerData.petGiveawayRecipient != null) {
                            tameable.setOwner(playerData.petGiveawayRecipient);
                            playerData.petGiveawayRecipient = null;
                            Messages.sendMessage(player, TextMode.Success.getColor(), MessageType.PetGiveawayConfirmation);
                            event.setCancelled(true);
                        }

                        return;
                    }
                    OfflinePlayer owner = instance.getServer().getOfflinePlayer(ownerID);
                    String ownerName = owner.getName();
                    if (ownerName == null) ownerName = "someone";
                    String message = instance.dataStore.getMessage(MessageType.NotYourPet, ownerName);
                    if (player.hasPermission("griefprevention.ignoreclaims"))
                        message += "  " + instance.dataStore.getMessage(MessageType.IgnoreClaimsAdvertisement);
                    Messages.sendMessage(player, TextMode.Err.getColor(), message);
                    event.setCancelled(true);
                    return;
                }
            }
            //world repair code for a now-fixed GP bug //TODO: necessary anymore?
            else {
                //ensure this entity can be tamed by players
                tameable.setOwner(null);
                if (tameable instanceof InventoryHolder)
                {
                    InventoryHolder holder = (InventoryHolder) tameable;
                    holder.getInventory().clear();
                }
            }
        }

        //don't allow interaction with item frames or armor stands in claimed areas without build permission
        if (entity.getType() == EntityType.ARMOR_STAND || entity instanceof Hanging) {
            String noBuildReason = allowBuild(player, entity.getLocation(), Material.ITEM_FRAME);
            if (noBuildReason != null) {
                Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason);
                event.setCancelled(true);
                return;
            }
        }

        //always allow interactions when player is in ignore claims mode
        if (playerData.ignoreClaims) return;

        //if the entity is a vehicle and we're preventing theft in claims
        if (configManager.getClaimsConfig().preventTheft && entity instanceof Vehicle) {
            //if the entity is in a claim
            Claim claim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
            if (claim != null) {
                //for storage entities, apply container rules (this is a potential theft)
                if (entity instanceof InventoryHolder) {
                    Supplier<String> noContainersReason = claim.checkPermission(player, ClaimPermission.Inventory, event);
                    if (noContainersReason != null) {
                        Messages.sendMessage(player, TextMode.Err.getColor(), noContainersReason.get());
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        //if the entity is an animal, apply container rules
        if ((configManager.getClaimsConfig().preventTheft && (entity instanceof Animals || entity instanceof Fish)) || (entity.getType() == EntityType.VILLAGER && configManager.getClaimsConfig().villagerTradingRequiresTrust))
        {
            //if the entity is in a claim
            Claim claim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
            if (claim != null)
            {
                Supplier<String> override = () ->
                {
                    String message = instance.dataStore.getMessage(MessageType.NoDamageClaimedEntity, claim.getOwnerName());
                    if (player.hasPermission("griefprevention.ignoreclaims"))
                        message += "  " + instance.dataStore.getMessage(MessageType.IgnoreClaimsAdvertisement);

                    return message;
                };
                final Supplier<String> noContainersReason = claim.checkPermission(player, ClaimPermission.Inventory, event, override);
                if (noContainersReason != null)
                {
                    Messages.sendMessage(player, TextMode.Err.getColor(), noContainersReason.get());
                    event.setCancelled(true);
                    return;
                }
            }
        }

        ItemStack itemInHand = GeneralUtils.getItemInHand(player, event.getHand());

        //if preventing theft, prevent leashing claimed creatures
        if (configManager.getClaimsConfig().preventTheft && entity instanceof Creature && itemInHand.getType() == Material.LEAD) {
            Claim claim = this.dataStore.getClaimAt(entity.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                Supplier<String> failureReason = claim.checkPermission(player, ClaimPermission.Inventory, event);
                if (failureReason != null) {
                    event.setCancelled(true);
                    Messages.sendMessage(player, TextMode.Err.getColor(), failureReason.get());
                    return;
                }
            }
        }

        // Name tags may only be used on entities that the player is allowed to kill.
        if (itemInHand.getType() == Material.NAME_TAG) {
            EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(player, entity, EntityDamageEvent.DamageCause.CUSTOM, 0);
            instance.entityEventHandler.onEntityDamage(damageEvent);
            if (damageEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    //when a player throws an egg
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerThrowEgg(PlayerEggThrowEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(event.getEgg().getLocation(), false, playerData.lastClaim);

        //allow throw egg if player is in ignore claims mode
        if (playerData.ignoreClaims || claim == null) return;

        Supplier<String> failureReason = claim.checkPermission(player, ClaimPermission.Inventory, event);
        if (failureReason != null) {
            String reason = failureReason.get();
            if (player.hasPermission("griefprevention.ignoreclaims")) {
                reason += "  " + instance.dataStore.getMessage(MessageType.IgnoreClaimsAdvertisement);
            }

            Messages.sendMessage(player, TextMode.Err.getColor(), reason);

            //cancel the event by preventing hatching
            event.setHatching(false);

            //only give the egg back if player is in survival or adventure
            if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                player.getInventory().addItem(event.getEgg().getItem());
            }
        }
    }

    //when a player reels in his fishing rod
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerFish(PlayerFishEvent event)
    {
        Entity entity = event.getCaught();
        if (entity == null) return;  //if nothing pulled, uninteresting event

        //if should be protected from pulling in land claims without permission
        if (entity.getType() == EntityType.ARMOR_STAND || entity instanceof Animals)
        {
            Player player = event.getPlayer();
            PlayerData playerData = instance.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = instance.dataStore.getClaimAt(entity.getLocation(), false, playerData.lastClaim);
            if (claim != null)
            {
                //if no permission, cancel
                Supplier<String> errorMessage = claim.checkPermission(player, ClaimPermission.Inventory, event);
                if (errorMessage != null)
                {
                    event.setCancelled(true);
                    Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.NoDamageClaimedEntity, claim.getOwnerName());
                    return;
                }
            }
        }
    }

    //when a player switches in-hand items
    @EventHandler(ignoreCancelled = true)
    public void onItemHeldChange(PlayerItemHeldEvent event)
    {
        Player player = event.getPlayer();

        //if he's switching to the golden shovel
        int newSlot = event.getNewSlot();
        ItemStack newItemStack = player.getInventory().getItem(newSlot);
        if (newItemStack != null && newItemStack.getType() == configManager.config_claims_modificationTool)
        {
            //give the player his available claim blocks count and claiming instructions, but only if he keeps the shovel equipped for a minimum time, to avoid mouse wheel spam
            if (instance.claimsEnabledForWorld(player.getWorld()))
            {
                EquipShovelProcessingTask task = new EquipShovelProcessingTask(player);
                instance.getServer().getScheduler().scheduleSyncDelayedTask(instance, task, 15L);  //15L is approx. 3/4 of a second
            }
        }
    }

    //block use of buckets within other players' claims
    private final Set<Material> commonAdjacentBlocks_water = EnumSet.of(Material.WATER, Material.FARMLAND, Material.DIRT, Material.STONE);
    private final Set<Material> commonAdjacentBlocks_lava = EnumSet.of(Material.LAVA, Material.DIRT, Material.STONE);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent bucketEvent) {
        if (!configManager.getSystemConfig().claimsEnabledForWorld(bucketEvent.getBlockClicked().getWorld())) return;

        Player player = bucketEvent.getPlayer();
        Block block = bucketEvent.getBlockClicked().getRelative(bucketEvent.getBlockFace());
        int minLavaDistance = 10;

        // Fixes #1155:
        // Prevents waterlogging blocks placed on a claim's edge.
        // Waterlogging a block affects the clicked block, and NOT the adjacent location relative to it.
        if (bucketEvent.getBucket() == Material.WATER_BUCKET && bucketEvent.getBlockClicked().getBlockData() instanceof Waterlogged) {
            block = bucketEvent.getBlockClicked();
        }

        //make sure the player is allowed to build at the location
        String noBuildReason = allowBuild(player, block.getLocation(), Material.WATER);
        if (noBuildReason != null) {
            Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason);
            bucketEvent.setCancelled(true);
            return;
        }

        //if the bucket is being used in a claim, allow for dumping lava closer to other players
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, playerData.lastClaim);
        if (claim != null) {
            minLavaDistance = 3;
        }
    }

    //see above
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBucketFill(PlayerBucketFillEvent bucketEvent) {
        Player player = bucketEvent.getPlayer();
        Block block = bucketEvent.getBlockClicked();

        if (!configManager.getSystemConfig().claimsEnabledForWorld(block.getWorld())) return;

        //make sure the player is allowed to build at the location
        String noBuildReason = allowBuild(player, block.getLocation(), Material.AIR);
        if (noBuildReason != null)
        {
            //exemption for cow milking (permissions will be handled by player interact with entity event instead)
            Material blockType = block.getType();
            if (blockType == Material.AIR)
                return;
            if (blockType.isSolid())
            {
                BlockData blockData = block.getBlockData();
                if (!(blockData instanceof Waterlogged) || !((Waterlogged) blockData).isWaterlogged())
                    return;
            }

            Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason);
            bucketEvent.setCancelled(true);
            return;
        }
    }

    //when a player interacts with the world
    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerInteract(PlayerInteractEvent event) {
        //not interested in left-click-on-air actions
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock(); //null returned here means interacting with air

        Material clickedBlockType = null;
        if (clickedBlock != null) {
            clickedBlockType = clickedBlock.getType();
        }
        else {
            clickedBlockType = Material.AIR;
        }

        PlayerData playerData = null;

        //Turtle eggs
        if (action == Action.PHYSICAL) {
            if (clickedBlockType != Material.TURTLE_EGG)
                return;
            playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                Supplier<String> noAccessReason = claim.checkPermission(player, ClaimPermission.Build, event);
                if (noAccessReason != null) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }

        //don't care about left-clicking on most blocks, this is probably a break action
        if (action == Action.LEFT_CLICK_BLOCK && clickedBlock != null) {
            if (clickedBlock.getY() < clickedBlock.getWorld().getMaxHeight() - 1 || event.getBlockFace() != BlockFace.UP) {
                Block adjacentBlock = clickedBlock.getRelative(event.getBlockFace());
                byte lightLevel = adjacentBlock.getLightFromBlocks();
                if (lightLevel == 15 && adjacentBlock.getType() == Material.FIRE) {
                    if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
                    if (claim != null) {
                        playerData.lastClaim = claim;

                        Supplier<String> noBuildReason = claim.checkPermission(player, ClaimPermission.Build, event);
                        if (noBuildReason != null) {
                            event.setCancelled(true);
                            Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason.get());
                            player.sendBlockChange(adjacentBlock.getLocation(), adjacentBlock.getType(), adjacentBlock.getData());
                            return;
                        }
                    }
                }
            }

            //exception for blocks on a specific watch list
            if (!this.onLeftClickWatchList(clickedBlockType))
            {
                return;
            }
        }

        //apply rules for containers and crafting blocks
        if (clickedBlock != null && configManager.getClaimsConfig().preventTheft && (
                event.getAction() == Action.RIGHT_CLICK_BLOCK && (
                        (this.isInventoryHolder(clickedBlock) && clickedBlock.getType() != Material.LECTERN) ||
                                clickedBlockType == Material.ANVIL ||
                                clickedBlockType == Material.BEACON ||
                                clickedBlockType == Material.BEE_NEST ||
                                clickedBlockType == Material.BEEHIVE ||
                                clickedBlockType == Material.BELL ||
                                clickedBlockType == Material.CAKE ||
                                clickedBlockType == Material.CARTOGRAPHY_TABLE ||
                                clickedBlockType == Material.CAULDRON ||
                                clickedBlockType == Material.WATER_CAULDRON ||
                                clickedBlockType == Material.LAVA_CAULDRON ||
                                clickedBlockType == Material.CAVE_VINES ||
                                clickedBlockType == Material.CAVE_VINES_PLANT ||
                                clickedBlockType == Material.CHIPPED_ANVIL ||
                                clickedBlockType == Material.DAMAGED_ANVIL ||
                                clickedBlockType == Material.GRINDSTONE ||
                                clickedBlockType == Material.JUKEBOX ||
                                clickedBlockType == Material.LOOM ||
                                clickedBlockType == Material.PUMPKIN ||
                                clickedBlockType == Material.RESPAWN_ANCHOR ||
                                clickedBlockType == Material.ROOTED_DIRT ||
                                clickedBlockType == Material.STONECUTTER ||
                                clickedBlockType == Material.SWEET_BERRY_BUSH
                        )))
        {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());

            //otherwise check permissions for the claim the player is in
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;
                Supplier<String> noContainersReason = claim.checkPermission(player, ClaimPermission.Inventory, event);
                if (noContainersReason != null) {
                    event.setCancelled(true);
                    Messages.sendMessage(player, TextMode.Err.getColor(), noContainersReason.get());
                    return;
                }
            }
        }

        //otherwise apply rules for doors and beds, if configured that way
        else if (clickedBlock != null &&

                (configManager.getClaimsConfig().lockWoodenDoors && Tag.WOODEN_DOORS.isTagged(clickedBlockType) ||

                        configManager.getClaimsConfig().preventButtonsSwitches && Tag.BEDS.isTagged(clickedBlockType) ||

                        configManager.getClaimsConfig().lockTrapDoors && Tag.WOODEN_TRAPDOORS.isTagged(clickedBlockType) ||

                        configManager.getClaimsConfig().lecternReadingRequiresAccessTrust && clickedBlockType == Material.LECTERN ||

                        configManager.getClaimsConfig().lockFenceGates && Tag.FENCE_GATES.isTagged(clickedBlockType)))
        {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                Supplier<String> noAccessReason = claim.checkPermission(player, ClaimPermission.Access, event);
                if (noAccessReason != null) {
                    event.setCancelled(true);
                    Messages.sendMessage(player, TextMode.Err.getColor(), noAccessReason.get());
                    return;
                }
            }
        }

        //otherwise apply rules for buttons and switches
        else if (clickedBlock != null && configManager.getClaimsConfig().preventButtonsSwitches && (Tag.BUTTONS.isTagged(clickedBlockType) || clickedBlockType == Material.LEVER)) {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                Supplier<String> noAccessReason = claim.checkPermission(player, ClaimPermission.Access, event);
                if (noAccessReason != null) {
                    event.setCancelled(true);
                    Messages.sendMessage(player, TextMode.Err.getColor(), noAccessReason.get());
                    return;
                }
            }
        }

        //otherwise apply rule for cake
        else if (clickedBlock != null && configManager.getClaimsConfig().preventTheft && (clickedBlockType == Material.CAKE || Tag.CANDLE_CAKES.isTagged(clickedBlockType))) {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;
                Supplier<String> noContainerReason = claim.checkPermission(player, ClaimPermission.Access, event);
                if (noContainerReason != null) {
                    event.setCancelled(true);
                    Messages.sendMessage(player, TextMode.Err.getColor(), noContainerReason.get());
                    return;
                }
            }
        }

        //apply rule for redstone and various decor blocks that require full trust
        else if (clickedBlock != null &&
                (
                        clickedBlockType == Material.NOTE_BLOCK ||
                                clickedBlockType == Material.REPEATER ||
                                clickedBlockType == Material.DRAGON_EGG ||
                                clickedBlockType == Material.DAYLIGHT_DETECTOR ||
                                clickedBlockType == Material.COMPARATOR ||
                                clickedBlockType == Material.REDSTONE_WIRE ||
                                Tag.FLOWER_POTS.isTagged(clickedBlockType) ||
                                Tag.CANDLES.isTagged(clickedBlockType)
                )) {
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                Supplier<String> noBuildReason = claim.checkPermission(player, ClaimPermission.Build, event);
                if (noBuildReason != null) {
                    event.setCancelled(true);
                    Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason.get());
                    return;
                }
            }
        }

        //otherwise handle right click (shovel, string, bonemeal) //RoboMWM: flint and steel
        else {
            //ignore all actions except right-click on a block or in the air
            if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

            //what's the player holding?
            EquipmentSlot hand = event.getHand();
            ItemStack itemInHand = GeneralUtils.getItemInHand(player, hand);
            Material materialInHand = itemInHand.getType();

            Set<Material> spawn_eggs = new HashSet<>();
            Set<Material> dyes = new HashSet<>();

            for (Material material : Material.values()) {
                if (material.isLegacy()) continue;
                if (material.name().endsWith("_SPAWN_EGG"))
                    spawn_eggs.add(material);
                else if (material.name().endsWith("_DYE"))
                    dyes.add(material);
            }

            //if it's bonemeal, armor stand, spawn egg, etc - check for build permission //RoboMWM: also check flint and steel to stop TNT ignition
            //add glowing ink sac and ink sac, due to their usage on signs
            if (clickedBlock != null && (materialInHand == Material.BONE_MEAL
                    || materialInHand == Material.ARMOR_STAND
                    || (spawn_eggs.contains(materialInHand) && configManager.getClaimsConfig().preventGlobalMonsterEggs)
                    || materialInHand == Material.END_CRYSTAL
                    || materialInHand == Material.FLINT_AND_STEEL
                    || materialInHand == Material.INK_SAC
                    || materialInHand == Material.GLOW_INK_SAC
                    || dyes.contains(materialInHand))) {
                String noBuildReason = allowBuild(player, clickedBlock
                                        .getLocation(),
                                clickedBlockType);
                if (noBuildReason != null) {
                    Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason);
                    event.setCancelled(true);
                }
                return;
            }
            else if (clickedBlock != null && Tag.ITEMS_BOATS.isTagged(materialInHand)) {
                if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
                if (claim != null) {
                    Supplier<String> reason = claim.checkPermission(player, ClaimPermission.Inventory, event);
                    if (reason != null) {
                        Messages.sendMessage(player, TextMode.Err.getColor(), reason.get());
                        event.setCancelled(true);
                    }
                }
                return;
            }

            //survival world minecart placement requires container trust, which is the permission required to remove the minecart later
            else if (clickedBlock != null &&
                    (materialInHand == Material.MINECART ||
                            materialInHand == Material.FURNACE_MINECART ||
                            materialInHand == Material.CHEST_MINECART ||
                            materialInHand == Material.TNT_MINECART ||
                            materialInHand == Material.HOPPER_MINECART)) {
                if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
                if (claim != null) {
                    Supplier<String> reason = claim.checkPermission(player, ClaimPermission.Inventory, event);
                    if (reason != null) {
                        Messages.sendMessage(player, TextMode.Err.getColor(), reason.get());
                        event.setCancelled(true);
                    }
                }
                return;
            }

            //if he's investigating a claim
            else if (materialInHand == configManager.getSystemConfig().investigationTool && hand == EquipmentSlot.HAND) {
                //if claims are disabled in this world, do nothing
                if (!configManager.getSystemConfig().claimsEnabledForWorld(player.getWorld())) return;

                //if holding shift (sneaking), show all claims in area
                if (player.isSneaking() && player.hasPermission("griefprevention.visualizenearbyclaims"))
                {
                    //find nearby claims
                    Set<Claim> claims = this.dataStore.getNearbyClaims(player.getLocation());

                    // alert plugins of a claim inspection, return if cancelled
                    ClaimInspectionEvent inspectionEvent = new ClaimInspectionEvent(player, null, claims, true);
                    Bukkit.getPluginManager().callEvent(inspectionEvent);
                    if (inspectionEvent.isCancelled()) return;

                    //visualize boundaries
                    BoundaryVisualization.visualizeNearbyClaims(player, inspectionEvent.getClaims(), player.getEyeLocation().getBlockY());
                    Messages.sendMessage(player, TextMode.Info.getColor(), MessageType.ShowNearbyClaims, String.valueOf(claims.size()));

                    return;
                }

                //FEATURE: shovel and stick can be used from a distance away
                if (action == Action.RIGHT_CLICK_AIR)
                {
                    //try to find a far away non-air block along line of sight
                    clickedBlock = getTargetBlock(player, 100);
                    clickedBlockType = clickedBlock.getType();
                }

                //if no block, stop here
                if (clickedBlock == null)
                {
                    return;
                }

                playerData = this.dataStore.getPlayerData(player.getUniqueId());

                //air indicates too far away
                if (clickedBlockType == Material.AIR)
                {
                    Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.TooFarAway);

                    // Remove visualizations
                    playerData.setVisibleBoundaries(null);
                    return;
                }

                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false /*ignore height*/, playerData.lastClaim);

                //no claim case
                if (claim == null)
                {
                    // alert plugins of a claim inspection, return if cancelled
                    ClaimInspectionEvent inspectionEvent = new ClaimInspectionEvent(player, clickedBlock, null);
                    Bukkit.getPluginManager().callEvent(inspectionEvent);
                    if (inspectionEvent.isCancelled()) return;

                    Messages.sendMessage(player, TextMode.Info.getColor(), MessageType.BlockNotClaimed);

                    playerData.setVisibleBoundaries(null);
                }

                //claim case
                else
                {
                    // alert plugins of a claim inspection, return if cancelled
                    ClaimInspectionEvent inspectionEvent = new ClaimInspectionEvent(player, clickedBlock, claim);
                    Bukkit.getPluginManager().callEvent(inspectionEvent);
                    if (inspectionEvent.isCancelled()) return;

                    playerData.lastClaim = claim;
                    Messages.sendMessage(player, TextMode.Info.getColor(), MessageType.BlockClaimed, claim.getOwnerName());

                    //visualize boundary
                    BoundaryVisualization.visualizeClaim(player, claim, VisualizationType.CLAIM);

                    if (player.hasPermission("griefprevention.seeclaimsize"))
                    {
                        Messages.sendMessage(player, TextMode.Info.getColor(), "  " + claim.getWidth() + "x" + claim.getHeight() + "=" + claim.getArea());
                    }

                    //if permission, tell about the player's offline time
                    if (!claim.isAdminClaim() && (player.hasPermission("griefprevention.deleteclaims") || player.hasPermission("griefprevention.seeinactivity")))
                    {
                        if (claim.parent != null)
                        {
                            claim = claim.parent;
                        }
                        Date lastLogin = new Date(Bukkit.getOfflinePlayer(claim.ownerID).getLastPlayed());
                        Date now = new Date();
                        long daysElapsed = (now.getTime() - lastLogin.getTime()) / (1000 * 60 * 60 * 24);

                        Messages.sendMessage(player, TextMode.Info.getColor(), MessageType.PlayerOfflineTime, String.valueOf(daysElapsed));

                        //drop the data we just loaded, if the player isn't online
                        if (instance.getServer().getPlayer(claim.ownerID) == null)
                            this.dataStore.clearCachedPlayerData(claim.ownerID);
                    }
                }

                return;
            }

            //if it's a golden shovel
            else if (materialInHand != configManager.config_claims_modificationTool || hand != EquipmentSlot.HAND) return;

            event.setCancelled(true);  //GriefPrevention exclusively reserves this tool  (e.g. no grass path creation for golden shovel)

            //disable golden shovel while under siege
            if (playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
//            if (playerData.siegeData != null)
//            {
//                GriefPrevention.sendMessage(player, TextMode.Err, MessageType.SiegeNoShovel);
//                event.setCancelled(true);
//                return;
//            }

            //FEATURE: shovel and stick can be used from a distance away
            if (action == Action.RIGHT_CLICK_AIR)
            {
                //try to find a far away non-air block along line of sight
                clickedBlock = getTargetBlock(player, 100);
                clickedBlockType = clickedBlock.getType();
            }

            //if no block, stop here
            if (clickedBlock == null)
            {
                return;
            }

            //can't use the shovel from too far away
            if (clickedBlockType == Material.AIR)
            {
                Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.TooFarAway);
                return;
            }

            //if the player is in restore nature mode, do only that
            UUID playerID = player.getUniqueId();
            playerData = this.dataStore.getPlayerData(player.getUniqueId());
//            if (playerData.shovelMode == ShovelMode.RestoreNature || playerData.shovelMode == ShovelMode.RestoreNatureAggressive)
//            {
//                //if the clicked block is in a claim, visualize that claim and deliver an error message
//                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
//                if (claim != null)
//                {
//                    Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.BlockClaimed, claim.getOwnerName());
//                    BoundaryVisualization.visualizeClaim(player, claim, VisualizationType.CONFLICT_ZONE, clickedBlock);
//                    return;
//                }
//
//                //figure out which chunk to repair
//                Chunk chunk = player.getWorld().getChunkAt(clickedBlock.getLocation());
//                //start the repair process
//
//                //set boundaries for processing
//                int miny = clickedBlock.getY();
//
//                //TODO check on this, we dont restore nature anymore
//                //if not in aggressive mode, extend the selection down to a little below sea level
//                if (!(playerData.shovelMode == ShovelMode.RestoreNatureAggressive))
//                {
//                    if (miny > instance.getSeaLevel(chunk.getWorld()) - 10)
//                    {
//                        miny = instance.getSeaLevel(chunk.getWorld()) - 10;
//                    }
//                }
//
////                instance.restoreChunk(chunk, miny, playerData.shovelMode == ShovelMode.RestoreNatureAggressive, 0, player);
//
//                return;
//            }
//
//            //if in restore nature fill mode
//            if (playerData.shovelMode == ShovelMode.RestoreNatureFill)
//            {
//                ArrayList<Material> allowedFillBlocks = new ArrayList<>();
//                Environment environment = clickedBlock.getWorld().getEnvironment();
//                if (environment == Environment.NETHER)
//                {
//                    allowedFillBlocks.add(Material.NETHERRACK);
//                }
//                else if (environment == Environment.THE_END)
//                {
//                    allowedFillBlocks.add(Material.END_STONE);
//                }
//                else
//                {
//                    allowedFillBlocks.add(Material.GRASS);
//                    allowedFillBlocks.add(Material.DIRT);
//                    allowedFillBlocks.add(Material.STONE);
//                    allowedFillBlocks.add(Material.SAND);
//                    allowedFillBlocks.add(Material.SANDSTONE);
//                    allowedFillBlocks.add(Material.ICE);
//                }
//
//                Block centerBlock = clickedBlock;
//
//                int maxHeight = centerBlock.getY();
//                int minx = centerBlock.getX() - playerData.fillRadius;
//                int maxx = centerBlock.getX() + playerData.fillRadius;
//                int minz = centerBlock.getZ() - playerData.fillRadius;
//                int maxz = centerBlock.getZ() + playerData.fillRadius;
//                int minHeight = maxHeight - 10;
//                minHeight = Math.max(minHeight, clickedBlock.getWorld().getMinHeight());
//
//                Claim cachedClaim = null;
//                for (int x = minx; x <= maxx; x++)
//                {
//                    for (int z = minz; z <= maxz; z++)
//                    {
//                        //circular brush
//                        Location location = new Location(centerBlock.getWorld(), x, centerBlock.getY(), z);
//                        if (location.distance(centerBlock.getLocation()) > playerData.fillRadius) continue;
//
//                        //default fill block is initially the first from the allowed fill blocks list above
//                        Material defaultFiller = allowedFillBlocks.get(0);
//
//                        //prefer to use the block the player clicked on, if it's an acceptable fill block
//                        if (allowedFillBlocks.contains(centerBlock.getType()))
//                        {
//                            defaultFiller = centerBlock.getType();
//                        }
//
//                        //if the player clicks on water, try to sink through the water to find something underneath that's useful for a filler
//                        else if (centerBlock.getType() == Material.WATER)
//                        {
//                            Block block = centerBlock.getWorld().getBlockAt(centerBlock.getLocation());
//                            while (!allowedFillBlocks.contains(block.getType()) && block.getY() > centerBlock.getY() - 10)
//                            {
//                                block = block.getRelative(BlockFace.DOWN);
//                            }
//                            if (allowedFillBlocks.contains(block.getType()))
//                            {
//                                defaultFiller = block.getType();
//                            }
//                        }
//
//                        //fill bottom to top
//                        for (int y = minHeight; y <= maxHeight; y++)
//                        {
//                            Block block = centerBlock.getWorld().getBlockAt(x, y, z);
//
//                            //respect claims
//                            Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, cachedClaim);
//                            if (claim != null)
//                            {
//                                cachedClaim = claim;
//                                break;
//                            }
//
//                            //only replace air, spilling water, snow, long grass
//                            if (block.getType() == Material.AIR || block.getType() == Material.SNOW || (block.getType() == Material.WATER && ((Levelled) block.getBlockData()).getLevel() != 0) || block.getType() == Material.GRASS)
//                            {
//                                //if the top level, always use the default filler picked above
//                                if (y == maxHeight)
//                                {
//                                    block.setType(defaultFiller);
//                                }
//
//                                //otherwise look to neighbors for an appropriate fill block
//                                else
//                                {
//                                    Block eastBlock = block.getRelative(BlockFace.EAST);
//                                    Block westBlock = block.getRelative(BlockFace.WEST);
//                                    Block northBlock = block.getRelative(BlockFace.NORTH);
//                                    Block southBlock = block.getRelative(BlockFace.SOUTH);
//
//                                    //first, check lateral neighbors (ideally, want to keep natural layers)
//                                    if (allowedFillBlocks.contains(eastBlock.getType()))
//                                    {
//                                        block.setType(eastBlock.getType());
//                                    }
//                                    else if (allowedFillBlocks.contains(westBlock.getType()))
//                                    {
//                                        block.setType(westBlock.getType());
//                                    }
//                                    else if (allowedFillBlocks.contains(northBlock.getType()))
//                                    {
//                                        block.setType(northBlock.getType());
//                                    }
//                                    else if (allowedFillBlocks.contains(southBlock.getType()))
//                                    {
//                                        block.setType(southBlock.getType());
//                                    }
//
//                                    //if all else fails, use the default filler selected above
//                                    else
//                                    {
//                                        block.setType(defaultFiller);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//
//                return;
//            }

            //if the player doesn't have claims permission, don't do anything
            if (!player.hasPermission("griefprevention.createclaims"))
            {
                Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.NoCreateClaimPermission);
                return;
            }

            //if he's resizing a claim and that claim hasn't been deleted since he started resizing it
            if (playerData.claimResizing != null && playerData.claimResizing.inDataStore)
            {
                if (clickedBlock.getLocation().equals(playerData.lastShovelLocation)) return;

                //figure out what the coords of his new claim would be
                int newx1, newx2, newz1, newz2, newy1, newy2;
                if (playerData.lastShovelLocation.getBlockX() == playerData.claimResizing.getLesserBoundaryCorner().getBlockX())
                {
                    newx1 = clickedBlock.getX();
                    newx2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockX();
                }
                else
                {
                    newx1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockX();
                    newx2 = clickedBlock.getX();
                }

                if (playerData.lastShovelLocation.getBlockZ() == playerData.claimResizing.getLesserBoundaryCorner().getBlockZ())
                {
                    newz1 = clickedBlock.getZ();
                    newz2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ();
                }
                else
                {
                    newz1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockZ();
                    newz2 = clickedBlock.getZ();
                }

                newy1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockY();
                newy2 = clickedBlock.getY() - configManager.config_claims_claimsExtendIntoGroundDistance;

                this.dataStore.resizeClaimWithChecks(player, playerData, newx1, newx2, newy1, newy2, newz1, newz2);

                return;
            }

            //otherwise, since not currently resizing a claim, must be starting a resize, creating a new claim, or creating a subdivision
            Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), true /*ignore height*/, playerData.lastClaim);

            //if within an existing claim, he's not creating a new one
            if (claim != null) {
                //if the player has permission to edit the claim or subdivision
                Supplier<String> noEditReason = claim.checkPermission(player, ClaimPermission.Edit, event, () -> instance.dataStore.getMessage(MessageType.CreateClaimFailOverlapOtherPlayer, claim.getOwnerName()));
                if (noEditReason == null)
                {
                    //if he clicked on a corner, start resizing it
                    if ((clickedBlock.getX() == claim.getLesserBoundaryCorner().getBlockX() || clickedBlock.getX() == claim.getGreaterBoundaryCorner().getBlockX()) && (clickedBlock.getZ() == claim.getLesserBoundaryCorner().getBlockZ() || clickedBlock.getZ() == claim.getGreaterBoundaryCorner().getBlockZ()))
                    {
                        playerData.claimResizing = claim;
                        playerData.lastShovelLocation = clickedBlock.getLocation();
                        Messages.sendMessage(player, TextMode.Instr.getColor(), MessageType.ResizeStart);
                    }

                    //if he didn't click on a corner and is in subdivision mode, he's creating a new subdivision
                    else if (playerData.shovelMode == ShovelMode.Subdivide)
                    {
                        //if it's the first click, he's trying to start a new subdivision
                        if (playerData.lastShovelLocation == null)
                        {
                            //if the clicked claim was a subdivision, tell him he can't start a new subdivision here
                            if (claim.parent != null)
                            {
                                Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.ResizeFailOverlapSubdivision);
                            }

                            //otherwise start a new subdivision
                            else
                            {
                                Messages.sendMessage(player, TextMode.Instr.getColor(), MessageType.SubdivisionStart);
                                playerData.lastShovelLocation = clickedBlock.getLocation();
                                playerData.claimSubdividing = claim;
                            }
                        }

                        //otherwise, he's trying to finish creating a subdivision by setting the other boundary corner
                        else
                        {
                            //if last shovel location was in a different world, assume the player is starting the create-claim workflow over
                            if (!playerData.lastShovelLocation.getWorld().equals(clickedBlock.getWorld()))
                            {
                                playerData.lastShovelLocation = null;
                                this.onPlayerInteract(event);
                                return;
                            }

                            //try to create a new claim (will return null if this subdivision overlaps another)
                            CreateClaimResult result = dataStore.createClaim(
                                    player.getWorld(),
                                    playerData.lastShovelLocation.getBlockX(), clickedBlock.getX(),
                                    playerData.lastShovelLocation.getBlockY() - configManager.config_claims_claimsExtendIntoGroundDistance, clickedBlock.getY() - configManager.config_claims_claimsExtendIntoGroundDistance,
                                    playerData.lastShovelLocation.getBlockZ(), clickedBlock.getZ(),
                                    null,  //owner is not used for subdivisions
                                    playerData.claimSubdividing,
                                    null, player);

                            //if it didn't succeed, tell the player why
                            if (!result.succeeded || result.claim == null)
                            {
                                if (result.claim != null)
                                {
                                    Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.CreateSubdivisionOverlap);
                                    BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CONFLICT_ZONE, clickedBlock);
                                }
                                else
                                {
                                    Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.CreateClaimFailOverlapRegion);
                                }

                                return;
                            }

                            //otherwise, advise him on the /trust command and show him his new subdivision
                            else
                            {
                                Messages.sendMessage(player, TextMode.Success.getColor(), MessageType.SubdivisionSuccess);
                                BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CLAIM, clickedBlock);
                                playerData.lastShovelLocation = null;
                                playerData.claimSubdividing = null;
                            }
                        }
                    }

                    //otherwise tell him he can't create a claim here, and show him the existing claim
                    //also advise him to consider /abandonclaim or resizing the existing claim
                    else
                    {
                        Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.CreateClaimFailOverlap);
                        BoundaryVisualization.visualizeClaim(player, claim, VisualizationType.CLAIM, clickedBlock);
                    }
                }

                //otherwise tell the player he can't claim here because it's someone else's claim, and show him the claim
                else
                {
                    Messages.sendMessage(player, TextMode.Err.getColor(), noEditReason.get());
                    BoundaryVisualization.visualizeClaim(player, claim, VisualizationType.CONFLICT_ZONE, clickedBlock);
                }

                return;
            }

            //otherwise, the player isn't in an existing claim!

            //if he hasn't already start a claim with a previous shovel action
            Location lastShovelLocation = playerData.lastShovelLocation;
            if (lastShovelLocation == null) {
                //if claims are not enabled in this world and it's not an administrative claim, display an error message and stop
                if (!instance.claimsEnabledForWorld(player.getWorld()))
                {
                    Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.ClaimsDisabledWorld);
                    return;
                }

                //if he's at the claim count per player limit already and doesn't have permission to bypass, display an error message
                if (configManager.config_claims_maxClaimsPerPlayer > 0 &&
                        !player.hasPermission("griefprevention.overrideclaimcountlimit") &&
                        playerData.getClaims().size() >= configManager.config_claims_maxClaimsPerPlayer)
                {
                    Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.ClaimCreationFailedOverClaimCountLimit);
                    return;
                }

                //remember it, and start him on the new claim
                playerData.lastShovelLocation = clickedBlock.getLocation();
                Messages.sendMessage(player, TextMode.Instr.getColor(), MessageType.ClaimStart);

                //show him where he's working
                BoundaryVisualization.visualizeArea(player, new BoundingBox(clickedBlock), VisualizationType.INITIALIZE_ZONE);
            }

            //otherwise, he's trying to finish creating a claim by setting the other boundary corner
            else {
                //if last shovel location was in a different world, assume the player is starting the create-claim workflow over
                if (!lastShovelLocation.getWorld().equals(clickedBlock.getWorld()))
                {
                    playerData.lastShovelLocation = null;
                    this.onPlayerInteract(event);
                    return;
                }

                //apply pvp rule
                if (playerData.inPvpCombat())
                {
                    Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.NoClaimDuringPvP);
                    return;
                }

                //apply minimum claim dimensions rule
                int newClaimWidth = Math.abs(playerData.lastShovelLocation.getBlockX() - clickedBlock.getX()) + 1;
                int newClaimHeight = Math.abs(playerData.lastShovelLocation.getBlockZ() - clickedBlock.getZ()) + 1;

                if (playerData.shovelMode != ShovelMode.Admin)
                {
                    if (newClaimWidth < configManager.config_claims_minWidth || newClaimHeight < configManager.config_claims_minWidth)
                    {
                        //this IF block is a workaround for craftbukkit bug which fires two events for one interaction
                        if (newClaimWidth != 1 && newClaimHeight != 1)
                        {
                            Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.NewClaimTooNarrow, String.valueOf(configManager.config_claims_minWidth));
                        }
                        return;
                    }

                    int newArea = newClaimWidth * newClaimHeight;
                    if (newArea < configManager.config_claims_minArea)
                    {
                        if (newArea != 1)
                        {
                            Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.ResizeClaimInsufficientArea, String.valueOf(configManager.config_claims_minArea));
                        }

                        return;
                    }
                }

                //if not an administrative claim, verify the player has enough claim blocks for this new claim
                if (playerData.shovelMode != ShovelMode.Admin)
                {
                    int newClaimArea = newClaimWidth * newClaimHeight;
                    int remainingBlocks = playerData.getRemainingClaimBlocks();
                    if (newClaimArea > remainingBlocks)
                    {
                        Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.CreateClaimInsufficientBlocks, String.valueOf(newClaimArea - remainingBlocks));
                        instance.dataStore.tryAdvertiseAdminAlternatives(player);
                        return;
                    }
                }
                else
                {
                    playerID = null;
                }

                //try to create a new claim
                CreateClaimResult result = this.dataStore.createClaim(
                        player.getWorld(),
                        lastShovelLocation.getBlockX(), clickedBlock.getX(),
                        lastShovelLocation.getBlockY() - configManager.config_claims_claimsExtendIntoGroundDistance, clickedBlock.getY() - configManager.config_claims_claimsExtendIntoGroundDistance,
                        lastShovelLocation.getBlockZ(), clickedBlock.getZ(),
                        playerID,
                        null, null,
                        player);

                //if it didn't succeed, tell the player why
                if (!result.succeeded || result.claim == null)
                {
                    if (result.claim != null)
                    {
                        Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.CreateClaimFailOverlapShort);
                        BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CONFLICT_ZONE, clickedBlock);
                    }
                    else
                    {
                        Messages.sendMessage(player, TextMode.Err.getColor(), MessageType.CreateClaimFailOverlapRegion);
                    }

                    return;
                }

                //otherwise, advise him on the /trust command and show him his new claim
                else
                {
                    Messages.sendMessage(player, TextMode.Success.getColor(), MessageType.CreateClaimSuccess);
                    BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CLAIM, clickedBlock);
                    playerData.lastShovelLocation = null;

                    //if it's a big claim, tell the player about subdivisions
                    if (!player.hasPermission("griefprevention.adminclaims") && result.claim.getArea() >= 1000)
                    {
                        Messages.sendMessage(player, TextMode.Info.getColor(), MessageType.BecomeMayor, 200L);
                        Messages.sendMessage(player, TextMode.Instr.getColor(), MessageType.SubdivisionVideo2, 201L, DataStore.SUBDIVISION_VIDEO_URL);
                    }

                    AutoExtendClaimTask.scheduleAsync(result.claim);
                }
            }
        }
    }

    // Stops an untrusted player from removing a book from a lectern
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    void onTakeBook(PlayerTakeLecternBookEvent event)
    {
        Player player = event.getPlayer();
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(event.getLectern().getLocation(), false, playerData.lastClaim);
        if (claim != null)
        {
            playerData.lastClaim = claim;
            Supplier<String> noContainerReason = claim.checkPermission(player, ClaimPermission.Inventory, event);
            if (noContainerReason != null)
            {
                event.setCancelled(true);
                player.closeInventory();
                Messages.sendMessage(player, TextMode.Err.getColor(), noContainerReason.get());
            }
        }
    }

    //determines whether a block type is an inventory holder.  uses a caching strategy to save cpu time
    private final ConcurrentHashMap<Material, Boolean> inventoryHolderCache = new ConcurrentHashMap<>();

    private boolean isInventoryHolder(Block clickedBlock)
    {

        Material cacheKey = clickedBlock.getType();
        Boolean cachedValue = this.inventoryHolderCache.get(cacheKey);
        if (cachedValue != null)
        {
            return cachedValue.booleanValue();

        }
        else
        {
            boolean isHolder = clickedBlock.getState() instanceof InventoryHolder;
            this.inventoryHolderCache.put(cacheKey, isHolder);
            return isHolder;
        }
    }

    private boolean onLeftClickWatchList(Material material)
    {
        switch (material)
        {
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case BIRCH_BUTTON:
            case JUNGLE_BUTTON:
            case ACACIA_BUTTON:
            case DARK_OAK_BUTTON:
            case STONE_BUTTON:
            case LEVER:
            case REPEATER:
            case CAKE:
            case DRAGON_EGG:
                return true;
            default:
                return false;
        }
    }

    static Block getTargetBlock(Player player, int maxDistance) throws IllegalStateException
    {
        Location eye = player.getEyeLocation();
        Material eyeMaterial = eye.getBlock().getType();
        boolean passThroughWater = (eyeMaterial == Material.WATER);
        BlockIterator iterator = new BlockIterator(player.getLocation(), player.getEyeHeight(), maxDistance);
        Block result = player.getLocation().getBlock().getRelative(BlockFace.UP);
        while (iterator.hasNext())
        {
            result = iterator.next();
            Material type = result.getType();
            if (type != Material.AIR &&
                    (!passThroughWater || type != Material.WATER) &&
                    type != Material.GRASS &&
                    type != Material.SNOW) return result;
        }

        return result;
    }

    public boolean isNewToServer(Player player) {
        if (player.getStatistic(Statistic.PICKUP, Material.OAK_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.SPRUCE_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.BIRCH_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.JUNGLE_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.ACACIA_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.DARK_OAK_LOG) > 0) return false;

        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        if (playerData.getClaims().size() > 0) return false;

        return true;
    }

    //ensures a piece of the managed world is loaded into server memory
    //(generates the chunk if necessary)
    private static void GuaranteeChunkLoaded(Location location) {
        Chunk chunk = location.getChunk();
        while (!chunk.isLoaded() || !chunk.load(true)) ;
    }

    public String allowBuild(Player player, Location location) {
        // TODO check all derivatives and rework API
        return this.allowBuild(player, location, location.getBlock().getType());
    }

    public String allowBuild(Player player, Location location, Material material) {
        if (!configManager.getSystemConfig().claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

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
}
