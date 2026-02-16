/*
 * Decompiled with CFR 0.152.
 */
package com.gmail.Orscrider.PvP1vs1.persistence;

import java.sql.SQLException;
import java.util.LinkedHashMap;

public interface DBConnectionInterface {
    public void connect();

    public boolean isClosed() throws SQLException;

    public void disconnect();

    public boolean dbTablesExists() throws SQLException;

    public void createTables() throws SQLException;

    public void removeTable(String var1) throws SQLException;

    public void addTableConstraints() throws SQLException;

    public boolean addArena(String var1) throws SQLException;

    public boolean removeArena(String var1) throws SQLException;

    public boolean arenaExists(String var1) throws SQLException;

    public void savePlayerScore(String var1, String var2, int var3) throws SQLException;

    public int getScoreOfPlayer(String var1) throws SQLException;

    public int getScoreOfPlayerAndArena(String var1, String var2) throws SQLException;

    public boolean addPlayerWin(String var1) throws SQLException;

    public boolean addPlayerLoss(String var1) throws SQLException;

    public int getPlayerWins(String var1) throws SQLException;

    public int getPlayerLosses(String var1) throws SQLException;

    public void ensureDmStatsColumns() throws SQLException;

    public boolean addPlayerDmWin(String var1) throws SQLException;

    public boolean addPlayerDmKill(String var1) throws SQLException;

    public boolean addPlayerDmDeath(String var1) throws SQLException;

    public int getPlayerDmWins(String var1) throws SQLException;

    public int getPlayerDmKills(String var1) throws SQLException;

    public int getPlayerDmDeaths(String var1) throws SQLException;

    public LinkedHashMap<String, Integer> getPlayerTopTenList() throws SQLException;

    public LinkedHashMap<String, Integer> getPlayerTopTenListForArena(String var1) throws SQLException;
}

