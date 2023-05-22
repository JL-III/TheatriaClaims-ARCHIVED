package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.listeners.EconomyHandler;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SellClaimBlocks implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
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
        return false;
    }
}
