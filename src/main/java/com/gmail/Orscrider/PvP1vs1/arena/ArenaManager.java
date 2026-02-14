/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.gmail.Orscrider.PvP1vs1.arena;

import com.gmail.Orscrider.PvP1vs1.Broadcaster;
import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import com.gmail.Orscrider.PvP1vs1.metrics.MetricsHandler;
import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.bukkit.entity.Player;

public class ArenaManager {
    private PvP1vs1 pl;
    private HashMap<String, GameManager> arenas = new HashMap();
    private HashMap<String, String> replacements = new HashMap();

    public ArenaManager(PvP1vs1 plugin) {
        this.pl = plugin;
        for (String arenaName : this.pl.getDataHandler().getArenaNames()) {
            this.arenas.put(arenaName, new GameManager(plugin, arenaName));
        }
        if (this.pl.getConfig().getBoolean("broadcaster.enabled")) {
            new Broadcaster(this, this.pl);
        }
    }

    public GameManager getArena(String arenaName) {
        return this.arenas.get(arenaName);
    }

    public boolean arenaExists(String arenaName) {
        return this.arenas.containsKey(arenaName);
    }

    public boolean addArena(String arenaName) {
        if (!this.arenas.containsKey(arenaName)) {
            this.pl.getDataHandler().addArenaConfig(arenaName);
            this.arenas.put(arenaName, new GameManager(this.pl, arenaName));
            DBConnectionController dbController = DBConnectionController.getInstance();
            dbController.addArena(arenaName);
            MetricsHandler.getInstance().updateRoundFormats();
            return true;
        }
        return false;
    }

    public boolean delArena(String arenaName) {
        if (this.arenas.containsKey(arenaName)) {
            this.disableArena(arenaName);
            this.arenas.remove(arenaName);
            DBConnectionController dbController = DBConnectionController.getInstance();
            dbController.removeArena(arenaName);
            this.pl.getDataHandler().delArenaConfig(arenaName);
            return true;
        }
        return false;
    }

    public HashMap<String, GameManager> getArenas() {
        return this.arenas;
    }

    public HashMap<String, GameManager> getEnabledArenas() {
        HashMap<String, GameManager> enabledArenas = new HashMap<String, GameManager>();
        for (Map.Entry<String, GameManager> arena : this.arenas.entrySet()) {
            if (!arena.getValue().getEnabled()) continue;
            enabledArenas.put(arena.getKey(), arena.getValue());
        }
        return enabledArenas;
    }

    public HashMap<String, GameManager> getDisabledArenas() {
        HashMap<String, GameManager> disabledArenas = new HashMap<String, GameManager>();
        for (Map.Entry<String, GameManager> arena : this.arenas.entrySet()) {
            if (arena.getValue().getEnabled()) continue;
            disabledArenas.put(arena.getKey(), arena.getValue());
        }
        return disabledArenas;
    }

    public boolean disableArena(String arenaName) {
        if (this.arenas.containsKey(arenaName)) {
            GameManager arena = this.arenas.get(arenaName);
            arena.setEnabled(false);
            this.pl.getDataHandler().getArenaConfig(arenaName).set("enabled", (Object)false);
            this.pl.getDataHandler().saveArenaConfig(arenaName);
            for (Player p : arena.getQueue().getList()) {
                this.replacements.put("{ARENA}", arenaName);
                this.pl.send1vs1Message("arenaWasDisabled", p, this.replacements);
            }
            arena.getQueue().clear();
            for (Player p : arena.getArenaPlayers()) {
                if (p == null) break;
                this.replacements.put("{ARENA}", arenaName);
                this.pl.send1vs1Message("arenaWasDisabled", p, this.replacements);
                arena.restorePlayer(p);
            }
            arena.reset();
            arena.cancelTimeOut();
            MetricsHandler.getInstance().updateRoundFormats();
            return true;
        }
        return false;
    }

    public String getRandomArena() {
        if (this.getEnabledArenas().size() > 0) {
            ArrayList<String> preferedQueues = new ArrayList<String>();
            Random r = new Random();
            for (Map.Entry<String, GameManager> arena : this.getEnabledArenas().entrySet()) {
                if (arena.getValue().getQueue().size() != 1) continue;
                preferedQueues.add(arena.getKey());
            }
            if (!preferedQueues.isEmpty()) {
                return (String)preferedQueues.get(r.nextInt(preferedQueues.size()));
            }
            ArrayList<String> arenaNames = new ArrayList<String>(this.getEnabledArenas().keySet());
            return arenaNames.get(r.nextInt(arenaNames.size()));
        }
        return null;
    }

    public boolean isFree(Player p) {
        boolean free = true;
        for (Map.Entry<String, GameManager> arena : this.arenas.entrySet()) {
            if (!arena.getValue().arenaPlayersContains(p) && !arena.getValue().getQueue().contains(p)) continue;
            free = false;
            break;
        }
        return free;
    }
}

