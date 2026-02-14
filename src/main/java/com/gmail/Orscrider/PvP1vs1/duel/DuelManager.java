/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.gmail.Orscrider.PvP1vs1.duel;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.duel.DuelInvitation;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DuelManager {
    PvP1vs1 pl;
    private CopyOnWriteArrayList<DuelInvitation> duelInvitations = new CopyOnWriteArrayList();

    public DuelManager(PvP1vs1 pl) {
        this.pl = pl;
        this.startTimeOutTask();
    }

    public void duel(Player challenger, Player challenged, String arena) {
        DuelInvitation latestDi = new DuelInvitation(challenger.getName(), challenged.getName(), System.currentTimeMillis(), arena);
        this.duelInvitations.add(latestDi);
        for (DuelInvitation di : this.duelInvitations) {
            if (di.equals(latestDi) || !di.getChallenged().equals(challenged)) continue;
            this.duelInvitations.remove(di);
        }
        HashMap<String, String> replacements = new HashMap<String, String>();
        if (arena != null) {
            replacements.put("{ARENA}", arena);
        } else {
            replacements.put("{ARENA}", "random");
        }
        replacements.put("{PLAYER}", challenged.getName());
        this.pl.send1vs1Message("youDuelPlayer", challenger, replacements);
        replacements.put("{PLAYER}", challenger.getName());
        this.pl.send1vs1Message("playerDuelsYou", challenged, replacements);
    }

    public boolean wasChallenged(String name) {
        for (DuelInvitation di : this.duelInvitations) {
            if (!di.getChallenged().equals(name)) continue;
            return true;
        }
        return false;
    }

    public boolean isChallenger(String name) {
        for (DuelInvitation di : this.duelInvitations) {
            if (!di.getChallenger().equals(name)) continue;
            return true;
        }
        return false;
    }

    public DuelInvitation getDuelInvitation(String name) {
        DuelInvitation latestDi = null;
        for (DuelInvitation di : this.duelInvitations) {
            if (!di.getChallenged().equals(name) && !di.getChallenger().equals(name) || latestDi != null && di.getTimeCreated() <= latestDi.getTimeCreated()) continue;
            latestDi = di;
        }
        return latestDi;
    }

    public void removeDuelInvitation(DuelInvitation di) {
        this.duelInvitations.remove(di);
    }

    private void startTimeOutTask() {
        Bukkit.getScheduler().runTaskTimer((Plugin)this.pl, new Runnable(){

            @Override
            public void run() {
                for (DuelInvitation di : DuelManager.this.duelInvitations) {
                    if (di.isAccepted() || System.currentTimeMillis() - (long)(DuelManager.this.pl.getConfig().getInt("duelInvitationTimeOut") * 1000) <= di.getTimeCreated()) continue;
                    DuelManager.this.duelInvitations.remove(di);
                }
            }
        }, 60L, 60L);
    }
}

