package org.shotrush.atom.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class PDCKeys {
    private final Plugin plugin;
    
    public final NamespacedKey SPECIALIZATION_LEVEL;
    public final NamespacedKey TOTAL_ACTIONS;
    public final NamespacedKey LAST_ACTION_TIME;
    public final NamespacedKey EFFICIENCY_MULTIPLIER;
    public final NamespacedKey CRAFTED_BY_SPECIALIST;
    public final NamespacedKey TOOL_DURABILITY_BONUS;
    public final NamespacedKey ITEM_QUALITY_TIER;

    public PDCKeys(Plugin plugin) {
        this.plugin = plugin;
        
        this.SPECIALIZATION_LEVEL = new NamespacedKey(plugin, "spec_level");
        this.TOTAL_ACTIONS = new NamespacedKey(plugin, "total_actions");
        this.LAST_ACTION_TIME = new NamespacedKey(plugin, "last_action");
        this.EFFICIENCY_MULTIPLIER = new NamespacedKey(plugin, "efficiency");
        this.CRAFTED_BY_SPECIALIST = new NamespacedKey(plugin, "crafted_by");
        this.TOOL_DURABILITY_BONUS = new NamespacedKey(plugin, "durability_bonus");
        this.ITEM_QUALITY_TIER = new NamespacedKey(plugin, "quality_tier");
    }
}
