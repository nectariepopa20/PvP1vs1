/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.gmail.Orscrider.PvP1vs1.arena;

import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import java.util.ArrayList;
import org.bukkit.entity.Player;

public class ArenaQueue {
    GameManager arena;
    private ArrayList<Player> queue = new ArrayList();
    private int maxPlayersInArena = 2;

    public ArenaQueue(GameManager arena) {
        this.arena = arena;
    }

    public void add(Player p) {
        if (!this.queue.contains(p)) {
            this.queue.add(p);
        }
    }

    public void add(Player p, int index) {
        if (!this.queue.contains(p)) {
            if (index > this.queue.size()) {
                this.queue.add(p);
            } else {
                this.queue.add(index, p);
            }
        }
    }

    public void removeIndex(int index) {
        if (this.queue.get(index) != null) {
            Player removed = this.queue.remove(index);
            this.arena.removeDuelPartner(removed);
        }
    }

    public void removePlayer(Player p) {
        if (this.queue.contains(p)) {
            this.queue.remove(p);
            this.arena.removeDuelPartner(p);
        }
    }

    public boolean contains(Player p) {
        return this.queue.contains(p);
    }

    public int size() {
        return this.queue.size();
    }

    public Player getPlayer(int index) {
        return this.queue.get(index);
    }

    public int indexOf(Player p) {
        return this.queue.indexOf(p);
    }

    public Player[] getNextArenaPlayers() {
        Player[] playersNext = null;
        if (this.queue.size() >= this.maxPlayersInArena) {
            playersNext = new Player[this.maxPlayersInArena];
            for (int i = 0; i < this.maxPlayersInArena; ++i) {
                playersNext[i] = this.queue.get(i);
            }
        }
        return playersNext;
    }

    public boolean nextArenaPlayersContainsPlayer(Player p) {
        if (this.getNextArenaPlayers() != null) {
            for (Player pl : this.getNextArenaPlayers()) {
                if (pl != p) continue;
                return true;
            }
        }
        return false;
    }

    public ArrayList<Player> getList() {
        return this.queue;
    }

    public void clear() {
        this.queue.clear();
    }
}

