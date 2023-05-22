package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.listeners.EconomyHandler;
import com.jliii.theatriaclaims.player.PlayerData;
import com.jliii.theatriaclaims.util.Messages;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BuyClaimBlocks implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
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
        return false;
    }
}
