package com.gmail.Orscrider.PvP1vs1.deathmatch;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;

public class DmCountdownThread extends Thread {

    public enum DmCountdownType {
        BEFORE_TELEPORT,
        BEFORE_FIGHT
    }

    private final PvP1vs1 pl;
    private final DmGameManager gameManager;
    private final HashMap<String, String> replacements = new HashMap<>();
    private final DmCountdownType countdownMode;

    public DmCountdownThread(PvP1vs1 plugin, DmGameManager gameManager, DmCountdownType type) {
        this.pl = plugin;
        this.gameManager = gameManager;
        this.countdownMode = type;
    }

    @Override
    public void run() {
        if (countdownMode == DmCountdownType.BEFORE_TELEPORT) {
            gameManager.setArenaStatus(DmGameManager.DmArenaMode.COUNTDOWN_LOBBY);
            FileConfiguration cfg = gameManager.getArenaConfig();
            int duration = cfg.getInt("countdown.lobby.duration", 10);
            List<Player> lobbyList = gameManager.getLobbyPlayers();
            int max = gameManager.getMaxPlayers();
            if (lobbyList.size() < max) return;
            Player[] players = lobbyList.subList(0, max).toArray(new Player[0]);
            for (int i = duration; i >= 0 && gameManager.isEnabled(); i--) {
                if (gameManager.getLobbyPlayers().size() < max) break;
                replacements.put("{COUNTDOWN}", String.valueOf(i));
                if (i == duration) {
                    for (Player p : players) {
                        if (p != null && p.isOnline()) {
                            pl.sendDmMessage("countdownLobby", p, replacements);
                            if (cfg.getBoolean("countdown.lobby.sound", true)) playSound(p);
                        }
                    }
                    doSleep(1000);
                    continue;
                }
                if (i == 1 || i == 2 || i == 3 || i == 4 || i == 5 || i == 10 || i == 15) {
                    for (Player p : players) {
                        if (p != null && p.isOnline()) {
                            pl.sendDmMessage("countdownLobby", p, replacements);
                            if (cfg.getBoolean("countdown.lobby.sound", true)) playSound(p);
                        }
                    }
                } else if (i == 0) {
                    for (Player p : players) {
                        if (p != null && p.isOnline()) {
                            pl.messageParserDm("getTeleportedIntoArena", p);
                            if (cfg.getBoolean("countdown.lobby.sound", true)) playSound(p);
                        }
                    }
                    joinArena(players);
                }
                doSleep(1000);
            }
            return;
        }
        if (countdownMode == DmCountdownType.BEFORE_FIGHT) {
            gameManager.setArenaStatus(DmGameManager.DmArenaMode.COUNTDOWN_BEFORE_FIGHT);
            FileConfiguration cfg = gameManager.getArenaConfig();
            int duration = cfg.getInt("countdown.beforeFight.duration", 5);
            List<Player> arenaList = gameManager.getArenaPlayers();
            Player[] players = arenaList.toArray(new Player[0]);
            for (int i = duration; i >= 0; i--) {
                boolean allOnline = true;
                for (Player p : players) {
                    if (p == null || !p.isOnline()) { allOnline = false; break; }
                }
                if (!gameManager.isEnabled() || !allOnline) {
                    gameManager.reset();
                    break;
                }
                replacements.put("{COUNTDOWN}", String.valueOf(i));
                if (i == duration) {
                    for (Player p : players) {
                        if (p != null && p.isOnline()) {
                            pl.sendDmMessage("countdownBeforeFight", p, replacements);
                            if (cfg.getBoolean("countdown.beforeFight.sound", true)) playSound(p);
                            if (cfg.getBoolean("countdown.beforeFight.blindness", false)) {
                                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration * 20, 1));
                            }
                        }
                    }
                    doSleep(1000);
                    continue;
                }
                if (i == 1 || i == 2 || i == 3 || i == 4 || i == 5 || i == 10 || i == 15) {
                    for (Player p : players) {
                        if (p != null && p.isOnline()) {
                            pl.sendDmMessage("countdownBeforeFight", p, replacements);
                            if (cfg.getBoolean("countdown.beforeFight.sound", true)) playSound(p);
                        }
                    }
                } else if (i == 0) {
                    for (Player p : players) {
                        if (p != null && p.isOnline()) {
                            String msg = pl.getDataHandler().getDmMessagesConfig().getString("afterPlayerFreezing", "&aGo!");
                            p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.translateAlternateColorCodes('&', msg));
                            if (cfg.getBoolean("countdown.beforeFight.sound", true)) playSound(p);
                        }
                    }
                    gameManager.setFightStarted();
                }
                doSleep(1000);
            }
        }
    }

    private void doSleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private void joinArena(final Player[] arenaPlayers) {
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin) pl, () -> gameManager.joinArena(arenaPlayers));
    }

    private void playSound(Player p) {
        Sound sound = null;
        try { sound = Sound.valueOf("ENTITY_EXPERIENCE_ORB_TOUCH"); }
        catch (IllegalArgumentException e) {
            try { sound = Sound.valueOf("SUCCESSFUL_HIT"); } catch (IllegalArgumentException ignored) {}
        }
        if (sound != null) p.playSound(p.getLocation(), sound, 200.0f, 0.0f);
    }
}
