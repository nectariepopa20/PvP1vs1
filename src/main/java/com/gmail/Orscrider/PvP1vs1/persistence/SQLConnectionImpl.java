/*
 * Decompiled with CFR 0.152.
 */
package com.gmail.Orscrider.PvP1vs1.persistence;

import com.gmail.Orscrider.PvP1vs1.util.LogHandler;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;

public class SQLConnectionImpl {
    protected Connection connection;
    private String[] databaseTables = new String[]{"players", "players_arenas", "arenas"};

    public boolean dbTablesExists() throws SQLException {
        boolean tablesExists = true;
        DatabaseMetaData dbm = this.connection.getMetaData();
        for (String table : this.databaseTables) {
            ResultSet tables = dbm.getTables(null, null, table, null);
            if (tables.next()) continue;
            LogHandler.severe("Missing database table: " + table);
            tablesExists = false;
        }
        return tablesExists;
    }

    public boolean isClosed() throws SQLException {
        return this.connection == null || this.connection.isClosed();
    }

    public void disconnect() {
        try {
            this.connection.close();
        }
        catch (SQLException e) {
            LogHandler.severe("Error on disconnecting from database", e);
        }
    }

    public void createTables() throws SQLException {
        String playerTableCS = "CREATE TABLE players (playerid varchar(36) NOT NULL, wins INTEGER NOT NULL, losses INTEGER NOT NULL, dmwins INTEGER NOT NULL DEFAULT 0, dmkills INTEGER NOT NULL DEFAULT 0, dmdeaths INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (playerid));";
        String playerArenaTableCS = "CREATE TABLE players_arenas (playerid varchar(36) NOT NULL, arenaid INTEGER(10) NOT NULL, score INTEGER(10) NOT NULL, PRIMARY KEY (playerid, arenaid));";
        String arenaTableCS = "CREATE TABLE arenas (arenaid INTEGER(10) NOT NULL, name varchar(100) NOT NULL, deleted INTEGER(1) NOT NULL, PRIMARY KEY (arenaid));";
        try {
            Statement statement = this.connection.createStatement();
            this.connection.setAutoCommit(false);
            statement.execute(playerTableCS);
            statement.execute(playerArenaTableCS);
            statement.execute(arenaTableCS);
            statement.close();
        }
        catch (SQLException e) {
            this.connection.rollback();
            throw e;
        }
        finally {
            this.connection.setAutoCommit(true);
        }
    }

    public void removeTable(String table) throws SQLException {
        String dropTable = "DROP TABLE " + table;
        Statement statement = null;
        try {
            statement = this.connection.createStatement();
            statement.executeUpdate(dropTable);
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (statement != null) {
                try {
                    statement.close();
                }
                catch (SQLException e) {
                    LogHandler.severe("Error on closing statement", e);
                }
            }
        }
    }

    public void addTableConstraints() throws SQLException {
        String fkPAP = "ALTER TABLE players_arenas ADD CONSTRAINT `FK_players_arenas_players` FOREIGN KEY `FK_players_arenas_players` (playerid) REFERENCES players (playerid) ON DELETE RESTRICT ON UPDATE RESTRICT";
        String fkPAA = "ALTER TABLE players_arenas ADD CONSTRAINT `FK_players_arenas_arenas` FOREIGN KEY `FK_players_arenas_arenas` (arenaid) REFERENCES arenas (arenaid) ON DELETE RESTRICT ON UPDATE RESTRICT";
        Statement statement = null;
        try {
            statement = this.connection.createStatement();
            statement.executeUpdate(fkPAP);
            statement.executeUpdate(fkPAA);
            statement.close();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (statement != null) {
                try {
                    statement.close();
                }
                catch (SQLException e) {
                    LogHandler.severe("Error on closing statement", e);
                }
            }
        }
    }

    private int getNextArenaId() throws SQLException {
        Statement statement = this.connection.createStatement();
        String arenaNextIdSQL = "SELECT MAX(arenaid) FROM arenas";
        ResultSet result = statement.executeQuery(arenaNextIdSQL);
        int nextId = 1;
        if (result.next()) {
            nextId = result.getInt(1) + 1;
        }
        return nextId;
    }

