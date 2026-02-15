package com.gmail.Orscrider.PvP1vs1.util;

import com.gmail.Orscrider.PvP1vs1.PvP1vs1;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for the lobby leave item (magma cream). Name and optional lore are read from messages config.
 */
public final class LobbyLeaveItem {

    private static final int LEAVE_ITEM_SLOT = 8;

    private LobbyLeaveItem() {
    }

    public static ItemStack create(PvP1vs1 plugin) {
        var messages = plugin.getDataHandler().getMessagesConfig();
        ItemStack item = new ItemStack(Material.MAGMA_CREAM, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = messages.getString("leaveItem.name", "&cLeave Lobby");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (messages.getBoolean("leaveItem.loreEnabled", false)) {
                List<String> loreList = messages.getStringList("leaveItem.lore");
                if (loreList != null && !loreList.isEmpty()) {
                    List<String> lore = new ArrayList<>();
                    for (String line : loreList) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    meta.setLore(lore);
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isLeaveItem(ItemStack item, PvP1vs1 plugin) {
        if (item == null || item.getType() != Material.MAGMA_CREAM) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String configName = plugin.getDataHandler().getMessagesConfig().getString("leaveItem.name", "&cLeave Lobby");
        String expected = ChatColor.translateAlternateColorCodes('&', configName);
        return expected.equals(meta.getDisplayName());
    }

    public static int getLeaveItemSlot() {
        return LEAVE_ITEM_SLOT;
    }
}
