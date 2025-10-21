package org.shotrush.atom.config;

import java.util.ArrayList;
import java.util.List;


public final class DefaultTrees {
    
    private static AtomConfig config;
    
    public static List<TreeDefinition> createDefaultTrees(AtomConfig atomConfig) {
        config = atomConfig;
        
        if (atomConfig.enableDynamicTreeGeneration()) {
            return List.of(createDynamicMainTree());
        } else {
            return List.of(createStaticMainTree());
        }
    }
    
    private static int xp(int depth) {
        return config != null ? config.getDepthXpRequirement(depth) : (depth * 5000);
    }
    
    
    private static TreeDefinition createDynamicMainTree() {
        return new TreeDefinition(
            "main",
            1.0,
            new TreeDefinition.NodeDefinition(
                "root",
                "All Skills",
                100000,
                "ROOT",
                List.of(
                    createStaticCluster("farmer", "Farmer", "Master the art of farming and animal husbandry"),
                    createStaticCluster("guardsman", "Guardsman", "Become a skilled warrior and defender"),
                    createStaticCluster("miner", "Miner", "Extract valuable resources from the earth"),
                    createStaticCluster("healer", "Healer", "Brew potions and support your allies"),
                    createStaticCluster("blacksmith", "Blacksmith", "Forge powerful tools and armor"),
                    createStaticCluster("builder", "Builder", "Construct magnificent structures"),
                    createStaticCluster("librarian", "Librarian", "Unlock the secrets of enchantment")
                )
            )
        );
    }
    
    
    private static TreeDefinition.NodeDefinition createStaticCluster(String id, String displayName, String description) {
        return new TreeDefinition.NodeDefinition(
            id,
            displayName,
            xp(1),
            "BRANCH",
            new ArrayList<>()
        );
    }
    
    
    private static TreeDefinition createStaticMainTree() {
        return new TreeDefinition(
            "main",
            1.0,
            new TreeDefinition.NodeDefinition(
                "root",
                "All Skills",
                100000,
                "ROOT",
                List.of(
                    createFarmerBranch(),
                    createGuardsmanBranch(),
                    createMinerBranch(),
                    createHealerBranch(),
                    createBlacksmithBranch(),
                    createBuilderBranch(),
                    createLibrarianBranch()
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createFarmerBranch() {
        return new TreeDefinition.NodeDefinition(
            "farmer",
            "Farmer",
            xp(1),
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "farmer.crop_farming",
                    "Crop Farming",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("farmer.crop_farming.wheat", "Wheat Farming", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.wheat.plant", "Plant Wheat", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.wheat.harvest", "Harvest Wheat", xp(4), "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("farmer.crop_farming.carrots", "Carrot Farming", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.carrots.plant", "Plant Carrots", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.carrots.harvest", "Harvest Carrots", xp(4), "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("farmer.crop_farming.potatoes", "Potato Farming", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.potatoes.plant", "Plant Potatoes", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.potatoes.harvest", "Harvest Potatoes", xp(4), "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("farmer.crop_farming.beetroot", "Beetroot Farming", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.beetroot.plant", "Plant Beetroot", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.beetroot.harvest", "Harvest Beetroot", xp(4), "LEAF", null)
                            )
                        )
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "farmer.animal_husbandry",
                    "Animal Husbandry",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("farmer.animal_husbandry.breed_cow", "Breed Cows", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("farmer.animal_husbandry.breed_sheep", "Breed Sheep", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("farmer.animal_husbandry.breed_pig", "Breed Pigs", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("farmer.animal_husbandry.breed_chicken", "Breed Chickens", 8000, "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "farmer.land_management",
                    "Land Management",
                    15000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("farmer.land_management.till_soil", "Till Soil", 7500, "LEAF", null),
                        new TreeDefinition.NodeDefinition("farmer.land_management.use_composter", "Use Composter", 7500, "LEAF", null)
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createGuardsmanBranch() {
        return new TreeDefinition.NodeDefinition(
            "guardsman",
            "Guardsman",
            xp(1),
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "guardsman.combat",
                    "Combat",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("guardsman.combat.kill_zombie", "Slay Zombies", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("guardsman.combat.kill_zombie.melee", "Melee Combat", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("guardsman.combat.kill_zombie.ranged", "Ranged Combat", xp(4), "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("guardsman.combat.kill_skeleton", "Slay Skeletons", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("guardsman.combat.kill_skeleton.bow", "Bow Combat", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("guardsman.combat.kill_skeleton.crossbow", "Crossbow Combat", xp(4), "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("guardsman.combat.kill_spider", "Slay Spiders", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("guardsman.combat.kill_creeper", "Slay Creepers", xp(3), "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "guardsman.defense",
                    "Defense",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("guardsman.defense.take_damage", "Endure Damage", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("guardsman.defense.block_damage", "Block Damage", xp(3), "LEAF", null)
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createMinerBranch() {
        return new TreeDefinition.NodeDefinition(
            "miner",
            "Miner",
            xp(1),
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "miner.stone_mining",
                    "Stone Mining",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("miner.stone_mining.stone", "Mine Stone", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.stone_mining.andesite", "Mine Andesite", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.stone_mining.diorite", "Mine Diorite", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.stone_mining.granite", "Mine Granite", xp(3), "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "miner.ore_mining",
                    "Ore Mining",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("miner.ore_mining.coal", "Mine Coal", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.ore_mining.copper", "Mine Copper", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.ore_mining.iron", "Mine Iron", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("miner.ore_mining.iron.overworld", "Overworld Iron", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("miner.ore_mining.iron.deepslate", "Deepslate Iron", xp(4), "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("miner.ore_mining.gold", "Mine Gold", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("miner.ore_mining.gold.overworld", "Overworld Gold", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("miner.ore_mining.gold.nether", "Nether Gold", xp(4), "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("miner.ore_mining.diamond", "Mine Diamond", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("miner.ore_mining.diamond.cave", "Cave Mining", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("miner.ore_mining.diamond.strip", "Strip Mining", xp(4), "LEAF", null)
                            )
                        )
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createHealerBranch() {
        return new TreeDefinition.NodeDefinition(
            "healer",
            "Healer",
            xp(1),
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "healer.brewing",
                    "Brewing",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("healer.brewing.healing_potions", "Brew Healing Potions", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("healer.brewing.strength_potions", "Brew Strength Potions", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("healer.brewing.regeneration_potions", "Brew Regeneration Potions", xp(3), "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "healer.support",
                    "Support",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("healer.support.heal_player", "Heal Players", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("healer.support.regenerate", "Regenerate Health", xp(3), "LEAF", null)
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createBlacksmithBranch() {
        return new TreeDefinition.NodeDefinition(
            "blacksmith",
            "Blacksmith",
            xp(1),
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "blacksmith.tool_crafting",
                    "Tool Crafting",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.pickaxes", "Craft Pickaxes", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.pickaxes.iron", "Iron Pickaxes", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.pickaxes.diamond", "Diamond Pickaxes", xp(4), "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.axes", "Craft Axes", xp(3), "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.axes.iron", "Iron Axes", xp(4), "LEAF", null),
                                new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.axes.diamond", "Diamond Axes", xp(4), "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.shovels", "Craft Shovels", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.hoes", "Craft Hoes", xp(3), "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "blacksmith.armor_crafting",
                    "Armor Crafting",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("blacksmith.armor_crafting.helmets", "Craft Helmets", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("blacksmith.armor_crafting.chestplates", "Craft Chestplates", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("blacksmith.armor_crafting.leggings", "Craft Leggings", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("blacksmith.armor_crafting.boots", "Craft Boots", xp(3), "LEAF", null)
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createBuilderBranch() {
        return new TreeDefinition.NodeDefinition(
            "builder",
            "Builder",
            xp(1),
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "builder.basic_building",
                    "Basic Building",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("builder.basic_building.place_dirt", "Place Dirt", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("builder.basic_building.place_wood", "Place Wood", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("builder.basic_building.place_cobblestone", "Place Cobblestone", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("builder.basic_building.place_planks", "Place Planks", xp(3), "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "builder.advanced_building",
                    "Advanced Building",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("builder.advanced_building.place_stone_bricks", "Place Stone Bricks", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("builder.advanced_building.place_quartz", "Place Quartz", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("builder.advanced_building.place_glass", "Place Glass", xp(3), "LEAF", null)
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createLibrarianBranch() {
        return new TreeDefinition.NodeDefinition(
            "librarian",
            "Librarian",
            xp(1),
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "librarian.enchanting",
                    "Enchanting",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("librarian.enchanting.enchant_tool", "Enchant Tools", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("librarian.enchanting.enchant_armor", "Enchant Armor", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("librarian.enchanting.enchant_weapon", "Enchant Weapons", xp(3), "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "librarian.knowledge",
                    "Knowledge",
                    xp(2),
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("librarian.knowledge.craft_bookshelf", "Craft Bookshelves", xp(3), "LEAF", null),
                        new TreeDefinition.NodeDefinition("librarian.knowledge.write_book", "Write Books", xp(3), "LEAF", null)
                    )
                )
            )
        );
    }
}
