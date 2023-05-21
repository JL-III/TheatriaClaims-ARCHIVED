package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.claim.ClaimPermission;
import com.jliii.theatriaclaims.claim.CreateClaimResult;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.ShovelMode;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.events.TrustChangedEvent;
import com.jliii.theatriaclaims.listeners.EconomyHandler;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.tasks.WelcomeTask;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.database.DataStore;
import com.jliii.theatriaclaims.util.GeneralUtils;
import com.jliii.theatriaclaims.util.Messages;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.player.PlayerName;
import com.jliii.theatriaclaims.visualization.BoundaryVisualization;
import com.jliii.theatriaclaims.visualization.VisualizationType;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Supplier;


public class ChungusCommand implements CommandExecutor {

    private EconomyHandler economyHandler;
    private ConfigManager configManager;

    public ChungusCommand(EconomyHandler economyHandler, ConfigManager configManager) {
        this.economyHandler = economyHandler;
        this.configManager = configManager;
    }

    //handles slash commands
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        Player player = null;

        if (sender instanceof Player) {
            player = (Player) sender;
        }

        //claim
        if (cmd.getName().equalsIgnoreCase("claim") && player != null) {
            if (!configManager.getSystemConfig().claimsEnabledForWorld(player.getWorld())) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ClaimsDisabledWorld);
                return true;
            }

            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());

            //default is chest claim radius, unless -1
            int radius = configManager.getSystemConfig().automaticClaimsForNewPlayersRadius;
            if (radius < 0) radius = (int) Math.ceil(Math.sqrt(configManager.getSystemConfig().minArea) / 2);

            //if player has any claims, respect claim minimum size setting
            if (playerData.getClaims().size() > 0) {
                //if player has exactly one land claim, this requires the claim modification tool to be in hand (or creative mode player)
                if (playerData.getClaims().size() == 1 && player.getGameMode() != GameMode.CREATIVE && player.getItemInHand().getType() != configManager.getSystemConfig().modificationTool) {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.MustHoldModificationToolForThat);
                    return true;
                }

                radius = (int) Math.ceil(Math.sqrt(configManager.getSystemConfig().minArea) / 2);
            }

            //allow for specifying the radius
            if (args.length > 0) {
                if (playerData.getClaims().size() < 2 && player.getGameMode() != GameMode.CREATIVE && player.getItemInHand().getType() != configManager.getSystemConfig().modificationTool) {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.RadiusRequiresGoldenShovel);
                    return true;
                }

                int specifiedRadius;
                try {
                    specifiedRadius = Integer.parseInt(args[0]);
                }
                catch (NumberFormatException e) {
                    return false;
                }

                if (specifiedRadius < radius) {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.MinimumRadius, String.valueOf(radius));
                    return true;
                }
                else {
                    radius = specifiedRadius;
                }
            }

            if (radius < 0) radius = 0;

            Location lc = player.getLocation().add(-radius, 0, -radius);
            Location gc = player.getLocation().add(radius, 0, radius);

            //player must have sufficient unused claim blocks
            int area = Math.abs((gc.getBlockX() - lc.getBlockX() + 1) * (gc.getBlockZ() - lc.getBlockZ() + 1));
            int remaining = playerData.getRemainingClaimBlocks();
            if (remaining < area) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.CreateClaimInsufficientBlocks, String.valueOf(area - remaining));
                TheatriaClaims.getInstance().getDatabaseManager().getDataStore().tryAdvertiseAdminAlternatives(player);
                return true;
            }

            CreateClaimResult result = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().createClaim(lc.getWorld(),
                    lc.getBlockX(), gc.getBlockX(),
                    lc.getBlockZ(), gc.getBlockZ(),
                    player.getUniqueId(), null, null, player);
            if (!result.succeeded || result.claim == null) {
                if (result.claim != null) {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.CreateClaimFailOverlapShort);

                    BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CONFLICT_ZONE, configManager);
                }
                else {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.CreateClaimFailOverlapRegion);
                }
            }
            else {
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.CreateClaimSuccess);

                if (configManager.getSystemConfig().claimsEnabledForWorld(player.getWorld())) {
                    Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
                }
                BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CLAIM, configManager);
                playerData.claimResizing = null;
                playerData.lastShovelLocation = null;

                // AutoExtendClaimTask.scheduleAsync(result.claim);
            }

            return true;
        }

        //extendclaim
        if (cmd.getName().equalsIgnoreCase("extendclaim") && player != null) {
            if (args.length < 1) {
                if (configManager.getSystemConfig().claimsEnabledForWorld(player.getLocation().getWorld())) {
                    Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
                }
                return false;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException e) {
                if (configManager.getSystemConfig().claimsEnabledForWorld(player.getLocation().getWorld())) {
                    Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
                }
                return false;
            }

            //requires claim modification tool in hand
            if (player.getGameMode() != GameMode.CREATIVE && player.getItemInHand().getType() != configManager.getSystemConfig().modificationTool) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.MustHoldModificationToolForThat);
                return true;
            }

            //must be standing in a land claim
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true, playerData.lastClaim);
            if (claim == null) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.StandInClaimToResize);
                return true;
            }

            //must have permission to edit the land claim you're in
            Supplier<String> errorMessage = claim.checkPermission(player, ClaimPermission.Edit, null);
            if (errorMessage != null) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.NotYourClaim);
                return true;
            }

            //determine new corner coordinates
            org.bukkit.util.Vector direction = player.getLocation().getDirection();
            if (direction.getY() > .75) {
                Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.ClaimsExtendToSky);
                return true;
            }

            if (direction.getY() < -.75) {
                Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.ClaimsAutoExtendDownward);
                return true;
            }

            Location lc = claim.getLesserBoundaryCorner();
            Location gc = claim.getGreaterBoundaryCorner();
            int newx1 = lc.getBlockX();
            int newx2 = gc.getBlockX();
            int newz1 = lc.getBlockZ();
            int newz2 = gc.getBlockZ();

            //if changing Z only
            if (Math.abs(direction.getX()) < .3) {
                if (direction.getZ() > 0) {
                    newz2 += amount;  //north
                }
                else {
                    newz1 -= amount;  //south
                }
            }

            //if changing X only
            else if (Math.abs(direction.getZ()) < .3) {
                if (direction.getX() > 0) {
                    newx2 += amount;  //east
                }
                else {
                    newx1 -= amount;  //west
                }
            }

            //diagonals
            else {
                if (direction.getX() > 0) {
                    newx2 += amount;
                }
                else {
                    newx1 -= amount;
                }

                if (direction.getZ() > 0) {
                    newz2 += amount;
                }
                else {
                    newz1 -= amount;
                }
            }

            //attempt resize
            playerData.claimResizing = claim;
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().resizeClaimWithChecks(player, playerData, newx1, newx2, newz1, newz2);
            playerData.claimResizing = null;

            return true;
        }

        //abandonclaim
        if (cmd.getName().equalsIgnoreCase("abandonclaim") && player != null) {
            return abandonClaimHandler(player, false);
        }

        //abandontoplevelclaim
        if (cmd.getName().equalsIgnoreCase("abandontoplevelclaim") && player != null) {
            return abandonClaimHandler(player, true);
        }

        //ignoreclaims
        if (cmd.getName().equalsIgnoreCase("ignoreclaims") && player != null) {
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());

            playerData.ignoreClaims = !playerData.ignoreClaims;

            //toggle ignore claims mode on or off
            if (!playerData.ignoreClaims)
            {
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.RespectingClaims);
            }
            else
            {
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.IgnoringClaims);
            }

            return true;
        }

        //abandonallclaims
        else if (cmd.getName().equalsIgnoreCase("abandonallclaims") && player != null)
        {
            if (args.length > 1) return false;

            if (args.length != 1 || !"confirm".equalsIgnoreCase(args[0]))
            {
                Messages.sendMessage(player,configManager, TextMode.Err.getColor(), MessageType.ConfirmAbandonAllClaims);
                return true;
            }

            //count claims
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            int originalClaimCount = playerData.getClaims().size();

            //check count
            if (originalClaimCount == 0)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.YouHaveNoClaims);
                return true;
            }

            if (configManager.getSystemConfig().abandonReturnRatio != 1.0D)
            {
                //adjust claim blocks
                for (Claim claim : playerData.getClaims())
                {
                    playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - (int) Math.ceil((claim.getArea() * (1 - configManager.getSystemConfig().abandonReturnRatio))));
                }
            }


            //delete them
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaimsForPlayer(player.getUniqueId(), false);

            //inform the player
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.SuccessfulAbandon, String.valueOf(remainingBlocks));

            //revert any current visualization
            playerData.setVisibleBoundaries(null);

            return true;
        }

        //trust <player>
        else if (cmd.getName().equalsIgnoreCase("trust") && player != null)
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //most trust commands use this helper method, it keeps them consistent
            handleTrustCommand(player, ClaimPermission.Build, args[0]);

            return true;
        }

        //transferclaim <player>
        else if (cmd.getName().equalsIgnoreCase("transferclaim") && player != null)
        {
            //which claim is the user in?
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true, null);
            if (claim == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.TransferClaimMissing);
                return true;
            }

            //check additional permission for admin claims
            if (claim.isAdminClaim() && !player.hasPermission("TheatriaClaims.adminclaims"))
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.TransferClaimPermission);
                return true;
            }

            UUID newOwnerID = null;  //no argument = make an admin claim
            String ownerName = "admin";

            if (args.length > 0)
            {
                OfflinePlayer targetPlayer = PlayerName.resolvePlayerByName(args[0]);
                if (targetPlayer == null)
                {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                    return true;
                }
                newOwnerID = targetPlayer.getUniqueId();
                ownerName = targetPlayer.getName();
            }

            //change ownerhsip
            try
            {
                TheatriaClaims.getInstance().getDatabaseManager().getDataStore().changeClaimOwner(claim, newOwnerID);
            }
            catch (DataStore.NoTransferException e)
            {
                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.TransferTopLevel);
                return true;
            }

            //confirm
            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.TransferSuccess);
            CustomLogger.log(player.getName() + " transferred a claim at " + GeneralUtils.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + ownerName + ".");

            return true;
        }

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

        //untrust <player> or untrust [<group>]
        else if (cmd.getName().equalsIgnoreCase("untrust") && player != null)
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //determine which claim the player is standing in
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

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
                for (Claim targetClaim : event.getClaims()) {
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

        //accesstrust <player>
        else if (cmd.getName().equalsIgnoreCase("accesstrust") && player != null)
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            handleTrustCommand(player, ClaimPermission.Access, args[0]);

            return true;
        }

        //containertrust <player>
        else if (cmd.getName().equalsIgnoreCase("containertrust") && player != null)
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            handleTrustCommand(player, ClaimPermission.Inventory, args[0]);

            return true;
        }

        //permissiontrust <player>
        else if (cmd.getName().equalsIgnoreCase("permissiontrust") && player != null)
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            handleTrustCommand(player, null, args[0]);  //null indicates permissiontrust to the helper method

            return true;
        }

        //restrictsubclaim
        else if (cmd.getName().equalsIgnoreCase("restrictsubclaim") && player != null)
        {
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true, playerData.lastClaim);
            if (claim == null || claim.parent == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.StandInSubclaim);
                return true;
            }

            // If player has /ignoreclaims on, continue
            // If admin claim, fail if this user is not an admin
            // If not an admin claim, fail if this user is not the owner
            if (!playerData.ignoreClaims && (claim.isAdminClaim() ? !player.hasPermission("TheatriaClaims.adminclaims") : !player.getUniqueId().equals(claim.parent.ownerID)))
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.OnlyOwnersModifyClaims, claim.getOwnerName());
                return true;
            }

            if (claim.getSubclaimRestrictions())
            {
                claim.setSubclaimRestrictions(false);
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.SubclaimUnrestricted);
            }
            else
            {
                claim.setSubclaimRestrictions(true);
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.SubclaimRestricted);
            }
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().saveClaim(claim);
            return true;
        }

        //buyclaimblocks
        else if (cmd.getName().equalsIgnoreCase("buyclaimblocks") && player != null)
        {
            //if economy is disabled, don't do anything
            EconomyHandler.EconomyWrapper economyWrapper = economyHandler.getWrapper();
            if (economyWrapper == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.BuySellNotConfigured);
                return true;
            }

            if (!player.hasPermission("TheatriaClaims.buysellclaimblocks"))
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.NoPermissionForCommand);
                return true;
            }

            //if purchase disabled, send error message
            if (configManager.getEconomyConfig().economy_claimBlocksPurchaseCost == 0) {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.OnlySellBlocks);
                return true;
            }

            Economy economy = economyWrapper.getEconomy();

            //if no parameter, just tell player cost per block and balance
            if (args.length != 1)
            {
                Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.BlockPurchaseCost, String.valueOf(configManager.getEconomyConfig().economy_claimBlocksPurchaseCost), String.valueOf(economy.getBalance(player)));
                return false;
            }
            else
            {
                PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());

                //try to parse number of blocks
                int blockCount;
                try
                {
                    blockCount = Integer.parseInt(args[0]);
                }
                catch (NumberFormatException numberFormatException)
                {
                    return false;  //causes usage to be displayed
                }

                if (blockCount <= 0)
                {
                    return false;
                }

                //if the player can't afford his purchase, send error message
                double balance = economy.getBalance(player);
                double totalCost = blockCount * configManager.getEconomyConfig().economy_claimBlocksPurchaseCost;
                if (totalCost > balance)
                {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.InsufficientFunds, String.valueOf(totalCost), String.valueOf(balance));
                }

                //otherwise carry out transaction
                else
                {
                    int newBonusClaimBlocks = playerData.getBonusClaimBlocks() + blockCount;

                    //if the player is going to reach max bonus limit, send error message
                    int bonusBlocksLimit = configManager.getEconomyConfig().economy_claimBlocksMaxBonus;
                    if (bonusBlocksLimit != 0 && newBonusClaimBlocks > bonusBlocksLimit)
                    {
                        Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.MaxBonusReached, String.valueOf(blockCount), String.valueOf(bonusBlocksLimit));
                        return true;
                    }

                    //withdraw cost
                    economy.withdrawPlayer(player, totalCost);

                    //add blocks
                    playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + blockCount);
                    TheatriaClaims.getInstance().getDatabaseManager().getDataStore().savePlayerData(player.getUniqueId(), playerData);

                    //inform player
                    Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.PurchaseConfirmation, String.valueOf(totalCost), String.valueOf(playerData.getRemainingClaimBlocks()));
                }

                return true;
            }
        }

        //sellclaimblocks <amount>
        else if (cmd.getName().equalsIgnoreCase("sellclaimblocks") && player != null)
        {
            //if economy is disabled, don't do anything
            EconomyHandler.EconomyWrapper economyWrapper = economyHandler.getWrapper();
            if (economyWrapper == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.BuySellNotConfigured);
                return true;
            }

            if (!player.hasPermission("TheatriaClaims.buysellclaimblocks"))
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.NoPermissionForCommand);
                return true;
            }

            //if disabled, error message
            if (configManager.getEconomyConfig().economy_claimBlocksSellValue == 0)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.OnlyPurchaseBlocks);
                return true;
            }

            //load player data
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            int availableBlocks = playerData.getRemainingClaimBlocks();

            //if no amount provided, just tell player value per block sold, and how many he can sell
            if (args.length != 1)
            {
                Messages.sendMessage(player, configManager, TextMode.Info.getColor(), MessageType.BlockSaleValue, String.valueOf(configManager.getEconomyConfig().economy_claimBlocksSellValue), String.valueOf(availableBlocks));
                return false;
            }

            //parse number of blocks
            int blockCount;
            try
            {
                blockCount = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException numberFormatException)
            {
                return false;  //causes usage to be displayed
            }

            if (blockCount <= 0)
            {
                return false;
            }

            //if he doesn't have enough blocks, tell him so
            if (blockCount > availableBlocks)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.NotEnoughBlocksForSale);
            }

            //otherwise carry out the transaction
            else
            {
                //compute value and deposit it
                double totalValue = blockCount * configManager.getEconomyConfig().economy_claimBlocksSellValue;
                economyWrapper.getEconomy().depositPlayer(player, totalValue);

                //subtract blocks
                playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() - blockCount);
                TheatriaClaims.getInstance().getDatabaseManager().getDataStore().savePlayerData(player.getUniqueId(), playerData);

                //inform player
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.BlockSaleConfirmation, String.valueOf(totalValue), String.valueOf(playerData.getRemainingClaimBlocks()));
            }

            return true;
        }

        //adminclaims
        else if (cmd.getName().equalsIgnoreCase("adminclaims") && player != null)
        {
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            playerData.shovelMode = ShovelMode.Admin;
            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AdminClaimsMode);

            return true;
        }

        //basicclaims
        else if (cmd.getName().equalsIgnoreCase("basicclaims") && player != null)
        {
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            playerData.shovelMode = ShovelMode.Basic;
            playerData.claimSubdividing = null;
            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.BasicClaimsMode);

            return true;
        }

        //subdivideclaims
        else if (cmd.getName().equalsIgnoreCase("subdivideclaims") && player != null)
        {
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
            playerData.shovelMode = ShovelMode.Subdivide;
            playerData.claimSubdividing = null;
            Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SubdivisionMode);
            Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SubdivisionVideo2, DataStore.SUBDIVISION_VIDEO_URL);

            return true;
        }

        //deleteclaim
        else if (cmd.getName().equalsIgnoreCase("deleteclaim") && player != null)
        {
            //determine which claim the player is standing in
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

            if (claim == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.DeleteClaimMissing);
            }
            else
            {
                //deleting an admin claim additionally requires the adminclaims permission
                if (!claim.isAdminClaim() || player.hasPermission("TheatriaClaims.adminclaims")) {
                    PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
                    if (claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion) {
                        Messages.sendMessage(player, configManager, TextMode.Warn.getColor(), MessageType.DeletionSubdivisionWarning);
                        playerData.warnedAboutMajorDeletion = true;
                    }
                    else {
                        // claim.removeSurfaceFluids(null);
                        TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaim(claim, true, true);
                        Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.DeleteSuccess);
                        CustomLogger.log(player.getName() + " deleted " + claim.getOwnerName() + "'s claim at " + GeneralUtils.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                        //revert any current visualization
                        playerData.setVisibleBoundaries(null);
                        playerData.warnedAboutMajorDeletion = false;
                    }
                }
                else
                {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.CantDeleteAdminClaim);
                }
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("claimexplosions") && player != null)
        {
            //determine which claim the player is standing in
            Claim claim = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

            if (claim == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.DeleteClaimMissing);
            }
            else
            {
                Supplier<String> noBuildReason = claim.checkPermission(player, ClaimPermission.Build, null);
                if (noBuildReason != null)
                {
                    Messages.sendMessage(player, TextMode.Err.getColor(), noBuildReason.get());
                    return true;
                }

                if (claim.areExplosivesAllowed)
                {
                    claim.areExplosivesAllowed = false;
                    Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.ExplosivesDisabled);
                }
                else
                {
                    claim.areExplosivesAllowed = true;
                    Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.ExplosivesEnabled);
                }
            }

            return true;
        }

        //deleteallclaims <player>
        else if (cmd.getName().equalsIgnoreCase("deleteallclaims"))
        {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //try to find that player
            OfflinePlayer otherPlayer = PlayerName.resolvePlayerByName(args[0]);
            if (otherPlayer == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                return true;
            }

            //delete all that player's claims
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaimsForPlayer(otherPlayer.getUniqueId(), true);

            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.DeleteAllSuccess, otherPlayer.getName());
            if (player != null)
            {
                CustomLogger.log(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".");

                //revert any current visualization
                if (player.isOnline())
                {
                    TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId()).setVisibleBoundaries(null);
                }
            }

            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("deleteclaimsinworld"))
        {
            //must be executed at the console
            if (player != null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ConsoleOnlyCommand);
                return true;
            }

            //requires exactly one parameter, the world name
            if (args.length != 1) return false;

            //try to find the specified world
            World world = Bukkit.getServer().getWorld(args[0]);
            if (world == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.WorldNotFound);
                return true;
            }

            //delete all claims in that world
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaimsInWorld(world, true);
            CustomLogger.log("Deleted all claims in world: " + world.getName() + ".");
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("deleteuserclaimsinworld"))
        {
            //must be executed at the console
            if (player != null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ConsoleOnlyCommand);
                return true;
            }

            //requires exactly one parameter, the world name
            if (args.length != 1) return false;

            //try to find the specified world
            World world = Bukkit.getServer().getWorld(args[0]);
            if (world == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.WorldNotFound);
                return true;
            }

            //delete all USER claims in that world
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaimsInWorld(world, false);
            CustomLogger.log("Deleted all user claims in world: " + world.getName() + ".");
            return true;
        }

        //claimbook
        else if (cmd.getName().equalsIgnoreCase("claimbook"))
        {
            //requires one parameter
            if (args.length != 1) return false;

            //try to find the specified player
            Player otherPlayer = TheatriaClaims.getInstance().getServer().getPlayer(args[0]);
            if (otherPlayer == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                return true;
            }
            else {
                WelcomeTask task = new WelcomeTask(otherPlayer, configManager);
                task.run();
                return true;
            }
        }

        //claimslist or claimslist <player>
        else if (cmd.getName().equalsIgnoreCase("claimslist"))
        {
            //at most one parameter
            if (args.length > 1) return false;

            //player whose claims will be listed
            OfflinePlayer otherPlayer;

            //if another player isn't specified, assume current player
            if (args.length < 1)
            {
                if (player != null)
                    otherPlayer = player;
                else
                    return false;
            }

            //otherwise if no permission to delve into another player's claims data
            else if (player != null && !player.hasPermission("TheatriaClaims.claimslistother"))
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.ClaimsListNoPermission);
                return true;
            }

            //otherwise try to find the specified player
            else
            {
                otherPlayer = PlayerName.resolvePlayerByName(args[0]);
                if (otherPlayer == null)
                {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                    return true;
                }
            }

            //load the target player's data
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(otherPlayer.getUniqueId());
            Vector<Claim> claims = playerData.getClaims();
            Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.StartBlockMath,
                    String.valueOf(playerData.getAccruedClaimBlocks()),
                    String.valueOf((playerData.getBonusClaimBlocks() + TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getGroupBonusBlocks(otherPlayer.getUniqueId()))),
                    String.valueOf((playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks() + TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getGroupBonusBlocks(otherPlayer.getUniqueId()))));
            if (claims.size() > 0)
            {
                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.ClaimsListHeader);
                for (int i = 0; i < playerData.getClaims().size(); i++)
                {
                    Claim claim = playerData.getClaims().get(i);
                    Messages.sendMessage(player, TextMode.Instr.getColor(), GeneralUtils.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + configManager.getMessagesConfig().getMessage(MessageType.ContinueBlockMath, String.valueOf(claim.getArea())));
                }

                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.EndBlockMath, String.valueOf(playerData.getRemainingClaimBlocks()));
            }

            //drop the data we just loaded, if the player isn't online
            if (!otherPlayer.isOnline())
                TheatriaClaims.getInstance().getDatabaseManager().getDataStore().clearCachedPlayerData(otherPlayer.getUniqueId());

            return true;
        }

        //adminclaimslist
        else if (cmd.getName().equalsIgnoreCase("adminclaimslist"))
        {
            //find admin claims
            Vector<Claim> claims = new Vector<>();
            for (Claim claim : TheatriaClaims.getInstance().getDatabaseManager().getDataStore().claims)
            {
                if (claim.ownerID == null)  //admin claim
                {
                    claims.add(claim);
                }
            }
            if (claims.size() > 0)
            {
                Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.ClaimsListHeader);
                for (Claim claim : claims) {
                    Messages.sendMessage(player, TextMode.Instr.getColor(), GeneralUtils.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                }
            }

            return true;
        }

        //unlockItems
        else if (cmd.getName().equalsIgnoreCase("unlockdrops") && player != null)
        {
            PlayerData playerData;

            if (player.hasPermission("TheatriaClaims.unlockothersdrops") && args.length == 1)
            {
                Player otherPlayer = Bukkit.getPlayer(args[0]);
                if (otherPlayer == null)
                {
                    Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                    return true;
                }

                playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(otherPlayer.getUniqueId());
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.DropUnlockOthersConfirmation, otherPlayer.getName());
            }
            else
            {
                playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.DropUnlockConfirmation);
            }

            playerData.dropsAreUnlocked = true;

            return true;
        }

        //deletealladminclaims
        else if (player != null && cmd.getName().equalsIgnoreCase("deletealladminclaims"))
        {
            if (!player.hasPermission("TheatriaClaims.deleteclaims"))
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.NoDeletePermission);
                return true;
            }

            //delete all admin claims
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().deleteClaimsForPlayer(null, true);  //null for owner id indicates an administrative claim

            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AllAdminDeleted);
            if (player != null)
            {
                CustomLogger.log(player.getName() + " deleted all administrative claims.");

                //revert any current visualization
                TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId()).setVisibleBoundaries(null);
            }

            return true;
        }

        //adjustbonusclaimblocks <player> <amount> or [<permission>] amount
        else if (cmd.getName().equalsIgnoreCase("adjustbonusclaimblocks"))
        {
            //requires exactly two parameters, the other player or group's name and the adjustment
            if (args.length != 2) return false;

            //parse the adjustment amount
            int adjustment;
            try
            {
                adjustment = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException numberFormatException)
            {
                return false;  //causes usage to be displayed
            }

            //if granting blocks to all players with a specific permission
            if (args[0].startsWith("[") && args[0].endsWith("]"))
            {
                String permissionIdentifier = args[0].substring(1, args[0].length() - 1);
                int newTotal = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().adjustGroupBonusBlocks(permissionIdentifier, adjustment);

                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AdjustGroupBlocksSuccess, permissionIdentifier, String.valueOf(adjustment), String.valueOf(newTotal));
                if (player != null)
                    CustomLogger.log(player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");

                return true;
            }

            //otherwise, find the specified player
            OfflinePlayer targetPlayer;
            try
            {
                UUID playerID = UUID.fromString(args[0]);
                targetPlayer = TheatriaClaims.getInstance().getServer().getOfflinePlayer(playerID);

            }
            catch (IllegalArgumentException e)
            {
                targetPlayer = PlayerName.resolvePlayerByName(args[0]);
            }

            if (targetPlayer == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                return true;
            }

            //give blocks to player
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(targetPlayer.getUniqueId());
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().savePlayerData(targetPlayer.getUniqueId(), playerData);

            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment), String.valueOf(playerData.getBonusClaimBlocks()));
            if (player != null)
                CustomLogger.log(player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".");

            return true;
        }

        //adjustbonusclaimblocksall <amount>
        else if (cmd.getName().equalsIgnoreCase("adjustbonusclaimblocksall"))
        {
            //requires exactly one parameter, the amount of adjustment
            if (args.length != 1) return false;

            //parse the adjustment amount
            int adjustment;
            try
            {
                adjustment = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException numberFormatException)
            {
                return false;  //causes usage to be displayed
            }

            //for each online player
            @SuppressWarnings("unchecked")
            Collection<Player> players = (Collection<Player>) TheatriaClaims.getInstance().getServer().getOnlinePlayers();
            StringBuilder builder = new StringBuilder();
            for (Player onlinePlayer : players)
            {
                UUID playerID = onlinePlayer.getUniqueId();
                PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(playerID);
                playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
                TheatriaClaims.getInstance().getDatabaseManager().getDataStore().savePlayerData(playerID, playerData);
                builder.append(onlinePlayer.getName()).append(' ');
            }

            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.AdjustBlocksAllSuccess, String.valueOf(adjustment));
            CustomLogger.log("Adjusted all " + players.size() + "players' bonus claim blocks by " + adjustment + ".  " + builder.toString());

            return true;
        }

        //setaccruedclaimblocks <player> <amount>
        else if (cmd.getName().equalsIgnoreCase("setaccruedclaimblocks"))
        {
            //requires exactly two parameters, the other player's name and the new amount
            if (args.length != 2) return false;

            //parse the adjustment amount
            int newAmount;
            try
            {
                newAmount = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException numberFormatException)
            {
                return false;  //causes usage to be displayed
            }

            //find the specified player
            OfflinePlayer targetPlayer = PlayerName.resolvePlayerByName(args[0]);
            if (targetPlayer == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                return true;
            }

            //set player's blocks
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(targetPlayer.getUniqueId());
            playerData.setAccruedClaimBlocks(newAmount);
            TheatriaClaims.getInstance().getDatabaseManager().getDataStore().savePlayerData(targetPlayer.getUniqueId(), playerData);

            Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.SetClaimBlocksSuccess);
            if (player != null)
                CustomLogger.log(player.getName() + " set " + targetPlayer.getName() + "'s accrued claim blocks to " + newAmount + ".");

            return true;
        }

        // else if (cmd.getName().equalsIgnoreCase("softmute"))
        // {
        //     //requires one parameter
        //     if (args.length != 1) return false;

        //     //find the specified player
        //     OfflinePlayer targetPlayer = PlayerName.resolvePlayerByName(args[0]);
        //     if (targetPlayer == null)
        //     {
        //         Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
        //         return true;
        //     }

        //     //toggle mute for player
        //     boolean isMuted = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().toggleSoftMute(targetPlayer.getUniqueId());
        //     if (isMuted)
        //     {
        //         Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.SoftMuted, targetPlayer.getName());
        //         String executorName = "console";
        //         if (player != null)
        //         {
        //             executorName = player.getName();
        //         }

        //         CustomLogger.log(executorName + " muted " + targetPlayer.getName() + ".");
        //     }
        //     else
        //     {
        //         Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.UnSoftMuted, targetPlayer.getName());
        //     }

        //     return true;
        // }
        else if (cmd.getName().equalsIgnoreCase("gpreload"))
        {
            configManager.loadConfig();
            configManager.getMessagesConfig().loadMessages();
            if (player != null)
            {
                Messages.sendMessage(player, TextMode.Success.getColor(), "Configuration updated.  If you have updated your Grief Prevention JAR, you still need to /reload or reboot your server.");
            }
            else
            {
                CustomLogger.log("Configuration updated.  If you have updated your Grief Prevention JAR, you still need to /reload or reboot your server.");
            }

            return true;
        }

        //givepet
        else if (cmd.getName().equalsIgnoreCase("givepet") && player != null)
        {
            //requires one parameter
            if (args.length < 1) return false;

            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(player.getUniqueId());

            //special case: cancellation
            if (args[0].equalsIgnoreCase("cancel"))
            {
                playerData.petGiveawayRecipient = null;
                Messages.sendMessage(player, configManager, TextMode.Success.getColor(), MessageType.PetTransferCancellation);
                return true;
            }

            //find the specified player
            OfflinePlayer targetPlayer = PlayerName.resolvePlayerByName(args[0]);
            if (targetPlayer == null)
            {
                Messages.sendMessage(player, configManager, TextMode.Err.getColor(), MessageType.PlayerNotFound2);
                return true;
            }

            //remember the player's ID for later pet transfer
            playerData.petGiveawayRecipient = targetPlayer;

            //send instructions
            Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.ReadyToTransferPet);

            return true;
        }

        //gpblockinfo
        else if (cmd.getName().equalsIgnoreCase("gpblockinfo") && player != null)
        {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            player.sendMessage("In Hand: " + inHand.getType().name());

            Block inWorld = player.getTargetBlockExact(300, FluidCollisionMode.ALWAYS);
            if (inWorld == null) inWorld = player.getEyeLocation().getBlock();
            player.sendMessage("In World: " + inWorld.getType().name());

            return true;
        }
        return false;
    }

    //helper method keeps the trust commands consistent and eliminates duplicate code
    public void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) {
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

    public boolean abandonClaimHandler(Player player, boolean deleteTopLevelClaim) {
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

    public String trustEntryToPlayerName(String entry) {
        if (entry.startsWith("[") || entry.equals("public")) {
            return entry;
        }
        else {
            return PlayerName.lookupPlayerName(entry);
        }
    }
}
