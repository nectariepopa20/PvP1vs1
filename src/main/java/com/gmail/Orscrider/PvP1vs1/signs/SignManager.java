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
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import com.gmail.Orscrider.PvP1vs1.deathmatch.DmGameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SignManager implements Listener {
    PvP1vs1 pl;
    List<Location> signs = new CopyOnWriteArrayList<>();
    List<QueueSignData> queueSigns = new CopyOnWriteArrayList<>();

    public static class QueueSignData {
        final String world;
        final int x, y, z;
        final String queueName;
        final String gamemode;

        QueueSignData(String world, int x, int y, int z, String queueName, String gamemode) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.queueName = queueName;
            this.gamemode = gamemode;
        }

        boolean matches(Location loc) {
            return loc.getWorld() != null && loc.getWorld().getName().equals(world)
                    && loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z;
        }
    }

    public SignManager(PvP1vs1 pl) {
        this.pl = pl;
        this.scanLoadedWorlds();
        this.loadQueueSigns();
        this.startUpdateTask();
        pl.getServer().getPluginManager().registerEvents((Listener) this, (Plugin) pl);
    }

    @SuppressWarnings("unchecked")
    private void loadQueueSigns() {
        queueSigns.clear();
        FileConfiguration cfg = pl.getDataHandler().getQueueSignsConfig();
        List<?> list = cfg.getList("signs");
        if (list == null) return;
        for (Object o : list) {
            if (o instanceof java.util.Map) {
                java.util.Map<String, Object> m = (java.util.Map<String, Object>) o;
                String world = (String) m.get("world");
                Object xx = m.get("x"), yy = m.get("y"), zz = m.get("z");
                String qn = (String) m.get("queueName");
                String gm = (String) m.get("gamemode");
                if (world != null && qn != null && gm != null && xx != null && yy != null && zz != null) {
                    queueSigns.add(new QueueSignData(world, ((Number) xx).intValue(), ((Number) yy).intValue(), ((Number) zz).intValue(), qn, gm));
                }
            }
        }
    }

    private void saveQueueSignsToConfig() {
        FileConfiguration cfg = pl.getDataHandler().getQueueSignsConfig();
        List<java.util.Map<String, Object>> mapList = new ArrayList<>();
        for (QueueSignData d : queueSigns) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("world", d.world);
            m.put("x", d.x);
            m.put("y", d.y);
            m.put("z", d.z);
            m.put("queueName", d.queueName);
            m.put("gamemode", d.gamemode);
            mapList.add(m);
        }
        cfg.set("signs", mapList);
        pl.getDataHandler().saveQueueSignsConfig();
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
        Bukkit.getScheduler().runTaskTimer((Plugin) this.pl, new Runnable() {
            @Override
            public void run() {
                for (Location loc : SignManager.this.signs) {
                    if (loc.getBlock().getState() instanceof Sign) {
                        Sign s = (Sign) loc.getBlock().getState();
                        if (!SignManager.this.isArenaSign(s) || !SignManager.this.isValidArena(ChatColor.stripColor(s.getLine(1)))) continue;
                        String arena = ChatColor.stripColor(s.getLine(1));
                        s.setLine(2, ChatColor.translateAlternateColorCodes('&', SignManager.this.pl.getArenaManager().getArena(arena).getArenaStatusInString()));
                        s.setLine(3, SignManager.this.pl.getArenaManager().getArena(arena).getLobbySize() + " " + SignManager.this.pl.getDataHandler().getMessagesConfig().getString("sign.inLobby"));
                        s.update();
                        continue;
                    }
                    SignManager.this.signs.remove(loc);
                }
                for (QueueSignData qd : queueSigns) {
                    World w = Bukkit.getWorld(qd.world);
                    if (w == null) continue;
                    Location loc = new Location(w, qd.x, qd.y, qd.z);
                    if (!(loc.getBlock().getState() instanceof Sign)) continue;
                    Sign s = (Sign) loc.getBlock().getState();
                    List<String> names = getPlayersInQueue(qd.gamemode, qd.queueName);
                    String line2 = names.isEmpty() ? ChatColor.translateAlternateColorCodes('&', "&0") : ChatColor.translateAlternateColorCodes('&', "&0" + String.join(", ", names));
                    if (line2.length() > 15) line2 = line2.substring(0, 15);
                    s.setLine(2, line2);
                    s.update();
                }
            }
        }, 100L, this.pl.getConfig().getLong("arenaSignUpdater.delay"));
    }

    private List<String> getPlayersInQueue(String gamemode, String queueName) {
        List<String> names = new ArrayList<>();
        List<String> arenas = pl.getDataHandler().getQueueArenas(gamemode, queueName);
        if (gamemode.equals("1v1")) {
            for (String an : arenas) {
                if (!pl.getArenaManager().arenaExists(an)) continue;
                GameManager gm = pl.getArenaManager().getArena(an);
                for (Player p : gm.getLobbyPlayers()) names.add(p.getName());
                Player[] ap = gm.getArenaPlayers();
                if (ap != null) for (Player p : ap) if (p != null) names.add(p.getName());
            }
        } else {
            for (String an : arenas) {
                if (!pl.getDmArenaManager().arenaExists(an)) continue;
                DmGameManager gm = pl.getDmArenaManager().getArena(an);
                for (Player p : gm.getLobbyPlayers()) names.add(p.getName());
                for (Player p : gm.getArenaPlayers()) if (p != null) names.add(p.getName());
            }
        }
        return names;
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
        if (ev.getLine(0).trim().equalsIgnoreCase("[join]")) {
            String line3 = ev.getLine(3).trim().toLowerCase();
            String queueName = ev.getLine(1).trim();
            if (line3.equals("1vs1") && ev.getPlayer().hasPermission("1vs1.queueSign.create") && pl.getDataHandler().queueExists("1v1", queueName)) {
                ev.setLine(0, ChatColor.translateAlternateColorCodes('&', "&a[JOIN]"));
                ev.setLine(1, ChatColor.translateAlternateColorCodes('&', "&l" + queueName));
                ev.setLine(2, ChatColor.translateAlternateColorCodes('&', "&0"));
                ev.setLine(3, ChatColor.translateAlternateColorCodes('&', "&a1vs1"));
                Location loc = ev.getBlock().getLocation();
                QueueSignData data = new QueueSignData(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), queueName, "1v1");
                queueSigns.add(data);
                saveQueueSignsToConfig();
            } else if (line3.equals("deathmatch") && ev.getPlayer().hasPermission("dm.queueSign.create") && pl.getDataHandler().queueExists("dm", queueName)) {
                ev.setLine(0, ChatColor.translateAlternateColorCodes('&', "&a[JOIN]"));
                ev.setLine(1, ChatColor.translateAlternateColorCodes('&', "&l" + queueName));
                ev.setLine(2, ChatColor.translateAlternateColorCodes('&', "&0"));
                ev.setLine(3, ChatColor.translateAlternateColorCodes('&', "&cDeathmatch"));
                Location loc = ev.getBlock().getLocation();
                QueueSignData data = new QueueSignData(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), queueName, "dm");
                queueSigns.add(data);
                saveQueueSignsToConfig();
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent ev) {
        if (ev.getAction() != Action.RIGHT_CLICK_BLOCK || ev.getClickedBlock() == null || !(ev.getClickedBlock().getState() instanceof Sign)) return;
        Sign sign = (Sign) ev.getClickedBlock().getState();
        Location loc = ev.getClickedBlock().getLocation();
        QueueSignData qd = null;
        for (QueueSignData d : queueSigns) {
            if (d.matches(loc)) { qd = d; break; }
        }
        if (qd != null) {
            ev.setCancelled(true);
            if (qd.gamemode.equals("1v1")) ev.getPlayer().chat("/1v1 queue join " + qd.queueName);
            else ev.getPlayer().chat("/dm queue join " + qd.queueName);
            return;
        }
        boolean is1vs1CommandSign = sign.getLine(1).equals("\u00a74[\u00a76\u00a7l1vs1\u00a74]");
        boolean isArenaSign = this.isArenaSign(sign);
        if (is1vs1CommandSign || isArenaSign) {
            ev.setCancelled(true);
            if (is1vs1CommandSign) {
                ev.getPlayer().chat("/1vs1 " + ChatColor.stripColor(sign.getLine(2) + ChatColor.stripColor(sign.getLine(3))));
            }
            if (isArenaSign) {
                ev.getPlayer().chat("/1vs1 join " + ChatColor.stripColor(sign.getLine(1)));
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

