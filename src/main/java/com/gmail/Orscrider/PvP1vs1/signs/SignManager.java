/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Sign
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.block.SignChangeEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.plugin.Plugin
 */
package com.gmail.Orscrider.PvP1vs1.signs;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

public class SignManager
implements Listener {
    PvP1vs1 pl;
    List<Location> signs = new CopyOnWriteArrayList<Location>();

    public SignManager(PvP1vs1 pl) {
        this.pl = pl;
        this.scanLoadedWorlds();
        this.startUpdateTask();
        pl.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)pl);
    }

    public void scanLoadedWorlds() {
        for (World w : Bukkit.getWorlds()) {
            for (Chunk ch : w.getLoadedChunks()) {
                for (BlockState tileEntity : ch.getTileEntities()) {
                    Sign s;
                    if (!(tileEntity instanceof Sign) || !this.isArenaSign(s = (Sign)tileEntity)) continue;
                    this.signs.add(s.getLocation());
                }
            }
        }
    }

    public void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer((Plugin)this.pl, new Runnable(){

            @Override
            public void run() {
                for (Location loc : SignManager.this.signs) {
                    if (loc.getBlock().getState() instanceof Sign) {
                        Sign s = (Sign)loc.getBlock().getState();
                        if (!SignManager.this.isArenaSign(s) || !SignManager.this.isValidArena(ChatColor.stripColor((String)s.getLine(1)))) continue;
                        String arena = ChatColor.stripColor((String)s.getLine(1));
                        s.setLine(2, ChatColor.translateAlternateColorCodes((char)'&', (String)SignManager.this.pl.getArenaManager().getArena(arena).getArenaStatusInString()));
                        s.setLine(3, SignManager.this.pl.getArenaManager().getArena(arena).getQueue().size() + " " + SignManager.this.pl.getDataHandler().getMessagesConfig().getString("sign.queued"));
                        s.update();
                        continue;
                    }
                    SignManager.this.signs.remove(loc);
                }
            }
        }, 100L, this.pl.getConfig().getLong("arenaSignUpdater.delay"));
    }

    @EventHandler
    public void onSignChange(SignChangeEvent ev) {
        if (ev.getLine(1).equalsIgnoreCase("[1vs1]") && ev.getPlayer().hasPermission("1vs1.sign.create")) {
            ev.setLine(0, "\u00a72     \u27b7\u27b7\u27b7\u27b7\u27b7\u27b7\u27b7\u27b7");
            ev.setLine(1, "\u00a74[\u00a76\u00a7l1vs1\u00a74]");
            ev.setLine(2, "\u00a73" + ev.getLine(2));
            ev.setLine(3, "\u00a73" + ev.getLine(3));
        }
        if (ev.getLine(0).equalsIgnoreCase("[1v1Arena]") && ev.getPlayer().hasPermission("1vs1.arenaSign.create")) {
            ev.setLine(0, "[\u00a761v1Arena\u00a70]");
            ev.setLine(1, "\u00a74" + ev.getLine(1));
            this.addSign(ev.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent ev) {
        if (ev.getAction() == Action.RIGHT_CLICK_BLOCK && ev.getClickedBlock().getState() instanceof Sign) {
            Sign sign = (Sign)ev.getClickedBlock().getState();
            if (sign.getLine(1).equals("\u00a74[\u00a76\u00a7l1vs1\u00a74]")) {
                ev.getPlayer().chat("/1vs1 " + ChatColor.stripColor((String)(sign.getLine(2) + ChatColor.stripColor((String)sign.getLine(3)))));
            }
            if (this.isArenaSign(sign)) {
                ev.getPlayer().chat("/1vs1 join " + ChatColor.stripColor((String)sign.getLine(1)));
            }
        }
    }

    public boolean isArenaSign(Sign s) {
        return s.getLine(0).equalsIgnoreCase("[\u00a761v1Arena\u00a70]");
    }

    private boolean isValidArena(String arena) {
        return this.pl.getArenaManager().getArenas().containsKey(arena);
    }

    public void addSign(Location loc) {
        this.signs.add(loc);
    }
}

