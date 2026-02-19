package com.gmail.Orscrider.PvP1vs1.scoreboard;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.arena.GameManager;
import com.gmail.Orscrider.PvP1vs1.deathmatch.DmGameManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages scoreboard display for 1v1 and DM lobby, game, and endgame.
 * Lines and title are read from messages config; placeholders are replaced and PlaceholderAPI applied.
 * Score numbers are hidden when running on Paper (1.20.4+).
 * Only updates when content actually changes to prevent flicker.
 */
public class ScoreboardManager {

    private static final int MAX_LINES = 15;
    private static final int MAX_PREFIX = 64;
    private static final int MAX_SUFFIX = 64;

    private final PvP1vs1 pl;
    private final Map<Player, Scoreboard> playerScoreboards = new HashMap<>();
    /** Cache of last displayed content per player to avoid redundant updates (flicker). Key: player UUID. */
    private final Map<UUID, CachedScoreboard> cache = new HashMap<>();
    private static volatile boolean paperNumberFormatTried = false;
    private static volatile Class<?> paperNumberFormatClass = null;
    private static volatile Object paperBlankNumberFormat = null;

    public ScoreboardManager(PvP1vs1 plugin) {
        this.pl = plugin;
    }

    /**
     * Apply plugin placeholders then PlaceholderAPI (if present). Replace {key} with values from map.
     */
    public String replace(Player player, String line, Map<String, String> placeholders) {
        if (line == null) return "";
        String out = line;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace(e.getKey(), e.getValue() != null ? e.getValue() : "");
            }
        }
        if (pl.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            out = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, out);
        }
        return ChatColor.translateAlternateColorCodes('&', out);
    }

    /**
     * Show a scoreboard from messages config path (e.g. "scoreboard.lobby") with given placeholders.
     * Uses 1v1 messages config.
     */
    public void show1v1(Player player, String configPath, Map<String, String> placeholders) {
        if (!pl.getDataHandler().getMessagesConfig().getBoolean("scoreboard.enabled", true)) return;
        String titlePath = configPath + ".title";
        String linesPath = configPath + ".lines";
        String title = pl.getDataHandler().getMessagesConfig().getString(titlePath, "&61v1");
        List<String> lines = pl.getDataHandler().getMessagesConfig().getStringList(linesPath);
        show(player, title, lines, placeholders);
    }

    /**
     * Show a scoreboard from DM messages config path with given placeholders.
     */
    public void showDm(Player player, String configPath, Map<String, String> placeholders) {
        if (!pl.getDataHandler().getDmMessagesConfig().getBoolean("scoreboard.enabled", true)) return;
        String titlePath = configPath + ".title";
        String linesPath = configPath + ".lines";
        String title = pl.getDataHandler().getDmMessagesConfig().getString(titlePath, "&4DM");
        List<String> lines = pl.getDataHandler().getDmMessagesConfig().getStringList(linesPath);
        show(player, title, lines, placeholders);
    }

    /**
     * Build and show scoreboard with title and lines. Lines are replaced with placeholders and PAPI.
     * Only applies updates when title or line content has changed to prevent flicker.
     */
    public void show(Player player, String title, List<String> lines, Map<String, String> placeholders) {
        if (player == null || !player.isOnline()) return;
        if (lines == null || lines.isEmpty()) {
            clear(player);
            cache.remove(player.getUniqueId());
            return;
        }
        List<String> resolvedLines = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_LINES, lines.size()); i++) {
            resolvedLines.add(replace(player, lines.get(i), placeholders));
        }
        String displayTitle = replace(player, title, placeholders);
        if (displayTitle.length() > 32) displayTitle = displayTitle.substring(0, 32);

        CachedScoreboard prev = cache.get(player.getUniqueId());
        if (prev != null && prev.title.equals(displayTitle) && prev.lines.equals(resolvedLines)) {
            return;
        }
        cache.put(player.getUniqueId(), new CachedScoreboard(displayTitle, resolvedLines));

        Scoreboard board = player.getScoreboard();
        if (board == pl.getServer().getScoreboardManager().getMainScoreboard()) {
            board = pl.getServer().getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }
        playerScoreboards.put(player, board);

        Objective obj = board.getObjective("pvp1v1");
        if (obj == null) {
            obj = board.registerNewObjective("pvp1v1", "dummy", displayTitle);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            tryHideScoreNumbers(obj);
        } else if (!obj.getDisplayName().equals(displayTitle)) {
            obj.setDisplayName(displayTitle);
        }

        int size = resolvedLines.size();
        for (int i = 0; i < size; i++) {
            String line = resolvedLines.get(i);
            String entry = getUniqueEntry(i);
            Team team = board.getTeam("line" + i);
            if (team == null) team = board.registerNewTeam("line" + i);
            if (!team.hasEntry(entry)) team.addEntry(entry);
            String prefix = line.length() <= MAX_PREFIX ? line : line.substring(0, MAX_PREFIX);
            String suffix = line.length() <= MAX_PREFIX ? "" : (line.length() <= MAX_PREFIX + MAX_SUFFIX ? line.substring(MAX_PREFIX) : line.substring(MAX_PREFIX, MAX_PREFIX + MAX_SUFFIX));
            if (!prefix.equals(team.getPrefix()) || !suffix.equals(team.getSuffix())) {
                team.setPrefix(prefix);
                team.setSuffix(suffix);
            }
            int score = size - i;
            if (obj.getScore(entry).getScore() != score) {
                obj.getScore(entry).setScore(score);
            }
        }
        for (int i = size; i < MAX_LINES; i++) {
            String entry = getUniqueEntry(i);
            Team team = board.getTeam("line" + i);
            if (team != null && team.hasEntry(entry)) {
                team.removeEntry(entry);
                board.resetScores(entry);
            }
        }
    }

    private static final class CachedScoreboard {
        final String title;
        final List<String> lines;
        CachedScoreboard(String title, List<String> lines) {
            this.title = title;
            this.lines = new ArrayList<>(lines);
        }
    }

    private String getUniqueEntry(int index) {
        String[] codes = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e"};
        return codes[index % codes.length] + "§r";
    }

    private void tryHideScoreNumbers(Objective objective) {
        if (paperBlankNumberFormat != null && paperNumberFormatClass != null) {
            try {
                objective.getClass().getMethod("numberFormat", paperNumberFormatClass).invoke(objective, paperBlankNumberFormat);
            } catch (Throwable ignored) {}
            return;
        }
        if (paperNumberFormatTried) return;
        paperNumberFormatTried = true;
        ClassLoader[] loaders = {
            objective.getClass().getClassLoader(),
            org.bukkit.Bukkit.class.getClassLoader(),
            Thread.currentThread().getContextClassLoader()
        };
        for (ClassLoader loader : loaders) {
            if (loader == null) continue;
            try {
                Class<?> numberFormatClass = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat", false, loader);
                Object blank = numberFormatClass.getMethod("blank").invoke(null);
                if (blank != null) {
                    objective.getClass().getMethod("numberFormat", numberFormatClass).invoke(objective, blank);
                    ScoreboardManager.paperNumberFormatClass = numberFormatClass;
                    ScoreboardManager.paperBlankNumberFormat = blank;
                    return;
                }
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Clear scoreboard for player (remove sidebar objective).
     */
    public void clear(Player player) {
        if (player == null || !player.isOnline()) return;
        cache.remove(player.getUniqueId());
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
        if (obj != null && "pvp1v1".equals(obj.getName())) obj.unregister();
        playerScoreboards.remove(player);
    }

    public void clearAll() {
        for (Player p : playerScoreboards.keySet()) {
            if (p != null && p.isOnline()) clear(p);
        }
        playerScoreboards.clear();
    }

    /**
     * Called every second from main thread. Updates scoreboards for all players in 1v1/DM lobby, game, or endgame.
     */
    public void updateAll() {
        if (!pl.getDataHandler().getMessagesConfig().getBoolean("scoreboard.enabled", true)
                && !pl.getDataHandler().getDmMessagesConfig().getBoolean("scoreboard.enabled", true)) {
            return;
        }
        for (Map.Entry<String, GameManager> e : pl.getArenaManager().getArenas().entrySet()) {
            GameManager gm = e.getValue();
            String arenaName = e.getKey();
            String queueKey = gm.getCurrentQueueName();
            String queueName = resolveQueueDisplayName("1v1", queueKey);

            for (Player p : gm.getLobbyPlayers()) {
                if (p == null || !p.isOnline()) continue;
                Map<String, String> map = new HashMap<>();
                map.put("{arena_name}", arenaName);
                map.put("{queue_name}", queueName);
                map.put("{player_count}", gm.getLobbySize() + "/2");
                int rem = gm.getLobbyCountdownRemaining();
                if (gm.getArenaStatus() == GameManager.arenaMode.COUNTDOWN_LOBBY && rem >= 0) {
                    String statusMsg = pl.getDataHandler().getMessagesConfig().getString("scoreboard.statusStartingIn", "&7Starting in &f{countdown}&7s").replace("{countdown}", String.valueOf(rem));
                    map.put("{status}", statusMsg);
                    map.put("{countdown}", String.valueOf(rem));
                } else {
                    String statusMsg = pl.getDataHandler().getMessagesConfig().getString("scoreboard.statusWaiting", "&7Waiting for players");
                    map.put("{status}", statusMsg);
                    map.put("{countdown}", "-");
                }
                show1v1(p, "scoreboard.lobby", map);
            }

            Player[] arenaPlayers = gm.getArenaPlayers();
            for (Player p : arenaPlayers) {
                if (p == null || !p.isOnline()) continue;
                if (gm.getArenaStatus() == GameManager.arenaMode.FIGHT) {
                    Player opp = p == arenaPlayers[0] ? arenaPlayers[1] : arenaPlayers[0];
                    Map<String, String> map = new HashMap<>();
                    map.put("{arena_name}", arenaName);
                    map.put("{queue_name}", resolveQueueDisplayName("1v1", gm.getCurrentQueueName()));
                    map.put("{opponent_name}", opp != null && opp.isOnline() ? opp.getName() : "-");
                    map.put("{opponent_health}", opp != null && opp.isOnline() ? String.valueOf(Math.max(0, (int) opp.getHealth())) : "0");
                    map.put("{time_left}", String.valueOf(gm.getTimeOut()));
                    int cur = gm.getCurrentRound();
                    int total = gm.getTotalRounds();
                    map.put("{round_current}", String.valueOf(cur));
                    map.put("{round_total}", String.valueOf(total));
                    map.put("{game_type}", "Bo" + total);
                    show1v1(p, "scoreboard.game", map);
                } else if (gm.getArenaStatus() == GameManager.arenaMode.BETWEEN_ROUNDS) {
                    Player winner = gm.getPlayerRoundWins(arenaPlayers[0]) > gm.getPlayerRoundWins(arenaPlayers[1]) ? arenaPlayers[0] : arenaPlayers[1];
                    Map<String, String> map = new HashMap<>();
                    map.put("{arena_name}", arenaName);
                    map.put("{queue_name}", resolveQueueDisplayName("1v1", gm.getCurrentQueueName()));
                    map.put("{round_winner}", winner != null ? winner.getName() : "-");
                    map.put("{winner_health}", winner != null && winner.isOnline() ? String.valueOf((int) winner.getHealth()) : "0");
                    map.put("{next_round_in}", String.valueOf(gm.getArenaConfig().getInt("winningTimer", 10)));
                    show1v1(p, "scoreboard.endgame", map);
                } else if (gm.getArenaStatus() == GameManager.arenaMode.WINNING_TIMER) {
                    Player winner = gm.getPostGameLoser() == arenaPlayers[0] ? arenaPlayers[1] : arenaPlayers[0];
                    Player loser = gm.getPostGameLoser();
                    Map<String, String> map = new HashMap<>();
                    map.put("{arena_name}", arenaName);
                    map.put("{queue_name}", resolveQueueDisplayName("1v1", gm.getCurrentQueueName()));
                    map.put("{game_winner}", winner != null ? winner.getName() : "-");
                    map.put("{game_loser}", loser != null ? loser.getName() : "-");
                    map.put("{round_total}", String.valueOf(gm.getTotalRounds()));
                    show1v1(p, "scoreboard.endgameGameOver", map);
                }
            }
        }

        for (Map.Entry<String, DmGameManager> e : pl.getDmArenaManager().getArenas().entrySet()) {
            DmGameManager dm = e.getValue();
            String arenaName = e.getKey();
            String queueName = resolveQueueDisplayName("dm", dm.getCurrentQueueName());

            for (Player p : dm.getLobbyPlayers()) {
                if (p == null || !p.isOnline()) continue;
                Map<String, String> map = new HashMap<>();
                map.put("{arena_name}", arenaName);
                map.put("{queue_name}", queueName);
                map.put("{player_count}", dm.getLobbySize() + "/" + dm.getMaxPlayers());
                map.put("{min_players}", String.valueOf(dm.getBareMinimumPlayers()));
                map.put("{optimal_players}", String.valueOf(dm.getOptimalMinimumPlayers()));
                map.put("{countdown_bare}", String.valueOf(dm.getLobbyCountdownDurationBare()));
                map.put("{countdown_optimal}", String.valueOf(dm.getLobbyCountdownDurationOptimal()));
                int rem = dm.getLobbyCountdownRemaining();
                if (dm.getArenaStatus() == DmGameManager.DmArenaMode.COUNTDOWN_LOBBY && rem >= 0) {
                    if (dm.getLobbySize() >= dm.getOptimalMinimumPlayers()) {
                        String statusMsg = pl.getDataHandler().getDmMessagesConfig().getString("scoreboard.statusOptimalStartingIn", "&7Optimal: starting in &f{countdown}&7s").replace("{countdown}", String.valueOf(rem));
                        map.put("{status}", statusMsg);
                    } else {
                        String statusMsg = pl.getDataHandler().getDmMessagesConfig().getString("scoreboard.statusMinimumStartingIn", "&7Minimum: starting in &f{countdown}&7s").replace("{countdown}", String.valueOf(rem));
                        map.put("{status}", statusMsg);
                    }
                    map.put("{countdown}", String.valueOf(rem));
                } else {
                    String statusMsg = pl.getDataHandler().getDmMessagesConfig().getString("scoreboard.statusWaiting", "&7Waiting for players");
                    map.put("{status}", statusMsg);
                    map.put("{countdown}", "-");
                }
                showDm(p, "scoreboard.lobby", map);
            }

            for (Player p : dm.getArenaPlayers()) {
                if (p == null || !p.isOnline()) continue;
                if (dm.getArenaStatus() == DmGameManager.DmArenaMode.FIGHT) {
                    Map<String, String> map = buildDmGameMap(dm, arenaName, queueName, p);
                    showDm(p, "scoreboard.game", map);
                }
            }
            if (dm.getArenaStatus() == DmGameManager.DmArenaMode.WINNING_PHASE) {
                Player winner = dm.getArenaPlayers().isEmpty() ? null : dm.getArenaPlayers().get(0);
                for (Player p : dm.getArenaPlayers()) {
                    if (p != null && p.isOnline()) showDmEndgame(dm, arenaName, queueName, p, winner);
                }
                for (Player p : dm.getSpectators()) {
                    if (p != null && p.isOnline()) showDmEndgame(dm, arenaName, queueName, p, winner);
                }
            }
        }
    }

    private String resolveQueueDisplayName(String gamemode, String queueKey) {
        if (queueKey == null || queueKey.isEmpty()) return "-";
        String display = pl.getDataHandler().getQueueScoreboardName(gamemode, queueKey);
        return display != null ? display : queueKey;
    }

    private Map<String, String> buildDmGameMap(DmGameManager dm, String arenaName, String queueName, Player viewer) {
        Map<String, String> map = new HashMap<>();
        map.put("{arena_name}", arenaName);
        map.put("{queue_name}", queueName);
        map.put("{players_left}", String.valueOf(dm.getArenaPlayers().size()));
        map.put("{your_kills}", String.valueOf(dm.getKills(viewer)));
        map.put("{time_left}", String.valueOf(dm.getTimeOut()));
        List<Map.Entry<String, Integer>> top = dm.getTopKillers(3);
        for (int i = 0; i < 3; i++) {
            if (i < top.size()) {
                map.put("{top_killer_" + (i + 1) + "}", top.get(i).getKey());
                map.put("{top_kills_" + (i + 1) + "}", String.valueOf(top.get(i).getValue()));
            } else {
                map.put("{top_killer_" + (i + 1) + "}", "-");
                map.put("{top_kills_" + (i + 1) + "}", "0");
            }
        }
        return map;
    }

    private void showDmEndgame(DmGameManager dm, String arenaName, String queueName, Player p, Player winner) {
        Map<String, String> map = new HashMap<>();
        map.put("{arena_name}", arenaName);
        map.put("{queue_name}", queueName);
        map.put("{winner}", winner != null ? winner.getName() : "-");
        map.put("{your_kills}", String.valueOf(dm.getKills(p)));
        List<Map.Entry<String, Integer>> top = dm.getTopKillers(3);
        for (int i = 0; i < 3; i++) {
            if (i < top.size()) {
                map.put("{top_killer_" + (i + 1) + "}", top.get(i).getKey());
                map.put("{top_kills_" + (i + 1) + "}", String.valueOf(top.get(i).getValue()));
            } else {
                map.put("{top_killer_" + (i + 1) + "}", "-");
                map.put("{top_kills_" + (i + 1) + "}", "0");
            }
        }
        showDm(p, "scoreboard.endgame", map);
    }
}
