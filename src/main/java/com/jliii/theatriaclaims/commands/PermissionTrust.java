package com.jliii.theatriaclaims.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class PermissionTrust implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //permissiontrust <player>
        else if (cmd.getName().equalsIgnoreCase("permissiontrust") && player != null)
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            handleTrustCommand(player, null, args[0]);  //null indicates permissiontrust to the helper method

            return true;
        }
        return false;
    }
}
