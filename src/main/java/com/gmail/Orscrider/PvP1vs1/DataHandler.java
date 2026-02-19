/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.configuration.Configuration
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.inventory.ItemStack
 */
package com.gmail.Orscrider.PvP1vs1;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import com.gmail.Orscrider.PvP1vs1.util.ArenaConfig;
import com.gmail.Orscrider.PvP1vs1.util.LogHandler;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class DataHandler {
    private File dataFolder;
    private FileConfiguration fileConfig;
    private static DataHandler dataHandler;
    private PvP1vs1 pl;
    HashMap<String, ArenaConfig> arenaConfigs = new HashMap();
    private File arenaFolder;
    private HashMap<String, ArenaConfig> dmArenaConfigs = new HashMap<>();
    private File dmArenaFolder;
    private FileConfiguration messagesConfig = null;
    private File messagesConfigFile = null;
    private static String MESSAGES_CONFIG_FILE_NAME;
    private static String ARENA_CONFIG_FILE_NAME;
    private static String DM_ARENA_CONFIG_FILE_NAME = "dm_arena.yml";
    private static String DM_MESSAGES_CONFIG_FILE_NAME = "dm_messages.yml";
    private FileConfiguration dmMessagesConfig = null;
    private File dmMessagesConfigFile = null;
    private FileConfiguration queuesConfig = null;
    private File queuesConfigFile = null;
    private FileConfiguration queueSignsConfig = null;
    private File queueSignsConfigFile = null;

    public DataHandler(File dataFolder, FileConfiguration fileConfig, PvP1vs1 pl) {
        if (dataHandler == null) {
            this.dataFolder = dataFolder;
            this.fileConfig = fileConfig;
            this.pl = pl;
            dataHandler = this;
            this.loadData();
        }
    }

    public static DataHandler getInstance() {
        if (dataHandler != null) {
            return dataHandler;
        }
        LogHandler.warning("DataHandler has not been initialized!");
        return null;
    }

    private void loadData() {
        this.getConfig().options().copyDefaults(true);
        this.reloadMessagesConfig();
        this.arenaFolder = new File(this.dataFolder, "/arenas/");
        if (!this.arenaFolder.exists() || !this.arenaFolder.isDirectory()) {
            this.arenaFolder.mkdirs();
        }
        for (String arenaName : this.arenaFolder.list()) {
            if (!arenaName.endsWith(".yml")) continue;
            arenaName = arenaName.replace(".yml", "");
            this.addArenaConfig(arenaName);
        }
        this.dmArenaFolder = new File(this.dataFolder, "dm_arenas/");
        if (!this.dmArenaFolder.exists() || !this.dmArenaFolder.isDirectory()) {
            this.dmArenaFolder.mkdirs();
        }
        String[] dmList = this.dmArenaFolder.list();
        if (dmList != null) {
            for (String name : dmList) {
                if (!name.endsWith(".yml")) continue;
                name = name.replace(".yml", "");
                this.addDmArenaConfig(name);
            }
        }
        this.reloadDmMessagesConfig();
        this.reloadQueuesConfig();
        this.reloadQueueSignsConfig();
    }

    public void reloadQueuesConfig() {
        if (this.queuesConfigFile == null) {
            this.queuesConfigFile = new File(this.dataFolder, "queues.yml");
        }
        this.queuesConfig = YamlConfiguration.loadConfiguration(this.queuesConfigFile);
        if (!this.queuesConfig.contains("1v1")) this.queuesConfig.createSection("1v1");
        if (!this.queuesConfig.contains("dm")) this.queuesConfig.createSection("dm");
        if (!this.queuesConfig.contains("1v1_scoreboard_names")) this.queuesConfig.createSection("1v1_scoreboard_names");
        if (!this.queuesConfig.contains("dm_scoreboard_names")) this.queuesConfig.createSection("dm_scoreboard_names");
        this.saveQueuesConfig();
    }

    /** Display name for queue on scoreboard; null = use queue name. */
    public String getQueueScoreboardName(String gamemode, String queueName) {
        String path = gamemode + "_scoreboard_names." + queueName;
        return getQueuesConfig().getString(path);
    }

    public void setQueueScoreboardName(String gamemode, String queueName, String displayName) {
        getQueuesConfig().set(gamemode + "_scoreboard_names." + queueName, displayName != null && !displayName.isEmpty() ? displayName : null);
        saveQueuesConfig();
    }

    public void saveQueuesConfig() {
        if (this.queuesConfig == null || this.queuesConfigFile == null) return;
        try {
            this.queuesConfig.save(this.queuesConfigFile);
        } catch (IOException e) {
            LogHandler.severe("Could not save queues.yml", e);
        }
    }

    public FileConfiguration getQueuesConfig() {
        if (this.queuesConfig == null) this.reloadQueuesConfig();
        return this.queuesConfig;
    }

    /** Gamemode is "1v1" or "dm". */
    public List<String> getQueueArenas(String gamemode, String queueName) {
        List<String> list = getQueuesConfig().getStringList(gamemode + "." + queueName);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public boolean queueExists(String gamemode, String queueName) {
        return getQueuesConfig().contains(gamemode + "." + queueName);
    }

    public boolean createQueue(String gamemode, String queueName) {
        if (queueExists(gamemode, queueName)) return false;
        getQueuesConfig().set(gamemode + "." + queueName, new ArrayList<String>());
        saveQueuesConfig();
        return true;
    }

    public boolean deleteQueue(String gamemode, String queueName) {
        if (!queueExists(gamemode, queueName)) return false;
        getQueuesConfig().set(gamemode + "." + queueName, null);
        getQueuesConfig().set(gamemode + "_scoreboard_names." + queueName, null);
        saveQueuesConfig();
        return true;
    }

    public boolean addArenaToQueue(String gamemode, String queueName, String arenaName) {
        if (!queueExists(gamemode, queueName)) return false;
        List<String> arenas = getQueueArenas(gamemode, queueName);
        if (arenas.contains(arenaName)) return false;
        arenas.add(arenaName);
        getQueuesConfig().set(gamemode + "." + queueName, arenas);
        saveQueuesConfig();
        return true;
    }

    public boolean remArenaFromQueue(String gamemode, String queueName, String arenaName) {
        if (!queueExists(gamemode, queueName)) return false;
        List<String> arenas = getQueueArenas(gamemode, queueName);
        if (!arenas.remove(arenaName)) return false;
        getQueuesConfig().set(gamemode + "." + queueName, arenas);
        saveQueuesConfig();
        return true;
    }

    public ArrayList<String> getQueueNames(String gamemode) {
        if (!getQueuesConfig().contains(gamemode)) return new ArrayList<>();
        return new ArrayList<>(getQueuesConfig().getConfigurationSection(gamemode).getKeys(false));
    }

    public void reloadQueueSignsConfig() {
        if (this.queueSignsConfigFile == null) {
            this.queueSignsConfigFile = new File(this.dataFolder, "queue_signs.yml");
        }
        this.queueSignsConfig = YamlConfiguration.loadConfiguration(this.queueSignsConfigFile);
        if (!this.queueSignsConfig.contains("signs")) this.queueSignsConfig.set("signs", new ArrayList<>());
        this.saveQueueSignsConfig();
    }

    public void saveQueueSignsConfig() {
        if (this.queueSignsConfig == null || this.queueSignsConfigFile == null) return;
        try {
            this.queueSignsConfig.save(this.queueSignsConfigFile);
        } catch (IOException e) {
            LogHandler.severe("Could not save queue_signs.yml", e);
        }
    }

    public FileConfiguration getQueueSignsConfig() {
        if (this.queueSignsConfig == null) this.reloadQueueSignsConfig();
        return this.queueSignsConfig;
    }

    public void reloadArenaConfigs() {
        for (String arena : this.arenaConfigs.keySet()) {
            this.reloadArenaConfig(arena);
        }
    }

    public void reloadArenaConfig(String arenaName) {
        File arenaConfigFile = this.arenaConfigs.get(arenaName).getFile();
        if (arenaConfigFile == null) {
            arenaConfigFile = new File(this.dataFolder, "/arenas/" + arenaName + ".yml");
        }
        YamlConfiguration arenaConfig = YamlConfiguration.loadConfiguration((File)arenaConfigFile);
        Reader arenaConfigReader = this.pl.getReaderForResource(ARENA_CONFIG_FILE_NAME);
        if (arenaConfigReader != null) {
            YamlConfiguration resourceArenaConfig = YamlConfiguration.loadConfiguration((Reader)arenaConfigReader);
            try {
                arenaConfigReader.close();
            }
            catch (IOException e) {
                LogHandler.severe(e.getMessage(), e);
            }
            arenaConfig.setDefaults((Configuration)resourceArenaConfig);
            arenaConfig.options().copyDefaults(true);
            this.arenaConfigs.put(arenaName, new ArenaConfig((FileConfiguration)arenaConfig, arenaConfigFile));
            this.saveArenaConfig(arenaName);
        }
    }

    public void saveArenaConfig(String arenaName) {
        FileConfiguration arenaConfig = this.getArenaConfig(arenaName);
        File arenaConfigFile = this.arenaConfigs.get(arenaName).getFile();
        if (arenaConfig == null || arenaConfigFile == null) {
            return;
        }
        try {
            arenaConfig.save(arenaConfigFile);
        }
        catch (IOException e) {
            LogHandler.severe("Could not save " + arenaName + ".yml", e);
        }
    }

    public FileConfiguration getArenaConfig(String arenaName) {
        FileConfiguration arenaConfig = null;
        if (this.arenaConfigs.containsKey(arenaName) && (arenaConfig = this.arenaConfigs.get(arenaName).getConfig()) == null) {
            this.reloadArenaConfig(arenaName);
        }
        return arenaConfig;
    }

    public boolean arenaFolderContains(String arenaName) {
        File file = new File(this.arenaFolder, arenaName + ".yml");
        return file.exists();
    }

    public void checkConfig(boolean economyAvailable) {
        for (String arenaName : this.arenaConfigs.keySet()) {
            FileConfiguration arenaConfig = this.getArenaConfig(arenaName);
            if (arenaConfig.getBoolean("prize.economy.enabled") && !economyAvailable) {
                LogHandler.warning(arenaName + ": Vault not found or no economy plugin installed! Can't use economy feature! Disabling economy feature in config...");
                arenaConfig.set("prize.economy.enabled", (Object)false);
            }
            this.saveArenaConfig(arenaName);
        }
    }

    public void reloadMessagesConfig() {
        if (this.messagesConfigFile == null) {
            this.messagesConfigFile = new File(this.dataFolder, "messages.yml");
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration((File)this.messagesConfigFile);
        Reader messagesConfigReader = this.pl.getReaderForResource(MESSAGES_CONFIG_FILE_NAME);
        if (messagesConfigReader != null) {
            YamlConfiguration resourceMessagesConfig = YamlConfiguration.loadConfiguration((Reader)messagesConfigReader);
            try {
                messagesConfigReader.close();
            }
            catch (IOException e) {
                LogHandler.severe(e.getMessage());
            }
            this.messagesConfig.setDefaults((Configuration)resourceMessagesConfig);
            this.messagesConfig.options().copyDefaults(true);
            this.saveMessagesConfig();
        }
    }

    public void saveMessagesConfig() {
        if (this.messagesConfig == null || this.messagesConfigFile == null) {
            return;
        }
        try {
            this.messagesConfig.save(this.messagesConfigFile);
        }
        catch (IOException e) {
            LogHandler.severe("Could not save messages.yml", e);
        }
    }

    public FileConfiguration getMessagesConfig() {
        if (this.messagesConfig == null) {
            this.reloadMessagesConfig();
        }
        return this.messagesConfig;
    }

    public void reloadDmMessagesConfig() {
        if (this.dmMessagesConfigFile == null) {
            this.dmMessagesConfigFile = new File(this.dataFolder, "dm_messages.yml");
        }
        this.dmMessagesConfig = YamlConfiguration.loadConfiguration(this.dmMessagesConfigFile);
        Reader r = this.pl.getReaderForResource(DM_MESSAGES_CONFIG_FILE_NAME);
        if (r != null) {
            YamlConfiguration defaultCfg = YamlConfiguration.loadConfiguration(r);
            try { r.close(); } catch (IOException e) { LogHandler.severe(e.getMessage()); }
            this.dmMessagesConfig.setDefaults((Configuration) defaultCfg);
            this.dmMessagesConfig.options().copyDefaults(true);
            this.saveDmMessagesConfig();
        }
    }

    public void saveDmMessagesConfig() {
        if (this.dmMessagesConfig == null || this.dmMessagesConfigFile == null) return;
        try {
            this.dmMessagesConfig.save(this.dmMessagesConfigFile);
        } catch (IOException e) {
            LogHandler.severe("Could not save dm_messages.yml", e);
        }
    }

    public FileConfiguration getDmMessagesConfig() {
        if (this.dmMessagesConfig == null) this.reloadDmMessagesConfig();
        return this.dmMessagesConfig;
    }

    public String getDmPrefix() {
        return ChatColor.translateAlternateColorCodes('&', getDmMessagesConfig().getString("prefix", "&8[&4DM&8] &r"));
    }

    public void addArenaConfig(String arenaName) {
        this.arenaConfigs.put(arenaName, new ArenaConfig(null, null));
        this.reloadArenaConfig(arenaName);
    }

    public void delArenaConfig(String arenaName) {
        File arenaFile = new File(this.arenaFolder, arenaName + ".yml");
        if (arenaFile.exists()) {
            arenaFile.delete();
        }
        this.arenaConfigs.remove(arenaName);
    }

    public ArrayList<String> getArenaNames() {
        return new ArrayList<String>(this.arenaConfigs.keySet());
    }

    public void setItems(ItemStack[] items, String arenaName, String configPath) {
        this.getArenaConfig(arenaName).set(configPath, (Object)items);
        this.saveArenaConfig(arenaName);
        this.reloadArenaConfig(arenaName);
    }

    public void setItemsWithoutAir(ItemStack[] items, String arenaName, String configPath) {
        ArrayList<ItemStack> itemList = new ArrayList<ItemStack>();
        for (ItemStack is : items) {
            if (is == null) continue;
            itemList.add(is);
        }
        this.setItems(itemList.toArray(new ItemStack[0]), arenaName, configPath);
    }

    public ItemStack[] getItems(String arenaName, String configPath) {
        List<?> items = this.getArenaConfig(arenaName).getList(configPath);
        if (items == null) return new ItemStack[0];
        return (ItemStack[]) items.toArray(new ItemStack[0]);
    }

    public void setSpawnInConfig(String arenaName, int spawn, Location loc) {
        this.getArenaConfig(arenaName).set("spawn" + spawn + ".world", (Object)loc.getWorld().getName());
        this.getArenaConfig(arenaName).set("spawn" + spawn + ".x", (Object)loc.getX());
        this.getArenaConfig(arenaName).set("spawn" + spawn + ".y", (Object)loc.getY());
        this.getArenaConfig(arenaName).set("spawn" + spawn + ".z", (Object)loc.getZ());
        this.getArenaConfig(arenaName).set("spawn" + spawn + ".yaw", (Object)Float.valueOf(loc.getYaw()));
        this.getArenaConfig(arenaName).set("spawn" + spawn + ".pitch", (Object)Float.valueOf(loc.getPitch()));
        this.saveArenaConfig(arenaName);
    }

    public void setLobbyInConfig(String arenaName, Location loc) {
        this.getArenaConfig(arenaName).set("lobby.world", (Object)loc.getWorld().getName());
        this.getArenaConfig(arenaName).set("lobby.x", (Object)loc.getX());
        this.getArenaConfig(arenaName).set("lobby.y", (Object)loc.getY());
        this.getArenaConfig(arenaName).set("lobby.z", (Object)loc.getZ());
        this.getArenaConfig(arenaName).set("lobby.yaw", (Object)Float.valueOf(loc.getYaw()));
        this.getArenaConfig(arenaName).set("lobby.pitch", (Object)Float.valueOf(loc.getPitch()));
        this.saveArenaConfig(arenaName);
    }

    public ArrayList<String> getDmArenaNames() {
        return new ArrayList<>(this.dmArenaConfigs.keySet());
    }

    public FileConfiguration getDmArenaConfig(String arenaName) {
        FileConfiguration cfg = null;
        if (this.dmArenaConfigs.containsKey(arenaName) && (cfg = this.dmArenaConfigs.get(arenaName).getConfig()) == null) {
            this.reloadDmArenaConfig(arenaName);
        }
        return this.dmArenaConfigs.containsKey(arenaName) ? this.dmArenaConfigs.get(arenaName).getConfig() : null;
    }

    public void addDmArenaConfig(String arenaName) {
        this.dmArenaConfigs.put(arenaName, new ArenaConfig(null, null));
        this.reloadDmArenaConfig(arenaName);
    }

    public void delDmArenaConfig(String arenaName) {
        File f = new File(this.dmArenaFolder, arenaName + ".yml");
        if (f.exists()) f.delete();
        this.dmArenaConfigs.remove(arenaName);
    }

    public void saveDmArenaConfig(String arenaName) {
        if (!this.dmArenaConfigs.containsKey(arenaName)) return;
        FileConfiguration cfg = this.dmArenaConfigs.get(arenaName).getConfig();
        File file = this.dmArenaConfigs.get(arenaName).getFile();
        if (cfg == null || file == null) return;
        try {
            cfg.save(file);
        } catch (IOException e) {
            LogHandler.severe("Could not save DM arena " + arenaName + ".yml", e);
        }
    }

    public void reloadDmArenaConfig(String arenaName) {
        File file = this.dmArenaConfigs.containsKey(arenaName) ? this.dmArenaConfigs.get(arenaName).getFile() : null;
        if (file == null) file = new File(this.dmArenaFolder, arenaName + ".yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        Reader r = this.pl.getReaderForResource(DM_ARENA_CONFIG_FILE_NAME);
        if (r != null) {
            YamlConfiguration defaultCfg = YamlConfiguration.loadConfiguration(r);
            try { r.close(); } catch (IOException e) { LogHandler.severe(e.getMessage(), e); }
            cfg.setDefaults((Configuration) defaultCfg);
            cfg.options().copyDefaults(true);
            this.dmArenaConfigs.put(arenaName, new ArenaConfig(cfg, file));
            this.saveDmArenaConfig(arenaName);
        }
    }

    public void setDmLobbyInConfig(String arenaName, Location loc) {
        this.getDmArenaConfig(arenaName).set("lobby.world", loc.getWorld().getName());
        this.getDmArenaConfig(arenaName).set("lobby.x", loc.getX());
        this.getDmArenaConfig(arenaName).set("lobby.y", loc.getY());
        this.getDmArenaConfig(arenaName).set("lobby.z", loc.getZ());
        this.getDmArenaConfig(arenaName).set("lobby.yaw", (float) loc.getYaw());
        this.getDmArenaConfig(arenaName).set("lobby.pitch", (float) loc.getPitch());
        this.saveDmArenaConfig(arenaName);
    }

    public void setDmSpawnInConfig(String arenaName, int spawnIndex, Location loc) {
        String path = "spawn" + spawnIndex + ".";
        this.getDmArenaConfig(arenaName).set(path + "world", loc.getWorld().getName());
        this.getDmArenaConfig(arenaName).set(path + "x", loc.getX());
        this.getDmArenaConfig(arenaName).set(path + "y", loc.getY());
        this.getDmArenaConfig(arenaName).set(path + "z", loc.getZ());
        this.getDmArenaConfig(arenaName).set(path + "yaw", (float) loc.getYaw());
        this.getDmArenaConfig(arenaName).set(path + "pitch", (float) loc.getPitch());
        this.saveDmArenaConfig(arenaName);
    }

    public void setDmItems(ItemStack[] armor, ItemStack[] contents, String arenaName) {
        this.getDmArenaConfig(arenaName).set("inventory.armor", armor);
        this.getDmArenaConfig(arenaName).set("inventory.items", contents);
        this.saveDmArenaConfig(arenaName);
        this.reloadDmArenaConfig(arenaName);
    }

    public ItemStack[] getDmItems(String arenaName, String configPath) {
        List<?> list = this.getDmArenaConfig(arenaName).getList(configPath);
        if (list == null) return new ItemStack[0];
        return (ItemStack[]) list.toArray(new ItemStack[0]);
    }

    public File getDmArenaFolder() {
        return this.dmArenaFolder;
    }

    public String getDatabaseType() {
        return this.getConfig().getString("database.type");
    }

    public String getDatabaseHost() {
        return this.getConfig().getString("database.host");
    }

    public String getDatabasePort() {
        return this.getConfig().getString("database.port");
    }

    public String getDatabaseUser() {
        return this.getConfig().getString("database.user");
    }

    public String getDatabasePassword() {
        return this.getConfig().getString("database.password");
    }

    public String getDatabaseName() {
        return this.getConfig().getString("database.database");
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.getConfig().getString("prefix"));
    }

    public File getArenaFolder() {
        return this.arenaFolder;
    }

    private FileConfiguration getConfig() {
        return this.fileConfig;
    }

    public File getDataFolder() {
        return this.dataFolder;
    }

    static {
        MESSAGES_CONFIG_FILE_NAME = "messages.yml";
        ARENA_CONFIG_FILE_NAME = "arena.yml";
    }
}

