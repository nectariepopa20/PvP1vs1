/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 */
package com.gmail.Orscrider.PvP1vs1.persistence;

import com.gmail.Orscrider.PvP1vs1.persistence.Arena;
import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionController;
import com.gmail.Orscrider.PvP1vs1.persistence.Player;
import com.gmail.Orscrider.PvP1vs1.persistence.PlayerArena;
import com.gmail.Orscrider.PvP1vs1.util.LogHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;

public class DBMigrationHandler {
    private static void migrateOldDBTable(DBConnectionController dbController) {
        LogHandler.info("Start migrating old database table...");
        LogHandler.info("Gettting old table data...");
        List<LinkedHashMap<String, String>> oldTableDBData = dbController.getOldTableDBData();
        ArrayList<PlayerArena> playerArenaList = new ArrayList<PlayerArena>();
        Player player = null;
        Arena arena = null;
        for (Map<String, String> map : oldTableDBData) {
            player = null;
            for (String key : map.keySet()) {
                arena = null;
                int score = 0;
                if (key.equals("Player")) {
                    String playerName = (String)map.get(key);
                    UUID playerId = Bukkit.getOfflinePlayer((String)playerName).getUniqueId();
                    player = new Player(playerId.toString());
                } else if (key.startsWith("ARENA_")) {
                    score = Integer.valueOf((String)map.get(key));
                    arena = new Arena(null, key.substring(6));
                }
                if (player == null || arena == null) continue;
                playerArenaList.add(new PlayerArena(player, arena, score));
            }
        }
        LogHandler.info("Saving migrated data...");
        dbController.saveMigrateOldTableData(playerArenaList);
        LogHandler.info("Migrating of old database table finished");
    }

    public static void migrateDB(DBConnectionController dbController) {
        if (dbController.oldSQLiteDBExists()) {
            DBMigrationHandler.migrateOldDBTable(dbController);
        }
    }
}

