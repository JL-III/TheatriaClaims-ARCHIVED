package com.jliii.theatriaclaims.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.MessageType;
import com.jliii.theatriaclaims.enums.TextMode;
import com.jliii.theatriaclaims.managers.ConfigManager;
import com.jliii.theatriaclaims.util.CustomLogger;
import com.jliii.theatriaclaims.util.DataStore;
import com.jliii.theatriaclaims.util.Messages;

public class WelcomeTask implements Runnable {

    private final Player player;
    private final ConfigManager configManager;
    private final CustomLogger customLogger;

    public WelcomeTask(Player player, ConfigManager configManager, CustomLogger customLogger){
        this.player = player;
        this.configManager = configManager;
        this.customLogger = customLogger;
    }

    @Override
    public void run() {
        //abort if player has logged out since this task was scheduled
        if (!this.player.isOnline()) return;

        //offer advice and a helpful link
        Messages.sendMessage(player, TextMode.Instr.getColor(), MessageType.AvoidGriefClaimLand, customLogger);
        Messages.sendMessage(player, TextMode.Instr.getColor(), MessageType.SurvivalBasicsVideo2, customLogger, DataStore.SURVIVAL_VIDEO_URL);

        //give the player a reference book for later
        if (configManager.getSystemConfig().supplyPlayerManual) {
            ItemFactory factory = Bukkit.getItemFactory();
            BookMeta meta = (BookMeta) factory.getItemMeta(Material.WRITTEN_BOOK);

            DataStore datastore = TheatriaClaims.instance.dataStore;
            meta.setAuthor(datastore.getMessage(MessageType.BookAuthor));
            meta.setTitle(datastore.getMessage(MessageType.BookTitle));

            StringBuilder page1 = new StringBuilder();
            String URL = datastore.getMessage(MessageType.BookLink, DataStore.SURVIVAL_VIDEO_URL);
            String intro = datastore.getMessage(MessageType.BookIntro);

            page1.append(URL).append("\n\n");
            page1.append(intro).append("\n\n");
            String editToolName = configManager.getSystemConfig().modificationTool.name().replace('_', ' ').toLowerCase();
            String infoToolName = configManager.getSystemConfig().investigationTool.name().replace('_', ' ').toLowerCase();
            String configClaimTools = datastore.getMessage(MessageType.BookTools, editToolName, infoToolName);
            page1.append(configClaimTools);
            if (configManager.getSystemConfig().automaticClaimsForNewPlayersRadius < 0) {
                page1.append(datastore.getMessage(MessageType.BookDisabledChestClaims));
            }

            StringBuilder page2 = new StringBuilder(datastore.getMessage(MessageType.BookUsefulCommands)).append("\n\n");
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
