package com.gmail.Orscrider.PvP1vs1.deathmatch;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.util.LobbyLeaveItem;
import com.gmail.Orscrider.PvP1vs1.util.ValueContainer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        FIGHT
    }

    private final PvP1vs1 pl;
    private final String arenaName;
    private final List<Player> lobbyPlayers = new ArrayList<>();
    private final List<Player> arenaPlayers = new ArrayList<>();
    private final Map<String, ValueContainer> valueContMap = new HashMap<>();
    private DmArenaMode arenaStatus = DmArenaMode.NORMAL;
    private boolean enabled = true;
    private DmTimeOut dmTimeOut;

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
        } else {
            setArenaStatus(DmArenaMode.LOBBY);
            broadcastPlayerLeftLobby(leftName);
        }
    }

    public void removeFromLobbyOnQuit(Player p) {
        if (!lobbyPlayers.contains(p)) return;
        String leftName = p.getName();
        lobbyPlayers.remove(p);
        valueContMap.remove(p.getName());
        if (lobbyPlayers.isEmpty()) {
            setArenaStatus(DmArenaMode.NORMAL);
        } else {
            setArenaStatus(DmArenaMode.LOBBY);
            broadcastPlayerLeftLobby(leftName);
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

    public void startGame() {
        int max = getMaxPlayers();
        if (lobbyPlayers.size() >= max && getArenaStatus() == DmArenaMode.LOBBY) {
            boolean lobbyCountdown = getArenaConfig().getBoolean("countdown.lobby.enabled", true);
            if (lobbyCountdown) {
                DmCountdownThread thread = new DmCountdownThread(pl, this, DmCountdownThread.DmCountdownType.BEFORE_TELEPORT);
                thread.start();
            } else {
                Player[] players = lobbyPlayers.subList(0, max).toArray(new Player[0]);
                joinArena(players);
            }
        }
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

        int idx = 0;
        for (Player p : arenaPlayers) {
            if (!p.isOnline()) continue;
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

    public void onPlayerDeath(Player dead) {
        arenaPlayers.remove(dead);
        restorePlayer(dead);
        if (arenaPlayers.size() <= 1) {
            Player winner = arenaPlayers.isEmpty() ? null : arenaPlayers.get(0);
            endGame(winner);
        }
    }

    public void endGame(Player winner) {
        for (Player p : new ArrayList<>(arenaPlayers)) {
            if (p != null && p.isOnline()) restorePlayer(p);
        }
        arenaPlayers.clear();
        reset();
        if (winner != null && winner.isOnline()) {
            HashMap<String, String> replacements = new HashMap<>();
            replacements.put("{WINNER}", winner.getName());
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("winAnnounce", winner, replacements);
        }
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
        p.teleport(values.getLoc(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
        valueContMap.remove(name);
    }

    public void reset() {
        arenaStatus = lobbyPlayers.isEmpty() ? DmArenaMode.NORMAL : DmArenaMode.LOBBY;
        dmTimeOut.resetTimeOut();
        arenaPlayers.clear();
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
