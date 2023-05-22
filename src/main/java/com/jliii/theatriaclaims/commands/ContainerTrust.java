package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.util.CommandHelpers;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ContainerTrust implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        //containertrust <player>
        else if (cmd.getName().equalsIgnoreCase("containertrust") && player != null)
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            CommandHelpers.handleTrustCommand(player, ClaimPermission.Inventory, args[0]);

            return true;
        }
        return false;
    }
}
