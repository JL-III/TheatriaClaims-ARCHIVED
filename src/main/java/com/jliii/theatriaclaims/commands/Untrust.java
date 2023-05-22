package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.events.TrustChangedEvent;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.player.PlayerName;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Untrust implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //untrust <player> or untrust [<group>]
        else if (cmd.getName().equalsIgnoreCase("untrust") && player != null)
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //determine which claim the player is standing in
            com.jliii.theatriaclaims.claim.Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

            //determine whether a single player or clearing permissions entirely
            boolean clearPermissions = false;
            OfflinePlayer otherPlayer = null;
            if (args[0].equals("all"))
            {
                if (claim == null || claim.checkPermission(player, ClaimPermission.Edit, null) == null)
                {
                    clearPermissions = true;
                }
                else
                {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ClearPermsOwnerOnly);
                    return true;
                }
            }
            else
            {
                //validate player argument or group argument
                if (!args[0].startsWith("[") || !args[0].endsWith("]"))
                {
                    otherPlayer = PlayerName.resolvePlayerByName(args[0]);
                    if (!clearPermissions && otherPlayer == null && !args[0].equals("public"))
                    {
                        //bracket any permissions - at this point it must be a permission without brackets
                        if (args[0].contains("."))
                        {
                            args[0] = "[" + args[0] + "]";
                        }
                        else
                        {
                            Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                            return true;
                        }
                    }

                    //correct to proper casing
                    if (otherPlayer != null)
                        args[0] = otherPlayer.getName();
                }
            }

            //if no claim here, apply changes to all his claims
            if (claim == null)
            {
                PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());

                String idToDrop = args[0];
                if (otherPlayer != null)
                {
                    idToDrop = otherPlayer.getUniqueId().toString();
                }

                //calling event
                TrustChangedEvent event = new TrustChangedEvent(player, playerData.getClaims(), null, false, idToDrop);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled())
                {
                    return true;
                }

                //dropping permissions
                for (com.jliii.theatriaclaims.claim.Claim targetClaim : event.getClaims()) {
                    claim = targetClaim;

                    //if untrusting "all" drop all permissions
                    if (clearPermissions)
                    {
                        claim.clearPermissions();
                    }

                    //otherwise drop individual permissions
                    else
                    {
                        claim.dropPermission(idToDrop);
                        claim.managers.remove(idToDrop);
                    }

                    //save changes
                    TheatriaClaims.getInstance().getDatabaseManager().getDataStore().saveClaim(claim);
                }

                //beautify for output
                if (args[0].equals("public"))
                {
                    args[0] = "the public";
                }

                //confirmation message
                if (!clearPermissions)
                {
                    Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.UntrustIndividualAllClaims, args[0]);
                }
                else
                {
                    Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.UntrustEveryoneAllClaims);
                }
            }

            //otherwise, apply changes to only this claim
            else if (claim.checkPermission(player, ClaimPermission.Manage, null) != null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.NoPermissionTrust, claim.getOwnerName());
                return true;
            }
            else
            {
                //if clearing all
                if (clearPermissions)
                {
                    //requires owner
                    if (claim.checkPermission(player, ClaimPermission.Edit, null) != null)
                    {
                        Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.UntrustAllOwnerOnly);
                        return true;
                    }

                    //calling the event
                    TrustChangedEvent event = new TrustChangedEvent(player, claim, null, false, args[0]);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled())
                    {
                        return true;
                    }

                    event.getClaims().forEach(Claim::clearPermissions);
                    Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.ClearPermissionsOneClaim);
                }

                //otherwise individual permission drop
                else
                {
                    String idToDrop = args[0];
                    if (otherPlayer != null)
                    {
                        idToDrop = otherPlayer.getUniqueId().toString();
                    }
                    boolean targetIsManager = claim.managers.contains(idToDrop);
                    if (targetIsManager && claim.checkPermission(player, ClaimPermission.Edit, null) != null)  //only claim owners can untrust managers
                    {
                        Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ManagersDontUntrustManagers, claim.getOwnerName());
                        return true;
                    }
                    else
                    {
                        //calling the event
                        TrustChangedEvent event = new TrustChangedEvent(player, claim, null, false, idToDrop);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled())
                        {
                            return true;
                        }

                        event.getClaims().forEach(targetClaim -> targetClaim.dropPermission(event.getIdentifier()));

                        //beautify for output
                        if (args[0].equals("public"))
                        {
                            args[0] = "the public";
                        }

                        Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.UntrustIndividualSingleClaim, args[0]);
                    }
                }

                //save changes
                TheatriaClaims.getInstance().getDatabaseManager().getDataStore().saveClaim(claim);
            }

            return true;
        }
        return false;
    }
}
