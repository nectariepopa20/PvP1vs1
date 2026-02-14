/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.milkbowl.vault.economy.Economy
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.RegisteredServiceProvider
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.gmail.Orscrider.PvP1vs1;

import com.gmail.Orscrider.PvP1vs1.CommandHandler;
import com.gmail.Orscrider.PvP1vs1.DataHandler;
import com.gmail.Orscrider.PvP1vs1.arena.ArenaManager;
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import com.gmail.Orscrider.PvP1vs1.arena.Listeners;
import com.gmail.Orscrider.PvP1vs1.duel.DuelManager;
import com.gmail.Orscrider.PvP1vs1.metrics.MetricsHandler;
import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionController;
import com.gmail.Orscrider.PvP1vs1.persistence.DBMigrationHandler;
import com.gmail.Orscrider.PvP1vs1.signs.SignManager;
import com.gmail.Orscrider.PvP1vs1.util.LogHandler;
import com.gmail.Orscrider.PvP1vs1.util.Updater;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class PvP1vs1
extends JavaPlugin {
    private ArenaManager arenaManager;
    private Listeners listeners;
    private CommandHandler cmdHandler;
    private DataHandler dataHandler;
    private Updater updater;
    private SignManager signManager;
    private DuelManager duelManager;
    private DBConnectionController dbController;
    public static Economy economy = null;

    public void onEnable() {
        LogHandler.init((Plugin)this);
        MetricsHandler metricsHandler = new MetricsHandler(this);
        this.dataHandler = new DataHandler(this.getDataFolder(), this.getConfig(), this);
        this.saveConfig();
        this.arenaManager = new ArenaManager(this);
        this.listeners = new Listeners(this, this.arenaManager);
        this.cmdHandler = new CommandHandler(this);
        this.duelManager = new DuelManager(this);
        this.dbController = DBConnectionController.getInstance();
        DBMigrationHandler.migrateDB(this.dbController);
        this.dbController.connect();
        this.signManager = new SignManager(this);
        this.getCommand("1vs1").setExecutor((CommandExecutor)this.cmdHandler);
        this.dataHandler.checkConfig(this.setupEconomy());
        if (this.getConfig().getBoolean("checkForUpdates")) {
            this.updater = new Updater(this);
            this.getServer().getPluginManager().registerEvents((Listener)this.updater, (Plugin)this);
        }
        metricsHandler.startMetrics();
        LogHandler.info("1vs1 enabled!");
    }

    public void onDisable() {
        for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
            for (Player p : arena.getValue().getArenaPlayers()) {
                if (p == null) break;
                HashMap<String, String> replacements = new HashMap<String, String>();
                replacements.put("{ARENA}", arena.getKey());
                this.send1vs1Message("pluginWasDisabled", p, replacements);
                arena.getValue().restorePlayer(p);
            }
        }
        this.dbController.disconnect();
        LogHandler.info("1vs1 disabled!");
    }

    public void send1vs1Message(String configPath, Player p, HashMap<String, String> replacements) {
        if (p != null && p.isOnline()) {
            String msg = this.dataHandler.getMessagesConfig().getString(configPath);
            if (replacements != null) {
                for (Map.Entry<String, String> a : replacements.entrySet()) {
                    msg = msg.replace(a.getKey(), a.getValue());
                }
            }
            p.sendMessage(this.getPrefix() + ChatColor.translateAlternateColorCodes((char)'&', (String)msg));
        }
    }

    public void messageParser(String configPath, Player p) {
        this.send1vs1Message(configPath, p, null);
    }

    protected boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider economyProvider = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = (Economy)economyProvider.getProvider();
        }
        return economy != null;
    }

    public void reloadEveryConfig() {
        this.dataHandler.reloadArenaConfigs();
        this.dataHandler.reloadMessagesConfig();
        this.reloadConfig();
        this.getConfig().options().copyDefaults(true);
        this.dataHandler.checkConfig(this.setupEconomy());
        this.saveConfig();
    }

    public String getPrefix() {
        return this.getDataHandler().getPrefix();
    }

    public Listeners getListeners() {
        return this.listeners;
    }

    public ArenaManager getArenaManager() {
        return this.arenaManager;
    }

    public DataHandler getDataHandler() {
        return this.dataHandler;
    }

    public SignManager getSignManager() {
        return this.signManager;
    }

    public DuelManager getDuelManager() {
        return this.duelManager;
    }

    public String getFilePath() {
        return this.getFile().getAbsolutePath();
    }

    public String getDownloadUrl() {
        return this.updater.getDownloadUrl();
    }

    public boolean isUpdateAvailable() {
        return this.updater.isUpdateAvailable();
    }

    public Reader getReaderForResource(String file) {
        return this.getTextResource(file);
    }
}

