package com.jliii.theatriaclaims.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.database.DataStore;
import com.jliii.theatriaclaims.util.Messages;

public class WelcomeTask implements Runnable {

    private final Player player;
    private final ConfigManager configManager;

    public WelcomeTask(Player player, ConfigManager configManager){
        this.player = player;
        this.configManager = configManager;
    }

    @Override
    public void run() {
        //abort if player has logged out since this task was scheduled
        if (!this.player.isOnline()) return;

        //offer advice and a helpful link
        Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.AvoidGriefClaimLand);
        Messages.sendMessage(player, configManager, TextMode.Instr.getColor(), MessageType.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);

        //give the player a reference book for later
        if (configManager.getSystemConfig().supplyPlayerManual) {
            ItemFactory factory = Bukkit.getItemFactory();
            BookMeta meta = (BookMeta) factory.getItemMeta(Material.WRITTEN_BOOK);

            meta.setAuthor(configManager.getMessagesConfig().getMessage(MessageType.BookAuthor));
            meta.setTitle(configManager.getMessagesConfig().getMessage(MessageType.BookTitle));

            StringBuilder page1 = new StringBuilder();
            String URL = configManager.getMessagesConfig().getMessage(MessageType.BookLink, DataStore.SURVIVAL_VIDEO_URL);
            String intro = configManager.getMessagesConfig().getMessage(MessageType.BookIntro);

            page1.append(URL).append("\n\n");
            page1.append(intro).append("\n\n");
            String editToolName = configManager.getSystemConfig().modificationTool.name().replace('_', ' ').toLowerCase();
            String infoToolName = configManager.getSystemConfig().investigationTool.name().replace('_', ' ').toLowerCase();
            String configClaimTools = configManager.getMessagesConfig().getMessage(MessageType.BookTools, editToolName, infoToolName);
            page1.append(configClaimTools);
            if (configManager.getSystemConfig().automaticClaimsForNewPlayersRadius < 0) {
                page1.append(configManager.getMessagesConfig().getMessage(MessageType.BookDisabledChestClaims));
            }

            StringBuilder page2 = new StringBuilder(configManager.getMessagesConfig().getMessage(MessageType.BookUsefulCommands)).append("\n\n");
            page2.append("/Trust /UnTrust /TrustList\n");
            page2.append("/ClaimsList\n");
            page2.append("/AbandonClaim\n\n");
            page2.append("/Claim /ExtendClaim\n");

            page2.append("/IgnorePlayer\n\n");

            page2.append("/SubdivideClaims\n");
            page2.append("/AccessTrust\n");
            page2.append("/ContainerTrust\n");
            page2.append("/PermissionTrust");

            meta.setPages(page1.toString(), page2.toString());

            ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
            item.setItemMeta(meta);
            player.getInventory().addItem(item);
        }
    }
}
