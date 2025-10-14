package org.shotrush.atom.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.manager.CraftingProjectManager;

import java.util.ArrayList;
import java.util.List;

public class CraftingProjectListener implements Listener {
    private final Atom plugin;
    private final CraftingProjectManager craftingProjectManager;
    
    public CraftingProjectListener(Atom plugin, CraftingProjectManager craftingProjectManager) {
        this.plugin = plugin;
        this.craftingProjectManager = craftingProjectManager;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!craftingProjectManager.isEnabled()) return;
        
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
        if (result == null) return;
        if (!shouldConvertToProject(result.getType())) return;
        
        CraftingInventory inventory = event.getInventory();
        List<ItemStack> components = new ArrayList<>();
        
        for (ItemStack item : inventory.getMatrix()) {
            if (item != null && item.getType() != Material.AIR) {
                components.add(item.clone());
            }
        }
        
        ItemStack project = craftingProjectManager.createCraftingProject(result.getType(), components);
        event.getInventory().setResult(project);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!craftingProjectManager.isEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();
        if (!shouldConvertToProject(result.getType())) return;
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendActionBar(Component.text("Crafting Project created"));
        });
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectConsume(PlayerItemConsumeEvent event) {
        if (!craftingProjectManager.isEnabled()) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (!craftingProjectManager.isCraftingProject(item)) return;
        completeConsumptionFromEvent(player, item);
    }
    
    private void completeConsumptionFromEvent(Player player, ItemStack project) {
        Material resultMaterial = craftingProjectManager.getProjectResult(project);
        
        if (resultMaterial == null) {
            player.sendActionBar(Component.text("Invalid crafting project"));
            return;
        }
        
        double successChance = craftingProjectManager.calculateSuccessChance(player, resultMaterial);
        player.sendActionBar(Component.text(String.format("%.0f%% success chance", successChance * 100)));
        
        CraftingProjectManager.CraftingResult result = craftingProjectManager.attemptCrafting(player, project);
        String actionId = craftingProjectManager.findCraftingAction(resultMaterial);
        double experience = result.isSuccess() ? 2.0 : 1.0;
        plugin.getActionManager().grantExperience(player, actionId, experience);
        
        if (result.isSuccess()) {
            player.getInventory().addItem(result.getResult());
            player.sendActionBar(Component.text("Crafted " + formatMaterialName(resultMaterial)));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.2f);
        } else {
            player.sendActionBar(Component.text("Crafting failed"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.8f);
        }
    }
    
    
    private boolean shouldConvertToProject(Material material) {
        return craftingProjectManager.getConvertibleMaterials().contains(material);
    }
    
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
            }
        }
        
        return formatted.toString().trim();
    }
    
}