    public boolean addArena(String arenaName) throws SQLException {
        String addArenasSQL = "INSERT INTO arenas (arenaid, name, deleted) VALUES (?, ?, 0)";
        int result = 0;
        PreparedStatement ps = null;
        try {
            ps = this.connection.prepareStatement(addArenasSQL);
            ps.setInt(1, this.getNextArenaId());
            ps.setString(2, arenaName);
            result = ps.executeUpdate();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e) {
                    LogHandler.severe("Error on closing prepared statement", e);
                }
            }
        }
        return result != 0;
    }

    public boolean removeArena(String arenaName) throws SQLException {
        String removeArenaSQL = "UPDATE arenas SET deleted = 1 WHERE name = ?";
        PreparedStatement ps = null;
        int result = 0;
        try {
            ps = this.connection.prepareStatement(removeArenaSQL);
            ps.setString(1, arenaName);
            result = ps.executeUpdate();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e) {
                    LogHandler.severe("Error on closing prepared statement", e);
                }
            }
        }
        return result != 0;
    }

    public boolean arenaExists(String arenaName) throws SQLException {
        boolean arenaExists = false;
        String getArenaSQL = "SELECT 1 FROM arenas WHERE name = ? AND deleted = 0";
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ps = this.connection.prepareStatement(getArenaSQL);
            ps.setString(1, arenaName);
            result = ps.executeQuery();
            if (result.next()) {
                arenaExists = true;
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (SQLException e2) {
                    LogHandler.severe("Error on closing result set", e2);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return arenaExists;
    }

    public void savePlayerScore(String playerId, String arenaName, int score) throws SQLException {
        if (this.arenaExists(arenaName)) {
            if (!this.playerExists(playerId)) {
                this.addPlayer(playerId);
            }
            if (!this.playerArenaExists(playerId, arenaName)) {
                this.addPlayerArenaScore(playerId, arenaName, score);
            } else {
                this.updatePlayerArenaScore(playerId, arenaName, score);
            }
        } else {
            LogHandler.warning("Cannot save player score because arena " + arenaName + " does not exist");
        }
    }

    private boolean addPlayerArenaScore(String playerId, String arenaName, int score) throws SQLException {
        String addPlayerScoreSQL = "INSERT INTO players_arenas (playerid, arenaid, score) VALUES (?, ?, ?)";
        PreparedStatement ps = null;
        int result = 0;
        try {
            ps = this.connection.prepareStatement(addPlayerScoreSQL);
            ps.setString(1, playerId);
            ps.setInt(2, this.getArenaIdForName(arenaName));
            ps.setInt(3, score);
            result = ps.executeUpdate();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return result != 0;
    }

    private Integer getArenaIdForName(String arenaName) throws SQLException {
        String arenaIdSQL = "SELECT arenaid FROM arenas WHERE name = ? AND deleted = 0";
        Integer arenaId = null;
        PreparedStatement ps = null;
        try {
            ps = this.connection.prepareStatement(arenaIdSQL);
            ps.setString(1, arenaName);
            ResultSet result = ps.executeQuery();
            if (result.next()) {
                arenaId = result.getInt(1);
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return arenaId;
    }

    private boolean updatePlayerArenaScore(String playerId, String arenaName, int score) throws SQLException {
        String updatePlayerScoreSQL = "UPDATE players_arenas SET score = score + ? WHERE playerid = ? AND arenaid IN (SELECT arenaid FROM arenas WHERE name = ? AND deleted = 0)";
        PreparedStatement ps = null;
        int result = 0;
        try {
            ps = this.connection.prepareStatement(updatePlayerScoreSQL);
            ps.setInt(1, score);
            ps.setString(2, playerId);
            ps.setString(3, arenaName);
            result = ps.executeUpdate();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return result != 0;
    }

    public boolean addPlayerWin(String playerId) throws SQLException {
        String updatePlayerWins = "UPDATE players SET wins = wins + 1 WHERE playerid = ?";
        PreparedStatement ps = null;
        int result = 0;
        try {
            ps = this.connection.prepareStatement(updatePlayerWins);
            ps.setString(1, playerId);
            result = ps.executeUpdate();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return result != 0;
    }

    public boolean addPlayerLoss(String playerId) throws SQLException {
        String updatePlayerLosses = "UPDATE players SET losses = losses + 1 WHERE playerid = ?";
        PreparedStatement ps = null;
        int result = 0;
        try {
            ps = this.connection.prepareStatement(updatePlayerLosses);
            ps.setString(1, playerId);
            result = ps.executeUpdate();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return result != 0;
    }

    public int getPlayerWins(String playerId) throws SQLException {
        String getPlayerWins = "SELECT wins FROM players WHERE playerid = ?";
        int wins = 0;
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ps = this.connection.prepareStatement(getPlayerWins);
            ps.setString(1, playerId);
            result = ps.executeQuery();
            if (result.next()) {
                wins = result.getInt(1);
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing result set", e1);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return wins;
    }

    public int getPlayerLosses(String playerId) throws SQLException {
        String getPlayerLosses = "SELECT losses FROM players WHERE playerid = ?";
        int losses = 0;
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ps = this.connection.prepareStatement(getPlayerLosses);
            ps.setString(1, playerId);
            result = ps.executeQuery();
            if (result.next()) {
                losses = result.getInt(1);
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing result set", e1);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return losses;
    }

    public void ensureDmStatsColumns() throws SQLException {
        String[] columns = new String[]{"dmwins", "dmkills", "dmdeaths"};
        for (String col : columns) {
            try {
                Statement st = this.connection.createStatement();
                String sql = "ALTER TABLE players ADD COLUMN " + col + " INTEGER NOT NULL DEFAULT 0";
                st.executeUpdate(sql);
                st.close();
            } catch (SQLException e) {
                if (e.getMessage() == null || (!e.getMessage().toLowerCase().contains("duplicate") && !e.getMessage().toLowerCase().contains("already exists"))) {
                    throw e;
                }
            }
        }
    }

    public boolean addPlayerDmWin(String playerId) throws SQLException {
        ensurePlayerExistsForDm(playerId);
        String sql = "UPDATE players SET dmwins = dmwins + 1 WHERE playerid = ?";
        PreparedStatement ps = null;
        try {
            ps = this.connection.prepareStatement(sql);
            ps.setString(1, playerId);
            return ps.executeUpdate() != 0;
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException e) { LogHandler.severe("Error on closing prepared statement", e); }
        }
    }

    public boolean addPlayerDmKill(String playerId) throws SQLException {
        ensurePlayerExistsForDm(playerId);
        String sql = "UPDATE players SET dmkills = dmkills + 1 WHERE playerid = ?";
        PreparedStatement ps = null;
        try {
            ps = this.connection.prepareStatement(sql);
            ps.setString(1, playerId);
            return ps.executeUpdate() != 0;
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException e) { LogHandler.severe("Error on closing prepared statement", e); }
        }
    }

    public boolean addPlayerDmDeath(String playerId) throws SQLException {
        ensurePlayerExistsForDm(playerId);
        String sql = "UPDATE players SET dmdeaths = dmdeaths + 1 WHERE playerid = ?";
        PreparedStatement ps = null;
        try {
            ps = this.connection.prepareStatement(sql);
            ps.setString(1, playerId);
            return ps.executeUpdate() != 0;
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException e) { LogHandler.severe("Error on closing prepared statement", e); }
        }
    }

    public int getPlayerDmWins(String playerId) throws SQLException {
        return getPlayerDmStat(playerId, "dmwins");
    }

    public int getPlayerDmKills(String playerId) throws SQLException {
        return getPlayerDmStat(playerId, "dmkills");
    }

    public int getPlayerDmDeaths(String playerId) throws SQLException {
        return getPlayerDmStat(playerId, "dmdeaths");
    }

    private void ensurePlayerExistsForDm(String playerId) throws SQLException {
        if (!playerExists(playerId)) {
            addPlayer(playerId);
        }
    }

    private int getPlayerDmStat(String playerId, String column) throws SQLException {
        String sql = "SELECT " + column + " FROM players WHERE playerid = ?";
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ps = this.connection.prepareStatement(sql);
            ps.setString(1, playerId);
            result = ps.executeQuery();
            return result.next() ? result.getInt(1) : 0;
        } finally {
            if (result != null) try { result.close(); } catch (SQLException e) { LogHandler.severe("Error on closing result set", e); }
            if (ps != null) try { ps.close(); } catch (SQLException e) { LogHandler.severe("Error on closing prepared statement", e); }
        }
    }

    private boolean playerArenaExists(String playerId, String arenaName) throws SQLException {
        boolean playerArenaExists = false;
        String getPlayerArenaSQL = "SELECT 1 FROM players_arenas pa JOIN arenas a ON pa.arenaid = a.arenaid WHERE pa.playerid = ? AND a.name = ?";
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ps = this.connection.prepareStatement(getPlayerArenaSQL);
            ps.setString(1, playerId);
            ps.setString(2, arenaName);
            result = ps.executeQuery();
            if (result.next()) {
                playerArenaExists = true;
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing result set", e1);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return playerArenaExists;
    }

    private boolean playerExists(String playerId) throws SQLException {
        boolean playerExists = false;
        String getPlayerSQL = "SELECT 1 FROM players WHERE playerid = ?";
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ps = this.connection.prepareStatement(getPlayerSQL);
            ps.setString(1, playerId);
            result = ps.executeQuery();
            if (result.next()) {
                playerExists = true;
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing result set", e1);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return playerExists;
    }

    private boolean addPlayer(String playerId) throws SQLException {
        String addPlayerSQL = "INSERT INTO players (playerid, wins, losses, dmwins, dmkills, dmdeaths) VALUES (?, 0, 0, 0, 0, 0)";
        PreparedStatement ps = null;
        int result = 0;
        try {
            ps = this.connection.prepareStatement(addPlayerSQL);
            ps.setString(1, playerId);
            result = ps.executeUpdate();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return result != 0;
    }

    public int getScoreOfPlayer(String playerId) throws SQLException {
        String scoreOfPlayerSQL = "SELECT SUM(score) FROM players_arenas WHERE playerid = ?";
        int score = 0;
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ps = this.connection.prepareStatement(scoreOfPlayerSQL);
            ps.setString(1, playerId);
            result = ps.executeQuery();
            if (result.next()) {
                score = result.getInt(1);
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing result set", e1);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return score;
    }

    public int getScoreOfPlayerAndArena(String playerId, String arenaName) throws SQLException {
        String scoreOfPlayerSQL = "SELECT pa.score FROM players_arenas pa JOIN arenas a ON pa.arenaid = a.arenaid WHERE pa.playerid = ? AND a.name = ? AND a.deleted = 0";
        int score = 0;
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ps = this.connection.prepareStatement(scoreOfPlayerSQL);
            ps.setString(1, playerId);
            ps.setString(2, arenaName);
            result = ps.executeQuery();
            if (result.next()) {
                score = result.getInt(1);
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing result set", e1);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return score;
    }

    public LinkedHashMap<String, Integer> getPlayerTopTenListForArena(String arenaName) throws SQLException {
        LinkedHashMap<String, Integer> topTen = new LinkedHashMap<String, Integer>();
        String topTenSQL = "SELECT p.playerid, pa.score FROM players_arenas pa JOIN players p ON pa.playerid = p.playerid JOIN arenas a ON a.arenaid = pa.arenaid AND a.deleted = 0 WHERE a.name = ? ORDER BY pa.score DESC LIMIT 10";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = this.connection.prepareStatement(topTenSQL);
            ps.setString(1, arenaName);
            rs = ps.executeQuery();
            while (rs.next()) {
                topTen.put(rs.getString(1), rs.getInt(2));
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing result set", e1);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing prepared statement", e1);
                }
            }
        }
        return topTen;
    }

    public LinkedHashMap<String, Integer> getPlayerTopTenList() throws SQLException {
        LinkedHashMap<String, Integer> topTen = new LinkedHashMap<String, Integer>();
        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = this.connection.createStatement();
            rs = statement.executeQuery("SELECT p.playerid, pa.score FROM players_arenas pa JOIN players p ON pa.playerid = p.playerid JOIN arenas a ON a.arenaid = pa.arenaid AND a.deleted = 0 ORDER BY pa.score DESC LIMIT 10");
            while (rs.next()) {
                topTen.put(rs.getString(1), rs.getInt(2));
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing result set", e1);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                }
                catch (SQLException e1) {
                    LogHandler.severe("Error on closing statement", e1);
                }
            }
        }
        return topTen;
    }
}

