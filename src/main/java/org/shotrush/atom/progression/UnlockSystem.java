package org.shotrush.atom.progression;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;

import java.util.*;


public final class UnlockSystem {
    
    private final Map<String, Set<Material>> blockUnlocks;
    private final Map<String, Set<EntityType>> mobUnlocks;
    private final Map<String, Set<Material>> craftingUnlocks;
    private final Map<String, Set<Material>> toolUnlocks;
    
    public UnlockSystem() {
        this.blockUnlocks = new HashMap<>();
        this.mobUnlocks = new HashMap<>();
        this.craftingUnlocks = new HashMap<>();
        this.toolUnlocks = new HashMap<>();
        initializeUnlocks();
    }
    
    
    private void initializeUnlocks() {
        initializeMinerUnlocks();
        initializeGuardsmanUnlocks();
        initializeFarmerUnlocks();
        initializeBlacksmithUnlocks();
        initializeBuilderUnlocks();
        initializeHealerUnlocks();
        initializeLibrarianUnlocks();
    }
    
    
    private void initializeMinerUnlocks() {
        
        addBlockUnlock("miner", Material.STONE, Material.COBBLESTONE, Material.ANDESITE, 
            Material.DIORITE, Material.GRANITE, Material.DEEPSLATE);
        
        
        addBlockUnlock("miner.stone_mining", Material.STONE, Material.COBBLESTONE, 
            Material.STONE_BRICKS, Material.SMOOTH_STONE);
        addBlockUnlock("miner.stone_mining.stone", Material.STONE, Material.SMOOTH_STONE);
        addBlockUnlock("miner.stone_mining.andesite", Material.ANDESITE, Material.POLISHED_ANDESITE);
        addBlockUnlock("miner.stone_mining.diorite", Material.DIORITE, Material.POLISHED_DIORITE);
        addBlockUnlock("miner.stone_mining.granite", Material.GRANITE, Material.POLISHED_GRANITE);
        
        
        addBlockUnlock("miner.ore_mining", Material.COAL_ORE, Material.COPPER_ORE, 
            Material.IRON_ORE, Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_COPPER_ORE);
        
        
        addBlockUnlock("miner.ore_mining.coal", Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE);
        addBlockUnlock("miner.ore_mining.copper", Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE);
        addBlockUnlock("miner.ore_mining.iron", Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE);
        addBlockUnlock("miner.ore_mining.gold", Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, 
            Material.NETHER_GOLD_ORE);
        addBlockUnlock("miner.ore_mining.diamond", Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE);
        
        
        addToolUnlock("miner", Material.WOODEN_PICKAXE, Material.STONE_PICKAXE);
        addToolUnlock("miner.ore_mining", Material.IRON_PICKAXE);
        addToolUnlock("miner.ore_mining.iron", Material.IRON_PICKAXE);
        addToolUnlock("miner.ore_mining.diamond", Material.DIAMOND_PICKAXE);
    }
    
    
    private void initializeGuardsmanUnlocks() {
        
        addMobUnlock("guardsman", EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER);
        
        
        addMobUnlock("guardsman.combat", EntityType.ZOMBIE, EntityType.SKELETON, 
            EntityType.SPIDER, EntityType.CREEPER, EntityType.ENDERMAN);
        
        
        addMobUnlock("guardsman.combat.kill_zombie", EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, 
            EntityType.HUSK, EntityType.DROWNED);
        addMobUnlock("guardsman.combat.kill_skeleton", EntityType.SKELETON, EntityType.STRAY, 
            EntityType.WITHER_SKELETON);
        addMobUnlock("guardsman.combat.kill_spider", EntityType.SPIDER, EntityType.CAVE_SPIDER);
        addMobUnlock("guardsman.combat.kill_creeper", EntityType.CREEPER);
        
        
        addToolUnlock("guardsman", Material.WOODEN_SWORD, Material.STONE_SWORD);
        addToolUnlock("guardsman.combat", Material.IRON_SWORD, Material.BOW);
        addToolUnlock("guardsman.combat.kill_skeleton", Material.CROSSBOW);
        addToolUnlock("guardsman.defense", Material.SHIELD);
        
        
        addCraftingUnlock("guardsman.defense", Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS);
        addCraftingUnlock("guardsman.defense.take_damage", Material.IRON_HELMET, Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS, Material.IRON_BOOTS);
    }
    
    
    private void initializeFarmerUnlocks() {
        
        addBlockUnlock("farmer", Material.DIRT, Material.GRASS_BLOCK, Material.FARMLAND);
        addToolUnlock("farmer", Material.WOODEN_HOE, Material.STONE_HOE);
        
        
        addBlockUnlock("farmer.crop_farming", Material.WHEAT, Material.CARROTS, Material.POTATOES);
        addToolUnlock("farmer.crop_farming", Material.IRON_HOE);
        
        
        addBlockUnlock("farmer.crop_farming.wheat", Material.WHEAT, Material.HAY_BLOCK);
        addCraftingUnlock("farmer.crop_farming.wheat", Material.BREAD, Material.CAKE);
        
        addBlockUnlock("farmer.crop_farming.carrots", Material.CARROTS);
        addCraftingUnlock("farmer.crop_farming.carrots", Material.GOLDEN_CARROT);
        
        addBlockUnlock("farmer.crop_farming.potatoes", Material.POTATOES);
        addCraftingUnlock("farmer.crop_farming.potatoes", Material.BAKED_POTATO);
        
        addBlockUnlock("farmer.crop_farming.beetroot", Material.BEETROOTS);
        addCraftingUnlock("farmer.crop_farming.beetroot", Material.BEETROOT_SOUP);
        
        
        addMobUnlock("farmer.animal_husbandry", EntityType.COW, EntityType.SHEEP, 
            EntityType.PIG, EntityType.CHICKEN);
        addMobUnlock("farmer.animal_husbandry.breed_cow", EntityType.COW);
        addMobUnlock("farmer.animal_husbandry.breed_sheep", EntityType.SHEEP);
        addMobUnlock("farmer.animal_husbandry.breed_pig", EntityType.PIG);
        addMobUnlock("farmer.animal_husbandry.breed_chicken", EntityType.CHICKEN);
        
        
        addBlockUnlock("farmer.land_management", Material.COMPOSTER, Material.BONE_MEAL);
    }
    
    
    private void initializeBlacksmithUnlocks() {
        
        addCraftingUnlock("blacksmith", Material.FURNACE, Material.ANVIL);
        
        
        addCraftingUnlock("blacksmith.tool_crafting", Material.IRON_PICKAXE, Material.IRON_AXE,
            Material.IRON_SHOVEL, Material.IRON_HOE);
        addCraftingUnlock("blacksmith.tool_crafting.pickaxes", Material.IRON_PICKAXE, 
            Material.DIAMOND_PICKAXE);
        addCraftingUnlock("blacksmith.tool_crafting.axes", Material.IRON_AXE, Material.DIAMOND_AXE);
        addCraftingUnlock("blacksmith.tool_crafting.shovels", Material.IRON_SHOVEL, 
            Material.DIAMOND_SHOVEL);
        
        
        addCraftingUnlock("blacksmith.armor_crafting", Material.IRON_HELMET, Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS, Material.IRON_BOOTS);
        addCraftingUnlock("blacksmith.armor_crafting.helmets", Material.IRON_HELMET, 
            Material.DIAMOND_HELMET);
        addCraftingUnlock("blacksmith.armor_crafting.chestplates", Material.IRON_CHESTPLATE,
            Material.DIAMOND_CHESTPLATE);
    }
    
    
    private void initializeBuilderUnlocks() {
        
        addBlockUnlock("builder", Material.DIRT, Material.COBBLESTONE, Material.OAK_PLANKS);
        
        
        addBlockUnlock("builder.basic_building", Material.DIRT, Material.COBBLESTONE, 
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS);
        addBlockUnlock("builder.basic_building.place_wood", Material.OAK_LOG, Material.SPRUCE_LOG,
            Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG);
        
        
        addBlockUnlock("builder.advanced_building", Material.STONE_BRICKS, Material.QUARTZ_BLOCK,
            Material.GLASS, Material.BLACK_STAINED_GLASS);
        addBlockUnlock("builder.advanced_building.place_stone_bricks", Material.STONE_BRICKS,
            Material.CHISELED_STONE_BRICKS, Material.CRACKED_STONE_BRICKS);
        addBlockUnlock("builder.advanced_building.place_quartz", Material.QUARTZ_BLOCK,
            Material.QUARTZ_PILLAR, Material.CHISELED_QUARTZ_BLOCK, Material.SMOOTH_QUARTZ);
    }
    
    
    private void initializeHealerUnlocks() {
        
        addCraftingUnlock("healer", Material.BREWING_STAND);
        
        
        addCraftingUnlock("healer.brewing", Material.POTION, Material.SPLASH_POTION);
        addCraftingUnlock("healer.brewing.healing_potions", Material.POTION);
        addCraftingUnlock("healer.brewing.strength_potions", Material.POTION);
        
        
        addCraftingUnlock("healer.support", Material.GOLDEN_APPLE);
    }
    
    
    private void initializeLibrarianUnlocks() {
        
        addCraftingUnlock("librarian", Material.BOOKSHELF, Material.BOOK);
        
        
        addCraftingUnlock("librarian.enchanting", Material.ENCHANTING_TABLE, Material.ANVIL);
        addBlockUnlock("librarian.enchanting", Material.ENCHANTING_TABLE, Material.BOOKSHELF);
        
        
        addCraftingUnlock("librarian.knowledge", Material.BOOK, Material.WRITABLE_BOOK);
    }
    
    
    
