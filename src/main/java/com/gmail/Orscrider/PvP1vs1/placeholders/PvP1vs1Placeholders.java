package com.gmail.Orscrider.PvP1vs1.placeholders;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionController;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.text.DecimalFormat;

/**
 * PlaceholderAPI expansion for PvP1vs1.
 * Placeholders: %pvp1vs1_1v1wins%, %pvp1vs1_1v1losses%, %pvp1vs1_1v1winrate%,
 * %pvp1vs1_dmwins%, %pvp1vs1_dmkills%, %pvp1vs1_dmdeaths%
 */
public class PvP1vs1Placeholders extends PlaceholderExpansion {

    private final PvP1vs1 plugin;
    private static final DecimalFormat ONE_DIGIT = new DecimalFormat("#,##0.0");

    public PvP1vs1Placeholders(PvP1vs1 plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "pvp1vs1";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty() ? "Orscrider" : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || !player.hasPlayedBefore()) {
            return "";
        }
        String playerId = player.getUniqueId().toString();
        DBConnectionController db = DBConnectionController.getInstance();
        if (db == null) return "";

        switch (params.toLowerCase()) {
            case "1v1wins":
                return String.valueOf(db.getPlayerWins(playerId));
            case "1v1losses":
                return String.valueOf(db.getPlayerLosses(playerId));
            case "1v1winrate":
                int wins = db.getPlayerWins(playerId);
                int losses = db.getPlayerLosses(playerId);
                if (wins + losses == 0) return "0";
                return ONE_DIGIT.format((double) wins / (wins + losses) * 100.0);
            case "dmwins":
                return String.valueOf(db.getPlayerDmWins(playerId));
            case "dmkills":
                return String.valueOf(db.getPlayerDmKills(playerId));
            case "dmdeaths":
                return String.valueOf(db.getPlayerDmDeaths(playerId));
            default:
                return null;
        }
    }
}
