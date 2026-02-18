package com.gmail.Orscrider.PvP1vs1.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Sends titles to players using Bukkit's native sendTitle API (no external dependency).
 * All timing values are in ticks (20 ticks = 1 second).
 */
public final class TitleHelper {

    private TitleHelper() {}

    /**
     * Send a title and subtitle to a player. Strings can contain & color codes.
     *
     * @param player   target player
     * @param title    main title (null or empty to hide)
     * @param subtitle subtitle (null or empty to hide)
     * @param fadeIn   ticks to fade in
     * @param stay     ticks to stay visible
     * @param fadeOut  ticks to fade out
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null || !player.isOnline()) return;
        String t = title == null ? "" : ChatColor.translateAlternateColorCodes('&', title);
        String s = subtitle == null ? "" : ChatColor.translateAlternateColorCodes('&', subtitle);
        player.sendTitle(t, s, fadeIn, stay, fadeOut);
    }
}