    private void addBlockUnlock(String skillPath, Material... materials) {
        blockUnlocks.computeIfAbsent(skillPath, k -> new HashSet<>())
            .addAll(Arrays.asList(materials));
    }
    
    private void addMobUnlock(String skillPath, EntityType... entities) {
        mobUnlocks.computeIfAbsent(skillPath, k -> new HashSet<>())
            .addAll(Arrays.asList(entities));
    }
    
    private void addCraftingUnlock(String skillPath, Material... materials) {
        craftingUnlocks.computeIfAbsent(skillPath, k -> new HashSet<>())
            .addAll(Arrays.asList(materials));
    }
    
    private void addToolUnlock(String skillPath, Material... materials) {
        toolUnlocks.computeIfAbsent(skillPath, k -> new HashSet<>())
            .addAll(Arrays.asList(materials));
    }
    
    
    
    
    public boolean canMineBlock(Player player, Material block, PlayerSkillData playerData, 
            Map<String, SkillNode> allNodes) {
        return hasUnlock(blockUnlocks, block, playerData, allNodes);
    }
    
    
    public boolean canFightMob(Player player, EntityType mob, PlayerSkillData playerData,
            Map<String, SkillNode> allNodes) {
        return hasUnlock(mobUnlocks, mob, playerData, allNodes);
    }
    
    
    public boolean canCraft(Player player, Material item, PlayerSkillData playerData,
            Map<String, SkillNode> allNodes) {
        return hasUnlock(craftingUnlocks, item, playerData, allNodes);
    }
    
    
    public boolean canUseTool(Player player, Material tool, PlayerSkillData playerData,
            Map<String, SkillNode> allNodes) {
        return hasUnlock(toolUnlocks, tool, playerData, allNodes);
    }
    
    
    private <T> boolean hasUnlock(Map<String, Set<T>> unlockMap, T item, 
            PlayerSkillData playerData, Map<String, SkillNode> allNodes) {
        
        
        for (String skillId : playerData.getAllIntrinsicXp().keySet()) {
            SkillNode node = allNodes.get(skillId);
            if (node == null) continue;
            
            
            Set<T> unlocks = unlockMap.get(skillId);
            if (unlocks != null && unlocks.contains(item)) {
                
                long xp = playerData.getAllIntrinsicXp().get(skillId);
                double ratio = (double) xp / node.maxXp();
                
                
                if (ratio >= 0.1) {
                    return true;
                }
            }
            
            
            for (SkillNode ancestor : node.ancestors()) {
                Set<T> ancestorUnlocks = unlockMap.get(ancestor.id());
                if (ancestorUnlocks != null && ancestorUnlocks.contains(item)) {
                    long ancestorXp = playerData.getAllIntrinsicXp().getOrDefault(ancestor.id(), 0L);
                    double ancestorRatio = (double) ancestorXp / ancestor.maxXp();
                    if (ancestorRatio >= 0.1) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    
    public Set<Material> getUnlockedBlocks(PlayerSkillData playerData, Map<String, SkillNode> allNodes) {
        return getUnlockedItems(blockUnlocks, playerData, allNodes);
    }
    
    
    public Set<EntityType> getUnlockedMobs(PlayerSkillData playerData, Map<String, SkillNode> allNodes) {
        return getUnlockedItems(mobUnlocks, playerData, allNodes);
    }
    
    
    private <T> Set<T> getUnlockedItems(Map<String, Set<T>> unlockMap, 
            PlayerSkillData playerData, Map<String, SkillNode> allNodes) {
        
        Set<T> unlocked = new HashSet<>();
        
        for (String skillId : playerData.getAllIntrinsicXp().keySet()) {
            SkillNode node = allNodes.get(skillId);
            if (node == null) continue;
            
            long xp = playerData.getAllIntrinsicXp().get(skillId);
            double ratio = (double) xp / node.maxXp();
            
            if (ratio >= 0.1) {
                Set<T> skillUnlocks = unlockMap.get(skillId);
                if (skillUnlocks != null) {
                    unlocked.addAll(skillUnlocks);
                }
            }
        }
        
        return unlocked;
    }
}
