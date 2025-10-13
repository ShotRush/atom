package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.SkillType;

public class CraftingHandler extends SkillHandler implements Listener {
    
    public CraftingHandler(Atom plugin) {
        super(plugin);
    }
    
    @Override
    public SkillType getSkillType() {
        return SkillType.CRAFTING;
    }
    
    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!plugin.getSkillConfig().getConfig(SkillType.CRAFTING).enabled) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        ItemStack result = event.getRecipe().getResult();
        String itemKey = normalizeKey(result.getType().name());
        
        if (!consumeHunger(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou're too hungry to craft!");
            return;
        }
        
        double successRoll = random.nextDouble();
        double successRate = plugin.getSkillManager().getSuccessRate(
            player.getUniqueId(),
            getSkillType(),
            itemKey,
            plugin.getSkillConfig().getConfig(SkillType.CRAFTING).baseSuccessRate,
            plugin.getSkillConfig().getConfig(SkillType.CRAFTING).maxSuccessRate,
            plugin.getSkillConfig().getConfig(SkillType.CRAFTING).experiencePerLevel
        );
        
        if (successRoll >= successRate) {
            double failType = random.nextDouble();
            
            if (failType < 0.33) {
                consumeCraftingMaterials(event);
                event.setCancelled(true);
                player.sendMessage("§cCrafting failed! Materials were consumed.");
            } else if (failType < 0.66 && isTool(result.getType())) {
                ItemStack lowQuality = result.clone();
                modifyDurability(lowQuality, 0.3);
                event.getInventory().setResult(lowQuality);
                player.sendMessage("§6Crafted a low-quality " + result.getType().name().toLowerCase().replace("_", " ") + "!");
            } else if (failType < 0.66 && isFood(result.getType())) {
                event.getInventory().setResult(new ItemStack(Material.ROTTEN_FLESH));
                player.sendMessage("§cThe food spoiled during crafting!");
            } else {
                consumeCraftingMaterials(event);
                event.setCancelled(true);
                player.sendMessage("§cCrafting failed! Materials were consumed.");
            }
        } else {
            if (isTool(result.getType())) {
                int exp = plugin.getSkillManager().getExperience(player.getUniqueId(), getSkillType(), itemKey);
                double durabilityBonus = Math.min(exp / 500.0, 0.5);
                ItemStack highQuality = result.clone();
                modifyDurability(highQuality, 1.0 + durabilityBonus);
                event.getInventory().setResult(highQuality);
                player.sendMessage("§aSuccessfully crafted a high-quality " + result.getType().name().toLowerCase().replace("_", " ") + "!");
            } else {
                player.sendMessage("§aSuccessfully crafted " + result.getType().name().toLowerCase().replace("_", " ") + "!");
            }
        }
        
        grantExperience(player, itemKey);
    }
    
    private void modifyDurability(ItemStack item, double multiplier) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int maxDurability = item.getType().getMaxDurability();
            int newMaxDurability = (int) (maxDurability * multiplier);
            int damage = maxDurability - newMaxDurability;
            damageable.setDamage(Math.max(0, damage));
            item.setItemMeta(meta);
        }
    }
    
    private boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || 
               name.endsWith("_AXE") || name.endsWith("_SHOVEL") || 
               name.endsWith("_HOE") || name.endsWith("_HELMET") ||
               name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") ||
               name.endsWith("_BOOTS");
    }
    
    private boolean isFood(Material material) {
        return material.isEdible();
    }

    private void consumeCraftingMaterials(CraftItemEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int i = 1; i < 10; i++) {
                ItemStack item = event.getInventory().getItem(i);
                if (item != null && !item.getType().isAir()) {
                    item.setAmount(item.getAmount() - 1);
                }
            }
        });
    }
}
