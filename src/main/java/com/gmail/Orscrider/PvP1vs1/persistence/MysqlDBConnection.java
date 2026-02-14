/*
 * Decompiled with CFR 0.152.
 */
package com.gmail.Orscrider.PvP1vs1.persistence;

import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionInterface;
import com.gmail.Orscrider.PvP1vs1.persistence.SQLConnectionImpl;
import com.gmail.Orscrider.PvP1vs1.util.LogHandler;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlDBConnection
extends SQLConnectionImpl
implements DBConnectionInterface {
    private String connectString;

    public MysqlDBConnection(String host, String port, String user, String password, String database) {
        this.connectString = "jdbc:mysql://" + host + ":" + port + "/" + database + "?user=" + user + "&password=" + password + "&autoreconnect=true";
    }

    @Override
    public void connect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            LogHandler.info("Connecting to mysql database...");
            this.connection = DriverManager.getConnection(this.connectString);
            if (!this.connection.isClosed()) {
                LogHandler.info("Connected!");
            }
        }
        catch (SQLException e) {
            LogHandler.severe("Error on connecting to mysql database", e);
        }
        catch (ClassNotFoundException e) {
            LogHandler.severe("Error on finding class", e);
        }
    }
}

