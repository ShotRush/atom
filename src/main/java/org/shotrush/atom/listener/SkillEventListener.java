package org.shotrush.atom.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.config.AtomConfig;
import org.shotrush.atom.detection.CraftDetection;
import org.shotrush.atom.effects.EffectManager;
import org.shotrush.atom.engine.XpEngine;
import org.shotrush.atom.manager.PlayerDataManager;
import org.shotrush.atom.model.PlayerSkillData;

import java.util.Objects;
import java.util.Optional;

public final class SkillEventListener implements Listener {
    
    private final AtomConfig config;
    private final PlayerDataManager dataManager;
    private final XpEngine xpEngine;
    private final EffectManager effectManager;
    private final org.shotrush.atom.advancement.AdvancementGenerator advancementGenerator;
    private final org.shotrush.atom.tree.Trees.Registry treeRegistry;
    private final org.shotrush.atom.ml.ActionAnalyzer actionAnalyzer;
    
    public SkillEventListener(
        AtomConfig config,
        PlayerDataManager dataManager, 
        XpEngine xpEngine,
        EffectManager effectManager,
        org.shotrush.atom.advancement.AdvancementGenerator advancementGenerator,
        org.shotrush.atom.tree.Trees.Registry treeRegistry,
        org.shotrush.atom.ml.ActionAnalyzer actionAnalyzer
    ) {
        this.config = Objects.requireNonNull(config);
        this.dataManager = Objects.requireNonNull(dataManager);
        this.xpEngine = Objects.requireNonNull(xpEngine);
        this.effectManager = Objects.requireNonNull(effectManager);
        this.advancementGenerator = Objects.requireNonNull(advancementGenerator);
        this.treeRegistry = Objects.requireNonNull(treeRegistry);
        this.actionAnalyzer = Objects.requireNonNull(actionAnalyzer);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();

        String context = org.shotrush.atom.ml.ActionAnalyzer.materialToContext(type);
        org.shotrush.atom.ml.ActionAnalyzer.ActionType actionType = isOre(type) ? 
            org.shotrush.atom.ml.ActionAnalyzer.ActionType.MINE_BLOCK : 
            org.shotrush.atom.ml.ActionAnalyzer.ActionType.BREAK_BLOCK;
        
        var result = actionAnalyzer.recordAction(player.getUniqueId(), actionType, context);
        xpEngine.awardXp(data, result.skillId(), result.xpAmount());

        if (result.skillId().startsWith("miner") && config.enablePenalties()) {
            effectManager.applyMiningPenalty(player, data);
        }

        if (block.getBlockData() instanceof Ageable ageable && ageable.getAge() == ageable.getMaximumAge()) {
            var harvestResult = actionAnalyzer.recordAction(player.getUniqueId(), 
                org.shotrush.atom.ml.ActionAnalyzer.ActionType.HARVEST_CROP, context + "_harvest");
            xpEngine.awardXp(data, harvestResult.skillId(), harvestResult.xpAmount());
        }
        
        for (var tree : treeRegistry.getAllTrees()) {
            advancementGenerator.updatePlayerAdvancements(player, data, tree);
        }
    }
    
    private boolean isOre(Material type) {
        String name = type.name();
        return name.contains("_ORE") || name.equals("ANCIENT_DEBRIS");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();
        
        String context = org.shotrush.atom.ml.ActionAnalyzer.materialToContext(type);
        org.shotrush.atom.ml.ActionAnalyzer.ActionType actionType = isCrop(type) ?
            org.shotrush.atom.ml.ActionAnalyzer.ActionType.PLANT_CROP :
            org.shotrush.atom.ml.ActionAnalyzer.ActionType.PLACE_BLOCK;
        
        var result = actionAnalyzer.recordAction(player.getUniqueId(), actionType, context);
        xpEngine.awardXp(data, result.skillId(), result.xpAmount());
        
        for (var tree : treeRegistry.getAllTrees()) {
            advancementGenerator.updatePlayerAdvancements(player, data, tree);
        }
    }
    
    private boolean isCrop(Material type) {
        return type == Material.WHEAT || type == Material.CARROTS || 
               type == Material.POTATOES || type == Material.BEETROOTS;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();

        String context = org.shotrush.atom.ml.ActionAnalyzer.entityToContext(event.getEntityType());
        var result = actionAnalyzer.recordAction(player.getUniqueId(), 
            org.shotrush.atom.ml.ActionAnalyzer.ActionType.KILL_MOB, context);
        xpEngine.awardXp(data, result.skillId(), result.xpAmount());
        
        for (var tree : treeRegistry.getAllTrees()) {
            advancementGenerator.updatePlayerAdvancements(player, data, tree);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();
        
        String context = org.shotrush.atom.ml.ActionAnalyzer.entityToContext(event.getEntityType());
        var result = actionAnalyzer.recordAction(player.getUniqueId(), 
            org.shotrush.atom.ml.ActionAnalyzer.ActionType.BREED_ANIMAL, context);
        xpEngine.awardXp(data, result.skillId(), result.xpAmount());
        
        for (var tree : treeRegistry.getAllTrees()) {
            advancementGenerator.updatePlayerAdvancements(player, data, tree);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();
        ItemStack result = event.getRecipe().getResult();
        Material type = result.getType();
        
        if (!CraftDetection.canCraftSucceed(event)) return;
        
        int craftedAmount = CraftDetection.calculateCraftedAmount(event);
        if (craftedAmount <= 0) return;

        String context = org.shotrush.atom.ml.ActionAnalyzer.materialToContext(type);
        var result2 = actionAnalyzer.recordAction(player.getUniqueId(), 
            org.shotrush.atom.ml.ActionAnalyzer.ActionType.CRAFT_ITEM, context);
        xpEngine.awardXp(data, result2.skillId(), result2.xpAmount() * craftedAmount);
        
        for (var tree : treeRegistry.getAllTrees()) {
            advancementGenerator.updatePlayerAdvancements(player, data, tree);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (event.getClickedBlock() == null) return;
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();
        Material blockType = event.getClickedBlock().getType();
        
        if (!isInteractableBlock(blockType)) return;
        
        String context = org.shotrush.atom.ml.ActionAnalyzer.materialToContext(blockType);
        org.shotrush.atom.ml.ActionAnalyzer.ActionType actionType = 
            actionAnalyzer.inferActionTypeFromContext(context);
        
        var result = actionAnalyzer.recordAction(player.getUniqueId(), actionType, context);
        xpEngine.awardXp(data, result.skillId(), result.xpAmount());
        
        for (var tree : treeRegistry.getAllTrees()) {
            advancementGenerator.updatePlayerAdvancements(player, data, tree);
        }
    }
    
    private boolean isInteractableBlock(Material material) {
        String name = material.name().toLowerCase();
        return name.contains("table") || name.contains("furnace") || name.contains("anvil") ||
               name.contains("grindstone") || name.contains("stonecutter") || name.contains("loom") ||
               name.contains("enchant") || name.contains("brew") || name.contains("composter") ||
               name.contains("farmland") || name.contains("smoker") || name.contains("blast");
    }
}
