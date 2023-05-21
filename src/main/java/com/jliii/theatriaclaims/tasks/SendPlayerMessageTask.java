package com.jliii.theatriaclaims.tasks;

import com.jliii.theatriaclaims.util.CustomLogger;
import org.bukkit.entity.Player;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.util.Messages;
import com.jliii.theatriaclaims.player.PlayerData;

import net.kyori.adventure.text.format.NamedTextColor;

//sends a message to a player
//used to send delayed messages, for example help text triggered by a player's chat
public class SendPlayerMessageTask implements Runnable {
    private final Player player;
    private final NamedTextColor color;
    private final String message;

    public SendPlayerMessageTask(Player player, NamedTextColor color, String message) {
        this.player = player;
        this.color = color;
        this.message = message;
    }

    @Override
    public void run() {
        if (player == null) {
            CustomLogger.log(color + message);
            return;
        }

        //if the player is dead, save it for after his respawn
        if (this.player.isDead()) {
            PlayerData playerData = TheatriaClaims.getInstance().getDatabaseManager().getDataStore().getPlayerData(this.player.getUniqueId());
            playerData.messageOnRespawn = this.color + this.message;
        }
        //otherwise send it immediately
        else {
            Messages.sendMessage(this.player, this.color, this.message);
        }
    }
}
