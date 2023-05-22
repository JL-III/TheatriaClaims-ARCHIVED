package com.jliii.theatriaclaims.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AbandonClaim implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //abandonclaim
        if (cmd.getName().equalsIgnoreCase("abandonclaim") && player != null) {
            return abandonClaimHandler(player, false);
        }
        return false;
    }
}
