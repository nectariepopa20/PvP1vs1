/*
 * Decompiled with CFR 0.152.
 */
package com.gmail.Orscrider.PvP1vs1.persistence;

import com.gmail.Orscrider.PvP1vs1.DataHandler;
import com.gmail.Orscrider.PvP1vs1.persistence.Arena;
import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionInterface;
import com.gmail.Orscrider.PvP1vs1.persistence.MysqlDBConnection;
import com.gmail.Orscrider.PvP1vs1.persistence.Player;
import com.gmail.Orscrider.PvP1vs1.persistence.PlayerArena;
import com.gmail.Orscrider.PvP1vs1.persistence.SQLiteDBConnection;
import com.gmail.Orscrider.PvP1vs1.util.LogHandler;
import java.io.File;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

public class DBConnectionController {
    private DBConnectionInterface dbCon;
    private static DBConnectionController dbController;

    private DBConnectionController() {
        DataHandler dataHandler = DataHandler.getInstance();
        String dbType = dataHandler.getDatabaseType();
        if (dbType.equals("sqlite")) {
            this.dbCon = new SQLiteDBConnection(dataHandler.getDataFolder().getAbsolutePath() + "/stats.db");
        } else if (dbType.equals("mysql")) {
            this.dbCon = new MysqlDBConnection(dataHandler.getDatabaseHost(), dataHandler.getDatabasePort(), dataHandler.getDatabaseUser(), dataHandler.getDatabasePassword(), dataHandler.getDatabaseName());
        } else {
            LogHandler.severe("Database '" + dbType + "' is not supported!");
        }
    }

    public static DBConnectionController getInstance() {
        if (dbController == null) {
            dbController = new DBConnectionController();
        }
        return dbController;
    }

    public void connect() {
        LogHandler.info("Connecting to database");
        try {
            if (this.dbCon.isClosed()) {
                this.dbCon.connect();
            }
        }
        catch (SQLException e) {
            LogHandler.severe("Error on establishing database connection", e);
        }
        try {
            if (!this.dbCon.dbTablesExists()) {
                this.dbCon.createTables();
            }
        }
        catch (SQLException e) {
            LogHandler.severe("Error on checking/creating missing database tables", e);
        }
    }

    public void disconnect() {
        LogHandler.info("Disconnecting from database");
        try {
            if (!this.dbCon.isClosed()) {
                this.dbCon.disconnect();
            }
        }
        catch (SQLException e) {
            LogHandler.severe("Error on disconnecting from database", e);
        }
    }

    public List<LinkedHashMap<String, String>> getOldTableDBData() {
        DataHandler dataHandler = DataHandler.getInstance();
        SQLiteDBConnection sqliteDBCon = new SQLiteDBConnection(dataHandler.getDataFolder().getAbsolutePath() + "/stats.db");
        sqliteDBCon.connect();
        List<LinkedHashMap<String, String>> oldTableData = null;
        try {
            oldTableData = sqliteDBCon.getOldTableData();
        }
        catch (SQLException e) {
            LogHandler.severe("Error on getting old table data", e);
        }
        try {
            sqliteDBCon.removeTable("Points");
        }
        catch (SQLException e) {
            LogHandler.severe("Error on removing old database table", e);
        }
        sqliteDBCon.disconnect();
        return oldTableData;
    }

    public void saveMigrateOldTableData(List<PlayerArena> playerArenaList) {
        Player player = null;
        Arena arena = null;
        this.connect();
        for (PlayerArena playerArena : playerArenaList) {
            if (playerArena != null) {
                player = playerArena.getPlayer();
                arena = playerArena.getArena();
                if (arena != null) {
                    try {
                        if (!this.dbCon.arenaExists(arena.getName())) {
                            this.dbCon.addArena(arena.getName());
                        }
                    }
                    catch (SQLException e) {
                        LogHandler.severe("Error on checking if arena exists", e);
                    }
                }
            }
            if (player == null || arena == null) continue;
            try {
                this.dbCon.savePlayerScore(player.getPlayerid(), arena.getName(), playerArena.getScore());
            }
            catch (SQLException e) {
                LogHandler.severe("Error on saving player score", e);
            }
        }
    }

