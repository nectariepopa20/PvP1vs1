/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.gmail.Orscrider.PvP1vs1;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.arena.ArenaManager;
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Broadcaster {
    private ArenaManager arenaManager;
    private PvP1vs1 pl;
    private HashMap<String, String> replacements = new HashMap();

    public Broadcaster(ArenaManager arenaManager, PvP1vs1 pl) {
        this.arenaManager = arenaManager;
        this.pl = pl;
        this.startBroadcaster();
    }

    private void startBroadcaster() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this.pl, new Runnable(){

            @Override
            public void run() {
                for (Map.Entry<String, GameManager> entry : Broadcaster.this.arenaManager.getArenas().entrySet()) {
                    GameManager arena = entry.getValue();
                    java.util.List<Player> lobbyPlayers = arena.getLobbyPlayers();
                    for (int i = 0; i < lobbyPlayers.size(); i++) {
                        Player p = lobbyPlayers.get(i);
                        Broadcaster.this.replacements.put("{ARENA}", entry.getKey());
                        Broadcaster.this.replacements.put("{COUNT}", String.valueOf(i + 1));
                        Broadcaster.this.pl.send1vs1Message("broadcastLobbyMessage", p, Broadcaster.this.replacements);
                    }
                }
            }
        }, 0L, this.pl.getConfig().getLong("broadcaster.delay") * 20L);
    }
}

