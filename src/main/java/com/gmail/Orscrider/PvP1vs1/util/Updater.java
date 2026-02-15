/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.plugin.Plugin
 *  org.json.simple.JSONArray
 *  org.json.simple.JSONObject
 *  org.json.simple.JSONValue
 */
package com.gmail.Orscrider.PvP1vs1.util;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Updater
implements Listener {
    PvP1vs1 pl;
    Logger log;
    int currentBuildNumber;
    boolean newVersionAvailable = false;
    String downloadURL;

    public Updater(PvP1vs1 pl) {
        this.pl = pl;
        this.log = pl.getLogger();
        String version = pl.getDescription().getVersion();
        if (version != null && version.contains("#")) {
            String[] parts = version.split("#");
            this.currentBuildNumber = parts.length >= 2 ? Integer.parseInt(parts[1].trim()) : parseVersionToNumber(version);
        } else {
            this.currentBuildNumber = parseVersionToNumber(version);
        }
        this.startUpdateChecker();
    }

    private int parseVersionToNumber(String version) {
        if (version == null || version.isEmpty()) return 0;
        try {
            String[] parts = version.trim().split("\\.");
            int major = parts.length > 0 ? Integer.parseInt(parts[0].replaceAll("[^0-9]", "")) : 0;
            int minor = parts.length > 1 ? Integer.parseInt(parts[1].replaceAll("[^0-9]", "")) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2].replaceAll("[^0-9]", "")) : 0;
            return major * 10000 + minor * 100 + patch;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void startUpdateChecker() {
        Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this.pl, new Runnable(){

            @Override
            public void run() {
                try {
                    int newBuildNumber = Updater.this.updateCheck();
                    Updater.this.log.info("***** 1vs1 Version Checker ***** ");
                    if (newBuildNumber > Updater.this.currentBuildNumber) {
                        Updater.this.newVersionAvailable = true;
                        Updater.this.log.warning("Newest Build: #" + newBuildNumber + " is out!");
                        Updater.this.log.warning("Current Build: #" + Updater.this.currentBuildNumber);
                        Updater.this.log.warning("Use /1vs1 update to update automatically");
                    } else if (Updater.this.currentBuildNumber > newBuildNumber) {
                        Updater.this.log.info("Stable Build: #" + newBuildNumber);
                        Updater.this.log.info("Current Build: #" + Updater.this.currentBuildNumber);
                        Updater.this.log.info("You use a development build, please report every bug you find");
                    } else {
                        Updater.this.log.info("Stable Build: #" + newBuildNumber);
                        Updater.this.log.info("Current Build: #" + Updater.this.currentBuildNumber);
                        Updater.this.log.info("No new version available");
                    }
                    Updater.this.log.info("*********************************");
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }, 0L, 144000L);
    }

    public int updateCheck() {
        try {
            URL url = new URL("https://api.curseforge.com/servermods/files?projectids=53223");
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(10000);
            conn.addRequestProperty("User-Agent", "Bukkit Plugin 1vs1 Update Checker");
            conn.setDoOutput(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = reader.readLine();
            JSONArray array = (JSONArray)JSONValue.parse((String)response);
            if (array.size() == 0) {
                this.pl.getLogger().warning("No files found or URL is bad");
                return this.currentBuildNumber;
            }
            JSONObject lastEntry = (JSONObject)array.get(array.size() - 1);
            String name = (String) lastEntry.get((Object)"name");
            this.downloadURL = ((String)lastEntry.get((Object)"downloadUrl")).replace("\\/", "/").trim();
            if (name != null && name.contains("#")) {
                String[] parts = name.split("#");
                if (parts.length >= 2) {
                    try {
                        return Integer.parseInt(parts[1].trim().replaceFirst("\\.", "").trim());
                    } catch (NumberFormatException ignored) { }
                }
            }
            return this.currentBuildNumber;
        }
        catch (IOException e) {
            e.printStackTrace();
            this.log.warning("Could not search for an update, check your internet connection and contact a developer if you think this is a bug");
            return this.currentBuildNumber;
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent ev) {
        if (ev.getPlayer().hasPermission("1vs1.notifyOnUpdate") && this.newVersionAvailable) {
            Bukkit.getScheduler().runTaskLater((Plugin)this.pl, new Runnable(){

                @Override
                public void run() {
                    Updater.this.pl.messageParser("updateAvailable", ev.getPlayer());
                    Updater.this.pl.messageParser("visitWebsite", ev.getPlayer());
                }
            }, 20L);
        }
    }

    public boolean isUpdateAvailable() {
        return this.newVersionAvailable;
    }

    public String getDownloadUrl() {
        return this.downloadURL;
    }
}

