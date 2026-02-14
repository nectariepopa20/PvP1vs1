/*
 * Decompiled with CFR 0.152.
 */
package com.gmail.Orscrider.PvP1vs1.duel;

public class DuelInvitation {
    private String challenged;
    private Long timeCreated;
    private String challenger;
    private String arena;
    private boolean isAccepted;

    public DuelInvitation(String challenger, String challenged, long timeCreated, String arena) {
        this.challenger = challenger;
        this.challenged = challenged;
        this.timeCreated = timeCreated;
        this.arena = arena;
    }

    public String getChallenger() {
        return this.challenger;
    }

    public String getChallenged() {
        return this.challenged;
    }

    public Long getTimeCreated() {
        return this.timeCreated;
    }

    public String getArena() {
        return this.arena;
    }

    public boolean isAccepted() {
        return this.isAccepted;
    }

    public void setAccepted(boolean value) {
        this.isAccepted = value;
    }
}

