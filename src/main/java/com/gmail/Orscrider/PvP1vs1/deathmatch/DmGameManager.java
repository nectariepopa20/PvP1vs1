package com.gmail.Orscrider.PvP1vs1.deathmatch;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionController;
import com.gmail.Orscrider.PvP1vs1.util.FireworkRandomizer;
import com.gmail.Orscrider.PvP1vs1.util.LobbyLeaveItem;
import com.gmail.Orscrider.PvP1vs1.util.ValueContainer;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a single deathmatch arena: lobby, countdown, spawns, and fight.
 * Uses its own gamestates; no coupling to 1v1 GameManager.
 */
public class DmGameManager {

    public enum DmArenaMode {
        NORMAL,
        LOBBY,
        COUNTDOWN_LOBBY,
        PREPARATION_BEFORE_FIGHT,
        COUNTDOWN_BEFORE_FIGHT,
        FIGHT,
        WINNING_PHASE
    }

    private final PvP1vs1 pl;
    private final String arenaName;
    private final List<Player> lobbyPlayers = new ArrayList<>();
    private final List<Player> arenaPlayers = new ArrayList<>();
    private final List<Player> spectators = new ArrayList<>();
    private final Map<String, Integer> killCounts = new HashMap<>();
    private final Map<String, ValueContainer> valueContMap = new HashMap<>();
    private DmArenaMode arenaStatus = DmArenaMode.NORMAL;
    private boolean enabled = true;
    private DmTimeOut dmTimeOut;
    private int winningTimerTaskId = -1;
    private int lobbyCountdownTaskId = -1;
    private final AtomicInteger lobbyCountdownRemaining = new AtomicInteger(0);

    public DmGameManager(PvP1vs1 plugin, String arenaName) {
        this.pl = plugin;
        this.arenaName = arenaName;
        this.dmTimeOut = new DmTimeOut(plugin, this);
        this.dmTimeOut.startTimeOut();
        this.enabled = getArenaConfig().getBoolean("enabled");
    }

    public FileConfiguration getArenaConfig() {
        return pl.getDataHandler().getDmArenaConfig(arenaName);
    }

    public int getMaxPlayers() {
        return getArenaConfig().getInt("maxPlayers", 8);
    }

    public int getBareMinimumPlayers() {
        int max = getMaxPlayers();
        int bare = getArenaConfig().getInt("bareMinimumPlayers", 2);
        return Math.max(1, Math.min(bare, max - 1));
    }

    public int getOptimalMinimumPlayers() {
        int max = getMaxPlayers();
        int bare = getBareMinimumPlayers();
        int optimal = getArenaConfig().getInt("optimalMinimumPlayers", Math.max(bare, max - 1));
        return Math.max(bare, Math.min(optimal, max));
    }

    public int getLobbyCountdownDurationBare() {
        return getArenaConfig().getInt("countdown.lobby.durationBare", 60);
    }

    public int getLobbyCountdownDurationOptimal() {
        return getArenaConfig().getInt("countdown.lobby.durationOptimal", 10);
    }

    public boolean hasLobbySet() {
        return getArenaConfig().getString("lobby.world") != null;
    }

    public Location getLobbyLocation() {
        FileConfiguration cfg = getArenaConfig();
        return new Location(
                Bukkit.getWorld(cfg.getString("lobby.world", "world")),
                cfg.getDouble("lobby.x"),
                cfg.getDouble("lobby.y"),
                cfg.getDouble("lobby.z"),
                (float) cfg.getInt("lobby.yaw"),
                (float) cfg.getInt("lobby.pitch")
        );
    }

