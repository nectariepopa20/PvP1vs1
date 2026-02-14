/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 */
package com.gmail.Orscrider.PvP1vs1.util;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;

public class ArenaConfig {
    private FileConfiguration config;
    private File file;

    public ArenaConfig(FileConfiguration config, File file) {
        this.config = config;
        this.file = file;
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public void setConfig(FileConfiguration config) {
        this.config = config;
    }

    public File getFile() {
        return this.file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String toString() {
        return "file: " + (this.file == null ? "" : this.file.getAbsolutePath()) + ", config: " + this.config;
    }
}

