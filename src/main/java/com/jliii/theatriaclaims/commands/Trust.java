package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.claim.ClaimPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Trust implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //trust <player>
        else if (cmd.getName().equalsIgnoreCase("trust") && player != null)
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //most trust commands use this helper method, it keeps them consistent
            handleTrustCommand(player, ClaimPermission.Build, args[0]);

            return true;
        }
        return false;
    }
}
