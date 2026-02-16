package com.gmail.Orscrider.PvP1vs1.deathmatch;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DmArenaManager {

    private final PvP1vs1 pl;
    private final HashMap<String, DmGameManager> arenas = new HashMap<>();
    private final HashMap<String, String> replacements = new HashMap<>();

    public DmArenaManager(PvP1vs1 plugin) {
        this.pl = plugin;
        for (String arenaName : pl.getDataHandler().getDmArenaNames()) {
            arenas.put(arenaName, new DmGameManager(plugin, arenaName));
        }
    }

    public DmGameManager getArena(String arenaName) {
        return arenas.get(arenaName);
    }

    public boolean arenaExists(String arenaName) {
        return arenas.containsKey(arenaName);
    }

    public boolean addArena(String arenaName) {
        if (arenas.containsKey(arenaName)) return false;
        pl.getDataHandler().addDmArenaConfig(arenaName);
        arenas.put(arenaName, new DmGameManager(pl, arenaName));
        return true;
    }

    public boolean delArena(String arenaName) {
        if (!arenas.containsKey(arenaName)) return false;
        disableArena(arenaName);
        arenas.remove(arenaName);
        pl.getDataHandler().delDmArenaConfig(arenaName);
        return true;
    }

    public HashMap<String, DmGameManager> getArenas() {
        return arenas;
    }

    public HashMap<String, DmGameManager> getEnabledArenas() {
        HashMap<String, DmGameManager> out = new HashMap<>();
        for (Map.Entry<String, DmGameManager> e : arenas.entrySet()) {
            if (e.getValue().isEnabled()) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    public HashMap<String, DmGameManager> getDisabledArenas() {
        HashMap<String, DmGameManager> out = new HashMap<>();
        for (Map.Entry<String, DmGameManager> e : arenas.entrySet()) {
            if (!e.getValue().isEnabled()) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    public boolean disableArena(String arenaName) {
        if (!arenas.containsKey(arenaName)) return false;
        DmGameManager arena = arenas.get(arenaName);
        arena.setEnabled(false);
        pl.getDataHandler().getDmArenaConfig(arenaName).set("enabled", false);
        pl.getDataHandler().saveDmArenaConfig(arenaName);
        for (Player p : arena.getLobbyPlayers()) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("arenaWasDisabled", p, replacements);
            arena.leaveLobby(p);
        }
        for (Player p : arena.getArenaPlayers()) {
            if (p != null && p.isOnline()) {
                replacements.put("{ARENA}", arenaName);
                pl.sendDmMessage("arenaWasDisabled", p, replacements);
                arena.restorePlayer(p);
            }
        }
        arena.reset();
        arena.cancelTimeOut();
        return true;
    }

    public String getRandomArena() {
        HashMap<String, DmGameManager> enabled = getEnabledArenas();
        if (enabled.isEmpty()) return null;
        ArrayList<String> names = new ArrayList<>(enabled.keySet());
        return names.get(new Random().nextInt(names.size()));
    }

    public boolean isFree(Player p) {
        for (DmGameManager arena : arenas.values()) {
            if (arena.arenaPlayersContains(p) || arena.isInLobby(p)) return false;
        }
        return true;
    }
}
