package org.shotrush.atom.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
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
import org.shotrush.atom.milestone.MilestoneManager;
import org.shotrush.atom.model.PlayerSkillData;

import java.util.Objects;
import java.util.Optional;

public final class SkillEventListener implements Listener {
    
    private final AtomConfig config;
    private final PlayerDataManager dataManager;
    private final XpEngine xpEngine;
    private final EffectManager effectManager;
    private final MilestoneManager milestoneManager;
    private final org.shotrush.atom.advancement.AdvancementGenerator advancementGenerator;
    private final org.shotrush.atom.tree.SkillTreeRegistry treeRegistry;
    
    public SkillEventListener(
        AtomConfig config,
        PlayerDataManager dataManager, 
        XpEngine xpEngine,
        EffectManager effectManager,
        MilestoneManager milestoneManager,
        org.shotrush.atom.advancement.AdvancementGenerator advancementGenerator,
        org.shotrush.atom.tree.SkillTreeRegistry treeRegistry
    ) {
        this.config = Objects.requireNonNull(config);
        this.dataManager = Objects.requireNonNull(dataManager);
        this.xpEngine = Objects.requireNonNull(xpEngine);
        this.effectManager = Objects.requireNonNull(effectManager);
        this.milestoneManager = Objects.requireNonNull(milestoneManager);
        this.advancementGenerator = Objects.requireNonNull(advancementGenerator);
        this.treeRegistry = Objects.requireNonNull(treeRegistry);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();
        
        handleMining(data, type);
        handleCropHarvest(data, block);
        
        for (var tree : treeRegistry.getAllTrees()) {
            advancementGenerator.updatePlayerAdvancements(player, data, tree);
        }
    }
    
    private void handleMining(PlayerSkillData data, Material type) {
        String skillId = switch (type) {
            case STONE -> "miner.stone_mining.stone";
            case ANDESITE -> "miner.stone_mining.andesite";
            case DIORITE -> "miner.stone_mining.diorite";
            case GRANITE -> "miner.stone_mining.granite";
            case COAL_ORE, DEEPSLATE_COAL_ORE -> "miner.ore_mining.coal";
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> "miner.ore_mining.copper";
            case IRON_ORE, DEEPSLATE_IRON_ORE -> "miner.ore_mining.iron";
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> "miner.ore_mining.gold";
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> "miner.ore_mining.diamond";
            default -> null;
        };
        
        if (skillId != null) {
            int xpAmount = config.getXpRate("mining.ore");
            xpEngine.awardXp(data, skillId, xpAmount);
        }
    }
    
    private void handleCropHarvest(PlayerSkillData data, Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() != ageable.getMaximumAge()) return;
        
        String skillId = switch (block.getType()) {
            case WHEAT -> "farmer.crop_farming.wheat.harvest";
            case CARROTS -> "farmer.crop_farming.carrots.harvest";
            case POTATOES -> "farmer.crop_farming.potatoes.harvest";
            case BEETROOTS -> "farmer.crop_farming.beetroot.harvest";
            default -> null;
        };
        
        if (skillId != null) {
            int xpAmount = config.getXpRate("farming.harvest");
            xpEngine.awardXp(data, skillId, xpAmount);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();
        
        String skillId = switch (type) {
            case DIRT, GRASS_BLOCK -> "builder.basic_building.place_dirt";
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG -> 
                "builder.basic_building.place_wood";
            case COBBLESTONE -> "builder.basic_building.place_cobblestone";
            case OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, JUNGLE_PLANKS, ACACIA_PLANKS, DARK_OAK_PLANKS, MANGROVE_PLANKS, CHERRY_PLANKS -> 
                "builder.basic_building.place_planks";
            case STONE_BRICKS -> "builder.advanced_building.place_stone_bricks";
            case QUARTZ_BLOCK -> "builder.advanced_building.place_quartz";
            case GLASS -> "builder.advanced_building.place_glass";
            default -> null;
        };
        
        if (skillId != null) {
            int xpAmount = config.getXpRate("building.basic");
            xpEngine.awardXp(data, skillId, xpAmount);
        }
        
        handleCropPlanting(data, type);
        
        for (var tree : treeRegistry.getAllTrees()) {
            advancementGenerator.updatePlayerAdvancements(player, data, tree);
        }
    }
    
