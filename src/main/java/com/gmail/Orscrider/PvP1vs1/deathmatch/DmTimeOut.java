package com.gmail.Orscrider.PvP1vs1.deathmatch;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class DmTimeOut {

    private final PvP1vs1 plugin;
    private final DmGameManager arena;
    private Integer timeOut;
    private final HashMap<String, String> replacements = new HashMap<>();
    private int taskID = -1;

    public DmTimeOut(PvP1vs1 plugin, DmGameManager gameManager) {
        this.plugin = plugin;
        this.arena = gameManager;
        this.timeOut = gameManager.getArenaConfig().getInt("timeOut", 300);
    }

    public void startTimeOut() {
        if (taskID >= 0) return;
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin) plugin, () -> {
            if (arena.getArenaStatus() != DmGameManager.DmArenaMode.FIGHT) {
                resetTimeOut();
                return;
            }
            List<Player> players = arena.getArenaPlayers();
            if (timeOut == 10 || timeOut == 20 || timeOut == 30 || timeOut == 60 || timeOut == 120 || timeOut == 180 || timeOut == 300 || timeOut == 600) {
                replacements.put("{TIME_LEFT}", String.valueOf(timeOut));
                for (Player p : players) {
                    if (p != null && p.isOnline()) plugin.sendDmMessage("timeLeft", p, replacements);
                }
            }
            if (timeOut <= 0) {
                for (Player p : players) {
                    if (p != null && p.isOnline()) plugin.messageParserDm("timeOut", p);
                }
                arena.endGame(getTimeOutWinner());
                return;
            }
            timeOut--;
        }, 0L, 20L);
    }

    public void resetTimeOut() {
        if (arena.getArenaConfig() != null) {
            timeOut = arena.getArenaConfig().getInt("timeOut", 300);
        }
    }

    public int getTimeOut() {
        return timeOut != null ? timeOut : 0;
    }

    public void cancelTimeOut() {
        if (taskID >= 0) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }

    private Player getTimeOutWinner() {
        List<Player> list = arena.getArenaPlayers();
        if (list.isEmpty()) return null;
        if (list.size() == 1) return list.get(0);
        Player best = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            Player p = list.get(i);
            if (p != null && p.getHealth() > best.getHealth()) best = p;
        }
        List<Player> tied = new java.util.ArrayList<>();
        for (Player p : list) {
            if (p != null && Math.abs(p.getHealth() - best.getHealth()) < 0.01) tied.add(p);
        }
        if (tied.size() <= 1) return best;
        return tied.get(new Random().nextInt(tied.size()));
    }
}
