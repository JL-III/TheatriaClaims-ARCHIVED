package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.claim.ClaimPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AccessTrust implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
//accesstrust <player>
        else if (cmd.getName().equalsIgnoreCase("accesstrust") && player != null)
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            handleTrustCommand(player, ClaimPermission.Access, args[0]);

            return true;
        }
        return false;
    }
}
