package org.shotrush.atom.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.ConfigManager;
import org.shotrush.atom.manager.EmergentBonusManager;
import org.shotrush.atom.model.TrackedAction;
import org.shotrush.atom.model.TriggerType;

import java.util.*;

public class CraftingEfficiencyListener implements Listener {
    private final Atom plugin;
    private final ConfigManager configManager;
    private final EmergentBonusManager emergentBonusManager;
    private final Random random;
    private final Set<UUID> autoCraftEnabled;
    
    public CraftingEfficiencyListener(Atom plugin, ConfigManager configManager, EmergentBonusManager emergentBonusManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.emergentBonusManager = emergentBonusManager;
        this.random = new Random();
        this.autoCraftEnabled = new HashSet<>();
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        String actionId = findCraftingAction(player);
        if (actionId == null) return;
        
        double efficiency = emergentBonusManager.getSpeedMultiplier(player, actionId);
        
        applyBonusOutput(event, player, efficiency);
        applyMaterialRefund(event, player, efficiency);
        handleAutoCraft(event, player, efficiency);
    }
    
    private void applyBonusOutput(CraftItemEvent event, Player player, double efficiency) {
        if (efficiency < 1.5) return;
        
        double bonusChance = Math.min((efficiency - 1.0) * 0.15, 0.5);
        
        if (random.nextDouble() < bonusChance) {
            ItemStack result = event.getRecipe().getResult().clone();
            int bonusAmount = calculateBonusAmount(efficiency);
            result.setAmount(bonusAmount);
            
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(result);
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            
            player.sendActionBar(Component.text("+" + bonusAmount + " bonus"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.2f);
        }
    }
    
    private int calculateBonusAmount(double efficiency) {
        if (efficiency >= 3.0) {
            return random.nextInt(3) + 1;
        } else if (efficiency >= 2.5) {
            return random.nextInt(2) + 1;
        } else {
            return 1;
        }
    }
    
    private void applyMaterialRefund(CraftItemEvent event, Player player, double efficiency) {
        if (efficiency < 2.0) return;
        
        double refundChance = Math.min((efficiency - 2.0) * 0.1, 0.3);
        
        if (random.nextDouble() < refundChance) {
            CraftingInventory craftingInv = event.getInventory();
            List<ItemStack> ingredients = new ArrayList<>();
            
            for (int i = 1; i < 10; i++) {
                ItemStack item = craftingInv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    ingredients.add(item.clone());
                }
            }
            
            if (!ingredients.isEmpty()) {
                ItemStack refundedItem = ingredients.get(random.nextInt(ingredients.size()));
                refundedItem.setAmount(1);
                
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(refundedItem);
                if (!leftover.isEmpty()) {
                    for (ItemStack item : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
                
                player.sendActionBar(Component.text("Material refunded"));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.3f, 1.5f);
            }
        }
    }
    
    private void handleAutoCraft(CraftItemEvent event, Player player, double efficiency) {
        if (efficiency < 2.5) return;
        if (!event.isShiftClick()) return;
        if (!autoCraftEnabled.contains(player.getUniqueId())) return;
        
        int maxCrafts = calculateMaxAutoCrafts(efficiency);
        Recipe recipe = event.getRecipe();
        ItemStack result = recipe.getResult();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            int crafted = 0;
            for (int i = 0; i < maxCrafts; i++) {
                if (!canCraft(player, event.getInventory())) break;
                
                ItemStack craftedItem = result.clone();
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(craftedItem);
                if (!leftover.isEmpty()) break;
                
                crafted++;
            }
            
            if (crafted > 0) {
                player.sendActionBar(Component.text("Auto-crafted " + crafted + "x"));
            }
        });
    }
    
    private int calculateMaxAutoCrafts(double efficiency) {
        if (efficiency >= 3.5) return 64;
        if (efficiency >= 3.0) return 32;
        if (efficiency >= 2.5) return 16;
        return 8;
    }
    
    private boolean canCraft(Player player, CraftingInventory craftingInv) {
        for (int i = 1; i < 10; i++) {
            ItemStack ingredient = craftingInv.getItem(i);
            if (ingredient != null && ingredient.getType() != Material.AIR) {
                if (!player.getInventory().contains(ingredient.getType(), 1)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private String findCraftingAction(Player player) {
        for (TrackedAction action : configManager.getActions().values()) {
            for (TrackedAction.ActionTrigger trigger : action.getTriggers()) {
                if (trigger.getType() == TriggerType.CRAFT_ITEM) {
                    return action.getId();
                }
            }
        }
        
        String dynamicActionId = "craft_general";
        return dynamicActionId;
    }
    
    public void toggleAutoCraft(Player player) {
        if (autoCraftEnabled.contains(player.getUniqueId())) {
            autoCraftEnabled.remove(player.getUniqueId());
            player.sendActionBar(Component.text("Auto-Craft disabled"));
        } else {
            double efficiency = emergentBonusManager.getSpeedMultiplier(player, findCraftingAction(player));
            if (efficiency < 2.5) {
                player.sendActionBar(Component.text("Need 2.5x efficiency for Auto-Craft"));
                return;
            }
            
            autoCraftEnabled.add(player.getUniqueId());
            player.sendActionBar(Component.text("Auto-Craft enabled"));
        }
    }
    
    public boolean hasAutoCraftEnabled(Player player) {
        return autoCraftEnabled.contains(player.getUniqueId());
    }
}
