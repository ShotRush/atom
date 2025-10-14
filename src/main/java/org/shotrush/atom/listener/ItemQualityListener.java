package org.shotrush.atom.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ConfigManager;
import org.shotrush.atom.manager.EmergentBonusManager;
import org.shotrush.atom.model.TrackedAction;
import org.shotrush.atom.model.TriggerType;
import org.shotrush.atom.util.PDCKeys;

public class ItemQualityListener implements Listener {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final EmergentBonusManager emergentBonusManager;
    private final PDCKeys pdcKeys;

    public ItemQualityListener(Atom plugin, ConfigManager configManager, EmergentBonusManager emergentBonusManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.emergentBonusManager = emergentBonusManager;
        this.pdcKeys = plugin.getPDCKeys();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();

        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.CRAFT_ITEM) {
                    double qualityMultiplier = emergentBonusManager.getQualityMultiplier(player, action.getId());
                    double efficiency = emergentBonusManager.getSpeedMultiplier(player, action.getId());
                    
                    if (qualityMultiplier > 1.0 || efficiency > 1.0) {
                        result.editMeta(meta -> {
                            applyDurabilityBonus(meta, efficiency);
                            PersistentDataContainer pdc = meta.getPersistentDataContainer();
                            pdc.set(pdcKeys.CRAFTED_BY_SPECIALIST, PersistentDataType.STRING, player.getName());
                            pdc.set(pdcKeys.ITEM_QUALITY_TIER, PersistentDataType.DOUBLE, qualityMultiplier);
                            
                            if (result.getType().getMaxDurability() > 0) {
                                double durabilityBonus = (qualityMultiplier - 1.0) * 0.3;
                                pdc.set(pdcKeys.TOOL_DURABILITY_BONUS, PersistentDataType.DOUBLE, durabilityBonus);
                            }
                        });
                    }
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamageWithQuality(org.bukkit.event.player.PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        
        item.editMeta(meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(pdcKeys.TOOL_DURABILITY_BONUS, PersistentDataType.DOUBLE)) {
                double bonus = pdc.get(pdcKeys.TOOL_DURABILITY_BONUS, PersistentDataType.DOUBLE);
                int originalDamage = event.getDamage();
                int newDamage = (int) (originalDamage * (1.0 - bonus));
                event.setDamage(Math.max(0, newDamage));
            }
        });
    }
    
    private void applyDurabilityBonus(org.bukkit.inventory.meta.ItemMeta meta, double efficiency) {
        if (efficiency < 1.5) return;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        double durabilityBonus = calculateCraftedDurabilityBonus(efficiency);
        
        if (durabilityBonus > 0) {
            pdc.set(pdcKeys.TOOL_DURABILITY_BONUS, PersistentDataType.DOUBLE, durabilityBonus);
        }
    }
    
    private double calculateCraftedDurabilityBonus(double efficiency) {
        if (efficiency >= 3.5) return 0.75;
        if (efficiency >= 3.0) return 0.60;
        if (efficiency >= 2.5) return 0.45;
        if (efficiency >= 2.0) return 0.30;
        if (efficiency >= 1.5) return 0.15;
        return 0.0;
    }
}
