package com.jliii.theatriaclaims.listeners;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.enums.ClaimsMode;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.managers.ConfigManager;
import com.jliii.theatriaclaims.managers.PermissionManager;
import com.jliii.theatriaclaims.util.DataStore;
import com.jliii.theatriaclaims.util.GeneralUtils;
import com.jliii.theatriaclaims.util.Messages;
import com.jliii.theatriaclaims.util.PendingItemProtection;
import com.jliii.theatriaclaims.util.PlayerData;
import com.jliii.theatriaclaims.util.PlayerName;

import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;
import java.util.function.Supplier;

public class EntityEventHandler implements Listener {
    private final DataStore dataStore;
    private final ConfigManager configManager;
    private final TheatriaClaims instance;
    private final NamespacedKey luredByPlayer;

    public EntityEventHandler(DataStore dataStore, TheatriaClaims plugin, ConfigManager configManager) {
        this.dataStore = dataStore;
        this.configManager = configManager;
        instance = plugin;
        luredByPlayer = new NamespacedKey(plugin, "lured_by_player");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityFormBlock(EntityBlockFormEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.PLAYER) {
            Player player = (Player) event.getEntity();
            String noBuildReason = PermissionManager.allowBuild(player, configManager, event.getBlock().getLocation(), event.getNewState().getType());
            if (noBuildReason != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityChangeBLock(EntityChangeBlockEvent event) {
        if (!configManager.getClaimsConfig().endermenMoveBlocks && event.getEntityType() == EntityType.ENDERMAN) {
            event.setCancelled(true);
        }
        else if (!configManager.getWorldConfig().silverfishBreakBlocks && event.getEntityType() == EntityType.SILVERFISH) {
            event.setCancelled(true);
        }
        else if (!configManager.getWorldConfig().rabbitsEatCrops && event.getEntityType() == EntityType.RABBIT) {
            event.setCancelled(true);
        }
        else if (!configManager.getClaimsConfig().ravagersBreakBlocks && event.getEntityType() == EntityType.RAVAGER) {
            event.setCancelled(true);
        }
        // All other handling depends on claims being enabled.
        else if (!configManager.getSystemConfig().claimsEnabledForWorld((event.getBlock().getWorld()))) {
            return;
        }

        // Handle projectiles changing blocks: TNT ignition, tridents knocking down pointed dripstone, etc.
        if (event.getEntity() instanceof Projectile) {
            handleProjectileChangeBlock(event, (Projectile) event.getEntity());
        }

        else if (event.getEntityType() == EntityType.WITHER) {
            Claim claim = dataStore.getClaimAt(event.getBlock().getLocation(), false, null);
            if (!claim.areExplosivesAllowed || !configManager.getClaimsConfig().blockClaimExplosions) {
                event.setCancelled(true);
            }
        }

        //don't allow crops to be trampled, except by a player with build permission
        else if (event.getTo() == Material.DIRT && event.getBlock().getType() == Material.FARMLAND) {
            if (event.getEntityType() != EntityType.PLAYER) {
                event.setCancelled(true);
            }
            else {
                Player player = (Player) event.getEntity();
                Block block = event.getBlock();
                if (PermissionManager.allowBreak(player, configManager, block, block.getLocation()) != null) {
                    event.setCancelled(true);
                }
            }
        }

        // Prevent breaking lily pads via collision with a boat.
        else if (event.getEntity() instanceof Vehicle && !event.getEntity().getPassengers().isEmpty()) {
            Entity driver = event.getEntity().getPassengers().get(0);
            if (driver instanceof Player) {
                Block block = event.getBlock();
                if (PermissionManager.allowBreak((Player) driver, configManager, block, block.getLocation()) != null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleProjectileChangeBlock(EntityChangeBlockEvent event, Projectile projectile) {
        Block block = event.getBlock();
        Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, null);

        ProjectileSource shooter = projectile.getShooter();

        if (shooter instanceof Player) {
            Supplier<String> denial = claim.checkPermission((Player) shooter, ClaimPermission.Build, event);

            // If the player cannot place the material being broken, disallow.
            if (denial != null) {
                // Unlike entities where arrows rebound and may cause multiple alerts,
                // projectiles lodged in blocks do not continuously re-trigger events.
                Messages.sendMessage((Player) shooter, TextMode.Err.getColor(), denial.get());
                event.setCancelled(true);
            }

            return;
        }

        // Allow change if projectile was shot by a dispenser in the same claim.
        if (isBlockSourceInClaim(shooter, claim))
            return;

        // Prevent change in all other cases.
        event.setCancelled(true);
    }

    private boolean isBlockSourceInClaim(ProjectileSource projectileSource, Claim claim) {
        return projectileSource instanceof BlockProjectileSource &&
                dataStore.getClaimAt(((BlockProjectileSource) projectileSource).getBlock().getLocation(), false, claim) == claim;
    }

    //when an entity explodes...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent explodeEvent) {
        handleExplosion(explodeEvent.getLocation(), explodeEvent.getEntity(), explodeEvent.blockList());
    }

    //when a block explodes...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockExplode(BlockExplodeEvent explodeEvent)
    {
        handleExplosion(explodeEvent.getBlock().getLocation(), null, explodeEvent.blockList());
    }


    void handleExplosion(Location location, Entity entity, List<Block> blocks) {
        //only applies to claims-enabled worlds
        World world = location.getWorld();

        if (!configManager.getSystemConfig().claimsEnabledForWorld(world)) return;

        //make a list of blocks which were allowed to explode
        List<Block> explodedBlocks = new ArrayList<>();
        Claim cachedClaim = null;
        for (Block block : blocks) {
            //always ignore air blocks
            if (block.getType() == Material.AIR) continue;

            //is it in a land claim?
            Claim claim = dataStore.getClaimAt(block.getLocation(), false, cachedClaim);
            if (claim != null) {
                cachedClaim = claim;
            }

            //if yes, apply claim exemptions if they should apply
            if (claim != null && (claim.areExplosivesAllowed || !configManager.getClaimsConfig().blockClaimExplosions)) {
                explodedBlocks.add(block);
                continue;
            }

        }

        //clear original damage list and replace with allowed damage list
        blocks.clear();
        blocks.addAll(explodedBlocks);
    }

    //when an entity picks up an item
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPickup(EntityChangeBlockEvent event) {
        //FEATURE: endermen don't steal claimed blocks

        //if its an enderman
        if (event.getEntity().getType() == EntityType.ENDERMAN) {
            //and the block is claimed
            if (this.dataStore.getClaimAt(event.getBlock().getLocation(), false, null) != null) {
                //he doesn't get to steal it
                event.setCancelled(true);
            }
        }
    }

    //when a painting is broken
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onHangingBreak(HangingBreakEvent event) {
        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(event.getEntity().getWorld())) return;

        //Ignore cases where itemframes should break due to no supporting blocks
        if (event.getCause() == RemoveCause.PHYSICS) return;

        //FEATURE: claimed paintings are protected from breakage

        //explosions don't destroy hangings
        if (event.getCause() == RemoveCause.EXPLOSION) {
            event.setCancelled(true);
            return;
        }

        //only allow players to break paintings, not anything else (like water and explosions)
        if (!(event instanceof HangingBreakByEntityEvent)) {
            event.setCancelled(true);
            return;
        }

        HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent) event;

        //who is removing it?
        Entity remover = entityEvent.getRemover();

        //again, making sure the breaker is a player
        if (remover.getType() != EntityType.PLAYER) {
            event.setCancelled(true);
            return;
        }

        //if the player doesn't have build permission, don't allow the breakage
        Player playerRemover = (Player) entityEvent.getRemover();
        String noBuildReason = PermissionManager.allowBuild(playerRemover, configManager, event.getEntity().getLocation(), Material.AIR);
        if (noBuildReason != null) {
            event.setCancelled(true);
            Messages.sendMessage(playerRemover, TextMode.Err.getColor(), noBuildReason);
        }
    }

    //when a painting is placed...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPaintingPlace(HangingPlaceEvent event)
    {
        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(event.getBlock().getWorld())) return;

        //FEATURE: similar to above, placing a painting requires build permission in the claim

        //if the player doesn't have permission, don't allow the placement
        String noBuildReason = PermissionManager.allowBuild(event.getPlayer(), configManager, event.getEntity().getLocation(), Material.PAINTING);
        if (noBuildReason != null)
        {
            event.setCancelled(true);
            Messages.sendMessage(event.getPlayer(), TextMode.Err.getColor(), noBuildReason);
            return;
        }

    }

    private boolean isMonster(Entity entity)
    {
        if (entity instanceof Monster) return true;

        EntityType type = entity.getType();
        if (type == EntityType.GHAST || type == EntityType.MAGMA_CUBE || type == EntityType.SHULKER)
            return true;

        if (type == EntityType.SLIME)
            return ((Slime) entity).getSize() > 0;

        if (type == EntityType.RABBIT)
            return ((Rabbit) entity).getRabbitType() == Rabbit.Type.THE_KILLER_BUNNY;

        if (type == EntityType.PANDA)
            return ((Panda) entity).getMainGene() == Panda.Gene.AGGRESSIVE;

        if ((type == EntityType.HOGLIN || type == EntityType.POLAR_BEAR) && entity instanceof Mob)
            return !entity.getPersistentDataContainer().has(luredByPlayer, PersistentDataType.BYTE) && ((Mob) entity).getTarget() != null;

        return false;
    }

    // Tag passive animals that can become aggressive so we can tell whether or not they are hostile later
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTarget(EntityTargetEvent event)
    {
        if (!configManager.getSystemConfig().claimsEnabledForWorld(event.getEntity().getWorld())) return;

        EntityType entityType = event.getEntityType();
        if (entityType != EntityType.HOGLIN && entityType != EntityType.POLAR_BEAR)
            return;

        if (event.getReason() == EntityTargetEvent.TargetReason.TEMPT)
            event.getEntity().getPersistentDataContainer().set(luredByPlayer, PersistentDataType.BYTE, (byte) 1);
        else
            event.getEntity().getPersistentDataContainer().remove(luredByPlayer);

    }

    //when an entity is damaged
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event)
    {
        this.handleEntityDamageEvent(event, true);
    }

    //when an entity is set on fire
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityCombustByEntity(EntityCombustByEntityEvent event)
    {
        //handle it just like we would an entity damge by entity event, except don't send player messages to avoid double messages
        //in cases like attacking with a flame sword or flame arrow, which would ALSO trigger the direct damage event handler

        EntityDamageByEntityEvent eventWrapper = new EntityDamageByEntityEvent(event.getCombuster(), event.getEntity(), DamageCause.FIRE_TICK, event.getDuration());
        this.handleEntityDamageEvent(eventWrapper, false);
        event.setCancelled(eventWrapper.isCancelled());
    }

    private void handleEntityDamageEvent(EntityDamageEvent event, boolean sendErrorMessagesToPlayers)
    {
        //monsters are never protected
        if (isMonster(event.getEntity())) return;

        //horse protections can be disabled
        if (event.getEntity() instanceof Horse && !configManager.getClaimsConfig().protectHorses) return;
        if (event.getEntity() instanceof Donkey && !configManager.getClaimsConfig().protectDonkeys) return;
        if (event.getEntity() instanceof Mule && !configManager.getClaimsConfig().protectDonkeys) return;
        if (event.getEntity() instanceof Llama && !configManager.getClaimsConfig().protectLlamas) return;
        //protected death loot can't be destroyed, only picked up or despawned due to expiration
        if (event.getEntityType() == EntityType.DROPPED_ITEM) {
            if (event.getEntity().hasMetadata("GP_ITEMOWNER")) {
                event.setCancelled(true);
            }
        }

        //protect pets from environmental damage types which could be easily caused by griefers
        if (event.getEntity() instanceof Tameable) {
            Tameable tameable = (Tameable) event.getEntity();
            if (tameable.isTamed()) {
                DamageCause cause = event.getCause();
                if (cause != null && (
                        cause == DamageCause.BLOCK_EXPLOSION ||
                        cause == DamageCause.ENTITY_EXPLOSION ||
                                cause == DamageCause.FALLING_BLOCK ||
                                cause == DamageCause.FIRE ||
                                cause == DamageCause.FIRE_TICK ||
                                cause == DamageCause.LAVA ||
                                cause == DamageCause.SUFFOCATION ||
                                cause == DamageCause.CONTACT ||
                                cause == DamageCause.DROWNING)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (handleBlockExplosionDamage(event)) return;

        //the rest is only interested in entities damaging entities (ignoring environmental damage)
        if (!(event instanceof EntityDamageByEntityEvent)) return;

        EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

        if (subEvent.getDamager() instanceof LightningStrike && subEvent.getDamager().hasMetadata("GP_TRIDENT"))
        {
            event.setCancelled(true);
            return;
        }
        //determine which player is attacking, if any
        Player attacker = null;
        Projectile arrow = null;
        Firework firework = null;
        Entity damageSource = subEvent.getDamager();

        if (damageSource != null)
        {
            if (damageSource.getType() == EntityType.PLAYER)
            {
                attacker = (Player) damageSource;
            }
            else if (damageSource instanceof Projectile)
            {
                arrow = (Projectile) damageSource;
                if (arrow.getShooter() instanceof Player)
                {
                    attacker = (Player) arrow.getShooter();
                }
            }
            else if (subEvent.getDamager() instanceof Firework)
            {
                damageSource = subEvent.getDamager();
                if (damageSource.hasMetadata("GP_FIREWORK"))
                {
                    List<MetadataValue> data = damageSource.getMetadata("GP_FIREWORK");
                    if (data != null && data.size() > 0)
                    {
                        firework = (Firework) damageSource;
                        attacker = (Player) data.get(0).value();
                    }
                }
            }

        }

        if (event instanceof EntityDamageByEntityEvent) {
            //don't track in worlds where claims are not enabled
            if (!configManager.getSystemConfig().claimsEnabledForWorld(event.getEntity().getWorld())) return;

            //if the damaged entity is a claimed item frame or armor stand, the damager needs to be a player with build trust in the claim
            if (subEvent.getEntityType() == EntityType.ITEM_FRAME
                    || subEvent.getEntityType() == EntityType.GLOW_ITEM_FRAME
                    || subEvent.getEntityType() == EntityType.ARMOR_STAND
                    || subEvent.getEntityType() == EntityType.VILLAGER
                    || subEvent.getEntityType() == EntityType.ENDER_CRYSTAL) {
                //allow for disabling villager protections in the config
                if (subEvent.getEntityType() == EntityType.VILLAGER && !configManager.getClaimsConfig().protectCreatures)
                    return;

                //don't protect polar bears, they may be aggressive
                if (subEvent.getEntityType() == EntityType.POLAR_BEAR) return;

                //decide whether it's claimed
                Claim cachedClaim = null;
                PlayerData playerData = null;
                if (attacker != null) {
                    playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
                    cachedClaim = playerData.lastClaim;
                }

                Claim claim = this.dataStore.getClaimAt(event.getEntity().getLocation(), false, cachedClaim);

                //if it's claimed
                if (claim != null) {
                    //if attacker isn't a player, cancel
                    if (attacker == null)
                    {
                        //exception case
                        if (event.getEntityType() == EntityType.VILLAGER && damageSource != null && damageSource instanceof Zombie)
                        {
                            return;
                        }

                        event.setCancelled(true);
                        return;
                    }

                    //otherwise player must have container trust in the claim
                    Supplier<String> failureReason = claim.checkPermission(attacker, ClaimPermission.Build, event);
                    if (failureReason != null) {
                        event.setCancelled(true);
                        if (sendErrorMessagesToPlayers)
                            Messages.sendMessage(attacker, TextMode.Err.getColor(), failureReason.get());
                        return;
                    }
                }
            }

            Claim cachedClaim = null;
            PlayerData playerData = null;

            //if not a player or an explosive, allow
            //RoboMWM: Or a lingering potion, or a witch
            if (attacker == null
                    && damageSource != null
                    && damageSource.getType() != EntityType.CREEPER
                    && damageSource.getType() != EntityType.WITHER
                    && damageSource.getType() != EntityType.ENDER_CRYSTAL
                    && damageSource.getType() != EntityType.AREA_EFFECT_CLOUD
                    && damageSource.getType() != EntityType.WITCH
                    && !(damageSource instanceof Projectile)
                    && !(damageSource instanceof Explosive)
                    && !(damageSource instanceof ExplosiveMinecart))
            {
                return;
            }

            if (attacker != null)
            {
                playerData = dataStore.getPlayerData(attacker.getUniqueId());
                cachedClaim = playerData.lastClaim;
            }

            Claim claim = this.dataStore.getClaimAt(event.getEntity().getLocation(), false, cachedClaim);

            //if it's claimed
            if (claim != null) {
                //if damaged by anything other than a player (exception villagers injured by zombies in admin claims), cancel the event
                //why exception?  so admins can set up a village which can't be CHANGED by players, but must be "protected" by players.
                //TODO: Discuss if this should only apply to admin claims...?
                if (attacker == null)
                {
                    //exception case
                    if (event.getEntityType() == EntityType.VILLAGER && damageSource != null && (damageSource.getType() == EntityType.ZOMBIE || damageSource.getType() == EntityType.VINDICATOR || damageSource.getType() == EntityType.EVOKER || damageSource.getType() == EntityType.EVOKER_FANGS || damageSource.getType() == EntityType.VEX))
                    {
                        return;
                    }

                    //all other cases
                    else
                    {
                        event.setCancelled(true);
                        if (damageSource instanceof Projectile)
                        {
                            damageSource.remove();
                        }
                    }
                }

                //otherwise the player damaging the entity must have permission, unless it's a dog in a pvp world
                else if (!(event.getEntity().getWorld().getPVP() && event.getEntity().getType() == EntityType.WOLF))
                {
                    Supplier<String> override = null;
                    if (sendErrorMessagesToPlayers)
                    {
                        final Player finalAttacker = attacker;
                        override = () ->
                        {
                            String message = configManager.getMessagesConfig().getMessage(MessageType.NoDamageClaimedEntity, claim.getOwnerName());
                            if (finalAttacker.hasPermission("griefprevention.ignoreclaims"))
                                message += "  " + configManager.getMessagesConfig().getMessage(MessageType.IgnoreClaimsAdvertisement);
                            return message;
                        };
                    }
                    Supplier<String> noContainersReason = claim.checkPermission(attacker, ClaimPermission.Inventory, event, override);
                    if (noContainersReason != null)
                    {
                        event.setCancelled(true);

                        //kill the arrow to avoid infinite bounce between crowded together animals //RoboMWM: except for tridents
                        if (arrow != null && arrow.getType() != EntityType.TRIDENT) arrow.remove();
                        if (damageSource != null && damageSource.getType() == EntityType.FIREWORK && event.getEntity().getType() != EntityType.PLAYER)
                            return;

                        if (sendErrorMessagesToPlayers)
                        {
                            Messages.sendMessage(attacker, TextMode.Err.getColor(), noContainersReason.get());
                        }
                        event.setCancelled(true);
                    }

                    //cache claim for later
                    if (playerData != null)
                    {
                        playerData.lastClaim = claim;
                    }
                }
            }
        }
    }

    /**
     * Handles entity damage caused by block explosions.
     *
     * @param event the EntityDamageEvent
     * @return true if the damage has been handled
     */
    private boolean handleBlockExplosionDamage(EntityDamageEvent event)
    {
        if (event.getCause() != DamageCause.BLOCK_EXPLOSION) return false;

        Entity entity = event.getEntity();

        // Skip players - does allow players to use block explosions to bypass PVP protections,
        // but also doesn't disable self-damage.
        if (entity instanceof Player) return false;

        Claim claim = TheatriaClaims.instance.dataStore.getClaimAt(entity.getLocation(), false, null);

        // Only block explosion damage inside claims.
        if (claim == null) return false;

        event.setCancelled(true);
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onCrossbowFireWork(EntityShootBowEvent shootEvent)
    {
        if (shootEvent.getEntity() instanceof Player && shootEvent.getProjectile() instanceof Firework)
        {
            shootEvent.getProjectile().setMetadata("GP_FIREWORK", new FixedMetadataValue(TheatriaClaims.instance, shootEvent.getEntity()));
        }
    }

    //when a vehicle is damaged
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onVehicleDamage(VehicleDamageEvent event) {
        //all of this is anti theft code
        if (!configManager.getClaimsConfig().preventTheft) return;

        //input validation
        if (event.getVehicle() == null) return;

        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(event.getVehicle().getWorld())) return;

        //determine which player is attacking, if any
        Player attacker = null;
        Entity damageSource = event.getAttacker();
        EntityType damageSourceType = null;

        //if damage source is null or a creeper, don't allow the damage when the vehicle is in a land claim
        if (damageSource != null)
        {
            damageSourceType = damageSource.getType();

            if (damageSource.getType() == EntityType.PLAYER)
            {
                attacker = (Player) damageSource;
            }
            else if (damageSource instanceof Projectile)
            {
                Projectile arrow = (Projectile) damageSource;
                if (arrow.getShooter() instanceof Player)
                {
                    attacker = (Player) arrow.getShooter();
                }
            }
            else if (damageSource instanceof Firework)
            {
                if (damageSource.hasMetadata("GP_FIREWORK"))
                {
                    List<MetadataValue> data = damageSource.getMetadata("GP_FIREWORK");
                    if (data != null && data.size() > 0)
                    {
                        attacker = (Player) data.get(0).value();
                    }
                }
            }
        }

        //if not a player and not an explosion, always allow
        if (attacker == null && damageSourceType != EntityType.CREEPER && damageSourceType != EntityType.WITHER && damageSourceType != EntityType.PRIMED_TNT) {
            return;
        }

        //NOTE: vehicles can be pushed around.
        //so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
        Claim cachedClaim = null;
        PlayerData playerData = null;

        if (attacker != null) {
            playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
            cachedClaim = playerData.lastClaim;
        }

        Claim claim = this.dataStore.getClaimAt(event.getVehicle().getLocation(), false, cachedClaim);

        //if it's claimed
        if (claim != null) {
            //if damaged by anything other than a player, cancel the event
            if (attacker == null) {
                event.setCancelled(true);
            }

            //otherwise the player damaging the entity must have permission
            else {
                final Player finalAttacker = attacker;
                Supplier<String> override = () -> {
                    String message = configManager.getMessagesConfig().getMessage(MessageType.NoDamageClaimedEntity, claim.getOwnerName());
                    if (finalAttacker.hasPermission("griefprevention.ignoreclaims"))
                        message += "  " + configManager.getMessagesConfig().getMessage(MessageType.IgnoreClaimsAdvertisement);
                    return message;
                };
                Supplier<String> noContainersReason = claim.checkPermission(attacker, ClaimPermission.Inventory, event, override);
                if (noContainersReason != null)
                {
                    event.setCancelled(true);
                    Messages.sendMessage(attacker, TextMode.Err.getColor(), noContainersReason.get());
                }

                //cache claim for later
                if (playerData != null)
                {
                    playerData.lastClaim = claim;
                }
            }
        }
    }

    public static final HashSet<PotionEffectType> positiveEffects = new HashSet<>(Arrays.asList
            (
                    PotionEffectType.ABSORPTION,
                    PotionEffectType.DAMAGE_RESISTANCE,
                    PotionEffectType.FAST_DIGGING,
                    PotionEffectType.FIRE_RESISTANCE,
                    PotionEffectType.HEAL,
                    PotionEffectType.HEALTH_BOOST,
                    PotionEffectType.INCREASE_DAMAGE,
                    PotionEffectType.INVISIBILITY,
                    PotionEffectType.JUMP,
                    PotionEffectType.NIGHT_VISION,
                    PotionEffectType.REGENERATION,
                    PotionEffectType.SATURATION,
                    PotionEffectType.SPEED,
                    PotionEffectType.WATER_BREATHING
            ));

}
