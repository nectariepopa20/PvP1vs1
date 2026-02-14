/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Server
 *  org.bukkit.entity.Player
 */
package com.gmail.Orscrider.PvP1vs1.util;

import com.gmail.Orscrider.PvP1vs1.util.LogHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;

public class Utils {
    public static Collection<? extends Player> getOnlinePlayers() {
        try {
            Method onlinePlayerMethod = Server.class.getMethod("getOnlinePlayers", new Class[0]);
            if (onlinePlayerMethod.getReturnType().equals(Collection.class)) {
                return (Collection)onlinePlayerMethod.invoke((Object)Bukkit.getServer(), new Object[0]);
            }
            return Arrays.asList((Player[])onlinePlayerMethod.invoke((Object)Bukkit.getServer(), new Object[0]));
        }
        catch (Exception ex) {
            LogHandler.severe("No valid getOnlinePlayers() method found!");
            return new HashSet();
        }
    }
}

