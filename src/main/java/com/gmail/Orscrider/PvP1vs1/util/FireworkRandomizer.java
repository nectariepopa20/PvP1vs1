/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.FireworkEffect$Type
 */
package com.gmail.Orscrider.PvP1vs1.util;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;

public class FireworkRandomizer {
    public static FireworkEffect.Type fireworkType(int i) {
        FireworkEffect.Type fwet = null;
        switch (i) {
            case 0: {
                fwet = FireworkEffect.Type.BALL;
                break;
            }
            case 1: {
                fwet = FireworkEffect.Type.BALL_LARGE;
                break;
            }
            case 2: {
                fwet = FireworkEffect.Type.BURST;
                break;
            }
            case 3: {
                fwet = FireworkEffect.Type.CREEPER;
                break;
            }
            case 4: {
                fwet = FireworkEffect.Type.STAR;
                break;
            }
            default: {
                fwet = FireworkEffect.Type.BALL_LARGE;
            }
        }
        return fwet;
    }

    public static Color fireworkColor(int i) {
        Color fwc = null;
        switch (i) {
            case 0: {
                fwc = Color.AQUA;
                break;
            }
            case 1: {
                fwc = Color.BLACK;
                break;
            }
            case 2: {
                fwc = Color.BLUE;
                break;
            }
            case 3: {
                fwc = Color.FUCHSIA;
                break;
            }
            case 4: {
                fwc = Color.GRAY;
                break;
            }
            case 5: {
                fwc = Color.GREEN;
                break;
            }
            case 6: {
                fwc = Color.LIME;
                break;
            }
            case 7: {
                fwc = Color.MAROON;
                break;
            }
            case 8: {
                fwc = Color.NAVY;
                break;
            }
            case 9: {
                fwc = Color.OLIVE;
                break;
            }
            case 10: {
                fwc = Color.ORANGE;
                break;
            }
            case 11: {
                fwc = Color.PURPLE;
                break;
            }
            case 12: {
                fwc = Color.RED;
                break;
            }
            case 13: {
                fwc = Color.SILVER;
                break;
            }
            case 14: {
                fwc = Color.TEAL;
                break;
            }
            case 15: {
                fwc = Color.WHITE;
                break;
            }
            case 16: {
                fwc = Color.YELLOW;
                break;
            }
            default: {
                fwc = Color.GREEN;
            }
        }
        return fwc;
    }
}

