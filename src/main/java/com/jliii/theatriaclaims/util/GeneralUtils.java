package com.jliii.theatriaclaims.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class GeneralUtils {

    public static ItemStack getItemInHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) return player.getInventory().getItemInOffHand();
        return player.getInventory().getItemInMainHand();
    }

}
