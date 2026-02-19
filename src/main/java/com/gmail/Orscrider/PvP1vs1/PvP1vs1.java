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
import com.gmail.Orscrider.PvP1vs1.deathmatch.DmArenaManager;
import com.gmail.Orscrider.PvP1vs1.deathmatch.DmCommandHandler;
import com.gmail.Orscrider.PvP1vs1.deathmatch.DmGameManager;
import com.gmail.Orscrider.PvP1vs1.deathmatch.DmListeners;
import com.gmail.Orscrider.PvP1vs1.duel.DuelManager;
import com.gmail.Orscrider.PvP1vs1.metrics.MetricsHandler;
import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionController;
import com.gmail.Orscrider.PvP1vs1.persistence.DBMigrationHandler;
import com.gmail.Orscrider.PvP1vs1.placeholders.PvP1vs1Placeholders;
import com.gmail.Orscrider.PvP1vs1.scoreboard.ScoreboardManager;
import com.gmail.Orscrider.PvP1vs1.signs.SignManager;
import com.gmail.Orscrider.PvP1vs1.util.LogHandler;
import com.gmail.Orscrider.PvP1vs1.util.TitleHelper;
import com.gmail.Orscrider.PvP1vs1.util.Updater;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
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
    private DmArenaManager dmArenaManager;
    private DmListeners dmListeners;
    private DmCommandHandler dmCommandHandler;
    private ScoreboardManager scoreboardManager;
    private int scoreboardTaskId = -1;
    private final Map<UUID, String> pending1v1QueueName = new HashMap<>();
    private final Map<UUID, String> pendingDmQueueName = new HashMap<>();
    public static Economy economy = null;

    public void setPending1v1QueueName(Player p, String queueName) {
        if (p != null) pending1v1QueueName.put(p.getUniqueId(), queueName);
    }
    public String getPending1v1QueueName(Player p) {
        return p != null ? pending1v1QueueName.get(p.getUniqueId()) : null;
    }
    public void clearPending1v1QueueName(Player p) {
        if (p != null) pending1v1QueueName.remove(p.getUniqueId());
    }
    public void setPendingDmQueueName(Player p, String queueName) {
        if (p != null) pendingDmQueueName.put(p.getUniqueId(), queueName);
    }
    public String getPendingDmQueueName(Player p) {
        return p != null ? pendingDmQueueName.get(p.getUniqueId()) : null;
    }
    public void clearPendingDmQueueName(Player p) {
        if (p != null) pendingDmQueueName.remove(p.getUniqueId());
    }

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
        this.dmArenaManager = new DmArenaManager(this);
        this.dmListeners = new DmListeners(this, this.dmArenaManager);
        this.dmCommandHandler = new DmCommandHandler(this);
        this.getCommand("dm").setExecutor((CommandExecutor)this.dmCommandHandler);
        this.dataHandler.checkConfig(this.setupEconomy());
        if (this.getConfig().getBoolean("checkForUpdates")) {
            this.updater = new Updater(this);
            this.getServer().getPluginManager().registerEvents((Listener)this.updater, (Plugin)this);
        }
        metricsHandler.startMetrics();
        if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PvP1vs1Placeholders(this).register();
            LogHandler.info("PlaceholderAPI expansion registered.");
        }
        this.scoreboardManager = new ScoreboardManager(this);
        this.scoreboardTaskId = this.getServer().getScheduler().scheduleSyncRepeatingTask((Plugin) this, () -> {
            if (this.scoreboardManager != null) this.scoreboardManager.updateAll();
        }, 20L, 10L);
        LogHandler.info("1vs1 enabled!");
    }

    public void onDisable() {
        for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
            GameManager gm = arena.getValue();
            for (Player p : gm.getLobbyPlayers()) {
                if (p != null && p.isOnline()) {
                    HashMap<String, String> replacements = new HashMap<>();
                    replacements.put("{ARENA}", arena.getKey());
                    this.send1vs1Message("pluginWasDisabled", p, replacements);
                    gm.leaveLobby(p);
                }
            }
            for (Player p : gm.getArenaPlayers()) {
                if (p == null) break;
                if (p.isOnline()) {
                    HashMap<String, String> replacements = new HashMap<>();
                    replacements.put("{ARENA}", arena.getKey());
                    this.send1vs1Message("pluginWasDisabled", p, replacements);
                    gm.restorePlayer(p);
                }
            }
        }
        if (this.dmArenaManager != null) {
            for (Map.Entry<String, DmGameManager> arena : this.dmArenaManager.getArenas().entrySet()) {
                DmGameManager gm = arena.getValue();
                for (Player p : gm.getLobbyPlayers()) {
                    if (p != null && p.isOnline()) {
                        HashMap<String, String> replacements = new HashMap<>();
                        replacements.put("{ARENA}", arena.getKey());
                        this.sendDmMessage("pluginWasDisabled", p, replacements);
                        gm.leaveLobby(p);
                    }
                }
                for (Player p : gm.getArenaPlayers()) {
                    if (p != null && p.isOnline()) {
                        HashMap<String, String> replacements = new HashMap<>();
                        replacements.put("{ARENA}", arena.getKey());
                        this.sendDmMessage("pluginWasDisabled", p, replacements);
                        gm.restorePlayer(p);
                    }
                }
            }
        }
        if (this.scoreboardTaskId >= 0) {
            this.getServer().getScheduler().cancelTask(this.scoreboardTaskId);
            this.scoreboardTaskId = -1;
        }
        if (this.scoreboardManager != null) {
            this.scoreboardManager.clearAll();
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

    public void sendDmMessage(String configPath, Player p, HashMap<String, String> replacements) {
        if (p != null && p.isOnline()) {
            String msg = this.dataHandler.getDmMessagesConfig().getString(configPath);
            if (msg == null) msg = "&c[DM] Missing message: " + configPath;
            if (replacements != null) {
                for (Map.Entry<String, String> a : replacements.entrySet()) {
                    msg = msg.replace(a.getKey(), a.getValue());
                }
            }
            p.sendMessage(this.dataHandler.getDmPrefix() + ChatColor.translateAlternateColorCodes((char)'&', msg));
        }
    }

    public void messageParserDm(String configPath, Player p) {
        this.sendDmMessage(configPath, p, null);
    }

    /**
     * Send a title from 1v1 messages config. stay = min(config duration, maxStayTicks).
     */
    public void send1vs1Title(Player p, String titleKey, String subtitleKey, HashMap<String, String> replacements, int maxStayTicks) {
        if (p == null || !p.isOnline()) return;
        String title = this.dataHandler.getMessagesConfig().getString(titleKey);
        String subtitle = this.dataHandler.getMessagesConfig().getString(subtitleKey);
        if (title == null) title = "";
        if (subtitle == null) subtitle = "";
        if (replacements != null) {
            for (Map.Entry<String, String> e : replacements.entrySet()) {
                title = title.replace(e.getKey(), e.getValue());
                subtitle = subtitle.replace(e.getKey(), e.getValue());
            }
        }
        int durationTicks = this.getConfig().getInt("titles.durationSeconds", 5) * 20;
        int stay = Math.min(durationTicks, Math.max(20, maxStayTicks));
        int fadeIn = this.getConfig().getInt("titles.fadeInTicks", 10);
        int fadeOut = this.getConfig().getInt("titles.fadeOutTicks", 10);
        TitleHelper.sendTitle(p, title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Send a title from DM messages config. stay = min(config duration, maxStayTicks).
     */
    public void sendDmTitle(Player p, String titleKey, String subtitleKey, HashMap<String, String> replacements, int maxStayTicks) {
        if (p == null || !p.isOnline()) return;
        String title = this.dataHandler.getDmMessagesConfig().getString(titleKey, "");
        String subtitle = this.dataHandler.getDmMessagesConfig().getString(subtitleKey, "");
        if (replacements != null) {
            for (Map.Entry<String, String> e : replacements.entrySet()) {
                title = title.replace(e.getKey(), e.getValue());
                subtitle = subtitle.replace(e.getKey(), e.getValue());
            }
        }
        int durationTicks = this.getConfig().getInt("titles.durationSeconds", 5) * 20;
        int stay = Math.min(durationTicks, Math.max(20, maxStayTicks));
        int fadeIn = this.getConfig().getInt("titles.fadeInTicks", 10);
        int fadeOut = this.getConfig().getInt("titles.fadeOutTicks", 10);
        TitleHelper.sendTitle(p, title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Cosmetic lightning at death location: effect only, no fire or damage.
     */
    public void playDeathLightning(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        World w = loc.getWorld();
        w.strikeLightningEffect(loc);
        try {
            w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1f);
        } catch (NoSuchFieldError | IllegalArgumentException ignored) {
            try {
                w.playSound(loc, Sound.valueOf("LIGHTNING"), 0.6f, 1f);
            } catch (Exception ignored2) {}
        }
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
        this.dataHandler.reloadDmMessagesConfig();
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

    public DmArenaManager getDmArenaManager() {
        return this.dmArenaManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return this.scoreboardManager;
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

