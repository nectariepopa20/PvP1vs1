/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package com.gmail.Orscrider.PvP1vs1.metrics;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import com.gmail.Orscrider.PvP1vs1.metrics.Metrics;
import com.gmail.Orscrider.PvP1vs1.util.LogHandler;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.plugin.Plugin;

public class MetricsHandler {
    private static MetricsHandler metricsHandler;
    private PvP1vs1 pl;
    private Metrics metrics;
    private Metrics.Graph roundsGraph;
    private HashMap<String, Integer> roundFormats = new HashMap();
    private int gamesPlayed = 0;
    private int loggedOutGames = 0;
    private int maxRoundsGames = 0;
    private int healthLeftPercentageDivisions = 10;
    private int[] healthLeftStats = new int[this.healthLeftPercentageDivisions];

    public MetricsHandler(PvP1vs1 pl) {
        if (metricsHandler == null) {
            metricsHandler = this;
            this.pl = pl;
        }
    }

    public void startMetrics() {
        try {
            this.metrics = new Metrics((Plugin)this.pl);
            Metrics.Graph arenaCountGraph = this.metrics.createGraph("Arena Count");
            arenaCountGraph.addPlotter(new Metrics.Plotter("Enabled arenas"){

                @Override
                public int getValue() {
                    return MetricsHandler.this.pl.getArenaManager().getEnabledArenas().size();
                }
            });
            arenaCountGraph.addPlotter(new Metrics.Plotter("Disabled arenas"){

                @Override
                public int getValue() {
                    return MetricsHandler.this.pl.getArenaManager().getDisabledArenas().size();
                }
            });
            Metrics.Graph gamesPlayedGraph = this.metrics.createGraph("Games Played");
            gamesPlayedGraph.addPlotter(new Metrics.Plotter("Games Played"){

                @Override
                public int getValue() {
                    return MetricsHandler.this.gamesPlayed;
                }

                @Override
                public void reset() {
                    MetricsHandler.this.gamesPlayed = 0;
                }
            });
            gamesPlayedGraph.addPlotter(new Metrics.Plotter("Logged Out"){

                @Override
                public int getValue() {
                    return MetricsHandler.this.loggedOutGames;
                }

                @Override
                public void reset() {
                    MetricsHandler.this.loggedOutGames = 0;
                }
            });
            gamesPlayedGraph.addPlotter(new Metrics.Plotter("Max Rounds"){

                @Override
                public int getValue() {
                    return MetricsHandler.this.maxRoundsGames;
                }

                @Override
                public void reset() {
                    MetricsHandler.this.maxRoundsGames = 0;
                }
            });
            this.roundsGraph = this.metrics.createGraph("Round Formats");
            this.updateRoundFormats();
            Metrics.Graph healthLeftGraph = this.metrics.createGraph("health_left_after_round");
            DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
            otherSymbols.setDecimalSeparator(',');
            otherSymbols.setGroupingSeparator('.');
            DecimalFormat oneDigit = new DecimalFormat("#,##0.#", otherSymbols);
            double length = this.healthLeftStats.length;
            for (int i = 0; i < this.healthLeftStats.length; ++i) {
                final int x = i;
                String floor = oneDigit.format((double)i / length * 100.0);
                String ceiling = oneDigit.format((double)(i + 1) / length * 100.0);
                healthLeftGraph.addPlotter(new Metrics.Plotter(floor + " - " + ceiling + " %"){

                    @Override
                    public int getValue() {
                        return MetricsHandler.this.healthLeftStats[x];
                    }

                    @Override
                    public void reset() {
                        ((MetricsHandler)MetricsHandler.this).healthLeftStats[x] = 0;
                    }
                });
            }
            this.metrics.start();
        }
        catch (IOException e) {
            LogHandler.warning("Failed to start Metrics");
        }
    }

    public void increaseGamesPlayed() {
        ++this.gamesPlayed;
    }

    public void increaseLoggedOutGames() {
        ++this.loggedOutGames;
    }

    public void increaseMaxRoundsGames() {
        ++this.maxRoundsGames;
    }

    public void addHealthLeftAfterRoundStat(double healthPercentage) {
        for (int i = 0; i < this.healthLeftStats.length; ++i) {
            if (!(healthPercentage <= (double)(i + 1) / Double.valueOf(this.healthLeftStats.length))) continue;
            this.healthLeftStats[i] = this.healthLeftStats[i] + 1;
            break;
        }
    }

    public void updateRoundFormats() {
        for (Map.Entry<String, Integer> entry : this.roundFormats.entrySet()) {
            this.roundFormats.put(entry.getKey(), 0);
        }
        for (GameManager gameManager : this.pl.getArenaManager().getEnabledArenas().values()) {
            final String roundFormat = gameManager.getArenaConfig().getString("rounds");
            if (!this.roundFormats.containsKey(roundFormat)) {
                this.roundFormats.put(roundFormat, 1);
            } else {
                this.roundFormats.put(roundFormat, this.roundFormats.get(roundFormat) + 1);
            }
            boolean plotterExists = false;
            for (Metrics.Plotter plotter : this.roundsGraph.getPlotters()) {
                if (!plotter.getColumnName().equals(roundFormat)) continue;
                plotterExists = true;
                break;
            }
            if (plotterExists) continue;
            this.roundsGraph.addPlotter(new Metrics.Plotter(roundFormat){

                @Override
                public int getValue() {
                    return (Integer)MetricsHandler.this.roundFormats.get(roundFormat);
                }
            });
        }
    }

    public static MetricsHandler getInstance() {
        if (metricsHandler != null) {
            return metricsHandler;
        }
        LogHandler.warning("MetricsHandler has not been initialized!");
        return null;
    }
}

