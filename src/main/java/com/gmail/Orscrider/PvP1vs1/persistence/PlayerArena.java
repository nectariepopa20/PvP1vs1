/*
 * Decompiled with CFR 0.152.
 */
package com.gmail.Orscrider.PvP1vs1.persistence;

import com.gmail.Orscrider.PvP1vs1.persistence.Arena;
import com.gmail.Orscrider.PvP1vs1.persistence.Player;

public class PlayerArena {
    private Player player;
    private Arena arena;
    private Integer score;

    public PlayerArena(Player player, Arena arena, Integer score) {
        this.player = player;
        this.arena = arena;
        this.score = score;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Arena getArena() {
        return this.arena;
    }

    public void setArena(Arena arena) {
        this.arena = arena;
    }

    public Integer getScore() {
        return this.score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}

