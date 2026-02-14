/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 */
package com.gmail.Orscrider.PvP1vs1;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@Deprecated
public class TopList {
    private PvP1vs1 pl;
    private Connection connection;
    private String DB_PATH;
    private Statement statement;

    public TopList(PvP1vs1 pl) {
        this.pl = pl;
        this.DB_PATH = pl.getDataFolder().getAbsolutePath() + "/stats.db";
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        this.connect();
    }

    public void connect() {
        try {
            this.pl.getLogger().info("Connecting to database...");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.DB_PATH);
            if (!this.connection.isClosed()) {
                this.pl.getLogger().info("Connected!");
            }
            this.statement = this.connection.createStatement();
            this.createTable();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTable() {
        try {
            if (this.connection.isClosed() || this.connection == null) {
                this.connect();
            }
            StringBuilder cmd = new StringBuilder();
            cmd.append("CREATE TABLE IF NOT EXISTS Points(Player String, Total int");
            for (Map.Entry<String, GameManager> arena : this.pl.getArenaManager().getArenas().entrySet()) {
                cmd.append(", ARENA_" + arena.getKey() + " int");
            }
            cmd.append(")");
            this.statement.execute(cmd.toString());
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addArena(String arenaName) {
        try {
            if (this.connection.isClosed() || this.connection == null) {
                this.connect();
            }
            if (this.columnExists("ARENA_" + arenaName)) {
                return;
            }
            try {
                this.statement.executeUpdate("ALTER TABLE Points ADD COLUMN ARENA_" + arenaName + " int");
            }
            catch (SQLException sQLException) {}
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetColumn(String arenaName) {
        try {
            if (this.connection.isClosed() || this.connection == null) {
                this.connect();
            }
            if (!this.columnExists("ARENA_" + arenaName)) {
                this.addArena(arenaName);
            }
            this.statement.executeUpdate("UPDATE Points SET ARENA_" + arenaName + " = 0");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addPlayer(String playerName) {
        try {
            if (this.connection.isClosed() || this.connection == null) {
                this.connect();
            }
            if (!this.tableContainsPlayer(playerName)) {
                ResultSet rs = this.statement.executeQuery("PRAGMA table_info(Points)");
                int columnCount = -2;
                while (rs.next()) {
                    ++columnCount;
                }
                StringBuilder cmd = new StringBuilder();
                cmd.append("INSERT INTO Points VALUES('" + playerName + "'" + ", 0");
                for (int i = 1; i <= columnCount; ++i) {
                    cmd.append(" ,0");
                }
                cmd.append(")");
                this.statement.executeUpdate(cmd.toString());
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getPoints(String playerName, String column) {
        int points = 0;
        try {
            if (this.connection.isClosed() || this.connection == null) {
                this.connect();
            }
            if (!this.tableContainsPlayer(playerName)) {
                this.addPlayer(playerName);
            }
            if (!this.columnExists(column) && !column.equals("Total")) {
                this.addArena(column);
            }
            ResultSet rs = this.statement.executeQuery("SELECT " + column + " FROM Points WHERE Player = '" + playerName + "'");
            while (rs.next()) {
                points = rs.getInt(1);
            }
            rs.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return points;
    }

    public boolean tableContainsPlayer(String name) {
        try {
            ResultSet rs;
            if (this.connection.isClosed() || this.connection == null) {
                this.connect();
            }
            if ((rs = this.statement.executeQuery("SELECT Player FROM Points WHERE Player = '" + name + "'")).next()) {
                return true;
            }
            rs.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void closeConnection() {
        try {
            if (!this.connection.isClosed()) {
                this.statement.close();
                this.connection.close();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void changePoints(String playerName, String arenaName, int points) {
        try {
            if (this.connection.isClosed() || this.connection == null) {
                this.connect();
            }
            if (!this.columnExists("ARENA_" + arenaName)) {
                this.addArena(arenaName);
            }
            this.statement.executeUpdate("UPDATE Points SET ARENA_" + arenaName + " = " + (this.getPoints(playerName, "ARENA_" + arenaName) + points) + " WHERE Player = '" + playerName + "'");
            this.statement.executeUpdate("UPDATE Points SET Total = " + (this.getPoints(playerName, "Total") + points) + " WHERE Player = '" + playerName + "'");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean columnExists(String column) {
        boolean columnExists = false;
        try {
            if (this.connection.isClosed() || this.connection == null) {
                this.connect();
            }
            ResultSet rs = this.statement.executeQuery("PRAGMA table_info(Points)");
            while (rs.next()) {
                if (!rs.getString(2).equals(column)) continue;
                columnExists = true;
            }
        }
        catch (SQLException sQLException) {
            // empty catch block
        }
        return columnExists;
    }

    public void showTopTen(String playerName, String column) {
        try {
            if (this.connection.isClosed() || this.connection == null) {
                this.connect();
            }
            Player p = Bukkit.getPlayer((String)playerName);
            LinkedHashMap<String, Integer> topTen = new LinkedHashMap<String, Integer>();
            ResultSet rs = this.statement.executeQuery("SELECT Player, " + column + " FROM Points ORDER BY " + column + " DESC LIMIT 10");
            while (rs.next()) {
                topTen.put(rs.getString(1), rs.getInt(2));
            }
            rs.close();
            p.sendMessage(ChatColor.GOLD + "=========================================");
            p.sendMessage(ChatColor.GOLD + "\u00a73\u27b7 \u00a72\u2582 \u2583 \u2584 \u2585 \u2586 \u2587 \u259b\u00a76 | \u00a74\u00a7l1vs1 \u00a76| \u00a72\u259c \u2587 \u2586 \u2585 \u2584 \u2583 \u2582 \u00a73\u27b7");
            p.sendMessage(ChatColor.GOLD + "=== \u00a72v. " + this.pl.getDescription().getVersion() + " \u00a76=== \u00a73TOP TEN\u00a76 ================");
            int i = 1;
            for (Map.Entry entry : topTen.entrySet()) {
                p.sendMessage(ChatColor.DARK_RED + String.valueOf(i) + "\u00a76.\u00a72 " + (String)entry.getKey() + " \u00a76-\u00a73 " + entry.getValue() + " \u00a76points");
                ++i;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

