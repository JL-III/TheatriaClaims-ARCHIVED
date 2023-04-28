/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2015 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jliii.theatriaclaims.util;

import com.google.common.io.Files;
import com.jliii.theatriaclaims.TheatriaClaims;
import com.jliii.theatriaclaims.enums.CustomLogEntryTypes;
import com.jliii.theatriaclaims.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomLogger {

    private final ConfigManager configManager;
    private static final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat filenameFormat = new SimpleDateFormat("yyyy_MM_dd");
    private final String logFolderPath = DataStore.dataLayerFolderPath + File.separator + "Logs";
    private final int secondsBetweenWrites = 300;

    //stringbuilder is not thread safe, stringbuffer is
    private static final StringBuffer queuedEntries = new StringBuffer();

    public CustomLogger(ConfigManager configManager) {
        this.configManager = configManager;
        //ensure log folder exists
        File logFolder = new File(this.logFolderPath);
        logFolder.mkdirs();

        //delete any outdated log files immediately
        this.DeleteExpiredLogs();

        //unless disabled, schedule recurring tasks
        int daysToKeepLogs = configManager.config_logs_daysToKeep;
        if (daysToKeepLogs > 0) {
            BukkitScheduler scheduler = TheatriaClaims.instance.getServer().getScheduler();
            final long ticksPerSecond = 20L;
            final long ticksPerDay = ticksPerSecond * 60 * 60 * 24;
            scheduler.runTaskTimerAsynchronously(TheatriaClaims.instance, new EntryWriter(), this.secondsBetweenWrites * ticksPerSecond, this.secondsBetweenWrites * ticksPerSecond);
            scheduler.runTaskTimerAsynchronously(TheatriaClaims.instance, new ExpiredLogRemover(), ticksPerDay, ticksPerDay);
        }
    }

    private static final Pattern inlineFormatterPattern = Pattern.compile("§.");

    public void AddEntry(String entry, CustomLogEntryTypes entryType) {
        //if disabled, do nothing
        int daysToKeepLogs = configManager.config_logs_daysToKeep;
        if (daysToKeepLogs == 0) return;

        //if entry type is not enabled, do nothing
        if (!isEnabledType(entryType)) return;

        //otherwise write to the in-memory buffer, after removing formatters
        Matcher matcher = inlineFormatterPattern.matcher(entry);
        entry = matcher.replaceAll("");
        String timestamp = timestampFormat.format(new Date());
        queuedEntries.append(timestamp).append(' ').append(entry).append('\n');
    }

    private boolean isEnabledType(CustomLogEntryTypes entryType) {
        if (entryType == CustomLogEntryTypes.Exception) return true;
        if (entryType == CustomLogEntryTypes.SocialActivity && !configManager.config_logs_socialEnabled)
            return false;
        if (entryType == CustomLogEntryTypes.SuspiciousActivity && !configManager.config_logs_suspiciousEnabled)
            return false;
        if (entryType == CustomLogEntryTypes.AdminActivity && !configManager.config_logs_adminEnabled)
            return false;
        if (entryType == CustomLogEntryTypes.Debug && !configManager.config_logs_debugEnabled) return false;
        if (entryType == CustomLogEntryTypes.MutedChat && !configManager.config_logs_mutedChatEnabled)
            return false;

        return true;
    }

    public void WriteEntries() {
        try {
            //if nothing to write, stop here
            if (this.queuedEntries.length() == 0) return;

            //determine filename based on date
            String filename = this.filenameFormat.format(new Date()) + ".log";
            String filepath = this.logFolderPath + File.separator + filename;
            File logFile = new File(filepath);

            //dump content
            Files.append(this.queuedEntries.toString(), logFile, Charset.forName("UTF-8"));

            //in case of a failure to write the above due to exception,
            //the unwritten entries will remain the buffer for the next write to retry
            this.queuedEntries.setLength(0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void DeleteExpiredLogs() {
        try {
            //get list of log files
            File logFolder = new File(logFolderPath);
            File[] files = logFolder.listFiles();

            //delete any created before x days ago
            int daysToKeepLogs = configManager.config_logs_daysToKeep;
            Calendar expirationBoundary = Calendar.getInstance();
            expirationBoundary.add(Calendar.DATE, -daysToKeepLogs);
            for (File file : files) {
                if (file.isDirectory()) continue;  //skip any folders

                String filename = file.getName().replace(".log", "");
                String[] dateParts = filename.split("_");  //format is yyyy_MM_dd
                if (dateParts.length != 3) continue;

                try {
                    int year = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]) - 1;
                    int day = Integer.parseInt(dateParts[2]);

                    Calendar filedate = Calendar.getInstance();
                    filedate.set(year, month, day);
                    if (filedate.before(expirationBoundary)) {
                        file.delete();
                    }
                }
                catch (NumberFormatException e) {
                    //throw this away - effectively ignoring any files without the correct filename format
                    AddLogEntry("Ignoring an unexpected file in the abridged logs folder: " + file.getName(), CustomLogEntryTypes.Debug, true);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //transfers the internal buffer to a log file
    private class EntryWriter implements Runnable {
        @Override
        public void run()
        {
            WriteEntries();
        }
    }

    private class ExpiredLogRemover implements Runnable {
        @Override
        public void run()
        {
            DeleteExpiredLogs();
        }
    }

    //adds a server log entry
    public synchronized void AddLogEntry(String entry, CustomLogEntryTypes customLogType, boolean excludeFromServerLogs) {
        AddEntry(entry, customLogType);
        if (!excludeFromServerLogs) Bukkit.getLogger().info(entry);
    }

    public synchronized void AddLogEntry(String entry, CustomLogEntryTypes customLogType) {
        AddLogEntry(entry, customLogType, false);
    }

    public synchronized void AddLogEntry(String entry) {
        AddLogEntry(entry, CustomLogEntryTypes.Debug);
    }
}
