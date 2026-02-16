package com.gmail.Orscrider.PvP1vs1.deathmatch;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.util.LobbyLeaveItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * Listeners for deathmatch only. Does not touch 1v1 arenas.
 */
public class DmListeners implements Listener {

    private final PvP1vs1 pl;
    private final DmArenaManager dmArenaManager;

    public DmListeners(PvP1vs1 plugin, DmArenaManager dmArenaManager) {
        this.pl = plugin;
        this.dmArenaManager = dmArenaManager;
        pl.getServer().getPluginManager().registerEvents(this, (Plugin) pl);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent ev) {
        if (ev.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) return;
        Player p = ev.getPlayer();
        for (Map.Entry<String, DmGameManager> e : dmArenaManager.getArenas().entrySet()) {
            DmGameManager arena = e.getValue();
            if (arena.isInLobby(p)) {
                if (p.hasPermission("dm.teleport")) return;
                int maxDist = arena.getArenaConfig().getInt("maximumTeleportDistance", 50);
                if (ev.getFrom().getWorld() != ev.getTo().getWorld() || ev.getFrom().distance(ev.getTo()) > maxDist) {
                    p.teleport(ev.getFrom());
                    pl.messageParserDm("teleportOutOfLobby", p);
                    ev.setCancelled(true);
                }
                return;
            }
            if ((arena.getArenaStatus() == DmGameManager.DmArenaMode.COUNTDOWN_BEFORE_FIGHT || arena.getArenaStatus() == DmGameManager.DmArenaMode.FIGHT)
                    && arena.arenaPlayersContains(p)) {
                if (p.hasPermission("dm.teleport")) return;
                int maxDist = arena.getArenaConfig().getInt("maximumTeleportDistance", 50);
                if (ev.getFrom().getWorld() != ev.getTo().getWorld() || ev.getFrom().distance(ev.getTo()) > maxDist) {
                    p.teleport(ev.getFrom());
                    pl.messageParserDm("teleportOutOfTheArena", p);
                    ev.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent ev) {
        Player p = ev.getPlayer();
        for (DmGameManager arena : dmArenaManager.getArenas().values()) {
            if (arena.isInLobby(p)) {
                pl.messageParserDm("blockPlaceInLobby", p);
                ev.setCancelled(true);
                return;
            }
            if (arena.arenaPlayersContains(p) && !p.hasPermission("dm.blockPlace")) {
                pl.messageParserDm("blockPlaceInArena", p);
                ev.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent ev) {
        Player p = ev.getPlayer();
        for (DmGameManager arena : dmArenaManager.getArenas().values()) {
            if (arena.isInLobby(p)) {
                ev.setCancelled(true);
                return;
            }
            if (arena.arenaPlayersContains(p) && !p.hasPermission("dm.blockBreak")) {
                ev.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent ev) {
        Player p = ev.getPlayer();
        for (DmGameManager arena : dmArenaManager.getArenas().values()) {
            if (arena.isInLobby(p)) {
                ev.setCancelled(true);
                return;
            }
            if (arena.arenaPlayersContains(p) && !p.hasPermission("dm.itemDrop")) {
                pl.messageParserDm("itemDropInArena", p);
                ev.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPickUp(PlayerPickupItemEvent ev) {
        Player p = ev.getPlayer();
        for (DmGameManager arena : dmArenaManager.getArenas().values()) {
            if (arena.isInLobby(p)) {
                ev.setCancelled(true);
                return;
            }
            if (arena.arenaPlayersContains(p) && !p.hasPermission("dm.itemPickUp")) {
                ev.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent ev) {
        Player p = ev.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!LobbyLeaveItem.isLeaveItem(hand, pl)) return;
        for (Map.Entry<String, DmGameManager> e : dmArenaManager.getArenas().entrySet()) {
            if (e.getValue().isInLobby(p)) {
                ev.setCancelled(true);
                e.getValue().leaveLobby(p);
                java.util.HashMap<String, String> replacements = new java.util.HashMap<>();
                replacements.put("{ARENA}", e.getKey());
                pl.sendDmMessage("leaveLobby", p, replacements);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent ev) {
        if (!(ev.getWhoClicked() instanceof Player)) return;
        Player p = (Player) ev.getWhoClicked();
        ItemStack clicked = ev.getCurrentItem();
        if (clicked != null && LobbyLeaveItem.isLeaveItem(clicked, pl)) {
            for (DmGameManager arena : dmArenaManager.getArenas().values()) {
                if (arena.isInLobby(p)) {
                    ev.setCancelled(true);
                    return;
                }
            }
        }
        ItemStack cursor = ev.getCursor();
        if (cursor != null && cursor.getType() != org.bukkit.Material.AIR && LobbyLeaveItem.isLeaveItem(cursor, pl)) {
            for (DmGameManager arena : dmArenaManager.getArenas().values()) {
                if (arena.isInLobby(p)) {
                    ev.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent ev) {
        if (!(ev.getWhoClicked() instanceof Player)) return;
        Player p = (Player) ev.getWhoClicked();
        for (DmGameManager arena : dmArenaManager.getArenas().values()) {
            if (!arena.isInLobby(p)) continue;
            if (ev.getOldCursor() != null && LobbyLeaveItem.isLeaveItem(ev.getOldCursor(), pl)) {
                ev.setCancelled(true);
                return;
            }
            int leaveSlot = 36 + LobbyLeaveItem.getLeaveItemSlot();
            for (int slot : ev.getRawSlots()) {
                if (slot == leaveSlot) {
                    ev.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent ev) {
        Player p = ev.getPlayer();
        for (DmGameManager arena : dmArenaManager.getArenas().values()) {
            if (!arena.arenaPlayersContains(p)) continue;
            if (arena.getArenaStatus() != DmGameManager.DmArenaMode.COUNTDOWN_BEFORE_FIGHT) continue;
            if (!arena.getArenaConfig().getBoolean("countdown.beforeFight.disableMoving", true)) continue;
            if (ev.getFrom().getBlockX() == ev.getTo().getBlockX() && ev.getFrom().getBlockY() == ev.getTo().getBlockY() && ev.getFrom().getBlockZ() == ev.getTo().getBlockZ())
                continue;
            p.teleport(ev.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogout(PlayerQuitEvent ev) {
        Player p = ev.getPlayer();
        for (DmGameManager arena : dmArenaManager.getArenas().values()) {
            if (arena.isInLobby(p)) {
                arena.removeFromLobbyOnQuit(p);
                return;
            }
            if (arena.arenaPlayersContains(p)) {
                arena.onPlayerDeath(p);
                return;
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent ev) {
        if (!(ev.getEntity() instanceof Player)) return;
        Player p = (Player) ev.getEntity();
        for (DmGameManager arena : dmArenaManager.getArenas().values()) {
            if (arena.isInLobby(p)) {
                ev.setCancelled(true);
                return;
            }
            if (arena.getArenaStatus() == DmGameManager.DmArenaMode.COUNTDOWN_BEFORE_FIGHT && arena.arenaPlayersContains(p)
                    && arena.getArenaConfig().getBoolean("countdown.beforeFight.invincibility", true)) {
                ev.setCancelled(true);
                return;
            }
            if (!arena.arenaPlayersContains(p)) continue;
            if (ev.getFinalDamage() >= p.getHealth()) {
                ev.setCancelled(true);
                arena.onPlayerDeath(p);
            }
            return;
        }
    }
}
