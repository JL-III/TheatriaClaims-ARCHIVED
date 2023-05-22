package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.player.PlayerName;
import com.jliii.theatriaclaims.util.GeneralUtils;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Vector;

public class ClaimList implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
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
            Vector<com.jliii.theatriaclaims.claim.Claim> claims = playerData.getClaims();
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
        return false;
    }
}
