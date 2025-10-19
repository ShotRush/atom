package org.shotrush.atom.config;

import java.util.List;

public final class DefaultTrees {
    
    public static List<TreeDefinition> createDefaultTrees() {
        return List.of(createMainTree());
    }
    
    private static TreeDefinition createMainTree() {
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
            50000,
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "farmer.crop_farming",
                    "Crop Farming",
                    25000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("farmer.crop_farming.wheat", "Wheat Farming", 10000, "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.wheat.plant", "Plant Wheat", 5000, "LEAF", null),
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.wheat.harvest", "Harvest Wheat", 5000, "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("farmer.crop_farming.carrots", "Carrot Farming", 10000, "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.carrots.plant", "Plant Carrots", 5000, "LEAF", null),
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.carrots.harvest", "Harvest Carrots", 5000, "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("farmer.crop_farming.potatoes", "Potato Farming", 10000, "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.potatoes.plant", "Plant Potatoes", 5000, "LEAF", null),
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.potatoes.harvest", "Harvest Potatoes", 5000, "LEAF", null)
                            )
                        ),
                        new TreeDefinition.NodeDefinition("farmer.crop_farming.beetroot", "Beetroot Farming", 10000, "BRANCH",
                            List.of(
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.beetroot.plant", "Plant Beetroot", 5000, "LEAF", null),
                                new TreeDefinition.NodeDefinition("farmer.crop_farming.beetroot.harvest", "Harvest Beetroot", 5000, "LEAF", null)
                            )
                        )
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "farmer.animal_husbandry",
                    "Animal Husbandry",
                    25000,
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
            50000,
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "guardsman.combat",
                    "Combat",
                    30000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("guardsman.combat.kill_zombie", "Slay Zombies", 10000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("guardsman.combat.kill_skeleton", "Slay Skeletons", 10000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("guardsman.combat.kill_spider", "Slay Spiders", 10000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("guardsman.combat.kill_creeper", "Slay Creepers", 10000, "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "guardsman.defense",
                    "Defense",
                    20000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("guardsman.defense.take_damage", "Endure Damage", 10000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("guardsman.defense.block_damage", "Block Damage", 10000, "LEAF", null)
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createMinerBranch() {
        return new TreeDefinition.NodeDefinition(
            "miner",
            "Miner",
            50000,
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "miner.stone_mining",
                    "Stone Mining",
                    20000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("miner.stone_mining.stone", "Mine Stone", 5000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.stone_mining.andesite", "Mine Andesite", 5000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.stone_mining.diorite", "Mine Diorite", 5000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.stone_mining.granite", "Mine Granite", 5000, "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "miner.ore_mining",
                    "Ore Mining",
                    30000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("miner.ore_mining.coal", "Mine Coal", 6000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.ore_mining.copper", "Mine Copper", 6000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.ore_mining.iron", "Mine Iron", 6000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.ore_mining.gold", "Mine Gold", 6000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("miner.ore_mining.diamond", "Mine Diamond", 6000, "LEAF", null)
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createHealerBranch() {
        return new TreeDefinition.NodeDefinition(
            "healer",
            "Healer",
            40000,
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "healer.brewing",
                    "Brewing",
                    25000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("healer.brewing.healing_potions", "Brew Healing Potions", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("healer.brewing.strength_potions", "Brew Strength Potions", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("healer.brewing.regeneration_potions", "Brew Regeneration Potions", 9000, "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "healer.support",
                    "Support",
                    15000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("healer.support.heal_player", "Heal Players", 7500, "LEAF", null),
                        new TreeDefinition.NodeDefinition("healer.support.regenerate", "Regenerate Health", 7500, "LEAF", null)
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createBlacksmithBranch() {
        return new TreeDefinition.NodeDefinition(
            "blacksmith",
            "Blacksmith",
            50000,
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "blacksmith.tool_crafting",
                    "Tool Crafting",
                    25000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.pickaxes", "Craft Pickaxes", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.axes", "Craft Axes", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.shovels", "Craft Shovels", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("blacksmith.tool_crafting.hoes", "Craft Hoes", 8000, "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "blacksmith.armor_crafting",
                    "Armor Crafting",
                    25000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("blacksmith.armor_crafting.helmets", "Craft Helmets", 6000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("blacksmith.armor_crafting.chestplates", "Craft Chestplates", 6000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("blacksmith.armor_crafting.leggings", "Craft Leggings", 6000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("blacksmith.armor_crafting.boots", "Craft Boots", 6000, "LEAF", null)
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createBuilderBranch() {
        return new TreeDefinition.NodeDefinition(
            "builder",
            "Builder",
            50000,
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "builder.basic_building",
                    "Basic Building",
                    25000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("builder.basic_building.place_dirt", "Place Dirt", 5000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("builder.basic_building.place_wood", "Place Wood", 5000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("builder.basic_building.place_cobblestone", "Place Cobblestone", 5000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("builder.basic_building.place_planks", "Place Planks", 5000, "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "builder.advanced_building",
                    "Advanced Building",
                    25000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("builder.advanced_building.place_stone_bricks", "Place Stone Bricks", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("builder.advanced_building.place_quartz", "Place Quartz", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("builder.advanced_building.place_glass", "Place Glass", 8000, "LEAF", null)
                    )
                )
            )
        );
    }
    
    private static TreeDefinition.NodeDefinition createLibrarianBranch() {
        return new TreeDefinition.NodeDefinition(
            "librarian",
            "Librarian",
            40000,
            "BRANCH",
            List.of(
                new TreeDefinition.NodeDefinition(
                    "librarian.enchanting",
                    "Enchanting",
                    25000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("librarian.enchanting.enchant_tool", "Enchant Tools", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("librarian.enchanting.enchant_armor", "Enchant Armor", 8000, "LEAF", null),
                        new TreeDefinition.NodeDefinition("librarian.enchanting.enchant_weapon", "Enchant Weapons", 9000, "LEAF", null)
                    )
                ),
                new TreeDefinition.NodeDefinition(
                    "librarian.knowledge",
                    "Knowledge",
                    15000,
                    "BRANCH",
                    List.of(
                        new TreeDefinition.NodeDefinition("librarian.knowledge.craft_bookshelf", "Craft Bookshelves", 7500, "LEAF", null),
                        new TreeDefinition.NodeDefinition("librarian.knowledge.write_book", "Write Books", 7500, "LEAF", null)
                    )
                )
            )
        );
    }
}
