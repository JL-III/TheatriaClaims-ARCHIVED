package com.jliii.theatriaclaims.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.CustomizableMessage;

public class MessagesConfig {

    private String[] messages;

    private CustomLogger customLogger;
    //TODO dont hardcode these values
    protected final static String dataLayerFolderPath = "plugins" + File.separator + "TheatriaClaimsData";

    final static String messagesFilePath = dataLayerFolderPath + File.separator + "messages.yml";


    public MessagesConfig(CustomLogger customLogger) {
        //load up all the messages from messages.yml
        this.customLogger = customLogger;
        this.loadMessages();
        customLogger.log("Customizable messages loaded.");
    }

    
    public void loadMessages() {
        MessageType[] messageIDs = MessageType.values();

        //in-memory cache for messages

        messages = new String[MessageType.values().length];

        HashMap<String, CustomizableMessage> defaults = new HashMap<>();

        //initialize defaults
        this.addDefault(defaults, MessageType.RespectingClaims, "Now respecting claims.", null);
        this.addDefault(defaults, MessageType.IgnoringClaims, "Now ignoring claims.", null);
        this.addDefault(defaults, MessageType.NoCreativeUnClaim, "You can't unclaim this land.  You can only make this claim larger or create additional claims.", null);
        this.addDefault(defaults, MessageType.SuccessfulAbandon, "Claims abandoned.  You now have {0} available claim blocks.", "0: remaining blocks");
        this.addDefault(defaults, MessageType.RestoreNatureActivate, "Ready to restore some nature!  Right click to restore nature, and use /BasicClaims to stop.", null);
        this.addDefault(defaults, MessageType.RestoreNatureAggressiveActivate, "Aggressive mode activated.  Do NOT use this underneath anything you want to keep!  Right click to aggressively restore nature, and use /BasicClaims to stop.", null);
        this.addDefault(defaults, MessageType.FillModeActive, "Fill mode activated with radius {0}.  Right click an area to fill.", "0: fill radius");
        this.addDefault(defaults, MessageType.TransferClaimPermission, "That command requires the administrative claims permission.", null);
        this.addDefault(defaults, MessageType.TransferClaimMissing, "There's no claim here.  Stand in the administrative claim you want to transfer.", null);
        this.addDefault(defaults, MessageType.TransferClaimAdminOnly, "Only administrative claims may be transferred to a player.", null);
        this.addDefault(defaults, MessageType.PlayerNotFound2, "No player by that name has logged in recently.", null);
        this.addDefault(defaults, MessageType.TransferTopLevel, "Only top level claims (not subdivisions) may be transferred.  Stand outside of the subdivision and try again.", null);
        this.addDefault(defaults, MessageType.TransferSuccess, "Claim transferred.", null);
        this.addDefault(defaults, MessageType.TrustListNoClaim, "Stand inside the claim you're curious about.", null);
        this.addDefault(defaults, MessageType.ClearPermsOwnerOnly, "Only the claim owner can clear all permissions.", null);
        this.addDefault(defaults, MessageType.UntrustIndividualAllClaims, "Revoked {0}'s access to ALL your claims.  To set permissions for a single claim, stand inside it.", "0: untrusted player");
        this.addDefault(defaults, MessageType.UntrustEveryoneAllClaims, "Cleared permissions in ALL your claims.  To set permissions for a single claim, stand inside it.", null);
        this.addDefault(defaults, MessageType.NoPermissionTrust, "You don't have {0}'s permission to manage permissions here.", "0: claim owner's name");
        this.addDefault(defaults, MessageType.ClearPermissionsOneClaim, "Cleared permissions in this claim.  To set permission for ALL your claims, stand outside them.", null);
        this.addDefault(defaults, MessageType.UntrustIndividualSingleClaim, "Revoked {0}'s access to this claim.  To set permissions for a ALL your claims, stand outside them.", "0: untrusted player");
        this.addDefault(defaults, MessageType.OnlySellBlocks, "Claim blocks may only be sold, not purchased.", null);
        this.addDefault(defaults, MessageType.BlockPurchaseCost, "Each claim block costs {0}.  Your balance is {1}.", "0: cost of one block; 1: player's account balance");
        this.addDefault(defaults, MessageType.ClaimBlockLimit, "You've reached your claim block limit.  You can't purchase more.", null);
        this.addDefault(defaults, MessageType.InsufficientFunds, "You don't have enough money.  You need {0}, but you only have {1}.", "0: total cost; 1: player's account balance");
        this.addDefault(defaults, MessageType.MaxBonusReached, "Can't purchase {0} more claim blocks. The server has a limit of {1} bonus claim blocks.", "0: block count; 1: bonus claims limit");
        this.addDefault(defaults, MessageType.PurchaseConfirmation, "Withdrew {0} from your account.  You now have {1} available claim blocks.", "0: total cost; 1: remaining blocks");
        this.addDefault(defaults, MessageType.OnlyPurchaseBlocks, "Claim blocks may only be purchased, not sold.", null);
        this.addDefault(defaults, MessageType.BlockSaleValue, "Each claim block is worth {0}.  You have {1} available for sale.", "0: block value; 1: available blocks");
        this.addDefault(defaults, MessageType.NotEnoughBlocksForSale, "You don't have that many claim blocks available for sale.", null);
        this.addDefault(defaults, MessageType.BlockSaleConfirmation, "Deposited {0} in your account.  You now have {1} available claim blocks.", "0: amount deposited; 1: remaining blocks");
        this.addDefault(defaults, MessageType.AdminClaimsMode, "Administrative claims mode active.  Any claims created will be free and editable by other administrators.", null);
        this.addDefault(defaults, MessageType.BasicClaimsMode, "Returned to basic claim creation mode.", null);
        this.addDefault(defaults, MessageType.SubdivisionMode, "Subdivision mode.  Use your shovel to create subdivisions in your existing claims.  Use /basicclaims to exit.", null);
        this.addDefault(defaults, MessageType.SubdivisionVideo2, "Click for Subdivision Help: {0}", "0:video URL");
        this.addDefault(defaults, MessageType.DeleteClaimMissing, "There's no claim here.", null);
        this.addDefault(defaults, MessageType.DeletionSubdivisionWarning, "This claim includes subdivisions.  If you're sure you want to delete it, use /DeleteClaim again.", null);
        this.addDefault(defaults, MessageType.DeleteSuccess, "Claim deleted.", null);
        this.addDefault(defaults, MessageType.CantDeleteAdminClaim, "You don't have permission to delete administrative claims.", null);
        this.addDefault(defaults, MessageType.DeleteAllSuccess, "Deleted all of {0}'s claims.", "0: owner's name");
        this.addDefault(defaults, MessageType.NoDeletePermission, "You don't have permission to delete claims.", null);
        this.addDefault(defaults, MessageType.AllAdminDeleted, "Deleted all administrative claims.", null);
        this.addDefault(defaults, MessageType.AdjustBlocksSuccess, "Adjusted {0}'s bonus claim blocks by {1}.  New total bonus blocks: {2}.", "0: player; 1: adjustment; 2: new total");
        this.addDefault(defaults, MessageType.AdjustBlocksAllSuccess, "Adjusted all online players' bonus claim blocks by {0}.", "0: adjustment amount");
        this.addDefault(defaults, MessageType.NotTrappedHere, "You can build here.  Save yourself.", null);
        this.addDefault(defaults, MessageType.RescuePending, "If you stay put for 10 seconds, you'll be teleported out.  Please wait.", null);
        this.addDefault(defaults, MessageType.AbandonClaimMissing, "Stand in the claim you want to delete, or consider /AbandonAllClaims.", null);
        this.addDefault(defaults, MessageType.NotYourClaim, "This isn't your claim.", null);
        this.addDefault(defaults, MessageType.DeleteTopLevelClaim, "To delete a subdivision, stand inside it.  Otherwise, use /AbandonTopLevelClaim to delete this claim and all subdivisions.", null);
        this.addDefault(defaults, MessageType.AbandonSuccess, "Claim abandoned.  You now have {0} available claim blocks.", "0: remaining claim blocks");
        this.addDefault(defaults, MessageType.ConfirmAbandonAllClaims, "Are you sure you want to abandon ALL of your claims?  Please confirm with /AbandonAllClaims confirm", null);
        this.addDefault(defaults, MessageType.CantGrantThatPermission, "You can't grant a permission you don't have yourself.", null);
        this.addDefault(defaults, MessageType.GrantPermissionNoClaim, "Stand inside the claim where you want to grant permission.", null);
        this.addDefault(defaults, MessageType.GrantPermissionConfirmation, "Granted {0} permission to {1} {2}.", "0: target player; 1: permission description; 2: scope (changed claims)");
        this.addDefault(defaults, MessageType.ManageUniversalPermissionsInstruction, "To manage permissions for ALL your claims, stand outside them.", null);
        this.addDefault(defaults, MessageType.ManageOneClaimPermissionsInstruction, "To manage permissions for a specific claim, stand inside it.", null);
        this.addDefault(defaults, MessageType.CollectivePublic, "the public", "as in 'granted the public permission to...'");
        this.addDefault(defaults, MessageType.BuildPermission, "build", null);
        this.addDefault(defaults, MessageType.ContainersPermission, "access containers and animals", null);
        this.addDefault(defaults, MessageType.AccessPermission, "use buttons and levers", null);
        this.addDefault(defaults, MessageType.PermissionsPermission, "manage permissions", null);
        this.addDefault(defaults, MessageType.LocationCurrentClaim, "in this claim", null);
        this.addDefault(defaults, MessageType.LocationAllClaims, "in all your claims", null);
        this.addDefault(defaults, MessageType.DonateItemsInstruction, "To give away the item(s) in your hand, left-click the chest again.", null);
        this.addDefault(defaults, MessageType.ChestFull, "This chest is full.", null);
        this.addDefault(defaults, MessageType.DonationSuccess, "Item(s) transferred to chest!", null);
        this.addDefault(defaults, MessageType.PlayerTooCloseForFire2, "You can't start a fire this close to another player.", null);
        this.addDefault(defaults, MessageType.TooDeepToClaim, "This chest can't be protected because it's too deep underground.  Consider moving it.", null);
        this.addDefault(defaults, MessageType.ChestClaimConfirmation, "This chest is protected.", null);
        this.addDefault(defaults, MessageType.AutomaticClaimNotification, "This chest and nearby blocks are protected from breakage and theft.", null);
        this.addDefault(defaults, MessageType.AutomaticClaimOtherClaimTooClose, "Cannot create a claim for your chest, there is another claim too close!", null);
        this.addDefault(defaults, MessageType.UnprotectedChestWarning, "This chest is NOT protected.  Consider using a golden shovel to expand an existing claim or to create a new one.", null);
        this.addDefault(defaults, MessageType.NoDamageClaimedEntity, "That belongs to {0}.", "0: owner name");
        this.addDefault(defaults, MessageType.ShovelBasicClaimMode, "Shovel returned to basic claims mode.", null);
        this.addDefault(defaults, MessageType.RemainingBlocks, "You may claim up to {0} more blocks.", "0: remaining blocks");
        this.addDefault(defaults, MessageType.CreativeBasicsVideo2, "Click for Land Claim Help: {0}", "{0}: video URL");
        this.addDefault(defaults, MessageType.SurvivalBasicsVideo2, "Click for Land Claim Help: {0}", "{0}: video URL");
        this.addDefault(defaults, MessageType.TrappedChatKeyword, "trapped;stuck", "When mentioned in chat, players get information about the /trapped command (multiple words can be separated with semi-colons)");
        this.addDefault(defaults, MessageType.TrappedInstructions, "Are you trapped in someone's land claim?  Try the /trapped command.", null);
        this.addDefault(defaults, MessageType.NoBedPermission, "{0} hasn't given you permission to sleep here.", "0: claim owner");
        this.addDefault(defaults, MessageType.NoWildernessBuckets, "You may only dump buckets inside your claim(s) or underground.", null);
        this.addDefault(defaults, MessageType.NoLavaNearOtherPlayer, "You can't place lava this close to {0}.", "0: nearby player");
        this.addDefault(defaults, MessageType.TooFarAway, "That's too far away.", null);
        this.addDefault(defaults, MessageType.BlockNotClaimed, "No one has claimed this block.", null);
        this.addDefault(defaults, MessageType.BlockClaimed, "That block has been claimed by {0}.", "0: claim owner");
        this.addDefault(defaults, MessageType.RestoreNaturePlayerInChunk, "Unable to restore.  {0} is in that chunk.", "0: nearby player");
        this.addDefault(defaults, MessageType.NoCreateClaimPermission, "You don't have permission to claim land.", null);
        this.addDefault(defaults, MessageType.ResizeClaimTooNarrow, "This new size would be too small.  Claims must be at least {0} blocks wide.", "0: minimum claim width");
        this.addDefault(defaults, MessageType.ResizeNeedMoreBlocks, "You don't have enough blocks for this size.  You need {0} more.", "0: how many needed");
        this.addDefault(defaults, MessageType.ClaimResizeSuccess, "Claim resized.  {0} available claim blocks remaining.", "0: remaining blocks");
        this.addDefault(defaults, MessageType.ResizeFailOverlap, "Can't resize here because it would overlap another nearby claim.", null);
        this.addDefault(defaults, MessageType.ResizeStart, "Resizing claim.  Use your shovel again at the new location for this corner.", null);
        this.addDefault(defaults, MessageType.ResizeFailOverlapSubdivision, "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use your shovel at a corner to resize it.", null);
        this.addDefault(defaults, MessageType.SubdivisionStart, "Subdivision corner set!  Use your shovel at the location for the opposite corner of this new subdivision.", null);
        this.addDefault(defaults, MessageType.CreateSubdivisionOverlap, "Your selected area overlaps another subdivision.", null);
        this.addDefault(defaults, MessageType.SubdivisionSuccess, "Subdivision created!  Use /trust to share it with friends.", null);
        this.addDefault(defaults, MessageType.CreateClaimFailOverlap, "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a corner to resize it.", null);
        this.addDefault(defaults, MessageType.CreateClaimFailOverlapOtherPlayer, "You can't create a claim here because it would overlap {0}'s claim.", "0: other claim owner");
        this.addDefault(defaults, MessageType.ClaimsDisabledWorld, "Land claims are disabled in this world.", null);
        this.addDefault(defaults, MessageType.ClaimStart, "Claim corner set!  Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.", null);
        this.addDefault(defaults, MessageType.NewClaimTooNarrow, "This claim would be too small.  Any claim must be at least {0} blocks wide.", "0: minimum claim width");
        this.addDefault(defaults, MessageType.ResizeClaimInsufficientArea, "This claim would be too small.  Any claim must use at least {0} total claim blocks.", "0: minimum claim area");
        this.addDefault(defaults, MessageType.CreateClaimInsufficientBlocks, "You don't have enough blocks to claim that entire area.  You need {0} more blocks.", "0: additional blocks needed");
        this.addDefault(defaults, MessageType.AbandonClaimAdvertisement, "To delete another claim and free up some blocks, use /AbandonClaim.", null);
        this.addDefault(defaults, MessageType.CreateClaimFailOverlapShort, "Your selected area overlaps an existing claim.", null);
        this.addDefault(defaults, MessageType.CreateClaimSuccess, "Claim created!  Use /trust to share it with friends.", null);
        this.addDefault(defaults, MessageType.RescueAbortedMoved, "You moved!  Rescue cancelled.", null);
        this.addDefault(defaults, MessageType.OnlyOwnersModifyClaims, "Only {0} can modify this claim.", "0: owner name");
        this.addDefault(defaults, MessageType.NoBuildPermission, "You don't have {0}'s permission to build here.", "0: owner name");
        this.addDefault(defaults, MessageType.NoAccessPermission, "You don't have {0}'s permission to use that.", "0: owner name.  access permission controls buttons, levers, and beds");
        this.addDefault(defaults, MessageType.NoContainersPermission, "You don't have {0}'s permission to use that.", "0: owner's name.  containers also include crafting blocks");
        this.addDefault(defaults, MessageType.OwnerNameForAdminClaims, "an administrator", "as in 'You don't have an administrator's permission to build here.'");
        this.addDefault(defaults, MessageType.ClaimTooSmallForEntities, "This claim isn't big enough for that.  Try enlarging it.", null);
        this.addDefault(defaults, MessageType.TooManyEntitiesInClaim, "This claim has too many entities already.  Try enlarging the claim or removing some animals, monsters, paintings, or minecarts.", null);
        this.addDefault(defaults, MessageType.YouHaveNoClaims, "You don't have any land claims.", null);
        this.addDefault(defaults, MessageType.ConfirmFluidRemoval, "Abandoning this claim will remove lava inside the claim.  If you're sure, use /AbandonClaim again.", null);
        this.addDefault(defaults, MessageType.AutoBanNotify, "Auto-banned {0}({1}).  See logs for details.", null);
        this.addDefault(defaults, MessageType.AdjustGroupBlocksSuccess, "Adjusted bonus claim blocks for players with the {0} permission by {1}.  New total: {2}.", "0: permission; 1: adjustment amount; 2: new total bonus");
        this.addDefault(defaults, MessageType.InvalidPermissionID, "Please specify a player name, or a permission in [brackets].", null);
        this.addDefault(defaults, MessageType.HowToClaimRegex, "(^|.*\\W)how\\W.*\\W(claim|protect|lock)(\\W.*|$)", "This is a Java Regular Expression.  Look it up before editing!  It's used to tell players about the demo video when they ask how to claim land.");
        this.addDefault(defaults, MessageType.NoBuildOutsideClaims, "You can't build here unless you claim some land first.", null);
        this.addDefault(defaults, MessageType.PlayerOfflineTime, "  Last login: {0} days ago.", "0: number of full days since last login");
        this.addDefault(defaults, MessageType.BuildingOutsideClaims, "Other players can build here, too.  Consider creating a land claim to protect your work!", null);
        this.addDefault(defaults, MessageType.TrappedWontWorkHere, "Sorry, unable to find a safe location to teleport you to.  Contact an admin.", null);
        this.addDefault(defaults, MessageType.UnclaimCleanupWarning, "The land you've unclaimed may be changed by other players or cleaned up by administrators.  If you've built something there you want to keep, you should reclaim it.", null);
        this.addDefault(defaults, MessageType.BuySellNotConfigured, "Sorry, buying and selling claim blocks is disabled.", null);
        this.addDefault(defaults, MessageType.NoTNTDamageAboveSeaLevel, "Warning: TNT will not destroy blocks above sea level.", null);
        this.addDefault(defaults, MessageType.NoTNTDamageClaims, "Warning: TNT will not destroy claimed blocks.", null);
        this.addDefault(defaults, MessageType.IgnoreClaimsAdvertisement, "To override, use /IgnoreClaims.", null);
        this.addDefault(defaults, MessageType.NoPermissionForCommand, "You don't have permission to do that.", null);
        this.addDefault(defaults, MessageType.ClaimsListNoPermission, "You don't have permission to get information about another player's land claims.", null);
        this.addDefault(defaults, MessageType.ExplosivesDisabled, "This claim is now protected from explosions.  Use /ClaimExplosions again to disable.", null);
        this.addDefault(defaults, MessageType.ExplosivesEnabled, "This claim is now vulnerable to explosions.  Use /ClaimExplosions again to re-enable protections.", null);
        this.addDefault(defaults, MessageType.ClaimExplosivesAdvertisement, "To allow explosives to destroy blocks in this land claim, use /ClaimExplosions.", null);
        this.addDefault(defaults, MessageType.NoPistonsOutsideClaims, "Warning: Pistons won't move blocks outside land claims.", null);
        this.addDefault(defaults, MessageType.SoftMuted, "Soft-muted {0}.", "0: The changed player's name.");
        this.addDefault(defaults, MessageType.UnSoftMuted, "Un-soft-muted {0}.", "0: The changed player's name.");
        this.addDefault(defaults, MessageType.DropUnlockAdvertisement, "Other players can't pick up your dropped items unless you /UnlockDrops first.", null);
        this.addDefault(defaults, MessageType.PickupBlockedExplanation, "You can't pick this up unless {0} uses /UnlockDrops.", "0: The item stack's owner.");
        this.addDefault(defaults, MessageType.DropUnlockConfirmation, "Unlocked your drops.  Other players may now pick them up (until you die again).", null);
        this.addDefault(defaults, MessageType.DropUnlockOthersConfirmation, "Unlocked {0}'s drops.", "0: The owner of the unlocked drops.");
        this.addDefault(defaults, MessageType.AdvertiseACandACB, "You may use /ACB to give yourself more claim blocks, or /AdminClaims to create a free administrative claim.", null);
        this.addDefault(defaults, MessageType.AdvertiseAdminClaims, "You could create an administrative land claim instead using /AdminClaims, which you'd share with other administrators.", null);
        this.addDefault(defaults, MessageType.AdvertiseACB, "You may use /ACB to give yourself more claim blocks.", null);
        this.addDefault(defaults, MessageType.NotYourPet, "That belongs to {0} until it's given to you with /GivePet.", "0: owner name");
        this.addDefault(defaults, MessageType.PetGiveawayConfirmation, "Pet transferred.", null);
        this.addDefault(defaults, MessageType.PetTransferCancellation, "Pet giveaway cancelled.", null);
        this.addDefault(defaults, MessageType.ReadyToTransferPet, "Ready to transfer!  Right-click the pet you'd like to give away, or cancel with /GivePet cancel.", null);
        this.addDefault(defaults, MessageType.AvoidGriefClaimLand, "Prevent grief!  If you claim your land, you will be grief-proof.", null);
        this.addDefault(defaults, MessageType.BecomeMayor, "Subdivide your land claim and become a mayor!", null);
        this.addDefault(defaults, MessageType.ClaimCreationFailedOverClaimCountLimit, "You've reached your limit on land claims.  Use /AbandonClaim to remove one before creating another.", null);
        this.addDefault(defaults, MessageType.CreateClaimFailOverlapRegion, "You can't claim all of this because you're not allowed to build here.", null);
        this.addDefault(defaults, MessageType.ResizeFailOverlapRegion, "You don't have permission to build there, so you can't claim that area.", null);
        this.addDefault(defaults, MessageType.ShowNearbyClaims, "Found {0} land claims.", "0: Number of claims found.");
        this.addDefault(defaults, MessageType.NoChatUntilMove, "Sorry, but you have to move a little more before you can chat.  We get lots of spam bots here.  :)", null);
        this.addDefault(defaults, MessageType.SetClaimBlocksSuccess, "Updated accrued claim blocks.", null);
        this.addDefault(defaults, MessageType.IgnoreConfirmation, "You're now ignoring chat messages from that player.", null);
        this.addDefault(defaults, MessageType.UnIgnoreConfirmation, "You're no longer ignoring chat messages from that player.", null);
        this.addDefault(defaults, MessageType.NotIgnoringPlayer, "You're not ignoring that player.", null);
        this.addDefault(defaults, MessageType.SeparateConfirmation, "Those players will now ignore each other in chat.", null);
        this.addDefault(defaults, MessageType.UnSeparateConfirmation, "Those players will no longer ignore each other in chat.", null);
        this.addDefault(defaults, MessageType.NotIgnoringAnyone, "You're not ignoring anyone.", null);
        this.addDefault(defaults, MessageType.TrustListHeader, "Explicit permissions here:", null);
        this.addDefault(defaults, MessageType.Manage, "Manage", null);
        this.addDefault(defaults, MessageType.Build, "Build", null);
        this.addDefault(defaults, MessageType.Containers, "Containers", null);
        this.addDefault(defaults, MessageType.Access, "Access", null);
        this.addDefault(defaults, MessageType.HasSubclaimRestriction, "This subclaim does not inherit permissions from the parent", null);
        this.addDefault(defaults, MessageType.StartBlockMath, "{0} blocks from play + {1} bonus = {2} total.", null);
        this.addDefault(defaults, MessageType.ClaimsListHeader, "Claims:", null);
        this.addDefault(defaults, MessageType.ContinueBlockMath, " (-{0} blocks)", null);
        this.addDefault(defaults, MessageType.EndBlockMath, " = {0} blocks left to spend", null);
        this.addDefault(defaults, MessageType.UntrustAllOwnerOnly, "Only the claim owner can clear all its permissions.", null);
        this.addDefault(defaults, MessageType.ManagersDontUntrustManagers, "Only the claim owner can demote a manager.", null);
        this.addDefault(defaults, MessageType.PlayerNotIgnorable, "You can't ignore that player.", null);
        this.addDefault(defaults, MessageType.NoEnoughBlocksForChestClaim, "Because you don't have any claim blocks available, no automatic land claim was created for you.  You can use /ClaimsList to monitor your available claim block total.", null);
        this.addDefault(defaults, MessageType.MustHoldModificationToolForThat, "You must be holding a golden shovel to do that.", null);
        this.addDefault(defaults, MessageType.StandInClaimToResize, "Stand inside the land claim you want to resize.", null);
        this.addDefault(defaults, MessageType.ClaimsExtendToSky, "Land claims always extend to max build height.", null);
        this.addDefault(defaults, MessageType.ClaimsAutoExtendDownward, "Land claims auto-extend deeper into the ground when you place blocks under them.", null);
        this.addDefault(defaults, MessageType.MinimumRadius, "Minimum radius is {0}.", "0: minimum radius");
        this.addDefault(defaults, MessageType.RadiusRequiresGoldenShovel, "You must be holding a golden shovel when specifying a radius.", null);
        this.addDefault(defaults, MessageType.ClaimTooSmallForActiveBlocks, "This claim isn't big enough to support any active block types (hoppers, spawners, beacons...).  Make the claim bigger first.", null);
        this.addDefault(defaults, MessageType.TooManyActiveBlocksInClaim, "This claim is at its limit for active block types (hoppers, spawners, beacons...).  Either make it bigger, or remove other active blocks first.", null);

        this.addDefault(defaults, MessageType.BookAuthor, "BigScary", null);
        this.addDefault(defaults, MessageType.BookTitle, "How to Claim Land", null);
        this.addDefault(defaults, MessageType.BookLink, "Click: {0}", "{0}: video URL");
        this.addDefault(defaults, MessageType.BookIntro, "Claim land to protect your stuff!  Click the link above to learn land claims in 3 minutes or less.  :)", null);
        this.addDefault(defaults, MessageType.BookTools, "Our claim tools are {0} and {1}.", "0: claim modification tool name; 1:claim information tool name");
        this.addDefault(defaults, MessageType.BookDisabledChestClaims, "  On this server, placing a chest will NOT claim land for you.", null);
        this.addDefault(defaults, MessageType.BookUsefulCommands, "Useful Commands:", null);
        this.addDefault(defaults, MessageType.NoProfanity, "Please moderate your language.", null);
        this.addDefault(defaults, MessageType.IsIgnoringYou, "That player is ignoring you.", null);
        this.addDefault(defaults, MessageType.ConsoleOnlyCommand, "That command may only be executed from the server console.", null);
        this.addDefault(defaults, MessageType.WorldNotFound, "World not found.", null);
        this.addDefault(defaults, MessageType.TooMuchIpOverlap, "Sorry, there are too many players logged in with your IP address.", null);

        this.addDefault(defaults, MessageType.StandInSubclaim, "You need to be standing in a subclaim to restrict it", null);
        this.addDefault(defaults, MessageType.SubclaimRestricted, "This subclaim's permissions will no longer inherit from the parent claim", null);
        this.addDefault(defaults, MessageType.SubclaimUnrestricted, "This subclaim's permissions will now inherit from the parent claim", null);

        this.addDefault(defaults, MessageType.NetherPortalTrapDetectionMessage, "It seems you might be stuck inside a nether portal. We will rescue you in a few seconds if that is the case!", "Sent to player on join, if they left while inside a nether portal.");

        //load the config file
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(messagesFilePath));

