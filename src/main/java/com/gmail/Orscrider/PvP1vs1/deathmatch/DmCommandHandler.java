package com.gmail.Orscrider.PvP1vs1.deathmatch;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.persistence.DBConnectionController;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DmCommandHandler implements CommandExecutor {

    private final PvP1vs1 pl;
    private final HashMap<String, String> replacements = new HashMap<>();

    public DmCommandHandler(PvP1vs1 plugin) {
        this.pl = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("dm")) return false;
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("You are not a player!");
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0) {
            sendDmHelp(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                return handleJoin(p, args);
            case "leave":
                return handleLeave(p);
            case "arena":
                return handleArena(p, args);
            case "queue":
                return handleQueue(p, args);
            case "rjoin":
            case "randomjoin":
                return handleRandomJoin(p);
            case "info":
                return handleInfo(p, args);
            case "stats":
                return handleStats(p, args);
            case "forcestart":
                return handleForceStart(p);
            default:
                sendDmHelp(p);
                return true;
        }
    }

    private boolean handleForceStart(Player p) {
        if (!p.hasPermission("dm.forcestart")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        DmGameManager arena = null;
        for (DmGameManager a : pl.getDmArenaManager().getArenas().values()) {
            if (a.isInLobby(p)) {
                arena = a;
                break;
            }
        }
        if (arena == null) {
            pl.messageParserDm("forceStartNotInLobby", p);
            return true;
        }
        int lobbySize = arena.getLobbySize();
        if (lobbySize < 2) {
            pl.messageParserDm("forceStartNotEnoughPlayers", p);
            return true;
        }
        if (!arena.forceStartGame()) {
            pl.messageParserDm("forceStartNotEnoughPlayers", p);
            return true;
        }
        replacements.put("{ARENA}", arena.getArenaName());
        replacements.put("{COUNT}", String.valueOf(lobbySize));
        pl.sendDmMessage("forceStartSuccess", p, replacements);
        return true;
    }

    private boolean handleJoin(Player p, String[] args) {
        if (!p.hasPermission("dm.join")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length != 2) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "/dm join <arena>");
            return true;
        }
        for (DmGameManager arena : pl.getDmArenaManager().getArenas().values()) {
            if (arena.isInLobby(p)) {
                replacements.put("{ARENA}", arena.getArenaName());
                pl.sendDmMessage("alreadyInLobby", p, replacements);
                return true;
            }
            if (arena.arenaPlayersContains(p)) {
                replacements.put("{ARENA}", arena.getArenaName());
                pl.sendDmMessage("alreadyInArena", p, replacements);
                return true;
            }
        }
        String arenaName = args[1];
        if (!pl.getDmArenaManager().arenaExists(arenaName)) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("arenaDoesNotExist", p, replacements);
            return true;
        }
        DmGameManager arena = pl.getDmArenaManager().getArena(arenaName);
        if (!arena.isEnabled()) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("arenaDisabled", p, replacements);
            return true;
        }
        if (!arena.hasLobbySet()) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("arenaLobbyNotSet", p, replacements);
            return true;
        }
        if (arena.getArenaStatus() != DmGameManager.DmArenaMode.NORMAL && arena.getArenaStatus() != DmGameManager.DmArenaMode.LOBBY) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("arenaInGame", p, replacements);
            return true;
        }
        if (arena.getLobbySize() >= arena.getMaxPlayers()) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("lobbyFull", p, replacements);
            return true;
        }
        arena.joinLobby(p);
        arena.broadcastPlayerJoinedLobby(p);
        replacements.put("{ARENA}", arena.getArenaName());
        pl.sendDmMessage("joinLobby", p, replacements);
        arena.startGame();
        return true;
    }

    private boolean handleLeave(Player p) {
        if (!p.hasPermission("dm.leave")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        for (Map.Entry<String, DmGameManager> e : pl.getDmArenaManager().getArenas().entrySet()) {
            if (e.getValue().isInLobby(p)) {
                e.getValue().leaveLobby(p);
                replacements.put("{ARENA}", e.getKey());
                pl.sendDmMessage("leaveLobby", p, replacements);
                return true;
            }
        }
        pl.messageParserDm("notInLobby", p);
        return true;
    }

    private boolean handleQueue(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "/dm queue <create|delete|addarena|remarena|list|join> [args]");
            return true;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "create":
                if (!p.hasPermission("dm.queue.create")) {
                    pl.messageParserDm("insufficientPermission", p);
                    return true;
                }
                if (args.length != 3) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "/dm queue create <name>");
                    return true;
                }
                if (pl.getDataHandler().createQueue("dm", args[2])) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.GREEN + "Queue \"" + args[2] + "\" created.");
                } else {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "Queue \"" + args[2] + "\" already exists.");
                }
                return true;
            case "delete":
                if (!p.hasPermission("dm.queue.delete")) {
                    pl.messageParserDm("insufficientPermission", p);
                    return true;
                }
                if (args.length != 3) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "/dm queue delete <name>");
                    return true;
                }
                if (pl.getDataHandler().deleteQueue("dm", args[2])) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.GREEN + "Queue \"" + args[2] + "\" deleted.");
                } else {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "Queue \"" + args[2] + "\" does not exist.");
                }
                return true;
            case "addarena":
                if (!p.hasPermission("dm.queue.addarena")) {
                    pl.messageParserDm("insufficientPermission", p);
                    return true;
                }
                if (args.length != 4) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "/dm queue addarena <queue> <arena>");
                    return true;
                }
                if (!pl.getDataHandler().queueExists("dm", args[2])) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "Queue \"" + args[2] + "\" does not exist.");
                    return true;
                }
                if (!pl.getDmArenaManager().arenaExists(args[3])) {
                    replacements.put("{ARENA}", args[3]);
                    pl.sendDmMessage("arenaDoesNotExist", p, replacements);
                    return true;
                }
                if (pl.getDataHandler().addArenaToQueue("dm", args[2], args[3])) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.GREEN + "Arena \"" + args[3] + "\" added to queue \"" + args[2] + "\".");
                } else {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "Arena already in queue.");
                }
                return true;
            case "remarena":
                if (!p.hasPermission("dm.queue.remarena")) {
                    pl.messageParserDm("insufficientPermission", p);
                    return true;
                }
                if (args.length != 4) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "/dm queue remarena <queue> <arena>");
                    return true;
                }
                if (pl.getDataHandler().remArenaFromQueue("dm", args[2], args[3])) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.GREEN + "Arena \"" + args[3] + "\" removed from queue \"" + args[2] + "\".");
                } else {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "Queue or arena not found.");
                }
                return true;
            case "list":
                if (!p.hasPermission("dm.queue.list")) {
                    pl.messageParserDm("insufficientPermission", p);
                    return true;
                }
                ArrayList<String> queues = pl.getDataHandler().getQueueNames("dm");
                if (queues.isEmpty()) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.GRAY + "No queues defined.");
                    return true;
                }
                p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.GOLD + "DM Queues:");
                for (String q : queues) {
                    List<String> arenas = pl.getDataHandler().getQueueArenas("dm", q);
                    p.sendMessage(ChatColor.GREEN + "  " + q + ChatColor.GRAY + " (" + String.join(", ", arenas) + ")");
                }
                return true;
            case "join":
                if (args.length != 3) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "/dm queue join <queue>");
                    return true;
                }
                String queueName = args[2];
                if (!pl.getDataHandler().queueExists("dm", queueName)) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "Queue \"" + queueName + "\" does not exist.");
                    return true;
                }
                List<String> arenaNames = pl.getDataHandler().getQueueArenas("dm", queueName);
                ArrayList<String> joinable = new ArrayList<>();
                for (String an : arenaNames) {
                    if (!pl.getDmArenaManager().arenaExists(an)) continue;
                    DmGameManager gm = pl.getDmArenaManager().getArena(an);
                    if (!gm.isEnabled() || !gm.hasLobbySet()) continue;
                    if (gm.getArenaStatus() != DmGameManager.DmArenaMode.NORMAL && gm.getArenaStatus() != DmGameManager.DmArenaMode.LOBBY) continue;
                    if (gm.getLobbySize() >= gm.getMaxPlayers()) continue;
                    if (gm.isInLobby(p) || gm.arenaPlayersContains(p)) {
                        replacements.put("{ARENA}", an);
                        pl.sendDmMessage("alreadyInLobby", p, replacements);
                        return true;
                    }
                    joinable.add(an);
                }
                for (DmGameManager a : pl.getDmArenaManager().getArenas().values()) {
                    if (a.isInLobby(p) || a.arenaPlayersContains(p)) {
                        replacements.put("{ARENA}", a.getArenaName());
                        pl.sendDmMessage("alreadyInLobby", p, replacements);
                        return true;
                    }
                }
                if (joinable.isEmpty()) {
                    p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "No arena in queue \"" + queueName + "\" is available to join.");
                    return true;
                }
                String chosen = null;
                int maxCount = -1;
                for (String an : joinable) {
                    DmGameManager gm = pl.getDmArenaManager().getArena(an);
                    int count = gm.getLobbySize() + gm.getArenaPlayers().size();
                    if (count > maxCount) {
                        maxCount = count;
                        chosen = an;
                    }
                }
                if (maxCount == 0) {
                    chosen = joinable.get(new Random().nextInt(joinable.size()));
                }
                p.chat("/dm join " + chosen);
                return true;
            default:
                p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "/dm queue <create|delete|addarena|remarena|list|join> [args]");
                return true;
        }
    }

    private boolean handleRandomJoin(Player p) {
        if (!p.hasPermission("dm.randomJoin")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        String random = pl.getDmArenaManager().getRandomArena();
        if (random != null) {
            p.chat("/dm join " + random);
            return true;
        }
        pl.sendDmMessage("noEnabledArenas", p, replacements);
        return true;
    }

    private boolean handleStats(Player p, String[] args) {
        if (!p.hasPermission("dm.stats")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        DBConnectionController db = DBConnectionController.getInstance();
        DecimalFormat oneDigit = new DecimalFormat("#,##0.0");
        if (args.length == 1) {
            int wins = db.getPlayerDmWins(p.getUniqueId().toString());
            int kills = db.getPlayerDmKills(p.getUniqueId().toString());
            int deaths = db.getPlayerDmDeaths(p.getUniqueId().toString());
            double winRate = (wins + deaths == 0) ? 0 : (double) wins / (wins + deaths) * 100.0;
            replacements.put("{DM_WINS}", String.valueOf(wins));
            replacements.put("{DM_KILLS}", String.valueOf(kills));
            replacements.put("{DM_DEATHS}", String.valueOf(deaths));
            replacements.put("{DM_WIN_RATE}", oneDigit.format(winRate));
            pl.sendDmMessage("stats", p, replacements);
            return true;
        }
        if (args.length != 2) {
            p.sendMessage(pl.getDataHandler().getDmPrefix() + ChatColor.RED + "/dm stats [player]");
            return true;
        }
        if (!p.hasPermission("dm.stats.otherPlayers")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || !target.hasPlayedBefore()) {
            replacements.put("{NAME}", args[1]);
            pl.sendDmMessage("statsPlayerNotFound", p, replacements);
            return true;
        }
        String targetId = target.getUniqueId().toString();
        int wins = db.getPlayerDmWins(targetId);
        int kills = db.getPlayerDmKills(targetId);
        int deaths = db.getPlayerDmDeaths(targetId);
        double winRate = (wins + deaths == 0) ? 0 : (double) wins / (wins + deaths) * 100.0;
        replacements.put("{NAME}", args[1]);
        replacements.put("{DM_WINS}", String.valueOf(wins));
        replacements.put("{DM_KILLS}", String.valueOf(kills));
        replacements.put("{DM_DEATHS}", String.valueOf(deaths));
        replacements.put("{DM_WIN_RATE}", oneDigit.format(winRate));
        pl.sendDmMessage("statsOtherPlayers", p, replacements);
        return true;
    }

    private boolean handleInfo(Player p, String[] args) {
        if (!p.hasPermission("dm.info")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length != 2) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "/dm info <arena>");
            return true;
        }
        if (!pl.getDmArenaManager().arenaExists(args[1])) {
            replacements.put("{ARENA}", args[1]);
            pl.sendDmMessage("arenaDoesNotExist", p, replacements);
            return true;
        }
        DmGameManager arena = pl.getDmArenaManager().getArena(args[1]);
        int lobbySize = arena.getLobbySize();
        int max = arena.getMaxPlayers();
        String arenaPlayersStr = "Nobody";
        if (arena.getArenaStatus() == DmGameManager.DmArenaMode.FIGHT || arena.getArenaStatus() == DmGameManager.DmArenaMode.COUNTDOWN_BEFORE_FIGHT) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arena.getArenaPlayers().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arena.getArenaPlayers().get(i).getName());
            }
            arenaPlayersStr = sb.length() > 0 ? sb.toString() : "Nobody";
        }
        String lobbyPlayersStr = "Nobody";
        if (lobbySize > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arena.getLobbyPlayers().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arena.getLobbyPlayers().get(i).getName());
            }
            lobbyPlayersStr = sb.toString();
        }
        for (String line : pl.getDataHandler().getDmMessagesConfig().getStringList("info")) {
            String msg = pl.getDataHandler().getDmPrefix() + ChatColor.translateAlternateColorCodes('&', line)
                    .replace("{ENABLED}", String.valueOf(arena.isEnabled()))
                    .replace("{GAME_STATUS}", arena.getArenaStatus().name())
                    .replace("{SIZE}", String.valueOf(lobbySize))
                    .replace("{IN_LOBBY}", arena.isInLobby(p) ? "Yes" : "No")
                    .replace("{LOBBY_COUNT}", String.valueOf(lobbySize))
                    .replace("{ARENA_PLAYERS}", arenaPlayersStr)
                    .replace("{LOBBY_PLAYERS}", lobbyPlayersStr)
                    .replace("{MAX}", String.valueOf(max));
            p.sendMessage(msg);
        }
        return true;
    }

    private boolean handleArena(Player p, String[] args) {
        if (!p.hasPermission("dm.arena.list") && !p.hasPermission("dm.arena.add") && !p.hasPermission("dm.arena.del")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length < 2) {
            sendArenaHelp(p);
            return true;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "list":
                return arenaList(p);
            case "add":
                return arenaAdd(p, args);
            case "del":
            case "delete":
            case "rem":
            case "remove":
                return arenaDel(p, args);
            case "setlobby":
                return arenaSetLobby(p, args);
            case "setspawn":
                return arenaSetSpawn(p, args);
            case "setmaxplayers":
                return arenaSetMaxPlayers(p, args);
            case "setinv":
            case "setinventory":
                return arenaSetInv(p, args);
            case "enable":
                return arenaEnable(p, args);
            case "disable":
                return arenaDisable(p, args);
            default:
                sendArenaHelp(p);
                return true;
        }
    }

    private boolean arenaList(Player p) {
        if (!p.hasPermission("dm.arena.list")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        StringBuilder enabled = new StringBuilder();
        StringBuilder disabled = new StringBuilder();
        if (pl.getDmArenaManager().getEnabledArenas().isEmpty()) {
            enabled.append(ChatColor.GOLD + "None");
        } else {
            for (String name : pl.getDmArenaManager().getEnabledArenas().keySet()) {
                enabled.append(ChatColor.GOLD).append(name).append(" ");
            }
        }
        if (pl.getDmArenaManager().getDisabledArenas().isEmpty()) {
            disabled.append(ChatColor.GRAY + "None");
        } else {
            for (String name : pl.getDmArenaManager().getDisabledArenas().keySet()) {
                disabled.append(ChatColor.GRAY).append(name).append(" ");
            }
        }
        replacements.put("{ENABLED_ARENAS}", enabled.toString());
        replacements.put("{DISABLED_ARENAS}", disabled.toString());
        pl.sendDmMessage("enabledArenas", p, replacements);
        pl.sendDmMessage("disabledArenas", p, replacements);
        return true;
    }

    private boolean arenaAdd(Player p, String[] args) {
        if (!p.hasPermission("dm.arena.add")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length != 3) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "/dm arena add <arena>");
            return true;
        }
        if (pl.getDmArenaManager().addArena(args[2])) {
            replacements.put("{ARENA}", args[2]);
            pl.sendDmMessage("arenaAdded", p, replacements);
        } else {
            replacements.put("{ARENA}", args[2]);
            pl.sendDmMessage("arenaAlreadyExists", p, replacements);
        }
        return true;
    }

    private boolean arenaDel(Player p, String[] args) {
        if (!p.hasPermission("dm.arena.del")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length != 3) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "/dm arena del <arena>");
            return true;
        }
        if (pl.getDmArenaManager().arenaExists(args[2])) {
            pl.getDmArenaManager().delArena(args[2]);
            replacements.put("{ARENA}", args[2]);
            pl.sendDmMessage("arenaDeleted", p, replacements);
        } else {
            replacements.put("{ARENA}", args[2]);
            pl.sendDmMessage("arenaDoesNotExist", p, replacements);
        }
        return true;
    }

    private boolean arenaSetLobby(Player p, String[] args) {
        if (!p.hasPermission("dm.arena.setlobby")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length != 3) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "/dm arena setlobby <arena>");
            return true;
        }
        if (!pl.getDmArenaManager().arenaExists(args[2])) {
            replacements.put("{ARENA}", args[2]);
            pl.sendDmMessage("arenaDoesNotExist", p, replacements);
            return true;
        }
        pl.getDataHandler().setDmLobbyInConfig(args[2], p.getLocation());
        replacements.put("{ARENA}", args[2]);
        pl.sendDmMessage("setLobby", p, replacements);
        return true;
    }

    private boolean arenaSetSpawn(Player p, String[] args) {
        if (!p.hasPermission("dm.arena.setspawn")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length != 4) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "/dm arena setspawn <1|2|...> <arena>");
            return true;
        }
        int spawnIndex;
        try {
            spawnIndex = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "Spawn number must be an integer.");
            return true;
        }
        if (spawnIndex < 1) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "Spawn number must be at least 1.");
            return true;
        }
        String arenaName = args[3];
        if (!pl.getDmArenaManager().arenaExists(arenaName)) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("arenaDoesNotExist", p, replacements);
            return true;
        }
        int max = pl.getDmArenaManager().getArena(arenaName).getMaxPlayers();
        if (spawnIndex > max) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "Spawn index must be between 1 and maxPlayers (" + max + ").");
            return true;
        }
        pl.getDataHandler().setDmSpawnInConfig(arenaName, spawnIndex, p.getLocation());
        replacements.put("{ARENA}", arenaName);
        pl.sendDmMessage("setSpawn1", p, replacements);
        return true;
    }

    private boolean arenaSetMaxPlayers(Player p, String[] args) {
        if (!p.hasPermission("dm.arena.setmaxplayers")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length != 4) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "/dm arena setmaxplayers <arena> <number>");
            return true;
        }
        String arenaName = args[2];
        int num;
        try {
            num = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "Max players must be an integer.");
            return true;
        }
        if (num < 2) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "Max players must be at least 2.");
            return true;
        }
        if (!pl.getDmArenaManager().arenaExists(arenaName)) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("arenaDoesNotExist", p, replacements);
            return true;
        }
        pl.getDataHandler().getDmArenaConfig(arenaName).set("maxPlayers", num);
        pl.getDataHandler().saveDmArenaConfig(arenaName);
        pl.getDmArenaManager().getArena(arenaName).setEnabled(pl.getDmArenaManager().getArena(arenaName).isEnabled());
        p.sendMessage(pl.getPrefix() + ChatColor.GREEN + "Max players for " + arenaName + " set to " + num + ".");
        return true;
    }

    private boolean arenaSetInv(Player p, String[] args) {
        if (!p.hasPermission("dm.arena.setInv")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length != 3) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "/dm arena setinv <arena>");
            return true;
        }
        String arenaName = args[2];
        if (!pl.getDmArenaManager().arenaExists(arenaName)) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("arenaDoesNotExist", p, replacements);
            return true;
        }
        pl.getDataHandler().setDmItems(p.getInventory().getArmorContents(), p.getInventory().getContents(), arenaName);
        replacements.put("{ARENA}", arenaName);
        pl.sendDmMessage("invSet", p, replacements);
        return true;
    }

    private boolean arenaEnable(Player p, String[] args) {
        if (!p.hasPermission("dm.arena.enable")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length != 3) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "/dm arena enable <arena>");
            return true;
        }
        String arenaName = args[2];
        if (!pl.getDmArenaManager().arenaExists(arenaName)) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("arenaDoesNotExist", p, replacements);
            return true;
        }
        pl.getDmArenaManager().getArena(arenaName).setEnabled(true);
        pl.getDataHandler().getDmArenaConfig(arenaName).set("enabled", true);
        pl.getDataHandler().saveDmArenaConfig(arenaName);
        replacements.put("{ARENA}", arenaName);
        pl.sendDmMessage("arenaWasEnabled", p, replacements);
        return true;
    }

    private boolean arenaDisable(Player p, String[] args) {
        if (!p.hasPermission("dm.arena.disable")) {
            pl.messageParserDm("insufficientPermission", p);
            return true;
        }
        if (args.length != 3) {
            p.sendMessage(pl.getPrefix() + ChatColor.RED + "/dm arena disable <arena>");
            return true;
        }
        String arenaName = args[2];
        if (!pl.getDmArenaManager().arenaExists(arenaName)) {
            replacements.put("{ARENA}", arenaName);
            pl.sendDmMessage("arenaDoesNotExist", p, replacements);
            return true;
        }
        pl.getDmArenaManager().disableArena(arenaName);
        replacements.put("{ARENA}", arenaName);
        pl.sendDmMessage("arenaWasDisabled", p, replacements);
        return true;
    }

    private void sendDmHelp(Player p) {
        p.sendMessage(ChatColor.GOLD + "========== Deathmatch ==========");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm join <arena>");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm leave");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm queue <create|delete|addarena|remarena|list|join>");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm stats [player]");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm forcestart");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm arena ...");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm rJoin  /dm info <arena>");
    }

    private void sendArenaHelp(Player p) {
        p.sendMessage(ChatColor.GOLD + "========== DM Arena ==========");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm arena list");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm arena add <arena>");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm arena del <arena>");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm arena setlobby <arena>");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm arena setspawn <1|2|...> <arena>");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm arena setmaxplayers <arena> <number>");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm arena setinv <arena>");
        p.sendMessage(ChatColor.DARK_GREEN + "  /dm arena enable/disable <arena>");
    }
}
