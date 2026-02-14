/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.gmail.Orscrider.PvP1vs1.arena;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import java.util.HashMap;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TimeOut {
    private PvP1vs1 plugin;
    private GameManager arena;
    private Integer timeOut;
    private HashMap<String, String> replacements = new HashMap();
    private int taskID;

    public TimeOut(PvP1vs1 plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.arena = gameManager;
        this.timeOut = gameManager.getArenaConfig().getInt("timeOut");
    }

    public void startTimeOut() {
        this.taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this.plugin, new Runnable(){

            @Override
            public void run() {
                if (TimeOut.this.arena.getArenaStatus() == GameManager.arenaMode.FIGHT) {
                    switch (TimeOut.this.timeOut) {
                        case 10: 
                        case 20: 
                        case 30: 
                        case 60: 
                        case 120: 
                        case 180: 
                        case 300: 
                        case 600: {
                            for (Player p : TimeOut.this.arena.getArenaPlayers()) {
                                TimeOut.this.replacements.put("{TIME_LEFT}", String.valueOf(TimeOut.this.timeOut));
                                TimeOut.this.plugin.send1vs1Message("timeLeft", p, TimeOut.this.replacements);
                            }
                            break;
                        }
                        case 0: {
                            for (Player p : TimeOut.this.arena.getArenaPlayers()) {
                                TimeOut.this.plugin.messageParser("timeOut", p);
                            }
                            TimeOut.this.arena.setRoundWinner(TimeOut.this.getTimeOutWinner());
                        }
                    }
                    if (TimeOut.this.timeOut > 0) {
                        TimeOut.this.timeOut = TimeOut.this.timeOut - 1;
                    }
                } else {
                    TimeOut.this.resetTimeOut();
                }
            }
        }, 0L, 20L);
    }

    public void resetTimeOut() {
        if (this.arena.getArenaConfig() != null) {
            this.timeOut = this.arena.getArenaConfig().getInt("timeOut");
        }
    }

    public int getTimeOut() {
        return this.timeOut;
    }

    public void cancelTimeOut() {
        Bukkit.getScheduler().cancelTask(this.taskID);
    }

    private Player getTimeOutWinner() {
        Player p1 = this.arena.getArenaPlayers()[0];
        Player p2 = this.arena.getArenaPlayers()[1];
        if (p1 == null) {
            return p2;
        }
        if (p2 == null) {
            return p1;
        }
        if (Double.compare(p1.getHealth(), p2.getHealth()) != 0) {
            return p1.getHealth() > p2.getHealth() ? p1 : p2;
        }
        Random r = new Random();
        return this.arena.getArenaPlayers()[r.nextInt(2)];
    }
}