    public void addTableConstraints() {
        try {
            this.dbCon.addTableConstraints();
        }
        catch (SQLException e) {
            LogHandler.severe("Error on adding table constraints", e);
        }
    }

    public boolean oldSQLiteDBExists() {
        DataHandler dataHandler = DataHandler.getInstance();
        boolean oldDBExists = false;
        File sqliteDB = new File(dataHandler.getDataFolder().getAbsolutePath() + "/stats.db");
        if (sqliteDB.exists()) {
            SQLiteDBConnection sqliteDBCon = new SQLiteDBConnection(dataHandler.getDataFolder().getAbsolutePath() + "/stats.db");
            sqliteDBCon.connect();
            String dbVersion = DBConnectionController.checkForDatabaseVersion(sqliteDBCon);
            if ("1.0".equals(dbVersion)) {
                oldDBExists = true;
            }
            sqliteDBCon.disconnect();
        }
        return oldDBExists;
    }

    private static String checkForDatabaseVersion(DBConnectionInterface dbCon) {
        String dbVersion = null;
        try {
            if (!dbCon.isClosed() && dbCon instanceof SQLiteDBConnection && ((SQLiteDBConnection)dbCon).oldDBTableExists()) {
                dbVersion = "1.0";
            }
        }
        catch (SQLException e) {
            LogHandler.severe("Error on checking database version", e);
        }
        return dbVersion;
    }

    public void addArena(String arenaName) {
        try {
            this.dbCon.addArena(arenaName);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on adding arena", e);
        }
    }

    public void removeArena(String arenaName) {
        try {
            this.dbCon.removeArena(arenaName);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on removing arena", e);
        }
    }

    public boolean arenaExists(String arenaName) {
        boolean exists = false;
        try {
            exists = this.dbCon.arenaExists(arenaName);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on check if arena exists", e);
        }
        return exists;
    }

    public void savePlayerScore(String playerId, String arenaName, int score) {
        try {
            this.dbCon.savePlayerScore(playerId, arenaName, score);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on saving player score", e);
        }
    }

    public void addPlayerWin(String playerId) {
        try {
            this.dbCon.addPlayerWin(playerId);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on updating count of wins of player", e);
        }
    }

    public void addPlayerLoss(String playerId) {
        try {
            this.dbCon.addPlayerLoss(playerId);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on updating count of losses of player", e);
        }
    }

    public int getPlayerWins(String playerId) {
        int wins = 0;
        try {
            wins = this.dbCon.getPlayerWins(playerId);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on getting count of wins of player", e);
        }
        return wins;
    }

    public int getPlayerLosses(String playerId) {
        int losses = 0;
        try {
            losses = this.dbCon.getPlayerLosses(playerId);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on getting count of losses of player", e);
        }
        return losses;
    }

    public int getScoreOfPlayer(String playerId) {
        int score = 0;
        try {
            score = this.dbCon.getScoreOfPlayer(playerId);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on getting score of player", e);
        }
        return score;
    }

    public int getScoreOfPlayerAndArena(String playerId, String arenaName) {
        int score = 0;
        try {
            score = this.dbCon.getScoreOfPlayerAndArena(playerId, arenaName);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on getting score of player in arena", e);
        }
        return score;
    }

    public LinkedHashMap<String, Integer> getPlayerTopTenListForArena(String arenaName) {
        LinkedHashMap<String, Integer> topTenList = null;
        try {
            topTenList = this.dbCon.getPlayerTopTenListForArena(arenaName);
        }
        catch (SQLException e) {
            LogHandler.severe("Error on getting player top ten list of arena", e);
        }
        return topTenList;
    }

    public LinkedHashMap<String, Integer> getPlayerTopTenList() {
        LinkedHashMap<String, Integer> topTenList = null;
        try {
            topTenList = this.dbCon.getPlayerTopTenList();
        }
        catch (SQLException e) {
            LogHandler.severe("Error on getting player top ten list", e);
        }
        return topTenList;
    }
}

