package com.jliii.theatriaclaims.util;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.events.TrustChangedEvent;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.player.PlayerName;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

public class CommandHelpers {
    //helper method keeps the trust commands consistent and eliminates duplicate code
    public static void handleTrustCommand(Player player, ConfigManager configManager, ClaimPermission permissionLevel, String recipientName) {
        //determine which claim the player is standing in
        Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

        //validate player or group argument
        String permission = null;
        OfflinePlayer otherPlayer = null;
        UUID recipientID = null;
        if (recipientName.startsWith("[") && recipientName.endsWith("]")) {
            permission = recipientName.substring(1, recipientName.length() - 1);
            if (permission == null || permission.isEmpty()) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.InvalidPermissionID);
                return;
            }
        }
        else {
            otherPlayer = PlayerName.resolvePlayerByName(recipientName);
            boolean isPermissionFormat = recipientName.contains(".");
            if (otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all") && !isPermissionFormat) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                return;
            }

            if (otherPlayer == null && isPermissionFormat) {
                //player does not exist and argument has a period so this is a permission instead
                permission = recipientName;
            }
            else if (otherPlayer != null) {
                recipientName = otherPlayer.getName();
                recipientID = otherPlayer.getUniqueId();
            }
            else {
                recipientName = "public";
            }
        }

        //determine which claims should be modified
        ArrayList<Claim> targetClaims = new ArrayList<>();
        if (claim == null) {
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            targetClaims.addAll(playerData.getClaims());
        }
        else {
            //check permission here
            if (claim.checkPermission(player, ClaimPermission.Manage, null) != null) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.NoPermissionTrust, claim.getOwnerName());
                return;
            }

            //see if the player has the level of permission he's trying to grant
            Supplier<String> errorMessage;

            //permission level null indicates granting permission trust
            if (permissionLevel == null) {
                errorMessage = claim.checkPermission(player, ClaimPermission.Edit, null);
                if (errorMessage != null) {
                    errorMessage = () -> "Only " + claim.getOwnerName() + " can grant /PermissionTrust here.";
                }
            }

            //otherwise just use the ClaimPermission enum values
            else {
                errorMessage = claim.checkPermission(player, permissionLevel, null);
            }

            //error message for trying to grant a permission the player doesn't have
            if (errorMessage != null) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.CantGrantThatPermission);
                return;
            }

            targetClaims.add(claim);
        }

        //if we didn't determine which claims to modify, tell the player to be specific
        if (targetClaims.size() == 0) {
            Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.GrantPermissionNoClaim);
            return;
        }

        String identifierToAdd = recipientName;
        if (permission != null) {
            identifierToAdd = "[" + permission + "]";
            //replace recipientName as well so the success message clearly signals a permission
            recipientName = identifierToAdd;
        }
        else if (recipientID != null) {
            identifierToAdd = recipientID.toString();
        }

        //calling the event
        TrustChangedEvent event = new TrustChangedEvent(player, targetClaims, permissionLevel, true, identifierToAdd);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        //apply changes
        for (Claim currentClaim : event.getClaims()) {
            if (permissionLevel == null) {
                if (!currentClaim.managers.contains(identifierToAdd)) {
                    currentClaim.managers.add(identifierToAdd);
                }
            }
            else {
                currentClaim.setPermission(identifierToAdd, permissionLevel);
            }
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().saveClaim(currentClaim);
        }

        //notify player
        if (recipientName.equals("public")) recipientName = configManager.getMessagesConfig().getMessage(MessageType.CollectivePublic);
        String permissionDescription;
        if (permissionLevel == null) {
            permissionDescription = configManager.getMessagesConfig().getMessage(MessageType.PermissionsPermission);
        }
        else if (permissionLevel == ClaimPermission.Build) {
            permissionDescription = configManager.getMessagesConfig().getMessage(MessageType.BuildPermission);
        }
        else if (permissionLevel == ClaimPermission.Access) {
            permissionDescription = configManager.getMessagesConfig().getMessage(MessageType.AccessPermission);
        }
        //ClaimPermission.Inventory
        else {
            permissionDescription = configManager.getMessagesConfig().getMessage(MessageType.ContainersPermission);
        }

        String location;
        if (claim == null) {
            location = configManager.getMessagesConfig().getMessage(MessageType.LocationAllClaims);
        }
        else {
            location = configManager.getMessagesConfig().getMessage(MessageType.LocationCurrentClaim);
        }

        Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.GrantPermissionConfirmation, recipientName, permissionDescription, location);
    }

    public static boolean abandonClaimHandler(Player player, ConfigManager configManager, boolean deleteTopLevelClaim) {
        PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());

        //which claim is being abandoned?
        Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);
        if (claim == null) {
            Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.AbandonClaimMissing);
        }

        //verify ownership
        else if (claim.checkPermission(player, ClaimPermission.Edit, null) != null) {
            Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.NotYourClaim);
        }

        //warn if has children and we're not explicitly deleting a top level claim
        else if (claim.children.size() > 0 && !deleteTopLevelClaim) {
            Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.DeleteTopLevelClaim);
            return true;
        }
        else {
            //delete it
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaim(claim, true, false);

            //adjust claim blocks when abandoning a top level claim
            if (configManager.getSystemConfig().abandonReturnRatio != 1.0D && claim.parent == null && claim.ownerID.equals(playerData.playerID)) {
                playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - (int) Math.ceil((claim.getArea() * (1 - configManager.getSystemConfig().abandonReturnRatio))));
            }

            //tell the player how many claim blocks he has left
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AbandonSuccess, String.valueOf(remainingBlocks));

            //revert any current visualization
            playerData.setVisibleBoundaries(null);

            playerData.warnedAboutMajorDeletion = false;
        }

        return true;

    }

    public static String trustEntryToPlayerName(String entry) {
        if (entry.startsWith("[") || entry.equals("public")) {
            return entry;
        }
        else {
            return PlayerName.lookupPlayerName(entry);
        }
    }
}
