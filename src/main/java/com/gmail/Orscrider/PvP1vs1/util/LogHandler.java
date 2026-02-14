/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package com.gmail.Orscrider.PvP1vs1.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

public class LogHandler {
    private static Logger logger;

    public static void init(String name) {
        logger = Logger.getLogger(name);
    }

    public static void init(Plugin plugin) {
        logger = plugin.getLogger();
    }

    public static void fine(String message) {
        logger.fine(message);
    }

    public static void finer(String message) {
        logger.finer(message);
    }

    public static void finest(String message) {
        logger.finest(message);
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void warning(String message) {
        logger.warning(message);
    }

    public static void severe(String message) {
        logger.severe(message);
    }

    public static void warning(String message, Exception err) {
        if (err == null) {
            LogHandler.warning(message);
        } else {
            logger.log(Level.WARNING, message, err);
        }
    }

    public static void severe(String message, Exception err) {
        if (err == null) {
            LogHandler.severe(message);
        } else {
            logger.log(Level.SEVERE, message, err);
        }
    }
}

