package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.ShovelMode;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AdminClaims implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //adminclaims
        else if (cmd.getName().equalsIgnoreCase("adminclaims") && player != null)
        {
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            playerData.shovelMode = ShovelMode.Admin;
            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AdminClaimsMode);

            return true;
        }
        return false;
    }
}
