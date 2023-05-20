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

package com.jliii.theatriaclaims.listeners;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.claim.CreateClaimResult;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.PistonMode;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.managers.ConfigManager;
import com.jliii.theatriaclaims.managers.PermissionManager;
import com.jliii.theatriaclaims.util.BoundingBox;
import com.jliii.theatriaclaims.util.DataStore;
import com.jliii.theatriaclaims.util.GeneralUtils;
import com.jliii.theatriaclaims.util.Messages;
import com.jliii.theatriaclaims.util.PlayerData;
import com.jliii.theatriaclaims.visualization.BoundaryVisualization;
import com.jliii.theatriaclaims.visualization.VisualizationType;

import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import java.io.ObjectInputFilter.Config;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

//event handlers related to blocks
public class BlockEventHandler implements Listener {
    //convenience reference to singleton datastore
    private final DataStore dataStore;

    private final ConfigManager configManager;

    private final EnumSet<Material> trashBlocks;

    //constructor
    public BlockEventHandler(DataStore dataStore, ConfigManager configManager) {
        this.dataStore = dataStore;
        this.configManager = configManager;
        //create the list of blocks which will not trigger a warning when they're placed outside of land claims
        this.trashBlocks = EnumSet.noneOf(Material.class);
        this.trashBlocks.add(Material.COBBLESTONE);
        this.trashBlocks.add(Material.TORCH);
        this.trashBlocks.add(Material.DIRT);
        this.trashBlocks.add(Material.OAK_SAPLING);
        this.trashBlocks.add(Material.SPRUCE_SAPLING);
        this.trashBlocks.add(Material.BIRCH_SAPLING);
        this.trashBlocks.add(Material.JUNGLE_SAPLING);
        this.trashBlocks.add(Material.ACACIA_SAPLING);
        this.trashBlocks.add(Material.DARK_OAK_SAPLING);
        this.trashBlocks.add(Material.GRAVEL);
        this.trashBlocks.add(Material.SAND);
        this.trashBlocks.add(Material.TNT);
        this.trashBlocks.add(Material.CRAFTING_TABLE);
        this.trashBlocks.add(Material.TUFF);
        this.trashBlocks.add(Material.COBBLED_DEEPSLATE);
    }

    //when a player breaks a block...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent breakEvent) {
        Player player = breakEvent.getPlayer();
        Block block = breakEvent.getBlock();

