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
    private FileConfiguration messagesConfig = null;
    private File messagesConfigFile = null;
    private static String MESSAGES_CONFIG_FILE_NAME;
    private static String ARENA_CONFIG_FILE_NAME;

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

