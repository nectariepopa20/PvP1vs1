/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.FireworkEffect
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Firework
 *  org.bukkit.entity.Player
 *  org.bukkit.event.player.PlayerTeleportEvent$TeleportCause
 *  org.bukkit.inventory.meta.FireworkMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 */
package com.gmail.Orscrider.PvP1vs1.arena;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.arena.CountdownThread;
import com.gmail.Orscrider.PvP1vs1.arena.TimeOut;
import com.gmail.Orscrider.PvP1vs1.duel.DuelInvitation;
import com.gmail.Orscrider.PvP1vs1.metrics.MetricsHandler;
import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionController;
import com.gmail.Orscrider.PvP1vs1.util.FireworkRandomizer;
import com.gmail.Orscrider.PvP1vs1.util.LobbyLeaveItem;
import com.gmail.Orscrider.PvP1vs1.util.Utils;
import com.gmail.Orscrider.PvP1vs1.util.ValueContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

public class GameManager {
    private static final int MAX_LOBBY_PLAYERS = 2;

    private PvP1vs1 pl;
    private final List<Player> lobbyPlayers = new ArrayList<>();
    private TimeOut timeOut = null;
    private arenaMode arenaStatus = arenaMode.NORMAL;
    private String arenaName;
    private boolean isEnabled = true;
    private int totalRounds = 3;
    private Player[] arenaPlayers = new Player[2];
    private int[] playerRoundWins = new int[2];
    private HashMap<String, ValueContainer> valueContMap = new HashMap();

    public GameManager(PvP1vs1 plugin, String arenaName) {
        this.pl = plugin;
        this.arenaName = arenaName;
        this.timeOut = new TimeOut(plugin, this);
        this.timeOut.startTimeOut();
        this.isEnabled = this.getArenaConfig().getBoolean("enabled");
        this.totalRounds = this.getArenaConfig().getInt("rounds");
    }

