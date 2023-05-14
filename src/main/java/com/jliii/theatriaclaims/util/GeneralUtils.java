package com.jliii.theatriaclaims.util;

import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.events.PreventBlockBreakEvent;
import com.jliii.theatriaclaims.managers.ConfigManager;

public class GeneralUtils {

    public static ItemStack getItemInHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) return player.getInventory().getItemInOffHand();
        return player.getInventory().getItemInMainHand();
    }

    public static String getfriendlyLocationString(Location location) {
        return location.getWorld().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
    }

    public static String allowBuild(Player player, ConfigManager configManager, Location location) {
        // TODO check all derivatives and rework API
        return allowBuild(player, configManager, location, location.getBlock().getType());
    }

    public static String allowBuild(Player player, ConfigManager configManager, Location location, Material material) {
        if (!configManager.getSystemConfig().claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = TheatriaClaims.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = TheatriaClaims.instance.dataStore.getClaimAt(location, false, playerData.lastClaim);

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

    public static String allowBreak(Player player, ConfigManager configManager, Block block, Location location) {
        return allowBreak(player, configManager, block, location, new BlockBreakEvent(block, player));
    }

    public static String allowBreak(Player player, ConfigManager configManager, Material material, Location location, BlockBreakEvent breakEvent) {
        return allowBreak(player, configManager, location.getBlock(), location, breakEvent);
    }

    public static String allowBreak(Player player, ConfigManager configManager, Block block, Location location, BlockBreakEvent breakEvent) {
        if (!configManager.getSystemConfig().claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = TheatriaClaims.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = TheatriaClaims.instance.dataStore.getClaimAt(location, false, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        else {
            //cache the claim for later reference
            playerData.lastClaim = claim;

            //if not in the wilderness, then apply claim rules (permissions, etc)
            Supplier<String> cancel = claim.checkPermission(player, ClaimPermission.Build, breakEvent);
            if (cancel != null && breakEvent != null) {
                PreventBlockBreakEvent preventionEvent = new PreventBlockBreakEvent(breakEvent);
                Bukkit.getPluginManager().callEvent(preventionEvent);
                if (preventionEvent.isCancelled()) {
                    cancel = null;
                }
            }
            if (cancel == null) return null;
            return cancel.get();
        }
    }

}
