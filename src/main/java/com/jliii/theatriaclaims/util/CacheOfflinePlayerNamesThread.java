package com.jliii.theatriaclaims.util;

import com.jliii.theatriaclaims.config.ConfigManager;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CacheOfflinePlayerNamesThread extends Thread {
    //thread to build the above cache
    private ConfigManager configManager;
    private final OfflinePlayer[] offlinePlayers;
    private final ConcurrentHashMap<String, UUID> playerNameToIDMap;

    public CacheOfflinePlayerNamesThread(ConfigManager configManager, OfflinePlayer[] offlinePlayers, ConcurrentHashMap<String, UUID> playerNameToIDMap) {
        this.configManager = configManager;
        this.offlinePlayers = offlinePlayers;
        this.playerNameToIDMap = playerNameToIDMap;
    }

    public void run() {
        CustomLogger.log("Caching offline player names...");
        long now = System.currentTimeMillis();
        final long millisecondsPerDay = 1000 * 60 * 60 * 24;
        for (OfflinePlayer player : offlinePlayers) {
            try {
                UUID playerID = player.getUniqueId();
                if (playerID == null) continue;
                long lastSeen = player.getLastPlayed();

                //if the player has been seen in the last 90 days, cache his name/UUID pair
                long diff = now - lastSeen;
                long daysDiff = diff / millisecondsPerDay;
                if (daysDiff <= configManager.getSystemConfig().advanced_offlineplayer_cache_days) {
                    String playerName = player.getName();
                    if (playerName == null) continue;
                    this.playerNameToIDMap.put(playerName, playerID);
                    this.playerNameToIDMap.put(playerName.toLowerCase(), playerID);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
