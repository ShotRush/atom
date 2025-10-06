package org.shotrush.atom.skill;

import org.bukkit.Material;
import org.shotrush.atom.api.SkillInfo;

public enum SkillType {
    @SkillInfo(icon = Material.DIAMOND_PICKAXE, description = "Mine ores and blocks")
    MINING,
    
    @SkillInfo(icon = Material.CRAFTING_TABLE, description = "Craft items with quality")
    CRAFTING,
    
    @SkillInfo(icon = Material.FURNACE, description = "Smelt in furnaces and campfires")
    SMELTING,
    
    @SkillInfo(icon = Material.DIAMOND_SWORD, description = "Deal damage to entities")
    COMBAT,
    
    @SkillInfo(icon = Material.FISHING_ROD, description = "Catch fish")
    FISHING,
    
    @SkillInfo(icon = Material.ENCHANTING_TABLE, description = "Enchant items")
    ENCHANTING,
    
    @SkillInfo(icon = Material.WHEAT, description = "Breed animals")
    BREEDING,
    
    @SkillInfo(icon = Material.ANVIL, description = "Repair items at anvils")
    REPAIRING,
    
    @SkillInfo(icon = Material.BREWING_STAND, description = "Brew potions")
    BREWING,
    
    @SkillInfo(icon = Material.GOLDEN_HOE, description = "Harvest crops")
    FARMING
}