        //for each message ID
        for (MessageType messageID : messageIDs) {
            //get default for this message
            CustomizableMessage messageData = defaults.get(messageID.name());

            //if default is missing, log an error and use some fake data for now so that the plugin can run
            if (messageData == null) {
                customLogger.log("Missing message for " + messageID.name() + ".  Please contact the developer.");
                messageData = new CustomizableMessage(messageID, "Missing message!  ID: " + messageID.name() + ".  Please contact a server admin.", null);
            }

            //read the message from the file, use default if necessary
            this.messages[messageID.ordinal()] = config.getString("Messages." + messageID.name() + ".Text", messageData.text);
            config.set("Messages." + messageID.name() + ".Text", this.messages[messageID.ordinal()]);

            //support color codes
            if (messageID != MessageType.HowToClaimRegex) {
                this.messages[messageID.ordinal()] = this.messages[messageID.ordinal()].replace('$', (char) 0x00A7);
            }

            if (messageData.notes != null) {
                messageData.notes = config.getString("Messages." + messageID.name() + ".Notes", messageData.notes);
                config.set("Messages." + messageID.name() + ".Notes", messageData.notes);
            }
        }

        //save any changes
        try {
            List<String> header = new ArrayList<>();
            header.add("Use a YAML editor like NotepadPlusPlus to edit this file.  \nAfter editing, back up your changes before reloading the server in case you made a syntax error.  \nUse dollar signs ($) for formatting codes, which are documented here: http://minecraft.gamepedia.com/Formatting_codes");
            config.options().setHeader(header);
            config.save(messagesFilePath);
        }
        catch (IOException exception) {
            customLogger.log("Unable to write to the configuration file at \"" + messagesFilePath + "\"");
        }

        defaults.clear();
        System.gc();
    }

    private void addDefault(HashMap<String, CustomizableMessage> defaults, MessageType id, String text, String notes) {
        CustomizableMessage message = new CustomizableMessage(id, text, notes);
        defaults.put(id.name(), message);
    }

    synchronized public String getMessage(MessageType messageID, String... args) {
        String message = messages[messageID.ordinal()];

        for (int i = 0; i < args.length; i++) {
            String param = args[i];
            message = message.replace("{" + i + "}", param);
        }
        return message;
    }
}