        //make sure the player is allowed to break at the location
        String noBuildReason = PermissionManager.allowBreak(player, configManager, block, block.getLocation(), breakEvent);
        if (noBuildReason != null) {
            Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason);
            breakEvent.setCancelled(true);
            return;
        }
    }

    //when a player places multiple blocks...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlocksPlace(BlockMultiPlaceEvent placeEvent) {
        Player player = placeEvent.getPlayer();

        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(placeEvent.getBlock().getWorld())) return;

        //make sure the player is allowed to build at the location
        for (BlockState block : placeEvent.getReplacedBlockStates()) {
            String noBuildReason = PermissionManager.allowBuild(player, configManager, block.getLocation(), block.getType());
            if (noBuildReason != null) {
                Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason);
                placeEvent.setCancelled(true);
                return;
            }
        }
    }

    //when a player places a block...
    @SuppressWarnings("null")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent placeEvent) {
        Player player = placeEvent.getPlayer();
        Block block = placeEvent.getBlock();

        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(placeEvent.getBlock().getWorld())) return;

        //make sure the player is allowed to build at the location
        String noBuildReason = PermissionManager.allowBuild(player, configManager, block.getLocation(), block.getType());
        if (noBuildReason != null) {
            // Allow players with container trust to place books in lecterns
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(block.getLocation(), true, playerData.lastClaim);
            if (block.getType() == Material.LECTERN && placeEvent.getBlockReplacedState().getType() == Material.LECTERN) {
                if (claim != null) {
                    playerData.lastClaim = claim;
                    Supplier<String> noContainerReason = claim.checkPermission(player, ClaimPermission.Inventory, placeEvent);
                    if (noContainerReason == null)
                        return;

                    placeEvent.setCancelled(true);
                    Messages.sendMessage(player, TextMode.Err.getColor(), noContainerReason.get());
                    return;
                }
            }
            Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason);
            placeEvent.setCancelled(true);
            return;
        }

        //if the block is being placed within or under an existing claim
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(block.getLocation(), true, playerData.lastClaim);

        //If block is a chest, don't allow a DoubleChest to form across a claim boundary
        denyConnectingDoubleChestsAcrossClaimBoundary(claim, block, player);
        
        if (claim != null) {
            playerData.lastClaim = claim;

            //warn about TNT not destroying claimed blocks
            if (block.getType() == Material.TNT && !claim.areExplosivesAllowed) {
                Messages.sendMessage(player, configManager, TextMode.Warn.getColor(), MessageType.NoTNTDamageClaims);
                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.ClaimExplosivesAdvertisement);
            }

            //allow for a build warning in the future
            playerData.warnedAboutBuildingOutsideClaims = false;
        }

        //FEATURE: automatically create a claim when a player who has no claims places a chest

        //otherwise if there's no claim, the player is placing a chest, and new player automatic claims are enabled
        else if (configManager.getSystemConfig().automaticClaimsForNewPlayersRadius > -1 && player.hasPermission("griefprevention.createclaims") && block.getType() == Material.CHEST) {
            int radius = configManager.getSystemConfig().automaticClaimsForNewPlayersRadius;
            //if the player doesn't have any claims yet, automatically create a claim centered at the chest
            if (playerData.getClaims().size() == 0) {
                //radius == 0 means protect ONLY the chest
                if (configManager.getSystemConfig().automaticClaimsForNewPlayersRadius == 0) {
                    this.dataStore.createClaim(block.getWorld(), block.getX(), block.getX(), block.getZ(), block.getZ(), player.getUniqueId(), null, null, player);
                    Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.ChestClaimConfirmation);
                }
                //otherwise, create a claim in the area around the chest
                else {
                    //if failure due to insufficient claim blocks available
                    if (playerData.getRemainingClaimBlocks() < Math.pow(1 + 2 * configManager.getSystemConfig().automaticClaimsForNewPlayersRadiusMin, 2)) {
                        Messages.sendMessage(player, configManager, TextMode.Warn.getColor(), MessageType.NoEnoughBlocksForChestClaim);
                        return;
                    }
                    //as long as the automatic claim overlaps another existing claim, shrink it
                    //note that since the player had permission to place the chest, at the very least, the automatic claim will include the chest
                    CreateClaimResult result = null;
                    while (radius >= configManager.getSystemConfig().automaticClaimsForNewPlayersRadiusMin) {
                        int area = (radius * 2 + 1) * (radius * 2 + 1);
                        if (playerData.getRemainingClaimBlocks() >= area) {
                            result = dataStore.createClaim(
                                    block.getWorld(),
                                    block.getX() - radius, block.getX() + radius,
                                    block.getZ() - radius, block.getZ() + radius,
                                    player.getUniqueId(),
                                    null, null,
                                    player);

                            if (result.succeeded) break;
                        }

                        radius--;
                    }

                    if (result != null && result.claim != null) {
                        if (result.succeeded) {
                            //notify and explain to player
                            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AutomaticClaimNotification);
                            //show the player the protected area
                            BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CLAIM, block, configManager);
                        }
                        else {
                            //notify and explain to player
                            Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.AutomaticClaimOtherClaimTooClose);
                            //show the player the protected area
                            BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CONFLICT_ZONE, block, configManager);
                        }
                    }
                }
                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
            }
            //check to see if this chest is in a claim, and warn when it isn't
            if (configManager.getClaimsConfig().preventTheft && this.dataStore.getClaimAt(block.getLocation(), false, playerData.lastClaim) == null) {
                Messages.sendMessage(player, configManager, TextMode.Warn.getColor(), MessageType.UnprotectedChestWarning);
            }
        }

    }

    static boolean isActiveBlock(Block block) {
        return isActiveBlock(block.getType());
    }

    public static boolean isActiveBlock(BlockState state) {
        return isActiveBlock(state.getType());
    }

    static boolean isActiveBlock(Material type) {
        if (type == Material.HOPPER || type == Material.BEACON || type == Material.SPAWNER) return true;
        return false;
    }

    private static final BlockFace[] HORIZONTAL_DIRECTIONS = new BlockFace[] {
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    };
    private void denyConnectingDoubleChestsAcrossClaimBoundary(Claim claim, Block block, Player player) {
        UUID claimOwner = null;
        if (claim != null)
            claimOwner = claim.getOwnerID();

        // Check for double chests placed just outside the claim boundary
        if (block.getBlockData() instanceof Chest) {
            for (BlockFace face : HORIZONTAL_DIRECTIONS) {
                Block relative = block.getRelative(face);
                if (!(relative.getBlockData() instanceof Chest)) continue;

                Claim relativeClaim = this.dataStore.getClaimAt(relative.getLocation(), true, claim);
                UUID relativeClaimOwner = relativeClaim == null ? null : relativeClaim.getOwnerID();

                // Chests outside claims should connect (both null)
                // and chests inside the same claim should connect (equal)
                if (Objects.equals(claimOwner, relativeClaimOwner)) break;

                // Change both chests to singular chests
                Chest chest = (Chest) block.getBlockData();
                chest.setType(Chest.Type.SINGLE);
                block.setBlockData(chest);

                Chest relativeChest = (Chest) relative.getBlockData();
                relativeChest.setType(Chest.Type.SINGLE);
                relative.setBlockData(relativeChest);

                // Resend relative chest block to prevent visual bug
                player.sendBlockChange(relative.getLocation(), relativeChest);
                break;
            }
        }
    }

    // // Prevent pistons pushing blocks into or out of claims.
    // @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    // public void onBlockPistonExtend(BlockPistonExtendEvent event) {
    //     onPistonEvent(event, event.getBlocks(), false);
    // }

    // // Prevent pistons pulling blocks into or out of claims.
    // @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    // public void onBlockPistonRetract(BlockPistonRetractEvent event) {
    //     onPistonEvent(event, event.getBlocks(), true);
    // }

    // // Handle piston push and pulls.
    // private void onPistonEvent(BlockPistonEvent event, List<Block> blocks, boolean isRetract) {
    //     PistonMode pistonMode = configManager.getClaimsConfig().pistonMovement;
    //     // Return if piston movements are ignored.
    //     if (pistonMode == PistonMode.IGNORED) return;

    //     // Don't check in worlds where claims are not enabled.
    //     if (!GriefPrevention.instance.claimsEnabledForWorld(event.getBlock().getWorld())) return;

    //     BlockFace direction = event.getDirection();
    //     Block pistonBlock = event.getBlock();
    //     Claim pistonClaim = this.dataStore.getClaimAt(pistonBlock.getLocation(), false,
    //             pistonMode != PistonMode.CLAIMS_ONLY, null);

    //     // A claim is required, but the piston is not inside a claim.
    //     if (pistonClaim == null && pistonMode == PistonMode.CLAIMS_ONLY) {
    //         event.setCancelled(true);
    //         return;
    //     }

    //     // If no blocks are moving, quickly check if another claim's boundaries are violated.
    //     if (blocks.isEmpty()) {
    //         // No block and retraction is always safe.
    //         if (isRetract) return;

    //         Block invadedBlock = pistonBlock.getRelative(direction);
    //         Claim invadedClaim = this.dataStore.getClaimAt(invadedBlock.getLocation(), false,
    //                 pistonMode != PistonMode.CLAIMS_ONLY, pistonClaim);
    //         if (invadedClaim != null && (pistonClaim == null || !Objects.equals(pistonClaim.getOwnerID(), invadedClaim.getOwnerID()))) {
    //             event.setCancelled(true);
    //         }

    //         return;
    //     }

    //     // Create bounding box for moved blocks.
    //     BoundingBox movedBlocks = BoundingBox.ofBlocks(blocks);
    //     // Expand to include invaded zone.
    //     movedBlocks.resize(direction, 1);

    //     if (pistonClaim != null) {
    //         // If blocks are all inside the same claim as the piston, allow.
    //         if (new BoundingBox(pistonClaim).contains(movedBlocks)) return;

    //         /*
    //          * In claims-only mode, all moved blocks must be inside of the owning claim.
    //          * From BigScary:
    //          *  - Could push into another land claim, don't want to spend CPU checking for that
    //          *  - Push ice out, place torch, get water outside the claim
    //          */
    //         if (pistonMode == PistonMode.CLAIMS_ONLY) {
    //             event.setCancelled(true);
    //             return;
    //         }
    //     }

    //     // Check if blocks are in line vertically.
    //     if (movedBlocks.getLength() == 1 && movedBlocks.getWidth() == 1) {
    //         // Pulling up is always safe. The claim may not contain the area pulled from, but claims cannot stack.
    //         if (isRetract && direction == BlockFace.UP) return;

    //         // Pushing down is always safe. The claim may not contain the area pushed into, but claims cannot stack.
    //         if (!isRetract && direction == BlockFace.DOWN) return;
    //     }

    //     // Assemble list of potentially intersecting claims from chunks interacted with.
    //     ArrayList<Claim> intersectable = new ArrayList<>();
    //     int chunkXMax = movedBlocks.getMaxX() >> 4;
    //     int chunkZMax = movedBlocks.getMaxZ() >> 4;

    //     for (int chunkX = movedBlocks.getMinX() >> 4; chunkX <= chunkXMax; ++chunkX) {
    //         for (int chunkZ = movedBlocks.getMinZ() >> 4; chunkZ <= chunkZMax; ++chunkZ) {
    //             ArrayList<Claim> chunkClaims = dataStore.chunksToClaimsMap.get(DataStore.getChunkHash(chunkX, chunkZ));
    //             if (chunkClaims == null) continue;

    //             for (Claim claim : chunkClaims) {
    //                 // Ensure claim is not piston claim and is in same world.
    //                 if (pistonClaim != claim && pistonBlock.getWorld().equals(claim.getLesserBoundaryCorner().getWorld()))
    //                     intersectable.add(claim);
    //             }
    //         }
    //     }

    //     BiPredicate<Claim, BoundingBox> intersectionHandler;
    //     final Claim finalPistonClaim = pistonClaim;

    //     // Fast mode: Bounding box intersection always causes a conflict, even if blocks do not conflict.
    //     if (pistonMode == PistonMode.EVERYWHERE_SIMPLE) {
    //         intersectionHandler = (claim, claimBoundingBox) -> {
    //             // If owners are different, cancel.
    //             if (finalPistonClaim == null || !Objects.equals(finalPistonClaim.getOwnerID(), claim.getOwnerID())) {
    //                 event.setCancelled(true);
    //                 return true;
    //             }

    //             // Otherwise, proceed to next claim.
    //             return false;
    //         };
    //     }
    //     // Precise mode: Bounding box intersection may not yield a conflict. Individual blocks must be considered.
    //     else {
    //         // Set up list of affected blocks.
    //         HashSet<Block> checkBlocks = new HashSet<>(blocks);

    //         // Add all blocks that will be occupied after the shift.
    //         for (Block block : blocks)
    //             if (block.getPistonMoveReaction() != PistonMoveReaction.BREAK)
    //                 checkBlocks.add(block.getRelative(direction));

    //         intersectionHandler = (claim, claimBoundingBox) -> {
    //             // Ensure that the claim contains an affected block.
    //             if (checkBlocks.stream().noneMatch(claimBoundingBox::contains)) return false;

    //             // If pushing this block will change ownership, cancel the event and take away the piston (for performance reasons).
    //             if (finalPistonClaim == null || !Objects.equals(finalPistonClaim.getOwnerID(), claim.getOwnerID())) {
    //                 event.setCancelled(true);
    //                 if (GriefPrevention.instance.config_pistonExplosionSound) {
    //                     pistonBlock.getWorld().createExplosion(pistonBlock.getLocation(), 0);
    //                 }
    //                 pistonBlock.getWorld().dropItem(pistonBlock.getLocation(), new ItemStack(event.isSticky() ? Material.STICKY_PISTON : Material.PISTON));
    //                 pistonBlock.setType(Material.AIR);
    //                 return true;
    //             }

    //             // Otherwise, proceed to next claim.
    //             return false;
    //         };
    //     }

    //     for (Claim claim : intersectable) {
    //         BoundingBox claimBoundingBox = new BoundingBox(claim);

    //         // Ensure claim intersects with block bounding box.
    //         if (!claimBoundingBox.intersects(movedBlocks)) continue;

    //         // Do additional mode-based handling.
    //         if (intersectionHandler.test(claim, claimBoundingBox)) return;
    //     }
    // }

    //blocks are ignited ONLY by flint and steel (not by being near lava, open flames, etc), unless configured otherwise
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockIgnite(BlockIgniteEvent igniteEvent) {
        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(igniteEvent.getBlock().getWorld())) return;

        if (igniteEvent.getCause() == IgniteCause.LIGHTNING && TheatriaClaims.instance.dataStore.getClaimAt(igniteEvent.getIgnitingEntity().getLocation(), false, null) != null) {
            igniteEvent.setCancelled(true); //BlockIgniteEvent is called before LightningStrikeEvent. See #532. However, see #1125 for further discussion on detecting trident-caused lightning.
        }

        // If a fire is started by a fireball from a dispenser, allow it if the dispenser is in the same claim.
        if (igniteEvent.getCause() == IgniteCause.FIREBALL && igniteEvent.getIgnitingEntity() instanceof Fireball) {
            ProjectileSource shooter = ((Fireball) igniteEvent.getIgnitingEntity()).getShooter();
            if (shooter instanceof BlockProjectileSource) {
                Claim claim = TheatriaClaims.instance.dataStore.getClaimAt(igniteEvent.getBlock().getLocation(), false, null);
                if (claim != null && TheatriaClaims.instance.dataStore.getClaimAt(((BlockProjectileSource) shooter).getBlock().getLocation(), false, claim) == claim) {
                    return;
                }
            }
        }

        // Arrow ignition.
        if (igniteEvent.getCause() == IgniteCause.ARROW && igniteEvent.getIgnitingEntity() != null) {
            // Flammable lightable blocks do not fire EntityChangeBlockEvent when igniting.
            BlockData blockData = igniteEvent.getBlock().getBlockData();
            if (blockData instanceof Lightable lightable) {
                // Set lit for resulting data in event. Currently unused, but may be in the future.
                lightable.setLit(true);

                // Call event.
                EntityChangeBlockEvent changeBlockEvent = new EntityChangeBlockEvent(igniteEvent.getIgnitingEntity(), igniteEvent.getBlock(), blockData);
                TheatriaClaims.instance.entityEventHandler.onEntityChangeBLock(changeBlockEvent);

                // Respect event result.
                if (changeBlockEvent.isCancelled()) {
                    igniteEvent.setCancelled(true);
                }
            }
            return;
        }

        if (!configManager.getWorldConfig().fireSpreads && igniteEvent.getCause() != IgniteCause.FLINT_AND_STEEL && igniteEvent.getCause() != IgniteCause.LIGHTNING) {
            igniteEvent.setCancelled(true);
        }
    }

    //fire doesn't spread unless configured to, but other blocks still do (mushrooms and vines, for example)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockSpread(BlockSpreadEvent spreadEvent) {
        if (spreadEvent.getSource().getType() != Material.FIRE) return;

        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(spreadEvent.getBlock().getWorld())) return;

        if (!configManager.getWorldConfig().fireSpreads) {
            spreadEvent.setCancelled(true);

            Block underBlock = spreadEvent.getSource().getRelative(BlockFace.DOWN);
            if (underBlock.getType() != Material.NETHERRACK) {
                spreadEvent.getSource().setType(Material.AIR);
            }

            return;
        }

        //never spread into a claimed area, regardless of settings
        if (this.dataStore.getClaimAt(spreadEvent.getBlock().getLocation(), false, null) != null) {
            if (configManager.getClaimsConfig().firespreads) return;
            spreadEvent.setCancelled(true);

            //if the source of the spread is not fire on netherrack, put out that source fire to save cpu cycles
            Block source = spreadEvent.getSource();
            if (source.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK)
            {
                source.setType(Material.AIR);
            }
        }
    }

    //blocks are not destroyed by fire, unless configured to do so
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBurn(BlockBurnEvent burnEvent) {
        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(burnEvent.getBlock().getWorld())) return;

        if (!configManager.getWorldConfig().fireDestroys) {
            burnEvent.setCancelled(true);
            Block block = burnEvent.getBlock();
            Block[] adjacentBlocks = new Block[]
                    {
                            block.getRelative(BlockFace.UP),
                            block.getRelative(BlockFace.DOWN),
                            block.getRelative(BlockFace.NORTH),
                            block.getRelative(BlockFace.SOUTH),
                            block.getRelative(BlockFace.EAST),
                            block.getRelative(BlockFace.WEST)
                    };

            //pro-actively put out any fires adjacent the burning block, to reduce future processing here
            for (Block adjacentBlock : adjacentBlocks) {
                if (adjacentBlock.getType() == Material.FIRE && adjacentBlock.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK) {
                    adjacentBlock.setType(Material.AIR);
                }
            }

            Block aboveBlock = block.getRelative(BlockFace.UP);
            if (aboveBlock.getType() == Material.FIRE) {
                aboveBlock.setType(Material.AIR);
            }
            return;
        }

        //never burn claimed blocks, regardless of settings
        if (this.dataStore.getClaimAt(burnEvent.getBlock().getLocation(), false, null) != null) {
            if (configManager.getClaimsConfig().firedamages) return;
            burnEvent.setCancelled(true);
        }
    }


    //ensures fluids don't flow into land claims from outside
    private Claim lastSpreadClaim = null;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockFromTo(BlockFromToEvent spreadEvent) {
        //always allow fluids to flow straight down
        if (spreadEvent.getFace() == BlockFace.DOWN) return;

        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(spreadEvent.getBlock().getWorld())) return;

        //where to?
        Block toBlock = spreadEvent.getToBlock();
        Location toLocation = toBlock.getLocation();
        Claim toClaim = this.dataStore.getClaimAt(toLocation, false, lastSpreadClaim);

        //if into a land claim, it must be from the same land claim
        if (toClaim != null) {
            this.lastSpreadClaim = toClaim;
            if (!toClaim.contains(spreadEvent.getBlock().getLocation(), false, true)) {
                //exception: from parent into subdivision
                if (toClaim.parent == null || !toClaim.parent.contains(spreadEvent.getBlock().getLocation(), false, false)) {
                    spreadEvent.setCancelled(true);
                }
            }
        }

    }

    //Stop projectiles from destroying blocks that don't fire a proper event
    @EventHandler(ignoreCancelled = true)
    private void chorusFlower(ProjectileHitEvent event) {
        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(event.getEntity().getWorld())) return;

        Block block = event.getHitBlock();

        // Ensure projectile affects block.
        if (block == null || block.getType() != Material.CHORUS_FLOWER)
            return;

        Claim claim = dataStore.getClaimAt(block.getLocation(), false, null);
        if (claim == null)
            return;

        Player shooter = null;
        Projectile projectile = event.getEntity();

        if (projectile.getShooter() instanceof Player)
            shooter = (Player) projectile.getShooter();

        if (shooter == null) {
            event.setCancelled(true);
            return;
        }

        Supplier<String> allowContainer = claim.checkPermission(shooter, ClaimPermission.Inventory, event);

        if (allowContainer != null) {
            event.setCancelled(true);
            Messages.sendMessage(shooter, TextMode.Err.getColor(), allowContainer.get());
            return;
        }
    }

    //ensures dispensers can't be used to dispense a block(like water or lava) or item across a claim boundary
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDispense(BlockDispenseEvent dispenseEvent) {
        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(dispenseEvent.getBlock().getWorld())) return;

        //from where?
        Block fromBlock = dispenseEvent.getBlock();
        BlockData fromData = fromBlock.getBlockData();
        if (!(fromData instanceof Dispenser)) return;
        Dispenser dispenser = (Dispenser) fromData;

        //to where?
        Block toBlock = fromBlock.getRelative(dispenser.getFacing());
        Claim fromClaim = this.dataStore.getClaimAt(fromBlock.getLocation(), false, null);
        Claim toClaim = this.dataStore.getClaimAt(toBlock.getLocation(), false, fromClaim);

        //wilderness to wilderness is OK
        if (fromClaim == null && toClaim == null) return;

        //within claim is OK
        if (fromClaim == toClaim) return;

        //everything else is NOT OK
        dispenseEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent growEvent) {
        //only take these potentially expensive steps if configured to do so
        if (!configManager.getClaimsConfig().limitTreeGrowth) return;

        //don't track in worlds where claims are not enabled
        if (!configManager.getSystemConfig().claimsEnabledForWorld(growEvent.getWorld())) return;

        Location rootLocation = growEvent.getLocation();
        Claim rootClaim = this.dataStore.getClaimAt(rootLocation, false, null);
        String rootOwnerName = null;

        //who owns the spreading block, if anyone?
        if (rootClaim != null) {
            //tree growth in subdivisions is dependent on who owns the top level claim
            if (rootClaim.parent != null) rootClaim = rootClaim.parent;

            //if an administrative claim, just let the tree grow where it wants
            if (rootClaim.isAdminClaim()) return;

            //otherwise, note the owner of the claim
            rootOwnerName = rootClaim.getOwnerName();
        }

        //for each block growing
        for (int i = 0; i < growEvent.getBlocks().size(); i++) {
            BlockState block = growEvent.getBlocks().get(i);
            Claim blockClaim = this.dataStore.getClaimAt(block.getLocation(), false, rootClaim);

            //if it's growing into a claim
            if (blockClaim != null) {
                //if there's no owner for the new tree, or the owner for the new tree is different from the owner of the claim
                if (rootOwnerName == null || !rootOwnerName.equals(blockClaim.getOwnerName())) {
                    growEvent.getBlocks().remove(i--);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        //prevent hoppers from picking-up items dropped by players on death

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof HopperMinecart || holder instanceof Hopper) {
            Item item = event.getItem();
            List<MetadataValue> data = item.getMetadata("GP_ITEMOWNER");
            //if this is marked as belonging to a player
            if (data != null && data.size() > 0) {
                UUID ownerID = (UUID) data.get(0).value();

                //has that player unlocked his drops?
                OfflinePlayer owner = TheatriaClaims.instance.getServer().getOfflinePlayer(ownerID);
                if (owner.isOnline()) {
                    PlayerData playerData = this.dataStore.getPlayerData(ownerID);

                    //if locked, don't allow pickup
                    if (!playerData.dropsAreUnlocked) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemFrameBrokenByBoat(final HangingBreakEvent event) {
        // Checks if the event is caused by physics - 90% of cases caused by a boat (other 10% would be block,
        // however since it's in a claim, unless you use a TNT block we don't need to worry about it).
        if (event.getCause() != HangingBreakEvent.RemoveCause.PHYSICS) {
            return;
        }

        // Cancels the event if in a claim, as we can not efficiently retrieve the person/entity who broke the Item Frame/Hangable Item.
        if (this.dataStore.getClaimAt(event.getEntity().getLocation(), false, null) != null) {
            event.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onNetherPortalCreate(final PortalCreateEvent event) {
        if (event.getReason() != PortalCreateEvent.CreateReason.NETHER_PAIR) {
            return;
        }

        // Ignore this event if preventNonPlayerCreatedPortals config option is disabled, and we don't know the entity.
        if (!(event.getEntity() instanceof Player) && !configManager.getClaimsConfig().preventNonPlayerCreatedPortals) {
            return;
        }

        for (BlockState blockState : event.getBlocks()) {
            Claim claim = this.dataStore.getClaimAt(blockState.getLocation(), false, null);
            if (claim != null) {
                if (event.getEntity() instanceof Player player) {
                    Supplier<String> noPortalReason = claim.checkPermission(player, ClaimPermission.Build, event);

                    if (noPortalReason != null) {
                        event.setCancelled(true);
                        Messages.sendMessage(player, TextMode.Err.getColor(), noPortalReason.get());
                        return;
                    }
                }
                else {
                    // Cancels the event if in a claim, as we can not efficiently retrieve the person/entity who created the portal.
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
