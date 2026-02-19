/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.ConsoleCommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.gmail.Orscrider.PvP1vs1;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import com.gmail.Orscrider.PvP1vs1.duel.DuelInvitation;
import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionController;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CommandHandler
implements CommandExecutor {
    private PvP1vs1 pl;
    private HashMap<String, String> replacements = new HashMap();

    public CommandHandler(PvP1vs1 plugin) {
        this.pl = plugin;
        this.replacements.put("{LINE_BREAK}", "\n");
    }

    /*
     * Enabled aggressive block sorting
     */
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player p;
        block115: {
            int len$;
            Player[] arr$;
            DuelInvitation di;
            block114: {
                Map.Entry<String, GameManager> arena;
                block112: {
                    Iterator<Map.Entry<String, GameManager>> i$;
                    block110: {
                        block113: {
                            block111: {
                                block109: {
                                    if (!cmd.getName().equalsIgnoreCase("1vs1")) return false;
                                    if (sender instanceof ConsoleCommandSender) {
                                        sender.sendMessage("You are not a player!");
                                        return true;
                                    }
                                    if (!(sender instanceof Player)) return false;
                                    p = (Player)sender;
                                    if (args.length == 0) {
                                        this.wrongCommandUsage(p);
                                        return true;
                                    }
                                    if (args.length > 4 && !args[0].equalsIgnoreCase("queue")) {
                                        this.wrongCommandUsage(p);
                                        return true;
                                    }
                                    if (args[0].equalsIgnoreCase("queue")) {
                                        return this.handle1v1Queue(p, args);
                                    }
                                    if (args[0].equalsIgnoreCase("reload")) {
                                        if (!p.hasPermission("1vs1.reload")) {
                                            this.pl.messageParser("insufficientPermission", p);
                                            return true;
                                        }
                                        this.pl.reloadEveryConfig();
                                        Iterator<Map.Entry<String, GameManager>> i$2 = this.pl.getArenaManager().getArenas().entrySet().iterator();
                                        while (true) {
                                            if (!i$2.hasNext()) {
                                                this.pl.messageParser("reload", p);
                                                return true;
                                            }
                                            Map.Entry<String, GameManager> arena2 = i$2.next();
                                            arena2.getValue().setEnabled(arena2.getValue().getArenaConfig().getBoolean("enabled"));
                                        }
                                    }
                                    if (!args[0].equalsIgnoreCase("join")) break block109;
                                    if (!(p.hasPermission("1vs1.join") || p.hasPermission("1vs1.accept") || p.hasPermission("1vs1.duel"))) {
                                        this.pl.messageParser("insufficientPermission", p);
                                        return true;
                                    }
                                    if (args.length != 2) {
                                        p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 join <arena>");
                                        return true;
                                    }
                                    i$ = this.pl.getArenaManager().getArenas().entrySet().iterator();
                                    break block110;
                                }
                                if (!args[0].equalsIgnoreCase("leave")) break block111;
                                if (!p.hasPermission("1vs1.leave")) {
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                                break block112;
                            }
                            if (args[0].equalsIgnoreCase("duel")) {
                                if (!p.hasPermission("1vs1.duel")) {
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                                if (args.length < 2) {
                                    p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 duel <player> [arena]");
                                    return true;
                                }
                                if (p.getName().equals(args[1])) {
                                    this.pl.send1vs1Message("cannotDuelYourself", p, null);
                                    return true;
                                }
                                if (!Bukkit.getOfflinePlayer((String)args[1]).isOnline()) {
                                    this.replacements.put("{PLAYER}", args[1]);
                                    this.pl.send1vs1Message("playerNotOnline", p, this.replacements);
                                    return true;
                                }
                                if (args.length < 3) {
                                    this.pl.getDuelManager().duel(p, Bukkit.getPlayer((String)args[1]), null);
                                    return true;
                                }
                                if (!p.hasPermission("1vs1.duel.arena")) {
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                                if (this.pl.getArenaManager().arenaExists(args[2])) {
                                    this.pl.getDuelManager().duel(p, Bukkit.getPlayer((String)args[1]), args[2]);
                                    return true;
                                }
                                this.replacements.put("{ARENA}", args[2]);
                                this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                return true;
                            }
                            if (!args[0].equalsIgnoreCase("accept")) break block113;
                            if (!p.hasPermission("1vs1.accept")) {
                                this.pl.messageParser("insufficientPermission", p);
                                return true;
                            }
                            if (!this.pl.getDuelManager().wasChallenged(p.getName())) {
                                this.pl.messageParser("notChallenged", p);
                                return true;
                            }
                            di = this.pl.getDuelManager().getDuelInvitation(p.getName());
                            arr$ = new Player[]{Bukkit.getPlayer((String)di.getChallenger()), Bukkit.getPlayer((String)di.getChallenged())};
                            len$ = arr$.length;
                            break block114;
                        }
                        if (args[0].equalsIgnoreCase("info")) {
                            if (!p.hasPermission("1vs1.info")) {
                                this.pl.messageParser("insufficientPermission", p);
                                return true;
                            }
                            if (args.length != 2) {
                                p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 info <arena>");
                                return true;
                            }
                            if (!this.pl.getArenaManager().arenaExists(args[1])) {
                                this.replacements.put("{ARENA}", args[1]);
                                this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                return true;
                            }
                            break block115;
                        } else {
                            block108: {
                                if (args[0].equalsIgnoreCase("rJoin") || args[0].equalsIgnoreCase("randomJoin")) {
                                    if (!p.hasPermission("1vs1.randomJoin")) {
                                        this.pl.messageParser("insufficientPermission", p);
                                        return true;
                                    }
                                    String randomArena = this.pl.getArenaManager().getRandomArena();
                                    if (randomArena != null) {
                                        p.chat("/1vs1 join " + randomArena);
                                        return true;
                                    }
                                    this.pl.send1vs1Message("noEnabledArenas", p, this.replacements);
                                    return true;
                                }
                                if (args[0].equalsIgnoreCase("update")) {
                                    if (!p.hasPermission("1vs1.update")) {
                                        this.pl.messageParser("insufficientPermission", p);
                                        return true;
                                    }
                                    if (!this.pl.getConfig().getBoolean("checkForUpdates")) {
                                        this.pl.messageParser("updaterNotEnabled", p);
                                        return true;
                                    }
                                    if (this.pl.isUpdateAvailable()) {
                                        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.pl, new Runnable(){

                                            @Override
                                            public void run() {
                                                try {
                                                    File updateJar = this.downloadNewJar(CommandHandler.this.pl.getDownloadUrl());
                                                    if (updateJar.exists() && updateJar.length() > 0L) {
                                                        File oldPluginJar = new File(CommandHandler.this.pl.getFilePath());
                                                        this.replaceActJar(oldPluginJar, updateJar);
                                                        updateJar.delete();
                                                        CommandHandler.this.pl.messageParser("updatedSuccessfully", p);
                                                    } else {
                                                        CommandHandler.this.pl.messageParser("updateFailed", p);
                                                    }
                                                }
                                                catch (IOException e) {
                                                    e.printStackTrace();
                                                    CommandHandler.this.pl.messageParser("updateFailed", p);
                                                }
                                            }

                                            private File downloadNewJar(String downloadUrl) throws IOException {
                                                boolean checkRedirect = true;
                                                URL url = new URL(downloadUrl);
                                                while (checkRedirect) {
                                                    HttpURLConnection con = (HttpURLConnection)url.openConnection();
                                                    int status = con.getResponseCode();
                                                    if (status != 200 && (status == 302 || status == 301 || status == 303)) {
                                                        String newURL = con.getHeaderField("Location");
                                                        url = new URL(newURL);
                                                        continue;
                                                    }
                                                    checkRedirect = false;
                                                }
                                                ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                                                File updateJar = new File(CommandHandler.this.pl.getDataFolder().getParent() + "/1vs1-Update.jar");
                                                FileOutputStream fos = new FileOutputStream(updateJar);
                                                fos.getChannel().transferFrom(rbc, 0L, Long.MAX_VALUE);
                                                fos.close();
                                                rbc.close();
                                                return updateJar;
                                            }

                                            private void replaceActJar(File pluginJar, File newJar) throws IOException {
                                                FileOutputStream oldJarFos = new FileOutputStream(pluginJar);
                                                FileInputStream newJarFis = new FileInputStream(newJar);
                                                ReadableByteChannel replaceJarRbc = Channels.newChannel(newJarFis);
                                                oldJarFos.getChannel().transferFrom(replaceJarRbc, 0L, Long.MAX_VALUE);
                                                oldJarFos.close();
                                                newJarFis.close();
                                            }
                                        });
                                        return true;
                                    }
                                    this.pl.messageParser("noUpdateAvailable", p);
                                    return true;
                                }
                                if (args[0].equalsIgnoreCase("top")) {
                                    if (args.length == 2) {
                                        if (!args[1].equalsIgnoreCase("ten")) {
                                            this.wrongTopCommandUsage(p);
                                            return true;
                                        }
                                        if (p.hasPermission("1vs1.top.ten")) {
                                            DBConnectionController dbController = DBConnectionController.getInstance();
                                            LinkedHashMap<String, Integer> playerTopTenList = dbController.getPlayerTopTenList();
                                            this.showTopTen(p, playerTopTenList);
                                            return true;
                                        }
                                        this.pl.messageParser("insufficientPermission", p);
                                        return true;
                                    }
                                    if (args.length != 3) {
                                        this.wrongTopCommandUsage(p);
                                        return true;
                                    }
                                    if (!args[1].equalsIgnoreCase("arena")) {
                                        this.wrongTopCommandUsage(p);
                                        return true;
                                    }
                                    if (!p.hasPermission("1vs1.top.arena")) {
                                        this.pl.messageParser("insufficientPermission", p);
                                        return true;
                                    }
                                    if (this.pl.getArenaManager().arenaExists(args[2])) {
                                        DBConnectionController dbController = DBConnectionController.getInstance();
                                        LinkedHashMap<String, Integer> playerTopTenListForArena = dbController.getPlayerTopTenListForArena(args[2]);
                                        this.showTopTen(p, playerTopTenListForArena);
                                        return true;
                                    }
                                    this.replacements.put("{ARENA}", args[2]);
                                    this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                    return true;
                                }
                                if (args[0].equalsIgnoreCase("stats")) {
                                    if (!p.hasPermission("1vs1.stats")) {
                                        this.pl.messageParser("insufficientPermission", p);
                                        return true;
                                    }
                                    if (args.length == 1) {
                                        DBConnectionController dbController = DBConnectionController.getInstance();
                                        DecimalFormat oneDigit = new DecimalFormat("#,##0.0");
                                        int wins = dbController.getPlayerWins(p.getUniqueId().toString());
                                        int losses = dbController.getPlayerLosses(p.getUniqueId().toString());
                                        this.replacements.put("{WINS}", "" + wins);
                                        this.replacements.put("{LOSSES}", "" + losses);
                                        this.replacements.put("{WIN_RATE}", "" + (wins == 0 ? Integer.valueOf(0) : oneDigit.format(Double.valueOf(wins) / Double.valueOf(wins + losses) * 100.0)));
                                        this.pl.send1vs1Message("stats", p, this.replacements);
                                        return true;
                                    }
                                    if (args.length != 2) {
                                        p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 stats [player]");
                                        return true;
                                    }
                                    if (!p.hasPermission("1vs1.stats.otherPlayers")) {
                                        this.pl.messageParser("insufficientPermission", p);
                                        return true;
                                    }
                                    DBConnectionController dbController = DBConnectionController.getInstance();
                                    DecimalFormat oneDigit = new DecimalFormat("#,##0.0");
                                    int wins = dbController.getPlayerWins(Bukkit.getOfflinePlayer((String)args[1]).getUniqueId().toString());
                                    int losses = dbController.getPlayerLosses(Bukkit.getOfflinePlayer((String)args[1]).getUniqueId().toString());
                                    this.replacements.put("{NAME}", args[1]);
                                    this.replacements.put("{WINS}", "" + wins);
                                    this.replacements.put("{LOSSES}", "" + losses);
                                    this.replacements.put("{WIN_RATE}", "" + (wins == 0 ? Integer.valueOf(0) : oneDigit.format(Double.valueOf(wins) / Double.valueOf(wins + losses) * 100.0)));
                                    this.pl.send1vs1Message("statsOtherPlayers", p, this.replacements);
                                    return true;
                                }
                                if (args[0].equalsIgnoreCase("points")) {
                                    if (args.length < 2) {
                                        this.wrongPointsCommandUsage(p);
                                        return true;
                                    }
                                    if (args[1].equalsIgnoreCase("total")) {
                                        if (!p.hasPermission("1vs1.points.total")) {
                                            this.pl.messageParser("insufficientPermission", p);
                                            return true;
                                        }
                                        if (args.length == 2) {
                                            DBConnectionController dbController = DBConnectionController.getInstance();
                                            this.replacements.put("{POINTS}", String.valueOf(dbController.getScoreOfPlayer(p.getUniqueId().toString())));
                                            this.pl.send1vs1Message("totalPoints", p, this.replacements);
                                            return true;
                                        }
                                        this.wrongPointsCommandUsage(p);
                                        return true;
                                    }
                                    if (args[1].equalsIgnoreCase("arena")) {
                                        if (!p.hasPermission("1vs1.points.arena")) {
                                            this.pl.messageParser("insufficientPermission", p);
                                            return true;
                                        }
                                        if (args.length != 3) {
                                            p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 points arena <arena>");
                                            return true;
                                        }
                                        if (this.pl.getArenaManager().arenaExists(args[2])) {
                                            DBConnectionController dbController = DBConnectionController.getInstance();
                                            this.replacements.put("{POINTS}", String.valueOf(dbController.getScoreOfPlayerAndArena(p.getUniqueId().toString(), args[2])));
                                            this.replacements.put("{ARENA}", args[2]);
                                            this.pl.send1vs1Message("pointsInArena", p, this.replacements);
                                            return true;
                                        }
                                        this.replacements.put("{ARENA}", args[2]);
                                        this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                        return true;
                                    }
                                    if (!args[1].equalsIgnoreCase("player")) {
                                        this.wrongPointsCommandUsage(p);
                                        return true;
                                    }
                                    if (args.length == 3) {
                                        if (p.hasPermission("1vs1.points.otherPlayers")) {
                                            this.replacements.put("{NAME}", args[2]);
                                            DBConnectionController dbController = DBConnectionController.getInstance();
                                            this.replacements.put("{POINTS}", String.valueOf(dbController.getScoreOfPlayer(Bukkit.getOfflinePlayer((String)args[2]).getUniqueId().toString())));
                                            this.pl.send1vs1Message("otherPlayersPoints", p, this.replacements);
                                            return true;
                                        }
                                        this.pl.messageParser("insufficientPermission", p);
                                        return true;
                                    }
                                    if (args.length != 4) {
                                        p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 points player <player> [arena]");
                                        return true;
                                    }
                                    if (!p.hasPermission("1vs1.points.otherPlayers.arena")) {
                                        this.pl.messageParser("insufficientPermission", p);
                                        return true;
                                    }
                                    if (this.pl.getArenaManager().arenaExists(args[3])) {
                                        this.replacements.put("{NAME}", args[2]);
                                        DBConnectionController dbController = DBConnectionController.getInstance();
                                        this.replacements.put("{POINTS}", String.valueOf(dbController.getScoreOfPlayerAndArena(Bukkit.getOfflinePlayer((String)args[2]).getUniqueId().toString(), args[3])));
                                        this.replacements.put("{ARENA}", args[3]);
                                        this.pl.send1vs1Message("otherPlayersArenaPoints", p, this.replacements);
                                        return true;
                                    }
                                    this.replacements.put("{ARENA}", args[3]);
                                    this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                    return true;
                                }
                                if (!args[0].equalsIgnoreCase("arena")) {
                                    this.wrongCommandUsage(p);
                                    return true;
                                }
                                if (args.length <= 1) {
                                    this.wrongArenaCommandUsage(p, 1);
                                    return true;
                                }
                                if (args[1].equalsIgnoreCase("1") || args[1].equalsIgnoreCase("2") || args[1].equalsIgnoreCase("3")) {
                                    this.wrongArenaCommandUsage(p, Integer.valueOf(args[1]));
                                    return true;
                                }
                                if (args[1].equalsIgnoreCase("setspawn1") || args[1].equalsIgnoreCase("setspawn2")) {
                                    if (args[1].equalsIgnoreCase("setspawn1") && p.hasPermission("1vs1.arena.setspawn1") || args[1].equalsIgnoreCase("setspawn2") && p.hasPermission("1vs1.arena.setspawn2")) {
                                        if (args.length == 3) {
                                            if (!this.pl.getArenaManager().arenaExists(args[2])) {
                                                this.replacements.put("{ARENA}", args[2]);
                                                this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                                return true;
                                            }
                                            if (args[2].equals(args[2])) {
                                                this.pl.getDataHandler().setSpawnInConfig(args[2], args[1].equalsIgnoreCase("setspawn1") ? 1 : 2, p.getLocation());
                                                this.replacements.put("{ARENA}", args[2]);
                                                this.pl.send1vs1Message(args[1].equalsIgnoreCase("setSpawn1") ? "setSpawn1" : "setSpawn2", p, this.replacements);
                                                return true;
                                            }
                                            break block108;
                                        } else {
                                            p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 arena " + (args[1].equalsIgnoreCase("setspawn1") ? "setspawn1" : "setspawn2") + " <arena>");
                                            return true;
                                        }
                                    }
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                            }
                            if (args[1].equalsIgnoreCase("setlobby")) {
                                if (!p.hasPermission("1vs1.arena.setlobby")) {
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                                if (args.length != 3) {
                                    p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 arena setlobby <arena>");
                                    return true;
                                }
                                if (!this.pl.getArenaManager().arenaExists(args[2])) {
                                    this.replacements.put("{ARENA}", args[2]);
                                    this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                    return true;
                                }
                                this.pl.getDataHandler().setLobbyInConfig(args[2], p.getLocation());
                                this.replacements.put("{ARENA}", args[2]);
                                this.pl.send1vs1Message("setLobby", p, this.replacements);
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("setInv") || args[1].equalsIgnoreCase("setInventory")) {
                                if (!p.hasPermission("1vs1.arena.setInv")) {
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                                if (args.length != 3) {
                                    p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 arena setInv <arena>");
                                    return true;
                                }
                                if (this.pl.getArenaManager().arenaExists(args[2])) {
                                    this.pl.getDataHandler().setItems(p.getInventory().getArmorContents(), args[2], "inventory.armor");
                                    this.pl.getDataHandler().setItems(p.getInventory().getContents(), args[2], "inventory.items");
                                    this.replacements.put("{ARENA}", args[2]);
                                    this.pl.send1vs1Message("invSet", p, this.replacements);
                                    return true;
                                }
                                this.replacements.put("{ARENA}", args[2]);
                                this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("setPrizes")) {
                                if (!p.hasPermission("1vs1.arena.setPrizes")) {
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                                if (args.length != 3) {
                                    p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 arena setPrizes <arena>");
                                    return true;
                                }
                                if (this.pl.getArenaManager().arenaExists(args[2])) {
                                    this.pl.getDataHandler().setItemsWithoutAir(p.getInventory().getContents(), args[2], "prize.items.itemPrizes");
                                    this.replacements.put("{ARENA}", args[2]);
                                    this.pl.send1vs1Message("invSet", p, this.replacements);
                                    return true;
                                }
                                this.replacements.put("{ARENA}", args[2]);
                                this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("list")) {
                                if (!p.hasPermission("1vs1.arena.list")) {
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                                StringBuilder enabledArenas = new StringBuilder();
                                StringBuilder disabledArenas = new StringBuilder();
                                if (this.pl.getArenaManager().getEnabledArenas().isEmpty()) {
                                    enabledArenas.append(ChatColor.GOLD + "None");
                                } else {
                                    for (Map.Entry<String, GameManager> arena3 : this.pl.getArenaManager().getEnabledArenas().entrySet()) {
                                        enabledArenas.append(ChatColor.GOLD + arena3.getKey() + " ");
                                    }
                                }
                                if (this.pl.getArenaManager().getDisabledArenas().isEmpty()) {
                                    disabledArenas.append(ChatColor.GRAY + "None");
                                } else {
                                    for (Map.Entry<String, GameManager> arena3 : this.pl.getArenaManager().getDisabledArenas().entrySet()) {
                                        disabledArenas.append(ChatColor.GRAY + arena3.getKey() + " ");
                                    }
                                }
                                this.replacements.put("{ENABLED_ARENAS}", enabledArenas.toString());
                                this.replacements.put("{DISABLED_ARENAS}", disabledArenas.toString());
                                this.pl.send1vs1Message("enabledArenas", p, this.replacements);
                                this.pl.send1vs1Message("disabledArenas", p, this.replacements);
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("add")) {
                                if (!p.hasPermission("1vs1.arena.add")) {
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                                if (args.length != 3) {
                                    p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 arena add <arena>");
                                    return true;
                                }
                                if (this.pl.getArenaManager().addArena(args[2])) {
                                    this.replacements.put("{ARENA}", args[2]);
                                    this.pl.send1vs1Message("arenaAdded", p, this.replacements);
                                    return true;
                                }
                                this.replacements.put("{ARENA}", args[2]);
                                this.pl.send1vs1Message("arenaAlreadyExists", p, this.replacements);
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("del") || args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("rem") || args[1].equalsIgnoreCase("remove")) {
                                if (!p.hasPermission("1vs1.arena.del")) {
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                                if (args.length != 3) {
                                    p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 arena del <arena>");
                                    return true;
                                }
                                if (this.pl.getArenaManager().arenaExists(args[2])) {
                                    this.pl.getArenaManager().delArena(args[2]);
                                    this.replacements.put("{ARENA}", args[2]);
                                    this.pl.send1vs1Message("arenaDeleted", p, this.replacements);
                                    return true;
                                }
                                this.replacements.put("{ARENA}", args[2]);
                                this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("disable")) {
                                if (!p.hasPermission("1vs1.arena.disable")) {
                                    this.pl.messageParser("insufficientPermission", p);
                                    return true;
                                }
                                if (args.length != 3) {
                                    p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 arena disable <arena>");
                                    return true;
                                }
                                if (this.pl.getArenaManager().arenaExists(args[2])) {
                                    this.pl.getArenaManager().disableArena(args[2]);
                                    this.replacements.put("{ARENA}", args[2]);
                                    this.pl.send1vs1Message("arenaWasDisabled", p, this.replacements);
                                    return true;
                                }
                                this.replacements.put("{ARENA}", args[2]);
                                this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                                return true;
                            }
                            if (!args[1].equalsIgnoreCase("enable")) {
                                this.wrongArenaCommandUsage(p, 1);
                                return true;
                            }
                            if (!p.hasPermission("1vs1.arena.enable")) {
                                this.pl.messageParser("insufficientPermission", p);
                                return true;
                            }
                            if (args.length != 3) {
                                p.sendMessage(this.pl.getPrefix() + "\u00a7c/1vs1 arena enable <arena>");
                                return true;
                            }
                            if (this.pl.getArenaManager().arenaExists(args[2])) {
                                this.pl.getArenaManager().getArena(args[2]).setEnabled(true);
                                this.pl.getDataHandler().getArenaConfig(args[2]).set("enabled", (Object)true);
                                this.pl.getDataHandler().saveArenaConfig(args[2]);
                                this.replacements.put("{ARENA}", args[2]);
                                this.pl.send1vs1Message("arenaWasEnabled", p, this.replacements);
                                return true;
                            }
                            this.replacements.put("{ARENA}", args[2]);
                            this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                            return true;
                        }
                    }
                    while (i$.hasNext()) {
                        Map.Entry<String, GameManager> arena4 = i$.next();
                        if (arena4.getValue().isInLobby(p)) {
                            this.replacements.put("{ARENA}", arena4.getKey());
                            this.pl.send1vs1Message("alreadyInLobby", p, this.replacements);
                            return true;
                        }
                        if (arena4.getValue().arenaPlayersContains(p)) {
                            this.replacements.put("{ARENA}", arena4.getKey());
                            this.pl.send1vs1Message("alreadyInArena", p, this.replacements);
                            return true;
                        }
                    }
                    if (!this.pl.getArenaManager().arenaExists(args[1])) {
                        this.replacements.put("{ARENA}", args[1]);
                        this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                        return true;
                    }
                    GameManager arena5 = this.pl.getArenaManager().getArena(args[1]);
                    if (!arena5.isEnabled()) {
                        this.replacements.put("{ARENA}", args[1]);
                        this.pl.send1vs1Message("arenaDisabled", p, this.replacements);
                        return true;
                    }
                    if (!arena5.hasLobbySet()) {
                        this.replacements.put("{ARENA}", args[1]);
                        this.pl.send1vs1Message("arenaLobbyNotSet", p, this.replacements);
                        return true;
                    }
                    GameManager.arenaMode status = arena5.getArenaStatus();
                    if (status == GameManager.arenaMode.COUNTDOWN_LOBBY || status == GameManager.arenaMode.PREPERATION_BEFORE_FIGHT
                            || status == GameManager.arenaMode.COUNTDOWN_BEFORE_FIGHT || status == GameManager.arenaMode.FIGHT
                            || status == GameManager.arenaMode.BETWEEN_ROUNDS) {
                        this.replacements.put("{ARENA}", args[1]);
                        this.pl.send1vs1Message("arenaInGame", p, this.replacements);
                        return true;
                    }
                    if (arena5.getLobbySize() >= 2) {
                        this.replacements.put("{ARENA}", args[1]);
                        this.pl.send1vs1Message("lobbyFull", p, this.replacements);
                        return true;
                    }
                    arena5.joinLobby(p);
                    String pendingQueue = this.pl.getPending1v1QueueName(p);
                    if (pendingQueue != null) {
                        arena5.setCurrentQueueName(pendingQueue);
                        this.pl.clearPending1v1QueueName(p);
                    }
                    arena5.broadcastPlayerJoinedLobby(p);
                    this.replacements.put("{ARENA}", arena5.getArenaName());
                    this.pl.send1vs1Message("joinLobby", p, this.replacements);
                    arena5.startGame();
                    return true;
                }
                Iterator<Map.Entry<String, GameManager>> i$ = this.pl.getArenaManager().getArenas().entrySet().iterator();
                while (i$.hasNext()) {
                    arena = i$.next();
                    if (arena.getValue().isInLobby(p)) {
                        arena.getValue().leaveLobby(p);
                        this.replacements.put("{ARENA}", arena.getKey());
                        this.pl.send1vs1Message("leaveLobby", p, this.replacements);
                        arena.getValue().removeDuelPartner(p);
                        return true;
                    }
                }
                this.pl.messageParser("notInLobby", p);
                return true;
            }
            for (int i$ = 0; i$ < len$; ++i$) {
                Player player = arr$[i$];
                if (this.pl.getArenaManager().isFree(player)) continue;
                this.pl.messageParser("notAbleToAcceptDuel", Bukkit.getPlayer((String)di.getChallenger()));
                this.pl.messageParser("notAbleToAcceptDuel", Bukkit.getPlayer((String)di.getChallenged()));
                return true;
            }
            String arena = di.getArena() != null ? di.getArena() : this.pl.getArenaManager().getRandomArena();
            Bukkit.getPlayer((String)di.getChallenger()).chat("/1vs1 join " + arena);
            Bukkit.getPlayer((String)di.getChallenged()).chat("/1vs1 join " + arena);
            di.setAccepted(true);
            return true;
        }
        GameManager arena = this.pl.getArenaManager().getArena(args[1]);
        int lobbySize = arena.getLobbySize();
        String arenaPlayersStr = "Nobody";
        if (arena.getArenaStatus() == GameManager.arenaMode.COUNTDOWN_BEFORE_FIGHT || arena.getArenaStatus() == GameManager.arenaMode.FIGHT) {
            Player[] ap = arena.getArenaPlayers();
            if (ap != null && ap[0] != null && ap[1] != null) {
                arenaPlayersStr = ap[0].getName() + ", " + ap[1].getName();
            }
        }
        String lobbyPlayersStr = "Nobody";
        if (lobbySize > 0) {
            List<Player> lp = arena.getLobbyPlayers();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lp.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(lp.get(i).getName());
            }
            lobbyPlayersStr = sb.toString();
        }
        for (String line : this.pl.getDataHandler().getMessagesConfig().getStringList("info")) {
            String msg = this.pl.getPrefix() + ChatColor.translateAlternateColorCodes('&', line)
                    .replace("{ENABLED}", String.valueOf(arena.isEnabled()))
                    .replace("{GAME_STATUS}", ChatColor.translateAlternateColorCodes('&', arena.getArenaStatusInString()))
                    .replace("{SIZE}", String.valueOf(lobbySize))
                    .replace("{IN_LOBBY}", arena.isInLobby(p) ? "Yes" : "No")
                    .replace("{LOBBY_COUNT}", String.valueOf(lobbySize))
                    .replace("{ARENA_PLAYERS}", arenaPlayersStr)
                    .replace("{LOBBY_PLAYERS}", lobbyPlayersStr);
            p.sendMessage(msg);
        }
        return true;
    }

    private boolean handle1v1Queue(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "/1v1 queue <create|delete|addarena|remarena|list|join|setscoreboardname> [args]");
            return true;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "create":
                if (!p.hasPermission("1vs1.queue.create")) {
                    this.pl.messageParser("insufficientPermission", p);
                    return true;
                }
                if (args.length != 3) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "/1v1 queue create <name>");
                    return true;
                }
                if (this.pl.getDataHandler().createQueue("1v1", args[2])) {
                    this.replacements.put("{QUEUE}", args[2]);
                    p.sendMessage(this.pl.getPrefix() + ChatColor.GREEN + "Queue \"" + args[2] + "\" created.");
                } else {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "Queue \"" + args[2] + "\" already exists.");
                }
                return true;
            case "delete":
                if (!p.hasPermission("1vs1.queue.delete")) {
                    this.pl.messageParser("insufficientPermission", p);
                    return true;
                }
                if (args.length != 3) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "/1v1 queue delete <name>");
                    return true;
                }
                if (this.pl.getDataHandler().deleteQueue("1v1", args[2])) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.GREEN + "Queue \"" + args[2] + "\" deleted.");
                } else {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "Queue \"" + args[2] + "\" does not exist.");
                }
                return true;
            case "addarena":
                if (!p.hasPermission("1vs1.queue.addarena")) {
                    this.pl.messageParser("insufficientPermission", p);
                    return true;
                }
                if (args.length != 4) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "/1v1 queue addarena <queue> <arena>");
                    return true;
                }
                if (!this.pl.getDataHandler().queueExists("1v1", args[2])) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "Queue \"" + args[2] + "\" does not exist.");
                    return true;
                }
                if (!this.pl.getArenaManager().arenaExists(args[3])) {
                    this.replacements.put("{ARENA}", args[3]);
                    this.pl.send1vs1Message("arenaDoesNotExist", p, this.replacements);
                    return true;
                }
                if (this.pl.getDataHandler().addArenaToQueue("1v1", args[2], args[3])) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.GREEN + "Arena \"" + args[3] + "\" added to queue \"" + args[2] + "\".");
                } else {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "Arena already in queue.");
                }
                return true;
            case "remarena":
                if (!p.hasPermission("1vs1.queue.remarena")) {
                    this.pl.messageParser("insufficientPermission", p);
                    return true;
                }
                if (args.length != 4) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "/1v1 queue remarena <queue> <arena>");
                    return true;
                }
                if (this.pl.getDataHandler().remArenaFromQueue("1v1", args[2], args[3])) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.GREEN + "Arena \"" + args[3] + "\" removed from queue \"" + args[2] + "\".");
                } else {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "Queue or arena not found.");
                }
                return true;
            case "list":
                if (!p.hasPermission("1vs1.queue.list")) {
                    this.pl.messageParser("insufficientPermission", p);
                    return true;
                }
                ArrayList<String> queues = this.pl.getDataHandler().getQueueNames("1v1");
                if (queues.isEmpty()) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.GRAY + "No queues defined.");
                    return true;
                }
                p.sendMessage(this.pl.getPrefix() + ChatColor.GOLD + "1v1 Queues:");
                for (String q : queues) {
                    List<String> arenas = this.pl.getDataHandler().getQueueArenas("1v1", q);
                    p.sendMessage(ChatColor.GREEN + "  " + q + ChatColor.GRAY + " (" + String.join(", ", arenas) + ")");
                }
                return true;
            case "join":
                if (args.length != 3) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "/1v1 queue join <queue>");
                    return true;
                }
                String queueName = args[2];
                if (!this.pl.getDataHandler().queueExists("1v1", queueName)) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "Queue \"" + queueName + "\" does not exist.");
                    return true;
                }
                List<String> arenaNames = this.pl.getDataHandler().getQueueArenas("1v1", queueName);
                ArrayList<String> joinable = new ArrayList<>();
                for (String an : arenaNames) {
                    if (!this.pl.getArenaManager().arenaExists(an)) continue;
                    GameManager gm = this.pl.getArenaManager().getArena(an);
                    if (!gm.isEnabled() || !gm.hasLobbySet()) continue;
                    GameManager.arenaMode st = gm.getArenaStatus();
                    if (st == GameManager.arenaMode.COUNTDOWN_LOBBY || st == GameManager.arenaMode.PREPERATION_BEFORE_FIGHT
                            || st == GameManager.arenaMode.COUNTDOWN_BEFORE_FIGHT || st == GameManager.arenaMode.FIGHT
                            || st == GameManager.arenaMode.BETWEEN_ROUNDS) continue;
                    if (gm.getLobbySize() >= 2) continue;
                    if (gm.isInLobby(p) || gm.arenaPlayersContains(p)) {
                        this.replacements.put("{ARENA}", an);
                        this.pl.send1vs1Message("alreadyInLobby", p, this.replacements);
                        return true;
                    }
                    joinable.add(an);
                }
                for (Map.Entry<String, GameManager> e : this.pl.getArenaManager().getArenas().entrySet()) {
                    if (e.getValue().isInLobby(p) || e.getValue().arenaPlayersContains(p)) {
                        this.replacements.put("{ARENA}", e.getKey());
                        this.pl.send1vs1Message("alreadyInLobby", p, this.replacements);
                        return true;
                    }
                }
                if (joinable.isEmpty()) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "No arena in queue \"" + queueName + "\" is available to join.");
                    return true;
                }
                String chosen = null;
                int maxCount = -1;
                for (String an : joinable) {
                    GameManager gm = this.pl.getArenaManager().getArena(an);
                    int count = gm.getLobbySize();
                    if (gm.getArenaStatus() != GameManager.arenaMode.NORMAL && gm.getArenaStatus() != GameManager.arenaMode.LOBBY) count += 2;
                    if (count > maxCount) {
                        maxCount = count;
                        chosen = an;
                    }
                }
                if (chosen == null || maxCount == 0) {
                    chosen = joinable.get(new Random().nextInt(joinable.size()));
                }
                this.pl.setPending1v1QueueName(p, queueName);
                p.chat("/1vs1 join " + chosen);
                return true;
            case "setscoreboardname":
                if (!p.hasPermission("1vs1.queue.setscoreboardname")) {
                    this.pl.messageParser("insufficientPermission", p);
                    return true;
                }
                if (args.length < 4) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "/1v1 queue setscoreboardname <queue> <name>");
                    return true;
                }
                if (!this.pl.getDataHandler().queueExists("1v1", args[2])) {
                    p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "Queue \"" + args[2] + "\" does not exist.");
                    return true;
                }
                String displayName1v1 = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                this.pl.getDataHandler().setQueueScoreboardName("1v1", args[2], displayName1v1);
                p.sendMessage(this.pl.getPrefix() + ChatColor.GREEN + "Scoreboard name for queue \"" + args[2] + "\" set to \"" + displayName1v1 + "\".");
                return true;
            default:
                p.sendMessage(this.pl.getPrefix() + ChatColor.RED + "/1v1 queue <create|delete|addarena|remarena|list|join|setscoreboardname> [args]");
                return true;
        }
    }

    private void wrongCommandUsage(Player p) {
        p.sendMessage(ChatColor.GOLD + "=========================================");
        p.sendMessage(ChatColor.GOLD + "\u00a73\u27b7 \u00a72\u2582 \u2583 \u2584 \u2585 \u2586 \u2587 \u259b\u00a76 | \u00a74\u00a7l1vs1 \u00a76| \u00a72\u259c \u2587 \u2586 \u2585 \u2584 \u2583 \u2582 \u00a73\u27b7");
        p.sendMessage(ChatColor.GOLD + "=== \u00a72v. " + this.pl.getDescription().getVersion() + " \u00a76== \u00a73by Orscrider\u00a76 ============");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 join           /1vs1 leave");
        p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 info           /1vs1 reload");
        p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 rJoin         /1vs1 arena");
        p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 top            /1vs1 points");
        p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 update        /1vs1 duel");
        p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 accept        /1vs1 stats");
    }

    private void wrongArenaCommandUsage(Player p, int page) {
        p.sendMessage(ChatColor.GOLD + "=========================================");
        p.sendMessage(ChatColor.GOLD + "\u00a73\u27b7 \u00a72\u2582 \u2583 \u2584 \u2585 \u2586 \u2587 \u259b\u00a76 | \u00a74\u00a7l1vs1 \u00a76| \u00a72\u259c \u2587 \u2586 \u2585 \u2584 \u2583 \u2582 \u00a73\u27b7");
        p.sendMessage(ChatColor.GOLD + "=== \u00a72v. " + this.pl.getDescription().getVersion() + " \u00a76=== \u00a73ARENA\u00a76 ====== \u00a72" + page + "/3 \u00a76======");
        if (page == 1) {
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena <page>");
            p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena list");
            p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena add <arena>");
            p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena del <arena>");
        }
        if (page == 2) {
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena setspawn1 <arena>");
            p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena setspawn2 <arena>");
            p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena setlobby <arena>");
            p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena enable <arena>");
            p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena disable <arena>");
        }
        if (page == 3) {
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena setInv <arena>");
            p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
            p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 arena setPrizes <arena>");
        }
    }

    private void wrongTopCommandUsage(Player p) {
        p.sendMessage(ChatColor.GOLD + "=========================================");
        p.sendMessage(ChatColor.GOLD + "\u00a73\u27b7 \u00a72\u2582 \u2583 \u2584 \u2585 \u2586 \u2587 \u259b\u00a76 | \u00a74\u00a7l1vs1 \u00a76| \u00a72\u259c \u2587 \u2586 \u2585 \u2584 \u2583 \u2582 \u00a73\u27b7");
        p.sendMessage(ChatColor.GOLD + "=== \u00a72v. " + this.pl.getDescription().getVersion() + " \u00a76==== \u00a73TOP\u00a76 ==================");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 top ten");
        p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 top arena <arena>");
    }

    public void wrongPointsCommandUsage(Player p) {
        p.sendMessage(ChatColor.GOLD + "=========================================");
        p.sendMessage(ChatColor.GOLD + "\u00a73\u27b7 \u00a72\u2582 \u2583 \u2584 \u2585 \u2586 \u2587 \u259b\u00a76 | \u00a74\u00a7l1vs1 \u00a76| \u00a72\u259c \u2587 \u2586 \u2585 \u2584 \u2583 \u2582 \u00a73\u27b7");
        p.sendMessage(ChatColor.GOLD + "=== \u00a72v. " + this.pl.getDescription().getVersion() + " \u00a76=== \u00a73POINTS\u00a76 ================");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 points total");
        p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 points arena <arena>");
        p.sendMessage(ChatColor.GOLD + "-----------------------------------------");
        p.sendMessage(ChatColor.DARK_GREEN + "   /1vs1 points player <player> [arena]");
    }

    private void showTopTen(Player p, LinkedHashMap<String, Integer> playerTopTenList) {
        p.sendMessage(ChatColor.GOLD + "=========================================");
        p.sendMessage(ChatColor.GOLD + "\u00a73\u27b7 \u00a72\u2582 \u2583 \u2584 \u2585 \u2586 \u2587 \u259b\u00a76 | \u00a74\u00a7l1vs1 \u00a76| \u00a72\u259c \u2587 \u2586 \u2585 \u2584 \u2583 \u2582 \u00a73\u27b7");
        p.sendMessage(ChatColor.GOLD + "=== \u00a72v. " + this.pl.getDescription().getVersion() + " \u00a76=== \u00a73TOP TEN\u00a76 ================");
        int i = 1;
        for (Map.Entry<String, Integer> entry : playerTopTenList.entrySet()) {
            String userName = Bukkit.getOfflinePlayer((UUID)UUID.fromString(entry.getKey())).getName();
            p.sendMessage(ChatColor.DARK_RED + String.valueOf(i) + "\u00a76.\u00a72 " + userName + " \u00a76-\u00a73 " + entry.getValue() + " \u00a76points");
            ++i;
        }
    }
}

