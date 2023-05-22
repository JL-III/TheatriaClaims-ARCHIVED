package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.Supplier;

public class TrustList implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
//trustlist
        else if (cmd.getName().equalsIgnoreCase("trustlist") && player != null)
        {
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true, null);

            //if no claim here, error message
            if (claim == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.TrustListNoClaim);
                return true;
            }

            //if no permission to manage permissions, error message
            Supplier<String> errorMessage = claim.checkPermission(player, ClaimPermission.Manage, null);
            if (errorMessage != null) {
                Messages.sendMessage(player, TextMode.Err.getColor(), errorMessage.get());
                return true;
            }

            //otherwise build a list of explicit permissions by permission level
            //and send that to the player
            ArrayList<String> builders = new ArrayList<>();
            ArrayList<String> containers = new ArrayList<>();
            ArrayList<String> accessors = new ArrayList<>();
            ArrayList<String> managers = new ArrayList<>();
            claim.getPermissions(builders, containers, accessors, managers);

            Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.TrustListHeader);

            StringBuilder permissions = new StringBuilder();
            permissions.append(ChatColor.GOLD).append('>');

            if (managers.size() > 0)
            {
                for (String manager : managers)
                    permissions.append(trustEntryToPlayerName(manager)).append(' ');
            }

            player.sendMessage(permissions.toString());
            permissions = new StringBuilder();
            permissions.append(ChatColor.YELLOW).append('>');

            if (builders.size() > 0)
            {
                for (String builder : builders)
                    permissions.append(trustEntryToPlayerName(builder)).append(' ');
            }

            player.sendMessage(permissions.toString());
            permissions = new StringBuilder();
            permissions.append(ChatColor.GREEN).append('>');

            if (containers.size() > 0)
            {
                for (String container : containers)
                    permissions.append(trustEntryToPlayerName(container)).append(' ');
            }

            player.sendMessage(permissions.toString());
            permissions = new StringBuilder();
            permissions.append(ChatColor.BLUE).append('>');

            if (accessors.size() > 0)
            {
                for (String accessor : accessors)
                    permissions.append(trustEntryToPlayerName(accessor)).append(' ');
            }

            player.sendMessage(permissions.toString());

            player.sendMessage(
                    ChatColor.GOLD + configManager.getMessagesConfig().getMessage(MessageType.Manage) + " " +
                            ChatColor.YELLOW + configManager.getMessagesConfig().getMessage(MessageType.Build) + " " +
                            ChatColor.GREEN + configManager.getMessagesConfig().getMessage(MessageType.Containers) + " " +
                            ChatColor.BLUE + configManager.getMessagesConfig().getMessage(MessageType.Access));

            if (claim.getSubclaimRestrictions()) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.HasSubclaimRestriction);
            }

            return true;
        }
        return false;
    }
}
