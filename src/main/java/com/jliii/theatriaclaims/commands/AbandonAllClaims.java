package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AbandonAllClaims implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
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
        return false;
    }
}
