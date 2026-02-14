/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.potion.PotionEffect
 */
package com.gmail.Orscrider.PvP1vs1.util;

import java.util.Collection;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class ValueContainer {
    private Location loc = null;
    private int level = 0;
    private float exp = 0.0f;
    private ItemStack[] armour = null;
    private ItemStack[] items = null;
    private GameMode gameMode = null;
    private int food = 0;
    private double health = 0.0;
    private double maxHealth = 0.0;
    private Collection<PotionEffect> potionEffects = null;

    public ValueContainer(Location loc, int level, float exp, ItemStack[] armour, ItemStack[] items, GameMode gameMode, int food, double health, double maxHealth, Collection<PotionEffect> potionEffects) {
        this.loc = loc;
        this.level = level;
        this.exp = exp;
        this.armour = armour;
        this.items = items;
        this.gameMode = gameMode;
        this.food = food;
        this.health = health;
        this.maxHealth = maxHealth;
        this.potionEffects = potionEffects;
    }

    public Location getLoc() {
        return this.loc;
    }

    public int getLevel() {
        return this.level;
    }

    public float getExp() {
        return this.exp;
    }

    public ItemStack[] getArmour() {
        return this.armour;
    }

    public ItemStack[] getItems() {
        return this.items;
    }

    public GameMode getGameMode() {
        return this.gameMode;
    }

    public int getFood() {
        return this.food;
    }

    public double getHealth() {
        return this.health;
    }

    public double getMaxHealth() {
        return this.maxHealth;
    }

    public Collection<PotionEffect> getPotionEffects() {
        return this.potionEffects;
    }
}

