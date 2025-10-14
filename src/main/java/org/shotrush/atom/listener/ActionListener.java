package org.shotrush.atom.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantInventory;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ActionManager;
import org.shotrush.atom.manager.ConfigManager;
import org.shotrush.atom.model.TrackedAction;
import org.shotrush.atom.model.TriggerType;

public class ActionListener implements Listener {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final ActionManager actionManager;

    public ActionListener(Atom plugin, ConfigManager configManager, ActionManager actionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.actionManager = actionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.BLOCK_BREAK && trigger.matches(material)) {
                    actionManager.grantExperience(player, action.getId(), action.getExperience());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.BLOCK_PLACE && trigger.matches(material)) {
                    actionManager.grantExperience(player, action.getId(), action.getExperience());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.ENTITY_DEATH && trigger.matches(event.getEntityType())) {
                    actionManager.grantExperience(killer, action.getId(), action.getExperience());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player)) return;
        Player player = (Player) event.getBreeder();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.ENTITY_BREED && trigger.matches(event.getEntityType())) {
                    actionManager.grantExperience(player, action.getId(), action.getExperience());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Material material = event.getRecipe().getResult().getType();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.CRAFT_ITEM && trigger.matches(material)) {
                    actionManager.grantExperience(player, action.getId(), action.getExperience());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material material = event.getItemType();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.FURNACE_EXTRACT && trigger.matches(material)) {
                    actionManager.grantExperience(player, action.getId(), action.getExperience() * event.getItemAmount());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.ENCHANT_ITEM) {
                    actionManager.grantExperience(player, action.getId(), action.getExperience());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory() instanceof MerchantInventory)) return;

        Player player = (Player) event.getWhoClicked();
        MerchantInventory merchantInventory = (MerchantInventory) event.getInventory();

        if (event.getSlot() == 2 && event.getCurrentItem() != null) {
            for (TrackedAction action : configManager.getActions().values()) {
                for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                    if (trigger.getType() == TriggerType.VILLAGER_TRADE) {
                        actionManager.grantExperience(player, action.getId(), action.getExperience());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFishCatch(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.FISH_CATCH) {
                    actionManager.grantExperience(player, action.getId(), action.getExperience());
                }
            }
        }
    }
}
