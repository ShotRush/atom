package org.shotrush.atom.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ActionManager;
import org.shotrush.atom.manager.ConfigManager;
import org.shotrush.atom.model.Restriction;
import org.shotrush.atom.model.TriggerType;

public class RestrictionListener implements Listener {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final ActionManager actionManager;

    public RestrictionListener(Atom plugin, ConfigManager configManager, ActionManager actionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.actionManager = actionManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakRestriction(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        for (Restriction restriction : configManager.getRestrictions().values()) {
            for (Restriction.RestrictionBlock block : restriction.getBlocks()) {
                if (block.getType() == TriggerType.BLOCK_BREAK && block.matches(material)) {
                    if (!actionManager.meetsRequirements(player, restriction.getActionRequirements())) {
                        event.setCancelled(true);
                        Component message = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(restriction.getDenyMessage());
                        player.sendActionBar(message);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftRestriction(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Material material = event.getRecipe().getResult().getType();

        for (Restriction restriction : configManager.getRestrictions().values()) {
            for (Restriction.RestrictionBlock block : restriction.getBlocks()) {
                if (block.getType() == TriggerType.CRAFT_ITEM && block.matches(material)) {
                    if (!actionManager.meetsRequirements(player, restriction.getActionRequirements())) {
                        event.setCancelled(true);
                        Component message = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(restriction.getDenyMessage());
                        player.sendActionBar(message);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmithingRestriction(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory().getType() != InventoryType.SMITHING) return;
        if (event.getSlot() != 3) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getCurrentItem();
        if (result == null) return;

        Material material = result.getType();

        for (Restriction restriction : configManager.getRestrictions().values()) {
            for (Restriction.RestrictionBlock block : restriction.getBlocks()) {
                if (block.getType() == TriggerType.SMITHING && block.matches(material)) {
                    if (!actionManager.meetsRequirements(player, restriction.getActionRequirements())) {
                        event.setCancelled(true);
                        Component message = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(restriction.getDenyMessage());
                        player.sendActionBar(message);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageRestriction(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        Material material = item.getType();

        for (Restriction restriction : configManager.getRestrictions().values()) {
            for (Restriction.RestrictionBlock block : restriction.getBlocks()) {
                if (block.getType() == TriggerType.ENTITY_DAMAGE && block.matchesItem(material)) {
                    if (!actionManager.meetsRequirements(player, restriction.getActionRequirements())) {
                        event.setCancelled(true);
                        Component message = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(restriction.getDenyMessage());
                        player.sendActionBar(message);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchantRestriction(EnchantItemEvent event) {
        Player player = event.getEnchanter();

        for (Restriction restriction : configManager.getRestrictions().values()) {
            for (Restriction.RestrictionBlock block : restriction.getBlocks()) {
                if (block.getType() == TriggerType.ENCHANT_ITEM) {
                    if (!actionManager.meetsRequirements(player, restriction.getActionRequirements())) {
                        if (restriction.isAllowFirstTime() && event.getExpLevelCost() <= restriction.getFirstTimeMaxLevel()) {
                            continue;
                        }
                        event.setCancelled(true);
                        Component message = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(restriction.getDenyMessage());
                        player.sendActionBar(message);
                        return;
                    }
                }
            }
        }
    }
}