    public void joinArena(Player[] players) {
        this.arenaPlayers = players;
        for (Player p : this.arenaPlayers) {
            if (!p.isOnline()) {
                this.pl.messageParser("yourOpponentLoggedOut", p == this.arenaPlayers[0] ? this.arenaPlayers[1] : this.arenaPlayers[0]);
                this.reset();
                return;
            }
            if (p.isDead()) {
                this.pl.messageParser("yourOpponentIsDead", p == this.arenaPlayers[0] ? this.arenaPlayers[1] : this.arenaPlayers[0]);
                this.pl.messageParser("youAreDead", p == this.arenaPlayers[0] ? this.arenaPlayers[0] : this.arenaPlayers[1]);
                this.reset();
                return;
            }
        }
        this.lobbyPlayers.clear();
        this.totalRounds = this.getArenaConfig().getInt("rounds");
        this.setArenaStatus(arenaMode.PREPERATION_BEFORE_FIGHT);
        if (this.pl.getDuelManager().isChallenger(this.arenaPlayers[0].getName())) {
            this.pl.getDuelManager().removeDuelInvitation(this.pl.getDuelManager().getDuelInvitation(this.arenaPlayers[0].getName()));
        }
        this.readyPlayers();
        List<String> commands = this.getArenaConfig().getStringList("commandsToRunAtStart");
        if (!commands.isEmpty()) {
            for (String command : commands) {
                String cmd = command.replace("{PLAYER1}", this.arenaPlayers[0].getName()).replace("{PLAYER2}", this.arenaPlayers[1].getName()).replace("{ARENA}", this.getArenaName()).replace("{ROUNDS}", String.valueOf(this.getTotalRounds()));
                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), cmd);
            }
        }
    }

    public void readyPlayers() {
        this.timeOut.resetTimeOut();
        Location spawn1 = new Location(Bukkit.getWorld((String)this.getArenaConfig().getString("spawn1.world")), this.getArenaConfig().getDouble("spawn1.x"), this.getArenaConfig().getDouble("spawn1.y"), this.getArenaConfig().getDouble("spawn1.z"), (float)this.getArenaConfig().getInt("spawn1.yaw"), (float)this.getArenaConfig().getInt("spawn1.pitch"));
        Location spawn2 = new Location(Bukkit.getWorld((String)this.getArenaConfig().getString("spawn2.world")), this.getArenaConfig().getDouble("spawn2.x"), this.getArenaConfig().getDouble("spawn2.y"), this.getArenaConfig().getDouble("spawn2.z"), (float)this.getArenaConfig().getInt("spawn2.yaw"), (float)this.getArenaConfig().getInt("spawn2.pitch"));
        for (final Player p : this.arenaPlayers) {
            p.closeInventory();
            if (p.isInsideVehicle()) {
                p.leaveVehicle();
            }
            p.teleport(p == this.arenaPlayers[0] ? spawn1 : spawn2, PlayerTeleportEvent.TeleportCause.UNKNOWN);
            p.getInventory().clear();
            p.getInventory().setArmorContents(this.pl.getDataHandler().getItems(this.arenaName, "inventory.armor"));
            p.getInventory().setContents(this.pl.getDataHandler().getItems(this.arenaName, "inventory.items"));
            p.updateInventory();
            p.setGameMode(GameMode.SURVIVAL);
            p.setFlying(false);
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this.pl, new Runnable(){

                @Override
                public void run() {
                    p.setFireTicks(0);
                }
            }, 1L);
            p.setMaxHealth(this.getArenaConfig().getDouble("health"));
            p.setHealth(this.getArenaConfig().getDouble("health"));
            p.setFoodLevel(20);
            for (PotionEffect pe : p.getActivePotionEffects()) {
                p.removePotionEffect(pe.getType());
            }
            this.refreshPlayer(p);
        }
        if (this.getArenaConfig().getBoolean("countdown.beforeFight.enabled")) {
            CountdownThread thread = new CountdownThread(this.pl, this, CountdownThread.countdownType.BEFORE_FIGHT);
            thread.start();
        } else {
            this.setArenaStatus(arenaMode.FIGHT);
        }
    }

    public void afterFight(Player winner) {
        Player loser;
        Player player = loser = winner == this.arenaPlayers[0] ? this.arenaPlayers[1] : this.arenaPlayers[0];
        if (this.getArenaConfig().getBoolean("winAnnouncement")) {
            Bukkit.broadcastMessage((String)(this.pl.getPrefix() + ChatColor.translateAlternateColorCodes((char)'&', (String)this.pl.getDataHandler().getMessagesConfig().getString("winAnnounce")).replace("{WINNER}", winner.getName()).replace("{LOSER}", loser.getName()).replace("{ARENA}", this.arenaName)));
        }
        this.reset();
        if (this.getArenaConfig().getBoolean("firework.inArena")) {
            this.spawnFirework(winner.getLocation());
        }
        this.restorePlayer(winner);
        this.restorePlayer(loser);
        this.refreshPlayer(winner);
        this.refreshPlayer(loser);
        if (this.getArenaConfig().getBoolean("firework.afterTeleport")) {
            this.spawnFirework(winner.getLocation());
        }
        DBConnectionController dbController = DBConnectionController.getInstance();
        dbController.savePlayerScore(winner.getUniqueId().toString(), this.arenaName, this.getArenaConfig().getInt("points.win"));
        dbController.savePlayerScore(loser.getUniqueId().toString(), this.arenaName, this.getArenaConfig().getInt("points.lose"));
        dbController.addPlayerWin(winner.getUniqueId().toString());
        dbController.addPlayerLoss(loser.getUniqueId().toString());
        List<String> commands = this.getArenaConfig().getStringList("commandsToRunAtEnd");
        if (!commands.isEmpty()) {
            for (String command : commands) {
                String cmd = command.replace("{WINNER}", winner.getName()).replace("{LOSER}", loser.getName()).replace("{ARENA}", this.getArenaName()).replace("{ROUNDS}", String.valueOf(this.getTotalRounds()));
                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), cmd);
            }
        }
        if (this.getArenaConfig().getBoolean("prize.items.enabled")) {
            winner.getInventory().addItem(this.pl.getDataHandler().getItems(this.arenaName, "prize.items.itemPrizes"));
        }
        if (this.getArenaConfig().getBoolean("prize.economy.enabled")) {
            PvP1vs1.economy.depositPlayer(Bukkit.getOfflinePlayer((String)winner.getName()), this.pl.getConfig().getDouble("prize.economy.amount"));
        }
        MetricsHandler.getInstance().increaseGamesPlayed();
    }

    public void restorePlayer(final Player p) {
        String name = p.getName();
        if (this.valueContMapcontains(name)) {
            ValueContainer values = this.valueContMap.get(name);
            p.closeInventory();
            try {
                p.getInventory().setContents(values.getItems());
                p.getInventory().setArmorContents(values.getArmour());
            }
            catch (NullPointerException nullPointerException) {
                // empty catch block
            }
            p.setLevel(values.getLevel());
            p.setExp(values.getExp());
            p.setGameMode(values.getGameMode());
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this.pl, new Runnable(){

                @Override
                public void run() {
                    p.setFireTicks(0);
                }
            }, 1L);
            p.setMaxHealth(values.getMaxHealth());
            p.setHealth(values.getHealth());
            p.setFoodLevel(values.getFood());
            for (PotionEffect pe : p.getActivePotionEffects()) {
                p.removePotionEffect(pe.getType());
            }
            p.addPotionEffects(values.getPotionEffects());
            p.teleport(values.getLoc(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
            this.valueContMap.remove(name);
        }
    }

    /**
     * Starts the lobby countdown when 2 players are in lobby, then teleports them to arena.
     */
    public void startGame() {
        if (this.lobbyPlayers.size() >= MAX_LOBBY_PLAYERS && this.getArenaStatus() == arenaMode.LOBBY) {
            boolean lobbyCountdown = this.getArenaConfig().getBoolean("countdown.lobby.enabled", this.getArenaConfig().getBoolean("countdown.beforeTeleport.enabled", true));
            if (lobbyCountdown) {
                CountdownThread thread = new CountdownThread(this.pl, this, CountdownThread.countdownType.BEFORE_TELEPORT);
                thread.start();
            } else {
                Player[] players = new Player[]{this.lobbyPlayers.get(0), this.lobbyPlayers.get(1)};
                this.joinArena(players);
            }
        }
    }

    public void removeDuelPartner(Player p) {
        if (!this.getArenaStatus().equals(arenaMode.PREPERATION_BEFORE_FIGHT)
                && this.pl.getDuelManager().getDuelInvitation(p.getName()) != null
                && this.pl.getDuelManager().getDuelInvitation(p.getName()).isAccepted()) {
            DuelInvitation di = this.pl.getDuelManager().getDuelInvitation(p.getName());
            String otherName = di.getChallenger().equals(p.getName()) ? di.getChallenged() : di.getChallenger();
            Player otherPlayer = Bukkit.getPlayer(otherName);
            this.pl.getDuelManager().removeDuelInvitation(di);
            if (otherPlayer != null && otherPlayer.isOnline() && this.lobbyPlayers.contains(otherPlayer)) {
                HashMap<String, String> replacements = new HashMap<>();
                replacements.put("{PLAYER}", p.getName());
                this.pl.send1vs1Message("duelPartnerLeftLobby", otherPlayer, replacements);
            }
        }
    }

    public boolean hasLobbySet() {
        return this.getArenaConfig().getString("lobby.world") != null;
    }

    public Location getLobbyLocation() {
        return new Location(
                Bukkit.getWorld(this.getArenaConfig().getString("lobby.world", "world")),
                this.getArenaConfig().getDouble("lobby.x"),
                this.getArenaConfig().getDouble("lobby.y"),
                this.getArenaConfig().getDouble("lobby.z"),
                (float) this.getArenaConfig().getInt("lobby.yaw"),
                (float) this.getArenaConfig().getInt("lobby.pitch")
        );
    }

    /**
     * Adds player to lobby: teleport, clear inv, give leave item, store previous state.
     * Caller must check arena state and lobby space before calling.
     */
    public void joinLobby(Player p) {
        if (this.lobbyPlayers.size() >= MAX_LOBBY_PLAYERS) {
            return;
        }
        ValueContainer valCont = new ValueContainer(p.getLocation(), p.getLevel(), p.getExp(),
                p.getInventory().getArmorContents(), p.getInventory().getContents(), p.getGameMode(),
                p.getFoodLevel(), p.getHealth(), p.getMaxHealth(), p.getActivePotionEffects());
        this.valueContMap.put(p.getName(), valCont);
        this.lobbyPlayers.add(p);
        this.setArenaStatus(arenaMode.LOBBY);

        p.closeInventory();
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        p.teleport(this.getLobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        p.getInventory().clear();
        p.getInventory().setItem(LobbyLeaveItem.getLeaveItemSlot(), LobbyLeaveItem.create(this.pl));
        p.updateInventory();
        p.setGameMode(GameMode.ADVENTURE);
        p.setFlying(false);
    }

    /**
     * Removes player from lobby and restores them to previous location/state.
     * Remaining players stay in lobby (e.g. when one leaves during countdown).
     */
    public void leaveLobby(Player p) {
        if (!this.lobbyPlayers.contains(p)) {
            return;
        }
        String leftName = p.getName();
        this.lobbyPlayers.remove(p);
        int countAfter = this.lobbyPlayers.size();
        if (p.isOnline()) {
            this.restorePlayer(p);
        } else {
            this.valueContMap.remove(p.getName());
        }
        if (this.lobbyPlayers.isEmpty()) {
            this.setArenaStatus(arenaMode.NORMAL);
        } else {
            this.setArenaStatus(arenaMode.LOBBY);
            this.broadcastPlayerLeftLobby(leftName, countAfter);
        }
    }

    /** Called when player quits while in lobby; does not restore (player offline). */
    public void removeFromLobbyOnQuit(Player p) {
        if (!this.lobbyPlayers.contains(p)) {
            return;
        }
        String leftName = p.getName();
        this.lobbyPlayers.remove(p);
        this.valueContMap.remove(p.getName());
        int countAfter = this.lobbyPlayers.size();
        if (this.lobbyPlayers.isEmpty()) {
            this.setArenaStatus(arenaMode.NORMAL);
        } else {
            this.setArenaStatus(arenaMode.LOBBY);
            this.broadcastPlayerLeftLobby(leftName, countAfter);
        }
    }

    private void broadcastPlayerLeftLobby(String playerName, int countAfter) {
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("{PLAYER}", playerName);
        replacements.put("{COUNT}", String.valueOf(countAfter));
        replacements.put("{MAX}", String.valueOf(MAX_LOBBY_PLAYERS));
        for (Player other : this.lobbyPlayers) {
            if (other != null && other.isOnline()) {
                this.pl.send1vs1Message("playerLeftLobby", other, replacements);
            }
        }
    }

    /** Sends "X has joined the game (n/2)" to all current lobby players (including the joiner). */
    public void broadcastPlayerJoinedLobby(Player joiner) {
        int count = this.lobbyPlayers.size();
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("{PLAYER}", joiner.getName());
        replacements.put("{COUNT}", String.valueOf(count));
        replacements.put("{MAX}", String.valueOf(MAX_LOBBY_PLAYERS));
        for (Player other : this.lobbyPlayers) {
            if (other != null && other.isOnline()) {
                this.pl.send1vs1Message("playerJoinedLobby", other, replacements);
            }
        }
    }

    public boolean isInLobby(Player p) {
        return this.lobbyPlayers.contains(p);
    }

    public List<Player> getLobbyPlayers() {
        return new ArrayList<>(this.lobbyPlayers);
    }

    public int getLobbySize() {
        return this.lobbyPlayers.size();
    }

    public void spawnFirework(Location loc) {
        Random r = new Random();
        Firework fw = (Firework)loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(FireworkEffect.builder().with(FireworkRandomizer.fireworkType(r.nextInt(5))).withColor(FireworkRandomizer.fireworkColor(r.nextInt(17))).withColor(FireworkRandomizer.fireworkColor(r.nextInt(17))).withColor(FireworkRandomizer.fireworkColor(r.nextInt(17))).flicker(r.nextBoolean()).trail(r.nextBoolean()).withFade(FireworkRandomizer.fireworkColor(r.nextInt(17))).build());
        fwm.setPower(1);
        fw.setFireworkMeta(fwm);
    }

    public boolean arenaPlayersContains(Player p) {
        for (Player pl : this.arenaPlayers) {
            if (pl != p) continue;
            return true;
        }
        return false;
    }

    public void reset() {
        this.arenaStatus = this.lobbyPlayers.isEmpty() ? arenaMode.NORMAL : arenaMode.LOBBY;
        this.timeOut.resetTimeOut();
        this.arenaPlayers = new Player[2];
        this.playerRoundWins = new int[2];
    }

    public boolean isGameOver(Player winner) {
        if (this.getPlayerRoundWins(winner) + this.getPlayerRoundWins(winner == this.arenaPlayers[0] ? this.arenaPlayers[1] : this.arenaPlayers[0]) == this.getTotalRounds() && this.getTotalRounds() != 1) {
            MetricsHandler.getInstance().increaseMaxRoundsGames();
        }
        return this.getPlayerRoundWins(winner) >= this.getTotalRounds() / 2 + 1 || this.totalRounds == 1 && this.getPlayerRoundWins(winner) == 1;
    }

    public void startRound(Player winner) {
        HashMap<String, String> replacements = new HashMap<String, String>();
        replacements.put("{WINNER}", winner.getName());
        replacements.put("{ROUND}", String.valueOf(this.getCurrentRound() - 1));
        replacements.put("{HEALTH}", String.valueOf(winner.getHealth() < 0.5 ? 1L : Math.round(winner.getHealth())));
        this.setArenaStatus(arenaMode.BETWEEN_ROUNDS);
        for (Player player : this.arenaPlayers) {
            this.pl.send1vs1Message("wonRound", player, replacements);
        }
        this.readyPlayers();
    }

    public void endGame(Player winner) {
        HashMap<String, String> replacements = new HashMap<String, String>();
        replacements.put("{WINNER}", winner.getName());
        replacements.put("{ROUND}", String.valueOf(this.getCurrentRound() - 1));
        replacements.put("{HEALTH}", String.valueOf(winner.getHealth() < 0.5 ? 1L : Math.round(winner.getHealth())));
        for (Player player : this.arenaPlayers) {
            this.pl.send1vs1Message("wonRound", player, replacements);
        }
        this.afterFight(winner);
    }

    public void setRoundWinner(Player winner) {
        this.increasePlayerRoundWins(winner);
        if (winner != null) {
            MetricsHandler.getInstance().addHealthLeftAfterRoundStat(winner.getHealth() / winner.getMaxHealth());
        }
        if (this.isGameOver(winner)) {
            this.endGame(winner);
        } else {
            this.startRound(winner);
        }
    }

    private void refreshPlayer(final Player p) {
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this.pl, new Runnable(){

            @Override
            public void run() {
                for (Player player : Utils.getOnlinePlayers()) {
                    if (p == null || !p.isOnline()) continue;
                    p.hidePlayer(player);
                }
                Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)GameManager.this.pl, new Runnable(){

                    @Override
                    public void run() {
                        for (Player player : Utils.getOnlinePlayers()) {
                            if (p == null || !p.isOnline()) continue;
                            p.showPlayer(player);
                        }
                    }
                }, 2L);
            }
        }, 15L);
    }

    public boolean valueContMapcontains(String name) {
        return this.valueContMap.containsKey(name);
    }

    public Player[] getArenaPlayers() {
        return this.arenaPlayers;
    }

    public void setArenaStatus(arenaMode mode) {
        this.arenaStatus = mode;
    }

    public arenaMode getArenaStatus() {
        return this.arenaStatus;
    }

    public String getArenaStatusInString() {
        switch (this.arenaStatus) {
            case LOBBY:
                return this.pl.getDataHandler().getMessagesConfig().getString("arenaModes.lobby")
                        .replace("{COUNT}", String.valueOf(this.lobbyPlayers.size()));
            case COUNTDOWN_LOBBY:
                return this.pl.getDataHandler().getMessagesConfig().getString("arenaModes.countdownLobby");
            case COUNTDOWN_BEFORE_FIGHT:
                return this.pl.getDataHandler().getMessagesConfig().getString("arenaModes.countdownBeforeFight");
            case FIGHT:
                return this.pl.getDataHandler().getMessagesConfig().getString("arenaModes.fight")
                        .replace("{ROUND}", String.valueOf(this.getCurrentRound()))
                        .replace("{MAX_ROUNDS}", String.valueOf(this.totalRounds))
                        .replace("{TIME_LEFT}", String.valueOf(this.timeOut.getTimeOut()));
            case NORMAL:
                return this.pl.getDataHandler().getMessagesConfig().getString("arenaModes.normal");
            default:
                return "";
        }
    }

    public String getArenaName() {
        return this.arenaName;
    }

    public FileConfiguration getArenaConfig() {
        return this.pl.getDataHandler().getArenaConfig(this.arenaName);
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public boolean getEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (enabled) {
            this.timeOut.startTimeOut();
            MetricsHandler.getInstance().updateRoundFormats();
        }
    }

    public int getTotalRounds() {
        return this.totalRounds;
    }

    public int getCurrentRound() {
        return this.playerRoundWins[0] + this.playerRoundWins[1] + 1;
    }

    public int getPlayerRoundWins(Player p) {
        int player = p == this.arenaPlayers[0] ? 0 : 1;
        return this.playerRoundWins[player];
    }

    public void increasePlayerRoundWins(Player p) {
        int winner = p == this.arenaPlayers[0] ? 0 : 1;
        this.playerRoundWins[winner] = this.playerRoundWins[winner] + 1;
    }

    public void cancelTimeOut() {
        this.timeOut.cancelTimeOut();
    }

    public static enum arenaMode {
        NORMAL,
        LOBBY,
        COUNTDOWN_LOBBY,
        PREPERATION_BEFORE_FIGHT,
        COUNTDOWN_BEFORE_FIGHT,
        FIGHT,
        BETWEEN_ROUNDS
    }
}

