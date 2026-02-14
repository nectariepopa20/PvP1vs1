/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerPickupItemEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.plugin.Plugin
 */
package com.gmail.Orscrider.PvP1vs1.arena;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.arena.ArenaManager;
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import com.gmail.Orscrider.PvP1vs1.metrics.MetricsHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

public class Listeners
implements Listener {
    private PvP1vs1 pl;
    private ArenaManager arenaManager;

    public Listeners(PvP1vs1 plugin, ArenaManager manager) {
        this.pl = plugin;
        this.arenaManager = manager;
        this.pl.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this.pl);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent ev) {
        Player p = ev.getPlayer();
        for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
            if (arena.getValue().getArenaStatus() != GameManager.arenaMode.COUNTDOWN_BEFORE_FIGHT && arena.getValue().getArenaStatus() != GameManager.arenaMode.FIGHT || !arena.getValue().arenaPlayersContains(p) || p.hasPermission("1vs1.teleport") || ev.getFrom().getWorld() == ev.getTo().getWorld() && !(ev.getFrom().distance(ev.getTo()) > (double)arena.getValue().getArenaConfig().getInt("maximumTeleportDistance"))) continue;
            p.teleport(ev.getFrom());
            this.pl.messageParser("teleportOutOfTheArena", p);
            ev.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent ev) {
        Player p = ev.getPlayer();
        for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
            if (!arena.getValue().arenaPlayersContains(p) || p.hasPermission("1vs1.blockPlace")) continue;
            this.pl.messageParser("blockPlaceInArena", p);
            ev.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent ev) {
        Player p = ev.getPlayer();
        for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
            if (!arena.getValue().arenaPlayersContains(p) || p.hasPermission("1vs1.blockBreak")) continue;
            ev.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent ev) {
        for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
            if (!arena.getValue().arenaPlayersContains(ev.getPlayer()) || arena.getValue().getArenaConfig().getBoolean("playerItemDrop") || ev.getPlayer().hasPermission("1vs1.itemDrop")) continue;
            this.pl.messageParser("itemDropInArena", ev.getPlayer());
            ev.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickUp(PlayerPickupItemEvent ev) {
        for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
            Player p = ev.getPlayer();
            if (!arena.getValue().arenaPlayersContains(p) || arena.getValue().getArenaConfig().getBoolean("playerItemPickUp") || p.hasPermission("1vs1.itemPickUp")) continue;
            ev.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent ev) {
        for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
            Player p = ev.getPlayer();
            if (!arena.getValue().arenaPlayersContains(p) || arena.getValue().getArenaStatus() != GameManager.arenaMode.COUNTDOWN_BEFORE_FIGHT || !arena.getValue().getArenaConfig().getBoolean("countdown.beforeFight.disableMoving") || ev.getFrom().getX() == ev.getTo().getX() && ev.getFrom().getY() == ev.getTo().getY() && ev.getFrom().getZ() == ev.getTo().getZ()) continue;
            p.teleport(ev.getFrom());
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent ev) {
        Player p = ev.getPlayer();
        for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
            if (arena.getValue().arenaPlayersContains(p) && (arena.getValue().getArenaStatus() == GameManager.arenaMode.FIGHT || arena.getValue().getArenaStatus() == GameManager.arenaMode.COUNTDOWN_BEFORE_FIGHT) && !p.hasPermission("1vs1.commandUse")) {
                this.pl.messageParser("commandUseInArena", p);
                ev.setCancelled(true);
            }
            if (!arena.getValue().getQueue().contains(p) || p.hasPermission("1vs1.commandBlacklist.bypass")) continue;
            List<String> commandBlacklist = this.pl.getConfig().getStringList("queue.commandBlacklist");
            block1: for (String bannedCmd : commandBlacklist) {
                for (String alias : this.getAliases(bannedCmd.split(" ")[0])) {
                    if (!ev.getMessage().toLowerCase().startsWith("/" + alias.toLowerCase() + bannedCmd.substring(bannedCmd.split(" ")[0].length()).toLowerCase())) continue;
                    this.pl.messageParser("commandBlacklist", p);
                    ev.setCancelled(true);
                    continue block1;
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onLogout(PlayerQuitEvent ev) {
        Player p = ev.getPlayer();
        for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
            if (arena.getValue().getArenaStatus() == GameManager.arenaMode.COUNTDOWN_BEFORE_TELEPORT && arena.getValue().getQueue().nextArenaPlayersContainsPlayer(p)) {
                return;
            }
            if (arena.getValue().arenaPlayersContains(p)) {
                arena.getValue().afterFight(p == arena.getValue().getArenaPlayers()[0] ? arena.getValue().getArenaPlayers()[1] : arena.getValue().getArenaPlayers()[0]);
                MetricsHandler.getInstance().increaseLoggedOutGames();
            }
            if (!arena.getValue().getQueue().contains(p)) continue;
            arena.getValue().getQueue().removePlayer(p);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent ev) {
        if (ev.getEntity() instanceof Player) {
            Player p = (Player)ev.getEntity();
            for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
                if (arena.getValue().getArenaConfig().getBoolean("countdown.beforeFight.invincibility") && arena.getValue().arenaPlayersContains(p) && arena.getValue().getArenaStatus() == GameManager.arenaMode.COUNTDOWN_BEFORE_FIGHT) {
                    ev.setCancelled(true);
                }
                if (!arena.getValue().arenaPlayersContains(p) || !(ev.getFinalDamage() >= p.getHealth())) continue;
                ev.setCancelled(true);
                Player winner = p == arena.getValue().getArenaPlayers()[0] ? arena.getValue().getArenaPlayers()[1] : arena.getValue().getArenaPlayers()[0];
                arena.getValue().setRoundWinner(winner);
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onDamageByEntity(EntityDamageByEntityEvent ev) {
        if (!ev.isCancelled() && ev.getEntity() instanceof Player && ev.getDamager() instanceof Player) {
            Player damaged = (Player)ev.getEntity();
            Player damager = (Player)ev.getDamager();
            for (Map.Entry<String, GameManager> arena : this.arenaManager.getArenas().entrySet()) {
                if (arena.getValue().getArenaStatus() != GameManager.arenaMode.COUNTDOWN_BEFORE_TELEPORT && arena.getValue().getQueue().contains(damaged) && this.pl.getConfig().getBoolean("queue.kick.wasHitByPlayer")) {
                    arena.getValue().getQueue().removePlayer(damaged);
                    this.pl.messageParser("wasHitByPlayer", damaged);
                    return;
                }
                if (arena.getValue().getArenaStatus() == GameManager.arenaMode.COUNTDOWN_BEFORE_TELEPORT || !arena.getValue().getQueue().contains(damager) || !this.pl.getConfig().getBoolean("queue.kick.hitPlayer")) continue;
                arena.getValue().getQueue().removePlayer(damager);
                this.pl.messageParser("hitPlayer", damager);
                return;
            }
        }
    }

    private List<String> getAliases(String cmd) {
        ArrayList<String> aliases = new ArrayList<String>();
        aliases.add(cmd);
        if (Bukkit.getPluginCommand((String)cmd) != null && Bukkit.getPluginCommand((String)cmd).getAliases() != null) {
            for (String alias : Bukkit.getPluginCommand((String)cmd).getAliases()) {
                aliases.add(alias);
            }
        }
        return aliases;
    }
}