    public void joinLobby(Player p) {
        if (lobbyPlayers.size() >= getMaxPlayers()) return;
        ValueContainer valCont = new ValueContainer(
                p.getLocation(), p.getLevel(), p.getExp(),
                p.getInventory().getArmorContents(), p.getInventory().getContents(), p.getGameMode(),
                p.getFoodLevel(), p.getHealth(), p.getMaxHealth(), p.getActivePotionEffects()
        );
        valueContMap.put(p.getName(), valCont);
        lobbyPlayers.add(p);
        setArenaStatus(DmArenaMode.LOBBY);

        p.closeInventory();
        if (p.isInsideVehicle()) p.leaveVehicle();
        p.teleport(getLobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        p.getInventory().clear();
        p.getInventory().setItem(LobbyLeaveItem.getLeaveItemSlot(), LobbyLeaveItem.create(pl));
        p.updateInventory();
        p.setGameMode(GameMode.ADVENTURE);
        p.setFlying(false);
        startLobbyCountdownIfNeeded();
    }

    public void leaveLobby(Player p) {
        if (!lobbyPlayers.contains(p)) return;
        String leftName = p.getName();
        lobbyPlayers.remove(p);
        if (p.isOnline()) {
            restorePlayer(p);
        } else {
            valueContMap.remove(p.getName());
        }
        if (lobbyPlayers.isEmpty()) {
            setArenaStatus(DmArenaMode.NORMAL);
            cancelLobbyCountdown();
        } else {
            setArenaStatus(DmArenaMode.LOBBY);
            broadcastPlayerLeftLobby(leftName);
            if (lobbyPlayers.size() < getBareMinimumPlayers()) {
                cancelLobbyCountdown();
                setArenaStatus(DmArenaMode.LOBBY);
            }
        }
    }

    public void removeFromLobbyOnQuit(Player p) {
        if (!lobbyPlayers.contains(p)) return;
        String leftName = p.getName();
        lobbyPlayers.remove(p);
        valueContMap.remove(p.getName());
        if (lobbyPlayers.isEmpty()) {
            setArenaStatus(DmArenaMode.NORMAL);
            cancelLobbyCountdown();
        } else {
            setArenaStatus(DmArenaMode.LOBBY);
            broadcastPlayerLeftLobby(leftName);
            if (lobbyPlayers.size() < getBareMinimumPlayers()) {
                cancelLobbyCountdown();
                setArenaStatus(DmArenaMode.LOBBY);
            }
        }
    }

    private void broadcastPlayerLeftLobby(String playerName) {
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("{PLAYER}", playerName);
        replacements.put("{COUNT}", String.valueOf(lobbyPlayers.size()));
        replacements.put("{MAX}", String.valueOf(getMaxPlayers()));
        for (Player other : lobbyPlayers) {
            if (other != null && other.isOnline()) {
                pl.sendDmMessage("playerLeftLobby", other, replacements);
            }
        }
    }

    public void broadcastPlayerJoinedLobby(Player joiner) {
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("{PLAYER}", joiner.getName());
        replacements.put("{COUNT}", String.valueOf(lobbyPlayers.size()));
        replacements.put("{MAX}", String.valueOf(getMaxPlayers()));
        for (Player other : lobbyPlayers) {
            if (other != null && other.isOnline()) {
                pl.sendDmMessage("playerJoinedLobby", other, replacements);
            }
        }
    }

    /**
     * Called when a player joins the lobby. Starts the lobby countdown when we reach
     * bareMinimumPlayers (with longer duration), or shortens it when we reach optimalMinimumPlayers.
     */
    public void startGame() {
        startLobbyCountdownIfNeeded();
    }

    private void startLobbyCountdownIfNeeded() {
        if (getArenaStatus() != DmArenaMode.LOBBY && getArenaStatus() != DmArenaMode.COUNTDOWN_LOBBY) return;
        if (!getArenaConfig().getBoolean("countdown.lobby.enabled", true)) return;
        int size = lobbyPlayers.size();
        int bare = getBareMinimumPlayers();
        if (size < bare) return;

        if (lobbyCountdownTaskId == -1) {
            setArenaStatus(DmArenaMode.COUNTDOWN_LOBBY);
            lobbyCountdownRemaining.set(getLobbyCountdownDurationBare());
            for (Player p : lobbyPlayers) {
                if (p != null && p.isOnline()) {
                    HashMap<String, String> reps = new HashMap<>();
                    reps.put("{COUNT}", String.valueOf(size));
                    reps.put("{MAX}", String.valueOf(getMaxPlayers()));
                    reps.put("{DURATION}", String.valueOf(getLobbyCountdownDurationBare()));
                    pl.sendDmMessage("lobbyCountdownStarted", p, reps);
                }
            }
            lobbyCountdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin) pl, this::lobbyCountdownTick, 20L, 20L);
        } else if (size >= getOptimalMinimumPlayers()) {
            int optimalDuration = getLobbyCountdownDurationOptimal();
            int current = lobbyCountdownRemaining.get();
            if (current > optimalDuration) {
                lobbyCountdownRemaining.set(optimalDuration);
                for (Player p : lobbyPlayers) {
                    if (p != null && p.isOnline()) {
                        HashMap<String, String> reps = new HashMap<>();
                        reps.put("{COUNTDOWN}", String.valueOf(optimalDuration));
                        pl.sendDmMessage("lobbyCountdownShortened", p, reps);
                    }
                }
            }
        }
    }

    private void lobbyCountdownTick() {
        if (getArenaStatus() != DmArenaMode.COUNTDOWN_LOBBY || lobbyCountdownTaskId < 0) return;
        int size = lobbyPlayers.size();
        if (size < getBareMinimumPlayers()) {
            cancelLobbyCountdown();
            setArenaStatus(DmArenaMode.LOBBY);
            for (Player p : lobbyPlayers) {
                if (p != null && p.isOnline()) pl.sendDmMessage("lobbyCountdownCancelled", p, new HashMap<>());
            }
            return;
        }
        if (size >= getOptimalMinimumPlayers()) {
            int opt = getLobbyCountdownDurationOptimal();
            int cur = lobbyCountdownRemaining.get();
            if (cur > opt) lobbyCountdownRemaining.set(opt);
        }
        int remaining = lobbyCountdownRemaining.decrementAndGet();
        FileConfiguration cfg = getArenaConfig();
        HashMap<String, String> replacements = new HashMap<>();
        if (remaining == 10 || remaining == 5 || remaining == 4 || remaining == 3 || remaining == 2 || remaining == 1) {
            replacements.put("{COUNTDOWN}", String.valueOf(remaining));
            for (Player p : lobbyPlayers) {
                if (p != null && p.isOnline()) {
                    pl.sendDmMessage("countdownLobby", p, replacements);
                    if (cfg.getBoolean("countdown.lobby.sound", true)) playLobbyCountdownSound(p);
                }
            }
        }
        if (remaining <= 0) {
            cancelLobbyCountdown();
            int max = getMaxPlayers();
            int take = Math.min(lobbyPlayers.size(), max);
            Player[] players = lobbyPlayers.subList(0, take).toArray(new Player[0]);
            for (Player p : players) {
                if (p != null && p.isOnline()) pl.messageParserDm("getTeleportedIntoArena", p);
            }
            joinArena(players);
        }
    }

    private void playLobbyCountdownSound(Player p) {
        org.bukkit.Sound sound = null;
        try { sound = org.bukkit.Sound.valueOf("ENTITY_EXPERIENCE_ORB_TOUCH"); }
        catch (IllegalArgumentException e) {
            try { sound = org.bukkit.Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"); } catch (IllegalArgumentException ignored) {}
        }
        if (sound != null) p.playSound(p.getLocation(), sound, 0.5f, 1f);
    }

    public void cancelLobbyCountdown() {
        if (lobbyCountdownTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(lobbyCountdownTaskId);
            lobbyCountdownTaskId = -1;
        }
        lobbyCountdownRemaining.set(0);
    }

    /**
     * Force start the game with current lobby players (requires at least 2).
     * Caller should check player is in this arena's lobby.
     * @return true if started, false if not enough players
     */
    public boolean forceStartGame() {
        if (getArenaStatus() != DmArenaMode.LOBBY && getArenaStatus() != DmArenaMode.COUNTDOWN_LOBBY) return false;
        if (lobbyPlayers.size() < 2) return false;
        cancelLobbyCountdown();
        int take = Math.min(lobbyPlayers.size(), getMaxPlayers());
        Player[] players = lobbyPlayers.subList(0, take).toArray(new Player[0]);
        joinArena(players);
        return true;
    }

    public void joinArena(Player[] players) {
        arenaPlayers.clear();
        for (Player p : players) {
            if (p == null || !p.isOnline()) continue;
            lobbyPlayers.remove(p);
            arenaPlayers.add(p);
        }
        lobbyPlayers.clear();
        setArenaStatus(DmArenaMode.PREPARATION_BEFORE_FIGHT);
        dmTimeOut.resetTimeOut();

        killCounts.clear();
        spectators.clear();
        int idx = 0;
        for (Player p : arenaPlayers) {
            if (!p.isOnline()) continue;
            killCounts.put(p.getName(), 0);
            Location spawn = getSpawnLocation(idx + 1);
            if (spawn != null) {
                p.closeInventory();
                if (p.isInsideVehicle()) p.leaveVehicle();
                p.teleport(spawn, PlayerTeleportEvent.TeleportCause.UNKNOWN);
            }
            p.getInventory().clear();
            p.getInventory().setArmorContents(pl.getDataHandler().getDmItems(arenaName, "inventory.armor"));
            p.getInventory().setContents(pl.getDataHandler().getDmItems(arenaName, "inventory.items"));
            p.updateInventory();
            p.setGameMode(GameMode.SURVIVAL);
            p.setFlying(false);
            p.setFireTicks(0);
            double health = getArenaConfig().getDouble("health", 20.0);
            p.setMaxHealth(health);
            p.setHealth(health);
            p.setFoodLevel(20);
            for (PotionEffect pe : p.getActivePotionEffects()) {
                p.removePotionEffect(pe.getType());
            }
            idx++;
        }
        updateAllListNames();

        if (getArenaConfig().getBoolean("countdown.beforeFight.enabled", true)) {
            DmCountdownThread thread = new DmCountdownThread(pl, this, DmCountdownThread.DmCountdownType.BEFORE_FIGHT);
            thread.start();
        } else {
            setArenaStatus(DmArenaMode.FIGHT);
        }
    }

    private Location getSpawnLocation(int spawnIndex) {
        FileConfiguration cfg = getArenaConfig();
        String worldName = cfg.getString("spawn" + spawnIndex + ".world");
        if (worldName == null) return null;
        return new Location(
                Bukkit.getWorld(worldName),
                cfg.getDouble("spawn" + spawnIndex + ".x"),
                cfg.getDouble("spawn" + spawnIndex + ".y"),
                cfg.getDouble("spawn" + spawnIndex + ".z"),
                (float) cfg.getInt("spawn" + spawnIndex + ".yaw"),
                (float) cfg.getInt("spawn" + spawnIndex + ".pitch")
        );
    }

    public void setFightStarted() {
        setArenaStatus(DmArenaMode.FIGHT);
    }

    public void onPlayerDeath(Player dead, Player killer) {
        DBConnectionController db = DBConnectionController.getInstance();
        if (dead != null) {
            db.addPlayerDmDeath(dead.getUniqueId().toString());
        }
        if (killer != null && killer != dead) {
            db.addPlayerDmKill(killer.getUniqueId().toString());
            killCounts.merge(killer.getName(), 1, Integer::sum);
            HashMap<String, String> reps = new HashMap<>();
            reps.put("{X}", killer.getName());
            reps.put("{Y}", dead.getName());
            String msg = pl.getDataHandler().getDmMessagesConfig().getString("deathBroadcast", "&c{X} &7killed &c{Y}");
            if (msg != null) {
                msg = org.bukkit.ChatColor.translateAlternateColorCodes('&', msg.replace("{X}", killer.getName()).replace("{Y}", dead.getName()));
                for (Player p : arenaPlayers) {
                    if (p != null && p.isOnline()) p.sendMessage(pl.getDataHandler().getDmPrefix() + msg);
                }
                for (Player p : spectators) {
                    if (p != null && p.isOnline()) p.sendMessage(pl.getDataHandler().getDmPrefix() + msg);
                }
            }
        }
        updateAllListNames();
        arenaPlayers.remove(dead);
        teleportToLobbyAsSpectator(dead);
        if (arenaPlayers.size() <= 1) {
            Player winner = arenaPlayers.isEmpty() ? null : arenaPlayers.get(0);
            startWinningPhase(winner);
        }
    }

    private void teleportToLobbyAsSpectator(Player p) {
        spectators.add(p);
        p.closeInventory();
        if (p.isInsideVehicle()) p.leaveVehicle();
        p.teleport(getLobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        p.getInventory().clear();
        p.getInventory().setItem(LobbyLeaveItem.getLeaveItemSlot(), LobbyLeaveItem.create(pl));
        p.updateInventory();
        p.setGameMode(GameMode.ADVENTURE);
        p.setFlying(false);
        p.setHealth(p.getMaxHealth());
        updateAllListNames();
    }

    private void updateAllListNames() {
        for (Player p : arenaPlayers) {
            if (p != null && p.isOnline()) {
                int k = killCounts.getOrDefault(p.getName(), 0);
                p.setPlayerListName(p.getName() + " [" + k + "]");
            }
        }
        for (Player p : spectators) {
            if (p != null && p.isOnline()) {
                int k = killCounts.getOrDefault(p.getName(), 0);
                p.setPlayerListName(p.getName() + " [" + k + "]");
            }
        }
    }

    public void leaveSpectator(Player p) {
        if (!spectators.contains(p)) return;
        spectators.remove(p);
        restorePlayer(p);
        resetPlayerListName(p);
    }

    public void removeSpectatorOnQuit(Player p) {
        if (!spectators.contains(p)) return;
        spectators.remove(p);
        valueContMap.remove(p.getName());
        resetPlayerListName(p);
    }

    private void resetPlayerListName(Player p) {
        if (p != null) try { p.setPlayerListName(p.getName()); } catch (Exception ignored) {}
    }

    public void startWinningPhase(Player winner) {
        setArenaStatus(DmArenaMode.WINNING_PHASE);
        dmTimeOut.cancelTimeOut();
        int totalSec = getArenaConfig().getInt("winningTimer", 10);
        int fireworkSec = Math.max(1, totalSec / 2);
        if (winner != null && winner.isOnline()) {
            DBConnectionController.getInstance().addPlayerDmWin(winner.getUniqueId().toString());
            HashMap<String, String> replacements = new HashMap<>();
            replacements.put("{WINNER}", winner.getName());
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("winAnnounce", winner, replacements);
        }
        final Player winRef = winner;
        final int fireworkTicks = fireworkSec * 20;
        final int totalTicks = totalSec * 20;
        winningTimerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin) pl, new Runnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 20;
                if (winRef != null && winRef.isOnline() && ticks <= fireworkTicks) {
                    spawnFirework(winRef.getLocation());
                }
                if (ticks >= totalTicks) {
                    Bukkit.getScheduler().cancelTask(winningTimerTaskId);
                    winningTimerTaskId = -1;
                    finishWinningPhaseAndRestoreAll(winRef);
                }
            }
        }, 20L, 20L);
    }

    private void finishWinningPhaseAndRestoreAll(Player winner) {
        if (winner != null && winner.isOnline()) restorePlayer(winner);
        for (Player p : new ArrayList<>(spectators)) {
            if (p != null && p.isOnline()) restorePlayer(p);
        }
        spectators.clear();
        arenaPlayers.clear();
        killCounts.clear();
        reset();
    }

    private void spawnFirework(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        Random r = new Random();
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(FireworkEffect.builder()
                .with(FireworkRandomizer.fireworkType(r.nextInt(5)))
                .withColor(FireworkRandomizer.fireworkColor(r.nextInt(17)))
                .withColor(FireworkRandomizer.fireworkColor(r.nextInt(17)))
                .withFade(FireworkRandomizer.fireworkColor(r.nextInt(17)))
                .flicker(r.nextBoolean()).trail(r.nextBoolean()).build());
        fwm.setPower(1);
        fw.setFireworkMeta(fwm);
    }

    public void endGame(Player winner) {
        startWinningPhase(winner);
    }

    public boolean isSpectator(Player p) {
        return spectators.contains(p);
    }

    public List<Player> getSpectators() {
        return new ArrayList<>(spectators);
    }

    public void restorePlayer(Player p) {
        String name = p.getName();
        if (!valueContMap.containsKey(name)) return;
        ValueContainer values = valueContMap.get(name);
        p.closeInventory();
        try {
            p.getInventory().setContents(values.getItems());
            p.getInventory().setArmorContents(values.getArmour());
        } catch (NullPointerException ignored) {}
        p.setLevel(values.getLevel());
        p.setExp(values.getExp());
        p.setGameMode(values.getGameMode());
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin) pl, () -> p.setFireTicks(0), 1L);
        p.setMaxHealth(values.getMaxHealth());
        p.setHealth(values.getHealth());
        p.setFoodLevel(values.getFood());
        for (PotionEffect pe : p.getActivePotionEffects()) {
            p.removePotionEffect(pe.getType());
        }
        p.addPotionEffects(values.getPotionEffects());
        p.teleport(values.getLoc(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        valueContMap.remove(name);
        resetPlayerListName(p);
    }

    public void reset() {
        cancelLobbyCountdown();
        arenaStatus = lobbyPlayers.isEmpty() ? DmArenaMode.NORMAL : DmArenaMode.LOBBY;
        dmTimeOut.resetTimeOut();
        arenaPlayers.clear();
        spectators.clear();
        killCounts.clear();
        if (winningTimerTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(winningTimerTaskId);
            winningTimerTaskId = -1;
        }
    }

    public boolean isInLobby(Player p) {
        return lobbyPlayers.contains(p);
    }

    public boolean arenaPlayersContains(Player p) {
        return arenaPlayers.contains(p);
    }

    public List<Player> getLobbyPlayers() {
        return new ArrayList<>(lobbyPlayers);
    }

    public List<Player> getArenaPlayers() {
        return new ArrayList<>(arenaPlayers);
    }

    public int getLobbySize() {
        return lobbyPlayers.size();
    }

    public String getArenaName() {
        return arenaName;
    }

    public DmArenaMode getArenaStatus() {
        return arenaStatus;
    }

    public void setArenaStatus(DmArenaMode mode) {
        this.arenaStatus = mode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) dmTimeOut.startTimeOut();
    }

    public void cancelTimeOut() {
        dmTimeOut.cancelTimeOut();
    }

    public int getTimeOut() {
        return dmTimeOut.getTimeOut();
    }
}
