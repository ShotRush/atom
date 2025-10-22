package org.shotrush.atom.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.config.AtomConfig;
import org.shotrush.atom.detection.CraftDetection;
import org.shotrush.atom.effects.EffectManager;
import org.shotrush.atom.engine.XpEngine;
import org.shotrush.atom.features.GameplayFeatures;
import org.shotrush.atom.manager.PlayerDataManager;
import org.shotrush.atom.model.PlayerSkillData;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public final class FeatureListener implements Listener {
    
    private final AtomConfig config;
    private final PlayerDataManager dataManager;
    private final XpEngine xpEngine;
    private final EffectManager effectManager;
    private final GameplayFeatures gameplayFeatures;
    private final Random random;
    
    public FeatureListener(
        AtomConfig config,
        PlayerDataManager dataManager,
        XpEngine xpEngine,
        EffectManager effectManager,
        GameplayFeatures gameplayFeatures
    ) {
        this.config = Objects.requireNonNull(config);
        this.dataManager = Objects.requireNonNull(dataManager);
        this.xpEngine = Objects.requireNonNull(xpEngine);
        this.effectManager = Objects.requireNonNull(effectManager);
        this.gameplayFeatures = Objects.requireNonNull(gameplayFeatures);
        this.random = new Random();
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();
        
        if (isCrop(event.getBlock().getType())) {
            double dropMultiplier = effectManager.getFarmingDropRateMultiplier(player, data);
            if (dropMultiplier < 1.0 && random.nextDouble() > dropMultiplier) {
                event.setDropItems(false);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (config.enableToolReinforcement() && gameplayFeatures.isReinforced(item)) {
            if (random.nextDouble() < 0.33) {
                event.setCancelled(true);
            }
            return;
        }
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();
        String category = getToolCategory(item.getType());
        if (category != null) {
            double durabilityMultiplier = effectManager.getToolDurabilityMultiplier(player, category, data);
            if (durabilityMultiplier != 1.0) {
                event.setDamage((int) (event.getDamage() * durabilityMultiplier));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        ItemStack result = event.getRecipe().getResult();
        
        if (config.enableToolReinforcement() && CraftDetection.isStoneToolCraft(result.getType())) {
            if (CraftDetection.hasIronInIngredients(event)) {
                event.setCurrentItem(gameplayFeatures.reinforce(result));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !config.enableXpTransfer()) return;
        
        if (gameplayFeatures.isXpItem(item)) {
            Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
            if (dataOpt.isEmpty()) return;
            
            PlayerSkillData data = dataOpt.get();
            String skillId = gameplayFeatures.getSkillId(item);
            long storedXp = gameplayFeatures.getStoredXp(item);
            
            if (skillId != null && storedXp > 0) {
                xpEngine.awardXp(data, skillId, storedXp);
                effectManager.sendXpGain(player, skillId, storedXp);
                
                item.setAmount(item.getAmount() - 1);
                event.setCancelled(true);
            }
        }
    }
    
    private boolean isCrop(Material material) {
        return material == Material.WHEAT || material == Material.CARROTS || 
               material == Material.POTATOES || material == Material.BEETROOTS;
    }
    
    private String getToolCategory(Material material) {
        String name = material.name();
        if (name.contains("HOE")) return "farmer";
        if (name.contains("PICKAXE")) return "miner";
        if (name.contains("AXE") && !name.contains("PICKAXE")) return "builder";
        if (name.contains("SHOVEL")) return "builder";
        return null;
    }
}
