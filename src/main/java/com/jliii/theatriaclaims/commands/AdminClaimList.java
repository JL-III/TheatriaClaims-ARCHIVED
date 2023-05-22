package com.jliii.theatriaclaims.commands;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.claim.Claim;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.util.GeneralUtils;
import com.jliii.theatriaclaims.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Vector;

public class AdminClaimList implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        //adminclaimslist
        else if (cmd.getName().equalsIgnoreCase("adminclaimslist"))
        {
            //find admin claims
            Vector<com.jliii.theatriaclaims.claim.Claim> claims = new Vector<>();
            for (com.jliii.theatriaclaims.claim.Claim claim : TheatriaClaims.getInstance().getDatabaseManager().getDataStore().claims)
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
        return false;
    }
}
