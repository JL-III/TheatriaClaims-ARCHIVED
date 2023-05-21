package com.jliii.theatriaclaims.tasks;

import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.config.ConfigManager;

public class TaskManager {
    private TheatriaClaims plugin;
    private ConfigManager configManager;

    public TaskManager(TheatriaClaims plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void registerScheduledSyncRepeatingTasks() {
        //unless claim block accrual is disabled, start the recurring per 10 minute event to give claim blocks to online players
        //20L ~ 1 second
        if (configManager.getSystemConfig().blocksAccruedPerHour_default > 0) {
            DeliverClaimBlocksTask task = new DeliverClaimBlocksTask(null, plugin, configManager);
            plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, task, 20L * 60 * 10, 20L * 60 * 10);
        }

        //start recurring cleanup scan for unused claims belonging to inactive players
        FindUnusedClaimsTask task2 = new FindUnusedClaimsTask(configManager);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, task2, 20L * 60, 20L * configManager.getSystemConfig().advanced_claim_expiration_check_rate);
    }

}
