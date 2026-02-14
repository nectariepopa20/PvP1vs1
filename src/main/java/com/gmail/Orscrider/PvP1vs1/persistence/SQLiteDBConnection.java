/*
 * Decompiled with CFR 0.152.
 */
package com.gmail.Orscrider.PvP1vs1.persistence;

import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionInterface;
import com.gmail.Orscrider.PvP1vs1.persistence.SQLConnectionImpl;
import com.gmail.Orscrider.PvP1vs1.util.LogHandler;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SQLiteDBConnection
extends SQLConnectionImpl
implements DBConnectionInterface {
    private String path;

    public SQLiteDBConnection(String path) {
        this.path = path;
    }

    @Override
    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            LogHandler.info("Connecting to sqlite database...");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.path);
            if (!this.connection.isClosed()) {
                LogHandler.info("Connected!");
            }
        }
        catch (SQLException e) {
            LogHandler.severe("Error on connecting to sqlite database", e);
        }
        catch (ClassNotFoundException e) {
            LogHandler.severe("Error on finding class", e);
        }
    }

    public boolean oldDBTableExists() throws SQLException {
        boolean tableExists = true;
        DatabaseMetaData dbm = this.connection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, "Points", null);
        if (!tables.next()) {
            tableExists = false;
        }
        return tableExists;
    }

    public List<LinkedHashMap<String, String>> getOldTableData() throws SQLException {
        LogHandler.info("Getting old table data...");
        String getPointsSQL = "SELECT * FROM Points";
        Statement statement = this.connection.createStatement();
        ResultSet result = statement.executeQuery(getPointsSQL);
        ResultSetMetaData rsmd = result.getMetaData();
        int columnCount = rsmd.getColumnCount();
        ArrayList<LinkedHashMap<String, String>> resultList = new ArrayList<LinkedHashMap<String, String>>();
        while (result.next()) {
            LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
            for (int i = 1; i <= columnCount; ++i) {
                map.put(rsmd.getColumnName(i), result.getString(i));
            }
            resultList.add(map);
        }
        return resultList;
    }
}

