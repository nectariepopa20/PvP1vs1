/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package com.gmail.Orscrider.PvP1vs1.arena;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CountdownThread
extends Thread {
    private PvP1vs1 pl;
    private GameManager gameManager;
    HashMap<String, String> replacements = new HashMap();
    private countdownType countdownMode;

    public CountdownThread(PvP1vs1 plugin, GameManager gameManager, countdownType type) {
        this.pl = plugin;
        this.gameManager = gameManager;
        this.countdownMode = type;
    }

    @Override
    public void run() {
        block28: {
            block29: {
                if (this.countdownMode != countdownType.BEFORE_TELEPORT) break block29;
                this.gameManager.setArenaStatus(GameManager.arenaMode.COUNTDOWN_LOBBY);
                org.bukkit.configuration.file.FileConfiguration cfg = this.gameManager.getArenaConfig();
                int duration = cfg.getInt("countdown.lobby.duration", cfg.getInt("countdown.beforeTeleport.duration", 5));
                java.util.List<Player> lobbyList = this.gameManager.getLobbyPlayers();
                if (lobbyList.size() < 2) break block28;
                Player[] players = new Player[]{lobbyList.get(0), lobbyList.get(1)};
                for (int i = duration; i >= 0 && this.gameManager.isEnabled(); --i) {
                    if (this.gameManager.getLobbyPlayers().size() < 2) break;
                    this.replacements.put("{COUNTDOWN}", String.valueOf(i));
                    if (i == duration) {
                        for (Player p : players) {
                            if (p != null && p.isOnline()) {
                                this.pl.send1vs1Message("countdownLobby", p, this.replacements);
                                if (cfg.getBoolean("countdown.lobby.sound", cfg.getBoolean("countdown.beforeTeleport.sound", true))) this.playSound(p);
                            }
                        }
                        try { Thread.sleep(1000L); } catch (InterruptedException e) { e.printStackTrace(); }
                        continue;
                    }
                    switch (i) {
                        case 1: case 2: case 3: case 4: case 5: case 10: case 15:
                            for (Player p : players) {
                                if (p != null && p.isOnline()) {
                                    this.pl.send1vs1Message("countdownLobby", p, this.replacements);
                                    if (cfg.getBoolean("countdown.lobby.sound", cfg.getBoolean("countdown.beforeTeleport.sound", true))) this.playSound(p);
                                }
                            }
                            break;
                        case 0:
                            for (Player p : players) {
                                if (p != null && p.isOnline()) {
                                    this.pl.messageParser("getTeleportedIntoArena", p);
                                    if (cfg.getBoolean("countdown.lobby.sound", cfg.getBoolean("countdown.beforeTeleport.sound", true))) this.playSound(p);
                                }
                            }
                            this.joinArena(players);
                            break;
                    }
                    try { Thread.sleep(1000L); } catch (InterruptedException e) { e.printStackTrace(); }
                }
                break block28;
            }
            if (this.countdownMode != countdownType.BEFORE_FIGHT) break block28;
            this.gameManager.setArenaStatus(GameManager.arenaMode.COUNTDOWN_BEFORE_FIGHT);
            int duration = this.gameManager.getArenaConfig().getInt("countdown.beforeFight.duration");
            Player[] players = this.gameManager.getArenaPlayers();
            for (int i = duration; i >= 0; --i) {
                if (!(this.gameManager.isEnabled() && players[0].isOnline() && players[1].isOnline())) {
                    this.gameManager.reset();
                    break;
                }
                this.replacements.put("{COUNTDOWN}", String.valueOf(i));
                this.replacements.put("{ROUND}", String.valueOf(this.gameManager.getCurrentRound()));
                if (i == duration) {
                    for (Player p : players) {
                        this.pl.send1vs1Message("countdownBeforeFight", p, this.replacements);
                        if (this.gameManager.getArenaConfig().getBoolean("countdown.beforeFight.sound")) {
                            this.playSound(p);
                        }
                        if (!this.gameManager.getArenaConfig().getBoolean("countdown.beforeFight.blindness")) continue;
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration * 20, 1));
                    }
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                switch (i) {
                    case 1: 
                    case 2: 
                    case 3: 
                    case 4: 
                    case 5: 
                    case 10: 
                    case 15: {
                        for (Player p : players) {
                            this.pl.send1vs1Message("countdownBeforeFight", p, this.replacements);
                            if (!this.gameManager.getArenaConfig().getBoolean("countdown.beforeFight.sound")) continue;
                            this.playSound(p);
                        }
                        break;
                    }
                    case 0: {
                        for (Player p : players) {
                            p.sendMessage(this.pl.getPrefix() + ChatColor.translateAlternateColorCodes((char)'&', (String)this.pl.getDataHandler().getMessagesConfig().getString("afterPlayerFreezing")));
                            if (!this.gameManager.getArenaConfig().getBoolean("countdown.beforeFight.sound")) continue;
                            this.playSound(p);
                        }
                        this.gameManager.setArenaStatus(GameManager.arenaMode.FIGHT);
                    }
                }
                try {
                    Thread.sleep(1000L);
                    continue;
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void joinArena(final Player[] arenaPlayers) {
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this.pl, new Runnable(){

            @Override
            public void run() {
                CountdownThread.this.gameManager.joinArena(arenaPlayers);
            }
        });
    }

    private void playSound(Player p) {
        Sound sound = null;
        try {
            sound = Sound.valueOf((String)"SUCCESSFUL_HIT");
        }
        catch (IllegalArgumentException e) {
            try {
                sound = Sound.valueOf((String)"ENTITY_EXPERIENCE_ORB_TOUCH");
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        if (sound != null) {
            p.playSound(p.getLocation(), sound, 200.0f, 0.0f);
        }
    }

    public static enum countdownType {
        BEFORE_TELEPORT,
        BEFORE_FIGHT;

    }
}