    private void handleCropPlanting(PlayerSkillData data, Material type) {
        String skillId = switch (type) {
            case WHEAT -> "farmer.crop_farming.wheat.plant";
            case CARROTS -> "farmer.crop_farming.carrots.plant";
            case POTATOES -> "farmer.crop_farming.potatoes.plant";
            case BEETROOTS -> "farmer.crop_farming.beetroot.plant";
            default -> null;
        };
        
        if (skillId != null) {
            int xpAmount = config.getXpRate("farming.plant");
            xpEngine.awardXp(data, skillId, xpAmount);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();
        
        String skillId = switch (event.getEntityType()) {
            case ZOMBIE -> "guardsman.combat.kill_zombie";
            case SKELETON -> "guardsman.combat.kill_skeleton";
            case SPIDER -> "guardsman.combat.kill_spider";
            case CREEPER -> "guardsman.combat.kill_creeper";
            default -> null;
        };
        
        if (skillId != null) {
            int xpAmount = config.getXpRate("combat.kill");
            xpEngine.awardXp(data, skillId, xpAmount);
        }
        
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
        
        String skillId = switch (event.getEntityType()) {
            case COW -> "farmer.animal_husbandry.breed_cows";
            case SHEEP -> "farmer.animal_husbandry.breed_sheep";
            case PIG -> "farmer.animal_husbandry.breed_pigs";
            case CHICKEN -> "farmer.animal_husbandry.breed_chickens";
            default -> null;
        };
        
        if (skillId != null) {
            int xpAmount = config.getXpRate("farming.breed");
            xpEngine.awardXp(data, skillId, xpAmount);
        }
        
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
        
        if (!CraftDetection.canCraftSucceed(event)) {
            return;
        }
        
        int craftedAmount = CraftDetection.calculateCraftedAmount(event);
        
        if (craftedAmount <= 0) {
            return;
        }
        
        String skillId = getCraftingSkillId(type);
        
        if (skillId != null) {
            String xpRateKey = skillId.contains("tool_crafting") ? "crafting.tool" : "crafting.armor";
            int xpAmount = config.getXpRate(xpRateKey) * craftedAmount;
            xpEngine.awardXp(data, skillId, xpAmount);
        }
        
        for (var tree : treeRegistry.getAllTrees()) {
            advancementGenerator.updatePlayerAdvancements(player, data, tree);
        }
    }
    
    private String getCraftingSkillId(Material type) {
        return switch (type) {
            case WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE -> 
                "blacksmith.tool_crafting.pickaxes";
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> 
                "blacksmith.tool_crafting.axes";
            case WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL -> 
                "blacksmith.tool_crafting.shovels";
            case WOODEN_HOE, STONE_HOE, IRON_HOE, GOLDEN_HOE, DIAMOND_HOE, NETHERITE_HOE -> 
                "blacksmith.tool_crafting.hoes";
            case LEATHER_HELMET, CHAINMAIL_HELMET, IRON_HELMET, GOLDEN_HELMET, DIAMOND_HELMET, NETHERITE_HELMET -> 
                "blacksmith.armor_crafting.helmets";
            case LEATHER_CHESTPLATE, CHAINMAIL_CHESTPLATE, IRON_CHESTPLATE, GOLDEN_CHESTPLATE, DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE -> 
                "blacksmith.armor_crafting.chestplates";
            case LEATHER_LEGGINGS, CHAINMAIL_LEGGINGS, IRON_LEGGINGS, GOLDEN_LEGGINGS, DIAMOND_LEGGINGS, NETHERITE_LEGGINGS -> 
                "blacksmith.armor_crafting.leggings";
            case LEATHER_BOOTS, CHAINMAIL_BOOTS, IRON_BOOTS, GOLDEN_BOOTS, DIAMOND_BOOTS, NETHERITE_BOOTS -> 
                "blacksmith.armor_crafting.boots";
            default -> null;
        };
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (event.getClickedBlock() == null) return;
        
        Optional<PlayerSkillData> dataOpt = dataManager.getCachedPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        
        PlayerSkillData data = dataOpt.get();
        Material blockType = event.getClickedBlock().getType();
        
        if (blockType == Material.FARMLAND && event.getItem() != null) {
            ItemStack item = event.getItem();
            if (item.getType() == Material.WOODEN_HOE || item.getType() == Material.STONE_HOE || 
                item.getType() == Material.IRON_HOE || item.getType() == Material.DIAMOND_HOE || 
                item.getType() == Material.NETHERITE_HOE) {
                int xpAmount = config.getXpRate("farming.till");
                xpEngine.awardXp(data, "farmer.land_management.till_soil", xpAmount);
            }
        }
        
        if (blockType == Material.COMPOSTER) {
            int xpAmount = config.getXpRate("farming.compost");
            xpEngine.awardXp(data, "farmer.land_management.use_composter", xpAmount);
        }
        
        for (var tree : treeRegistry.getAllTrees()) {
            advancementGenerator.updatePlayerAdvancements(player, data, tree);
        }
    }
}
