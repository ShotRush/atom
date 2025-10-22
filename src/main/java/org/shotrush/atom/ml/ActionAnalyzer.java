package org.shotrush.atom.ml;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ActionAnalyzer {
    
    private final org.shotrush.atom.Atom plugin;
    private final Map<String, ActionStats> actionStatistics;
    private final Map<String, Integer> actionFrequency;
    private final Map<String, Long> lastActionTime;
    private final Map<String, String> learnedCategories;
    private final Map<UUID, Map<String, Integer>> playerActionPatterns;
    
    private final Map<String, CategoryData> categories;
    private final int memoryCapacity;
    private int totalExamples;
    private final Path mlBrainPath;
    private int trainingLevel;
    
    private final Map<String, String> materialTiers;
    private final Map<String, String> toolTypes;
    private final Map<String, java.util.List<String>> materialHierarchy;
    
    private final Map<String, Map<String, Double>> qTable;
    private final Map<String, Integer> stateActionCounts;
    private final double learningRate = 0.1;
    private final double discountFactor = 0.95;
    private final double explorationRate = 0.1;
    
    public ActionAnalyzer(org.shotrush.atom.Atom plugin) {
        this.plugin = plugin;
        this.actionStatistics = new ConcurrentHashMap<>();
        this.actionFrequency = new ConcurrentHashMap<>();
        this.lastActionTime = new ConcurrentHashMap<>();
        this.learnedCategories = new ConcurrentHashMap<>();
        this.playerActionPatterns = new ConcurrentHashMap<>();
        
        this.categories = new ConcurrentHashMap<>();
        this.memoryCapacity = 1000;
        this.totalExamples = 0;
        this.trainingLevel = 1;
        
        this.materialTiers = new ConcurrentHashMap<>();
        this.toolTypes = new ConcurrentHashMap<>();
        this.materialHierarchy = new ConcurrentHashMap<>();
        
        this.qTable = new ConcurrentHashMap<>();
        this.stateActionCounts = new ConcurrentHashMap<>();
        
        buildDynamicHierarchies();
        
        this.mlBrainPath = plugin.getDataFolder().toPath().resolve("ml-brain");
        
        try {
            Files.createDirectories(mlBrainPath);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create ML brain directory: " + e.getMessage());
        }
        
        Path brainFile = mlBrainPath.resolve("classifier.dat");
        boolean modelExists = Files.exists(brainFile);
        
        plugin.getLogger().info("[ML] Checking for existing model at: " + brainFile);
        plugin.getLogger().info("[ML] Model exists: " + modelExists);
        
        if (!loadMLBrain()) {
            plugin.getLogger().info("[ML] No trained model found. Training new model...");
            initializeMLCategories();
            autoTrainUntilAccurate();
        } else {
            plugin.getLogger().info("[ML] Loaded trained model from disk (Level " + trainingLevel + ")");
            
            int quizSize = 500 * trainingLevel;
            int correct = trainWithQuiz(quizSize);
            double accuracy = (double) correct / quizSize * 100;
            
            plugin.getLogger().info(String.format("[ML] Verifying model accuracy: %.1f%%", accuracy));
            
            if (accuracy < 96.0) {
                plugin.getLogger().warning(String.format("[ML] Model accuracy below 96%% (%.1f%%), retraining...", accuracy));
                autoTrainUntilAccurate();
            } else {
                plugin.getLogger().info("[ML] Model accuracy verified. Ready to use.");
            }
        }
    }
    
    private void autoTrainUntilAccurate() {
        int currentIteration = 0;
        double targetAccuracy = 96.0;
        double currentAccuracy = 0.0;
        
        int baseQuizSize = 200 * trainingLevel;
        int baseSyntheticSize = 20000 * trainingLevel;
        
        plugin.getLogger().info(String.format("[ML] Starting training (Level %d: %d quiz, %d synthetic per iteration)", 
            trainingLevel, baseQuizSize, baseSyntheticSize));
        plugin.getLogger().warning("[ML] Server startup will block until 96% accuracy is reached!");
        
        trainWithSyntheticData(baseSyntheticSize * 2);
        
        while (currentAccuracy < targetAccuracy) {
            int syntheticExamples = baseSyntheticSize + (baseSyntheticSize * currentIteration / 2);
            trainWithSyntheticData(syntheticExamples);
            
            int quizSize = baseQuizSize;
            TrainingResult result = trainWithQuizAndGetRLAccuracy(quizSize);
            currentAccuracy = result.naiveBayesAccuracy;
            double rlAccuracy = result.rlAccuracy;
            
            currentIteration++;
            
            plugin.getLogger().info(String.format("[ML] Training iteration %d: NB=%.1f%%, RL=%.1f%% (target: %.1f%%)", 
                currentIteration, currentAccuracy, rlAccuracy, targetAccuracy));
            
            if (currentAccuracy >= targetAccuracy) {
                plugin.getLogger().info(String.format("[ML] ✓ Reached target accuracy in %d iterations", 
                    currentIteration));
                plugin.getLogger().info(String.format("[ML] Final: Naive Bayes=%.1f%%, Q-Learning=%.1f%%", 
                    currentAccuracy, rlAccuracy));
                
                if (currentAccuracy >= 99.0) {
                    trainingLevel++;
                    plugin.getLogger().info(String.format("[ML] Bonus: 99%% accuracy reached! Training level increased to %d", 
                        trainingLevel));
                }
                break;
            }
        }
        
        saveMLBrain();
        plugin.getLogger().info("[ML] Training complete. Server startup continuing...");
    }
    
    private void buildDynamicHierarchies() {
        for (org.bukkit.Material material : org.bukkit.Material.values()) {
            String name = material.name().toLowerCase();
            
            if (name.startsWith("wooden_") || name.contains("oak_") || name.contains("birch_") || 
                name.contains("spruce_") || name.contains("jungle_") || name.contains("acacia_") || 
                name.contains("dark_oak_") || name.contains("mangrove_") || name.contains("cherry_")) {
                materialTiers.put(name, "wood");
            } else if (name.startsWith("stone_") || name.contains("cobblestone")) {
                materialTiers.put(name, "stone");
            } else if (name.startsWith("iron_") || name.contains("iron")) {
                materialTiers.put(name, "iron");
            } else if (name.startsWith("gold_") || name.contains("gold")) {
                materialTiers.put(name, "gold");
            } else if (name.startsWith("diamond_") || name.contains("diamond")) {
                materialTiers.put(name, "diamond");
            } else if (name.startsWith("netherite_") || name.contains("netherite")) {
                materialTiers.put(name, "netherite");
            }
            
            if (name.endsWith("_pickaxe")) toolTypes.put(name, "pickaxe");
            else if (name.endsWith("_axe") && !name.endsWith("_pickaxe")) toolTypes.put(name, "axe");
            else if (name.endsWith("_sword")) toolTypes.put(name, "sword");
            else if (name.endsWith("_hoe")) toolTypes.put(name, "hoe");
            else if (name.endsWith("_shovel")) toolTypes.put(name, "shovel");
            
            String[] parts = name.split("_");
            materialHierarchy.put(name, java.util.Arrays.asList(parts));
        }
        
        plugin.getLogger().info("Built dynamic hierarchies: " + materialTiers.size() + " tiers, " + 
            toolTypes.size() + " tools, " + materialHierarchy.size() + " materials");
    }
    
    private void initializeMLCategories() {
        learnFromMinecraftData();
        plugin.getLogger().info("ML initialized with " + totalExamples + " examples from Minecraft data");
    }
    
    private void learnFromMinecraftData() {
        int before = totalExamples;
        
        learnFromMaterials();
        learnFromRecipes();
        learnFromEntityTypes();
        learnFromMinecraftAdvancements();
        learnFromTags();
        learnFromRecipeIngredients();
        learnFromBlockStates();
        learnFromBiomes();
        learnFromEnchantments();
        learnFromPotionEffects();
        learnFromSounds();
        learnFromParticles();
        learnFromStatistics();
        
        int learned = totalExamples - before;
        plugin.getLogger().info("Learned " + learned + " examples from Minecraft data");
    }
    
    private void learnFromMaterials() {
        for (org.bukkit.Material material : org.bukkit.Material.values()) {
            if (!material.isItem()) continue;
            
            String name = material.name().toLowerCase();
            java.util.List<String> parts = materialHierarchy.get(name);
            if (parts == null) continue;
            
            String category = inferCategoryFromParts(parts, name);
            if (category != null) {
                String action = inferActionFromParts(parts, name);
                String tool = inferToolFromParts(parts, name);
                learn(category, extractFeatures(name, action, tool));
            }
        }
    }
    
    private String inferCategoryFromParts(java.util.List<String> parts, String fullName) {
        if (parts.contains("ore") || parts.contains("stone") || parts.contains("cobblestone")) return "miner";
        if (parts.contains("log") || parts.contains("planks") || parts.contains("stairs") || 
            parts.contains("slab") || parts.contains("fence") || parts.contains("door")) return "builder";
        if (parts.contains("seeds") || parts.contains("wheat") || parts.contains("carrot") || 
            parts.contains("potato") || parts.contains("crop")) return "farmer";
        if (parts.contains("sword") || parts.contains("bow") || parts.contains("arrow")) return "guardsman";
        if (parts.contains("helmet") || parts.contains("chestplate") || parts.contains("leggings") || 
            parts.contains("boots") || parts.contains("pickaxe") || parts.contains("axe") || 
            parts.contains("shovel") || parts.contains("hoe")) {
            String tier = materialTiers.get(fullName);
            return (tier != null && (tier.equals("wood") || tier.equals("stone"))) ? "builder" : "blacksmith";
        }
        if (parts.contains("enchant") || parts.contains("book")) return "librarian";
        if (parts.contains("potion") || parts.contains("brewing")) return "healer";
        return null;
    }
    
    private String inferActionFromParts(java.util.List<String> parts, String fullName) {
        if (parts.contains("ore") || parts.contains("stone") || parts.contains("log")) return "BREAK_BLOCK";
        if (parts.contains("planks") || parts.contains("stairs") || parts.contains("slab")) return "PLACE_BLOCK";
        if (parts.contains("sword") || parts.contains("pickaxe") || parts.contains("potion")) return "CRAFT_ITEM";
        return "USE";
    }
    
    private String inferToolFromParts(java.util.List<String> parts, String fullName) {
        if (parts.contains("ore") || parts.contains("stone")) return "pickaxe";
        if (parts.contains("log")) return "axe";
        if (parts.contains("sword")) return "sword";
        return "hand";
    }
    
    private void learnFromRecipes() {
        java.util.Iterator<org.bukkit.inventory.Recipe> recipes = org.bukkit.Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            org.bukkit.inventory.Recipe recipe = recipes.next();
            org.bukkit.Material result = recipe.getResult().getType();
            String name = result.name().toLowerCase();
            
            java.util.List<String> parts = materialHierarchy.get(name);
            if (parts != null) {
                String category = inferCategoryFromParts(parts, name);
                if (category != null) {
                    learn(category, extractFeatures(name, "CRAFT_ITEM", "hand"));
                }
            }
        }
    }
    
    private void learnFromEntityTypes() {
        for (org.bukkit.entity.EntityType type : org.bukkit.entity.EntityType.values()) {
            if (!type.isAlive()) continue;
            
            String name = type.name().toLowerCase();
            Class<? extends org.bukkit.entity.Entity> entityClass = type.getEntityClass();
            
            if (entityClass != null) {
                if (org.bukkit.entity.Monster.class.isAssignableFrom(entityClass)) {
                    learn("guardsman", extractFeatures(name, "ENTITY_KILL", "sword"));
                } else if (org.bukkit.entity.Animals.class.isAssignableFrom(entityClass)) {
                    learn("farmer", extractFeatures(name, "ENTITY_KILL", "sword"));
                    learn("farmer", extractFeatures(name, "BREED_ANIMAL", "hand"));
                } else if (org.bukkit.entity.WaterMob.class.isAssignableFrom(entityClass)) {
                    learn("farmer", extractFeatures(name, "FISH", "fishing_rod"));
                } else if (org.bukkit.entity.Ambient.class.isAssignableFrom(entityClass)) {
                    learn("farmer", extractFeatures(name, "ENTITY_KILL", "sword"));
                }
            }
        }
    }
    
    private void learnFromMinecraftAdvancements() {
        java.util.Iterator<org.bukkit.advancement.Advancement> advancements = org.bukkit.Bukkit.advancementIterator();
        while (advancements.hasNext()) {
            org.bukkit.advancement.Advancement advancement = advancements.next();
            String key = advancement.getKey().getKey();
            String[] keyParts = key.split("/");
            
            for (String part : keyParts) {
                java.util.List<String> parts = java.util.Arrays.asList(part.split("_"));
                String category = inferCategoryFromParts(parts, part);
                if (category != null) {
                    String action = inferActionFromParts(parts, part);
                    String tool = inferToolFromParts(parts, part);
                    learn(category, extractFeatures(part, action, tool));
                    break;
                }
            }
        }
    }
    
    private void learnFromTags() {
        for (org.bukkit.Tag<org.bukkit.Material> tag : org.bukkit.Bukkit.getTags("blocks", org.bukkit.Material.class)) {
            for (org.bukkit.Material material : tag.getValues()) {
                String matName = material.name().toLowerCase();
                java.util.List<String> parts = materialHierarchy.get(matName);
                if (parts != null) {
                    String category = inferCategoryFromParts(parts, matName);
                    if (category != null) {
                        String action = inferActionFromParts(parts, matName);
                        String tool = inferToolFromParts(parts, matName);
                        learn(category, extractFeatures(matName, action, tool));
                    }
                }
            }
        }
        
        for (org.bukkit.Tag<org.bukkit.Material> tag : org.bukkit.Bukkit.getTags("items", org.bukkit.Material.class)) {
            for (org.bukkit.Material material : tag.getValues()) {
                String matName = material.name().toLowerCase();
                java.util.List<String> parts = materialHierarchy.get(matName);
                if (parts != null) {
                    String category = inferCategoryFromParts(parts, matName);
                    if (category != null) {
                        String action = inferActionFromParts(parts, matName);
                        String tool = inferToolFromParts(parts, matName);
                        learn(category, extractFeatures(matName, action, tool));
                    }
                }
            }
        }
    }
    
    private void learnFromRecipeIngredients() {
        java.util.Iterator<org.bukkit.inventory.Recipe> recipes = org.bukkit.Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            org.bukkit.inventory.Recipe recipe = recipes.next();
            org.bukkit.Material result = recipe.getResult().getType();
            String resultName = result.name().toLowerCase();
            
            java.util.List<String> resultParts = materialHierarchy.get(resultName);
            if (resultParts != null) {
                String resultCategory = inferCategoryFromParts(resultParts, resultName);
                if (resultCategory != null) {
                    learn(resultCategory, extractFeatures(resultName, "CRAFT_ITEM", "hand"));
                }
            }
            
            if (recipe instanceof org.bukkit.inventory.ShapedRecipe shaped) {
                for (org.bukkit.inventory.ItemStack ingredient : shaped.getIngredientMap().values()) {
                    if (ingredient == null || ingredient.getType() == org.bukkit.Material.AIR) continue;
                    String ingName = ingredient.getType().name().toLowerCase();
                    java.util.List<String> ingParts = materialHierarchy.get(ingName);
                    if (ingParts != null) {
                        String ingCategory = inferCategoryFromParts(ingParts, ingName);
                        if (ingCategory != null) {
                            String action = inferActionFromParts(ingParts, ingName);
                            String tool = inferToolFromParts(ingParts, ingName);
                            learn(ingCategory, extractFeatures(ingName, action, tool));
                        }
                    }
                }
            }
            
            if (recipe instanceof org.bukkit.inventory.ShapelessRecipe shapeless) {
                for (org.bukkit.inventory.RecipeChoice ingredient : shapeless.getChoiceList()) {
                    if (ingredient instanceof org.bukkit.inventory.RecipeChoice.MaterialChoice matChoice) {
                        for (org.bukkit.Material mat : matChoice.getChoices()) {
                            String ingName = mat.name().toLowerCase();
                            java.util.List<String> ingParts = materialHierarchy.get(ingName);
                            if (ingParts != null) {
                                String ingCategory = inferCategoryFromParts(ingParts, ingName);
                                if (ingCategory != null) {
                                    learn(ingCategory, extractFeatures(ingName, "CRAFT_ITEM", "hand"));
                                }
                            }
                        }
                    }
                }
            }
            
            if (recipe instanceof org.bukkit.inventory.FurnaceRecipe furnace) {
                org.bukkit.Material input = furnace.getInput().getType();
                String inputName = input.name().toLowerCase();
                java.util.List<String> inputParts = materialHierarchy.get(inputName);
                if (inputParts != null) {
                    String inputCategory = inferCategoryFromParts(inputParts, inputName);
                    if (inputCategory != null) {
                        learn(inputCategory, extractFeatures(inputName, "SMELT_ITEM", "hand"));
                    }
                }
            }
        }
    }
    
    private void learnFromBlockStates() {
        for (org.bukkit.Material material : org.bukkit.Material.values()) {
            if (!material.isBlock()) continue;
            
            try {
                org.bukkit.block.data.BlockData blockData = material.createBlockData();
                String name = material.name().toLowerCase();
                
                if (blockData instanceof org.bukkit.block.data.Ageable) {
                    learn("farmer", extractFeatures(name, "BREAK_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.Powerable) {
                    learn("builder", extractFeatures(name, "USE", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.Openable) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.Lightable) {
                    learn("builder", extractFeatures(name, "USE", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.Waterlogged) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.Bisected) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.Directional) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.Orientable) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.Rotatable) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Furnace) {
                    learn("blacksmith", extractFeatures(name, "USE", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Chest) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Bed) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Door) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.TrapDoor) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Gate) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Fence) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Stairs) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Slab) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Wall) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Campfire) {
                    learn("healer", extractFeatures(name, "USE", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.BrewingStand) {
                    learn("healer", extractFeatures(name, "USE", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.EnderChest) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Hopper) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Dispenser) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Observer) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Piston) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.RedstoneWire) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Repeater) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Comparator) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Lectern) {
                    learn("librarian", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Beehive) {
                    learn("farmer", extractFeatures(name, "USE", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Farmland) {
                    learn("farmer", extractFeatures(name, "USE", "hoe"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Sapling) {
                    learn("farmer", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Leaves) {
                    learn("builder", extractFeatures(name, "BREAK_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Bamboo) {
                    learn("farmer", extractFeatures(name, "BREAK_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Cocoa) {
                    learn("farmer", extractFeatures(name, "BREAK_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.SeaPickle) {
                    learn("farmer", extractFeatures(name, "BREAK_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Scaffolding) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Bell) {
                    learn("builder", extractFeatures(name, "USE", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Lantern) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Candle) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.GlowLichen) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.PointedDripstone) {
                    learn("miner", extractFeatures(name, "BREAK_BLOCK", "pickaxe"));
                }
                if (blockData instanceof org.bukkit.block.data.type.SculkSensor) {
                    learn("miner", extractFeatures(name, "BREAK_BLOCK", "pickaxe"));
                }
                if (blockData instanceof org.bukkit.block.data.type.AmethystCluster) {
                    learn("miner", extractFeatures(name, "BREAK_BLOCK", "pickaxe"));
                }
                if (blockData instanceof org.bukkit.block.data.type.RespawnAnchor) {
                    learn("builder", extractFeatures(name, "USE", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.Jigsaw) {
                    learn("builder", extractFeatures(name, "PLACE_BLOCK", "hand"));
                }
                if (blockData instanceof org.bukkit.block.data.type.StructureBlock) {
                    learn("builder", extractFeatures(name, "USE", "hand"));
                }
            } catch (Exception ignored) {}
        }
    }
    
    private void learnFromBiomes() {
        for (org.bukkit.block.Biome biome : org.bukkit.block.Biome.values()) {
            String name = biome.name().toLowerCase();
            
            if (name.contains("ocean") || name.contains("river")) {
                learn("farmer", extractFeatures(name, "FISH", "fishing_rod"));
            }
            if (name.contains("mountain") || name.contains("cave")) {
                learn("miner", extractFeatures(name, "BREAK_BLOCK", "pickaxe"));
            }
            if (name.contains("forest") || name.contains("jungle")) {
                learn("builder", extractFeatures(name, "BREAK_BLOCK", "axe"));
            }
            if (name.contains("plains") || name.contains("field")) {
                learn("farmer", extractFeatures(name, "BREAK_BLOCK", "hoe"));
            }
        }
    }
    
    private void learnFromEnchantments() {
        for (org.bukkit.enchantments.Enchantment enchant : org.bukkit.enchantments.Enchantment.values()) {
            String name = enchant.getKey().getKey().toLowerCase();
            
            if (name.contains("sharpness") || name.contains("smite") || name.contains("bane")) {
                learn("guardsman", extractFeatures(name, "ENTITY_KILL", "sword"));
            }
            if (name.contains("efficiency") || name.contains("fortune") || name.contains("silk")) {
                learn("miner", extractFeatures(name, "BREAK_BLOCK", "pickaxe"));
            }
            if (name.contains("protection") || name.contains("thorns")) {
                learn("guardsman", extractFeatures(name, "USE", "armor"));
            }
            if (name.contains("unbreaking") || name.contains("mending")) {
                learn("blacksmith", extractFeatures(name, "USE", "hand"));
            }
            
            learn("librarian", extractFeatures(name, "USE", "enchanting_table"));
        }
    }
    
    private void learnFromPotionEffects() {
        for (org.bukkit.potion.PotionEffectType effect : org.bukkit.potion.PotionEffectType.values()) {
            if (effect == null) continue;
            String name = effect.getKey().getKey().toLowerCase();
            
            if (name.contains("heal") || name.contains("regen") || name.contains("absorption")) {
                learn("healer", extractFeatures(name, "CRAFT_ITEM", "brewing_stand"));
            }
            if (name.contains("strength") || name.contains("speed") || name.contains("jump")) {
                learn("healer", extractFeatures(name, "CRAFT_ITEM", "brewing_stand"));
            }
            if (name.contains("poison") || name.contains("harm") || name.contains("weakness")) {
                learn("healer", extractFeatures(name, "CRAFT_ITEM", "brewing_stand"));
            }
        }
    }
    
    private void learnFromSounds() {
        for (org.bukkit.Sound sound : org.bukkit.Sound.values()) {
            String name = sound.name().toLowerCase();
            
            if (name.contains("block_stone") || name.contains("block_anvil")) {
                learn("miner", extractFeatures(name, "BREAK_BLOCK", "pickaxe"));
            }
            if (name.contains("block_wood")) {
                learn("builder", extractFeatures(name, "BREAK_BLOCK", "axe"));
            }
            if (name.contains("entity_zombie") || name.contains("entity_skeleton")) {
                learn("guardsman", extractFeatures(name, "ENTITY_KILL", "sword"));
            }
            if (name.contains("entity_cow") || name.contains("entity_sheep")) {
                learn("farmer", extractFeatures(name, "BREED_ANIMAL", "hand"));
            }
            if (name.contains("block_brewing_stand") || name.contains("item_bottle")) {
                learn("healer", extractFeatures(name, "CRAFT_ITEM", "hand"));
            }
        }
    }
    
    private void learnFromParticles() {
        for (org.bukkit.Particle particle : org.bukkit.Particle.values()) {
            String name = particle.name().toLowerCase();
            
            if (name.contains("flame") || name.contains("smoke") || name.contains("lava")) {
                learn("blacksmith", extractFeatures(name, "SMELT_ITEM", "furnace"));
            }
            if (name.contains("enchant") || name.contains("spell")) {
                learn("librarian", extractFeatures(name, "USE", "enchanting_table"));
            }
            if (name.contains("heart") || name.contains("heal")) {
                learn("healer", extractFeatures(name, "USE", "potion"));
            }
            if (name.contains("crit") || name.contains("damage")) {
                learn("guardsman", extractFeatures(name, "ENTITY_KILL", "sword"));
            }
        }
    }
    
    private void learnFromStatistics() {
        for (org.bukkit.Statistic stat : org.bukkit.Statistic.values()) {
            String name = stat.name().toLowerCase();
            
            if (name.contains("mine") || name.contains("break")) {
                learn("miner", extractFeatures(name, "BREAK_BLOCK", "pickaxe"));
            }
            if (name.contains("craft")) {
                learn("blacksmith", extractFeatures(name, "CRAFT_ITEM", "hand"));
            }
            if (name.contains("kill") || name.contains("mob")) {
                learn("guardsman", extractFeatures(name, "ENTITY_KILL", "sword"));
            }
            if (name.contains("breed") || name.contains("animal")) {
                learn("farmer", extractFeatures(name, "BREED_ANIMAL", "hand"));
            }
            if (name.contains("fish")) {
                learn("farmer", extractFeatures(name, "FISH", "fishing_rod"));
            }
            if (name.contains("enchant")) {
                learn("librarian", extractFeatures(name, "USE", "enchanting_table"));
            }
            if (name.contains("brew") || name.contains("potion")) {
                learn("healer", extractFeatures(name, "CRAFT_ITEM", "brewing_stand"));
            }
        }
    }
    
    private void learn(String category, List<String> features) {
        CategoryData data = categories.computeIfAbsent(category, k -> new CategoryData());
        data.exampleCount++;
        
        Set<String> uniqueFeatures = new HashSet<>(features);
        for (String feature : uniqueFeatures) {
            data.featureCounts.merge(feature, 1, Integer::sum);
            data.totalFeatures++;
        }
        
        totalExamples++;
        
        if (totalExamples > memoryCapacity) {
            forgetOldest();
        }
    }
    
    private void forgetOldest() {
        String categoryToForget = categories.entrySet().stream()
            .min(Comparator.comparingInt(e -> e.getValue().exampleCount))
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (categoryToForget != null) {
            CategoryData data = categories.get(categoryToForget);
            if (data.exampleCount > 1) {
                data.exampleCount--;
                totalExamples--;
            }
        }
    }
    
    public ActionResult recordAction(UUID playerId, ActionType type, String context) {
        String actionKey = type.name().toLowerCase() + "." + context.toLowerCase();
        
        actionFrequency.merge(actionKey, 1, Integer::sum);
        lastActionTime.put(actionKey, System.currentTimeMillis());
        
        playerActionPatterns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .merge(context.toLowerCase(), 1, Integer::sum);
        
        int xpAmount = calculateXpAmount(actionKey, type);
        
        String state = buildState(type, context, inferToolFromContext(playerId, context));
        String category = determineCategoryWithRL(playerId, type, context, state);
        String rootCategory = extractRootCategory(category);
        String skillId = generateSkillId(rootCategory, type, context);
        
        ensureNodeExists(skillId, rootCategory, context);
        
        updateStatistics(actionKey, xpAmount);
        
        return new ActionResult(skillId, xpAmount, calculateImportance(actionKey));
    }
    
    private String determineCategoryWithRL(UUID playerId, ActionType type, String context, String state) {
        String deterministicCategory = applyDeterministicRules(type, context);
        if (deterministicCategory != null) {
            provideReward(state, deterministicCategory, 1.0);
            return deterministicCategory;
        }
        
        Random random = new Random();
        if (random.nextDouble() < explorationRate) {
            String tool = inferToolFromContext(playerId, context);
            List<String> features = extractFeatures(context, type.name(), tool);
            String category = classifyFeatures(features);
            
            provideReward(state, category, 0.5);
            return category;
        } else {
            String bestAction = selectBestAction(state);
            if (bestAction != null) {
                return bestAction;
            }
            
            String tool = inferToolFromContext(playerId, context);
            List<String> features = extractFeatures(context, type.name(), tool);
            return classifyFeatures(features);
        }
    }
    
    private String buildState(ActionType type, String context, String tool) {
        return type.name() + ":" + context.toLowerCase() + ":" + tool;
    }
    
    private String selectBestAction(String state) {
        Map<String, Double> actions = qTable.get(state);
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        
        return actions.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    public void provideReward(String state, String action, double reward) {
        Map<String, Double> actions = qTable.computeIfAbsent(state, k -> new ConcurrentHashMap<>());
        
        double currentQ = actions.getOrDefault(action, 0.0);
        
        double maxFutureQ = qTable.values().stream()
            .flatMap(m -> m.values().stream())
            .max(Double::compareTo)
            .orElse(0.0);
        
        double newQ = currentQ + learningRate * (reward + discountFactor * maxFutureQ - currentQ);
        
        actions.put(action, newQ);
        
        String stateActionKey = state + ":" + action;
        stateActionCounts.merge(stateActionKey, 1, Integer::sum);
    }
    
    public void provideFeedback(UUID playerId, ActionType type, String context, String expectedCategory, boolean correct) {
        String tool = inferToolFromContext(playerId, context);
        String state = buildState(type, context, tool);
        
        double reward = correct ? 1.0 : -1.0;
        provideReward(state, expectedCategory, reward);
        
        if (correct) {
            List<String> features = extractFeatures(context, type.name(), tool);
            learn(expectedCategory, features);
        }
        
        plugin.getLogger().info(String.format("[RL] Feedback: %s -> %s (%s) | Reward: %.1f", 
            state, expectedCategory, correct ? "✓" : "✗", reward));
    }
    
    private String extractRootCategory(String category) {
        int firstDot = category.indexOf('.');
        if (firstDot == -1) {
            return category;
        }
        return category.substring(0, firstDot);
    }
    
    private void ensureNodeExists(String skillId, String rootCategory, String context) {
        if (plugin.getTreeRegistry() == null) return;
        
        var mainTree = plugin.getTreeRegistry().getTree("main");
        if (mainTree.isEmpty()) return;
        
        if (mainTree.get().getNode(skillId).isPresent()) {
            return;
        }
        
        String[] parts = skillId.split("\\.");
        if (parts.length < 2) return;
        
        String parentId = parts.length == 2 ? parts[0] : String.join(".", java.util.Arrays.copyOfRange(parts, 0, parts.length - 1));
        var parentNode = mainTree.get().getNode(parentId);
        
        if (parentNode.isEmpty()) return;
        
        String nodeName = parts[parts.length - 1];
        String displayName = formatDisplayName(nodeName);
        
        var newNode = org.shotrush.atom.model.SkillNode.builder()
            .id(skillId)
            .displayName(displayName)
            .parent(parentNode.get())
            .maxXp(5000)
            .type(org.shotrush.atom.model.SkillNode.NodeType.LEAF)
            .build();
        
        parentNode.get().addChild(newNode);
        
        if (plugin.getAdvancementGenerator() != null) {
            plugin.getAdvancementGenerator().generateNodeAdvancement(newNode, parentId);
            plugin.getLogger().info("[ML] Created node: " + skillId + " (" + displayName + ")");
        }
    }
    
    private String formatDisplayName(String nodeName) {
        return java.util.Arrays.stream(nodeName.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(java.util.stream.Collectors.joining(" "));
    }
    
    private int calculateXpAmount(String actionKey, ActionType type) {
        int frequency = actionFrequency.getOrDefault(actionKey, 1);
        int totalActions = actionFrequency.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalActions == 0) return type.baseXp;
        
        double rarity = 1.0 - ((double) frequency / totalActions);
        
        double multiplier = 0.5 + (rarity * 2.5);
        
        int baseXp = type.baseXp;
        
        return (int) Math.max(1, baseXp * multiplier);
    }
    private String determineCategory(UUID playerId, ActionType type, String context) {
        String cacheKey = type.name() + ":" + context.toLowerCase();
        if (learnedCategories.containsKey(cacheKey)) {
            return learnedCategories.get(cacheKey);
        }
        
        String ruleBasedCategory = applyDeterministicRules(type, context);
        if (ruleBasedCategory != null) {
            learnedCategories.put(cacheKey, ruleBasedCategory);
            String tool = inferToolFromContext(playerId, context);
            List<String> features = extractFeatures(context, type.name(), tool);
            learn(ruleBasedCategory, features);
            return ruleBasedCategory;
        }
        
        String tool = inferToolFromContext(playerId, context);
        List<String> features = extractFeatures(context, type.name(), tool);
        String category = classifyFeatures(features);
        
        learnedCategories.put(cacheKey, category);
        learn(category, features);
        
        return category;
    }
    
    private String applyDeterministicRules(ActionType type, String context) {
        String lower = context.toLowerCase();
        
        switch (type) {
            case BREAK_BLOCK:
                if (lower.contains("ore") || lower.contains("stone") || lower.contains("cobblestone") ||
                    lower.contains("andesite") || lower.contains("diorite") || lower.contains("granite") ||
                    lower.contains("deepslate") || lower.contains("netherrack") || lower.contains("debris")) {
                    return "miner";
                }
                if (lower.contains("wheat") || lower.contains("carrot") || lower.contains("potato") ||
                    lower.contains("beetroot") || lower.contains("melon") || lower.contains("pumpkin")) {
                    return "farmer";
                }
                if (lower.contains("log") || lower.contains("wood")) {
                    return null;
                }
                break;
                
            case PLACE_BLOCK:
                if (lower.contains("plank") || lower.contains("log") || lower.contains("wood") ||
                    lower.contains("brick") || lower.contains("concrete") || lower.contains("glass") ||
                    lower.contains("door") || lower.contains("fence") || lower.contains("stairs") ||
                    lower.contains("slab") || lower.contains("wall")) {
                    return "builder";
                }
                if (lower.contains("wheat") || lower.contains("carrot") || lower.contains("potato") ||
                    lower.contains("beetroot") || lower.contains("seeds") || lower.contains("farmland")) {
                    return "farmer";
                }
                break;
                
            case KILL_MOB:
                if (lower.contains("zombie") || lower.contains("skeleton") || lower.contains("spider") ||
                    lower.contains("creeper") || lower.contains("enderman") || lower.contains("blaze") ||
                    lower.contains("wither") || lower.contains("ghast") || lower.contains("slime") ||
                    lower.contains("phantom") || lower.contains("drowned") || lower.contains("husk") ||
                    lower.contains("stray") || lower.contains("witch") || lower.contains("pillager") ||
                    lower.contains("vindicator") || lower.contains("evoker") || lower.contains("ravager") ||
                    lower.contains("hoglin") || lower.contains("piglin") || lower.contains("zoglin")) {
                    return "guardsman";
                }
                if (lower.contains("cow") || lower.contains("sheep") || lower.contains("pig") ||
                    lower.contains("chicken") || lower.contains("rabbit") || lower.contains("llama")) {
                    return "farmer";
                }
                break;
                
            case BREED_ANIMAL:
                return "farmer";
                
            case CRAFT_ITEM:
                if (lower.contains("sword") || lower.contains("pickaxe") || lower.contains("axe") ||
                    lower.contains("shovel") || lower.contains("hoe") || lower.contains("helmet") ||
                    lower.contains("chestplate") || lower.contains("leggings") || lower.contains("boots")) {
                    return "blacksmith";
                }
                if (lower.contains("enchant") || lower.contains("book")) {
                    return "librarian";
                }
                if (lower.contains("potion") || lower.contains("brew") || lower.contains("golden_apple")) {
                    return "healer";
                }
                if (lower.contains("door") || lower.contains("fence") || lower.contains("stairs") ||
                    lower.contains("slab") || lower.contains("plank")) {
                    return "builder";
                }
                break;
                
            case INTERACT:
                if (lower.contains("anvil") || lower.contains("smithing") || lower.contains("furnace")) {
                    return "blacksmith";
                }
                if (lower.contains("lectern") || lower.contains("bookshelf") || lower.contains("enchant")) {
                    return "librarian";
                }
                if (lower.contains("brew") || lower.contains("cauldron")) {
                    return "healer";
                }
                if (lower.contains("farmland") || lower.contains("composter")) {
                    return "farmer";
                }
                break;
        }
        
        return null;
    }
    
    private List<String> extractFeatures(String context, String actionType, String... additionalFeatures) {
        List<String> features = new ArrayList<>();
        
        String[] tokens = context.toLowerCase().split("_");
        features.addAll(Arrays.asList(tokens));
        
        features.add("action:" + actionType.toLowerCase());
        
        for (String additional : additionalFeatures) {
            if (additional != null && !additional.isEmpty()) {
                features.add("tool:" + additional.toLowerCase());
            }
        }
        
        return features;
    }
    
    private String classifyFeatures(List<String> features) {
        if (categories.isEmpty()) {
            return "builder";
        }
        
        Map<String, Double> scores = new HashMap<>();
        
        for (Map.Entry<String, CategoryData> entry : categories.entrySet()) {
            String category = entry.getKey();
            CategoryData data = entry.getValue();
            
            double categoryProb = Math.log((double) data.exampleCount / totalExamples);
            double featureScore = 0.0;
            
            for (String feature : features) {
                int featureCount = data.featureCounts.getOrDefault(feature, 0);
                double prob = (featureCount + 1.0) / (data.totalFeatures + data.featureCounts.size() + 1.0);
                featureScore += Math.log(prob + 1e-10);
            }
            
            double totalScore = categoryProb + featureScore;
            scores.put(category, totalScore);
        }
        
        String bestCategory = applyContextualRules(features, scores);
        
        return bestCategory != null ? bestCategory : "builder";
    }
    
    private String applyContextualRules(List<String> features, Map<String, Double> scores) {
        boolean hasAxe = features.stream().anyMatch(f -> f.contains("axe"));
        boolean hasPickaxe = features.stream().anyMatch(f -> f.contains("pickaxe"));
        boolean hasSword = features.stream().anyMatch(f -> f.contains("sword"));
        boolean hasHoe = features.stream().anyMatch(f -> f.contains("hoe"));
        boolean hasFishingRod = features.stream().anyMatch(f -> f.contains("fishing"));
        
        boolean hasWood = features.stream().anyMatch(f -> f.contains("log") || f.contains("wood") || f.contains("plank"));
        boolean hasCrop = features.stream().anyMatch(f -> f.contains("wheat") || f.contains("carrot") || f.contains("potato"));
        boolean hasFish = features.stream().anyMatch(f -> f.contains("fish") || f.contains("cod") || f.contains("salmon"));
        boolean hasAnimal = features.stream().anyMatch(f -> f.contains("cow") || f.contains("sheep") || f.contains("pig") || f.contains("chicken"));
        
        if (hasWood) {
            if (hasAxe) {
                return "blacksmith";
            } else if (hasPickaxe) {
                return "builder";
            }
        }
        
        if (hasFish) {
            if (hasFishingRod) {
                return "farmer";
            }
        }
        
        if (hasCrop && hasHoe) {
            return "farmer";
        }
        
        if (hasAnimal) {
            if (hasSword) {
                return "farmer";
            }
        }
        
        String bestCategory = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        
        return bestCategory;
    }
    
    private String inferToolFromContext(UUID playerId, String context) {
        Map<String, Integer> patterns = playerActionPatterns.get(playerId);
        if (patterns == null) return "hand";
        
        if (context.contains("sword") || patterns.containsKey("sword")) return "sword";
        if (context.contains("axe") || patterns.containsKey("axe")) return "axe";
        if (context.contains("bow") || patterns.containsKey("bow")) return "bow";
        if (context.contains("crossbow") || patterns.containsKey("crossbow")) return "crossbow";
        if (context.contains("shield") || patterns.containsKey("shield")) return "shield";
        if (context.contains("pickaxe") || patterns.containsKey("pickaxe")) return "pickaxe";
        if (context.contains("shovel") || patterns.containsKey("shovel")) return "shovel";
        if (context.contains("hoe") || patterns.containsKey("hoe")) return "hoe";
        
        return "hand";
    }
    
    private String inferCategoryFromPlayerBehavior(UUID playerId, String context) {
        Map<String, Integer> playerActions = playerActionPatterns.get(playerId);
        if (playerActions == null || playerActions.size() < 10) {
            return null;
        }
        
        Map<String, Double> categorySimilarity = new HashMap<>();
        String[] categories = {"miner", "farmer", "guardsman", "blacksmith", "builder", "librarian", "healer"};
        
        for (String category : categories) {
            double similarity = calculateSimilarityToCategory(context, category, playerActions);
            categorySimilarity.put(category, similarity);
        }
        
        return categorySimilarity.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    private double calculateSimilarityToCategory(String context, String category, Map<String, Integer> playerActions) {
        double similarity = 0.0;
        int totalActions = playerActions.values().stream().mapToInt(Integer::intValue).sum();
        
        for (Map.Entry<String, Integer> entry : playerActions.entrySet()) {
            String action = entry.getKey();
            int frequency = entry.getValue();
            double weight = (double) frequency / totalActions;
            
            if (learnedCategories.getOrDefault(action, "").equals(category)) {
                similarity += weight * calculateStringDistance(context, action);
            }
        }
        
        return similarity;
    }
    
    private double calculateStringDistance(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    
    private String generateSkillId(String category, ActionType type, String context) {
        String lowerContext = context.toLowerCase();
        
        String depth2 = inferDepth2Cluster(category, type, lowerContext);
        String depth3 = inferDepth3Cluster(category, depth2, lowerContext);
        String depth4 = inferDepth4Cluster(category, depth2, depth3, lowerContext);
        String depth5 = inferDepth5Cluster(category, depth2, depth3, depth4, lowerContext);
        
        StringBuilder skillId = new StringBuilder(category);
        if (depth2 != null && !depth2.isEmpty()) {
            skillId.append(".").append(depth2);
            if (depth3 != null && !depth3.isEmpty()) {
                skillId.append(".").append(depth3);
                if (depth4 != null && !depth4.isEmpty()) {
                    skillId.append(".").append(depth4);
                    if (depth5 != null && !depth5.isEmpty()) {
                        skillId.append(".").append(depth5);
                    }
                }
            }
        }
        return skillId.toString();
    }
    
    private String inferDepth2Cluster(String category, ActionType type, String context) {
        java.util.List<String> parts = materialHierarchy.get(context.toLowerCase());
        if (parts == null || parts.isEmpty()) return type.name().toLowerCase();
        
        if (parts.contains("ore")) return "ore_mining";
        if (parts.contains("log") || parts.contains("wood")) return "wood_gathering";
        if (parts.contains("stone") || parts.contains("cobblestone")) return "stone_mining";
        if (parts.contains("crop") || parts.contains("wheat") || parts.contains("carrot") || parts.contains("potato")) return "crop_farming";
        if (parts.contains("sword") || parts.contains("axe") || parts.contains("pickaxe")) return "tool_crafting";
        if (parts.contains("helmet") || parts.contains("chestplate") || parts.contains("leggings") || parts.contains("boots")) return "armor_crafting";
        if (parts.contains("potion") || parts.contains("brew")) return "brewing";
        if (parts.contains("enchant") || parts.contains("book")) return "enchanting";
        
        return type.name().toLowerCase();
    }
    
    private String inferDepth3Cluster(String category, String depth2, String context) {
        java.util.List<String> parts = materialHierarchy.get(context.toLowerCase());
        if (parts == null || parts.size() < 2) return null;
        
        String tier = materialTiers.get(context.toLowerCase());
        if (tier != null) return tier;
        
        for (String part : parts) {
            if (part.equals("diamond") || part.equals("emerald") || part.equals("netherite")) return part;
            if (part.equals("iron") || part.equals("gold") || part.equals("copper")) return part;
            if (part.equals("coal") || part.equals("redstone") || part.equals("lapis")) return part;
            if (part.equals("wheat") || part.equals("carrot") || part.equals("potato")) return part;
        }
        
        return null;
    }
    
    private String inferDepth4Cluster(String category, String depth2, String depth3, String context) {
        if (depth3 == null) return null;
        
        java.util.List<String> parts = materialHierarchy.get(context.toLowerCase());
        if (parts == null || parts.size() < 3) return null;
        
        if (context.contains("deepslate")) return "deepslate";
        if (context.contains("raw")) return "raw";
        if (context.contains("nether")) return "nether";
        
        if (parts.size() >= 3) return parts.get(2);
        
        return null;
    }
    
    private String inferDepth5Cluster(String category, String depth2, String depth3, String depth4, String context) {
        if (depth4 == null) return null;
        
        java.util.List<String> parts = materialHierarchy.get(context.toLowerCase());
        if (parts == null || parts.size() < 4) return null;
        
        return parts.get(3);
    }
    
    private double calculateImportance(String actionKey) {
        int frequency = actionFrequency.getOrDefault(actionKey, 1);
        int totalActions = actionFrequency.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalActions == 0) return 0.5;
        double rarity = 1.0 - ((double) frequency / totalActions);
        double rarityScore = rarity * 0.4;
        long lastTime = lastActionTime.getOrDefault(actionKey, 0L);
        long timeSince = System.currentTimeMillis() - lastTime;
        double recencyScore = Math.max(0, 0.3 - (timeSince / 3600000.0 * 0.1));
        double frequencyScore = Math.min(0.3, (double) frequency / 100.0 * 0.3);
        
        return Math.min(1.0, rarityScore + recencyScore + frequencyScore);
    }
    
    private void updateStatistics(String actionKey, int xpAmount) {
        actionStatistics.compute(actionKey, (k, stats) -> {
            if (stats == null) {
                return new ActionStats(1, xpAmount, xpAmount, xpAmount);
            }
            int newCount = stats.count + 1;
            int newTotal = stats.totalXp + xpAmount;
            int newMin = Math.min(stats.minXp, xpAmount);
            int newMax = Math.max(stats.maxXp, xpAmount);
            return new ActionStats(newCount, newTotal, newMin, newMax);
        });
    }
    
    public Map<String, ActionStats> getActionStatistics() {
        return new HashMap<>(actionStatistics);
    }
    
    public Map<String, Integer> getActionFrequency() {
        return new HashMap<>(actionFrequency);
    }
    
    public boolean saveMLBrain() {
        try {
            Path brainFile = mlBrainPath.resolve("classifier.dat");
            
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(brainFile)))) {
                
                oos.writeInt(trainingLevel);
                oos.writeInt(totalExamples);
                oos.writeInt(categories.size());
                
                for (Map.Entry<String, CategoryData> entry : categories.entrySet()) {
                    oos.writeUTF(entry.getKey());
                    CategoryData data = entry.getValue();
                    oos.writeInt(data.exampleCount);
                    oos.writeInt(data.totalFeatures);
                    oos.writeInt(data.featureCounts.size());
                    
                    for (Map.Entry<String, Integer> feature : data.featureCounts.entrySet()) {
                        oos.writeUTF(feature.getKey());
                        oos.writeInt(feature.getValue());
                    }
                }
                
                oos.writeInt(qTable.size());
                for (Map.Entry<String, Map<String, Double>> stateEntry : qTable.entrySet()) {
                    oos.writeUTF(stateEntry.getKey());
                    oos.writeInt(stateEntry.getValue().size());
                    for (Map.Entry<String, Double> actionEntry : stateEntry.getValue().entrySet()) {
                        oos.writeUTF(actionEntry.getKey());
                        oos.writeDouble(actionEntry.getValue());
                    }
                }
            }
            
            plugin.getLogger().info("ML brain saved (" + totalExamples + " examples, " + categories.size() + " categories)");
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save ML brain: " + e.getMessage());
            return false;
        }
    }
    
    public boolean loadMLBrain() {
        Path brainFile = mlBrainPath.resolve("classifier.dat");
        
        if (!Files.exists(brainFile)) {
            return false;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(brainFile)))) {
            
            trainingLevel = ois.readInt();
            totalExamples = ois.readInt();
            int categoryCount = ois.readInt();
            
            categories.clear();
            
            for (int i = 0; i < categoryCount; i++) {
                String categoryName = ois.readUTF();
                CategoryData data = new CategoryData();
                data.exampleCount = ois.readInt();
                data.totalFeatures = ois.readInt();
                int featureCount = ois.readInt();
                
                for (int j = 0; j < featureCount; j++) {
                    String featureName = ois.readUTF();
                    int featureValue = ois.readInt();
                    data.featureCounts.put(featureName, featureValue);
                }
                
                categories.put(categoryName, data);
            }
            
            qTable.clear();
            int qTableSize = ois.readInt();
            for (int i = 0; i < qTableSize; i++) {
                String state = ois.readUTF();
                int actionCount = ois.readInt();
                Map<String, Double> actions = new ConcurrentHashMap<>();
                for (int j = 0; j < actionCount; j++) {
                    String action = ois.readUTF();
                    double qValue = ois.readDouble();
                    actions.put(action, qValue);
                }
                qTable.put(state, actions);
            }
            
            plugin.getLogger().info("ML brain loaded (" + totalExamples + " examples, " + categories.size() + " categories)");
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load ML brain: " + e.getMessage());
            return false;
        }
    }
    
    private record TrainingResult(double naiveBayesAccuracy, double rlAccuracy) {}
    
    private TrainingResult trainWithQuizAndGetRLAccuracy(int questionCount) {
        Random random = new Random();
        int correct = 0;
        int rlCorrect = 0;
        
        String[][] quizData = {
            {"diamond_ore", "BREAK_BLOCK", "pickaxe", "miner"},
            {"iron_ore", "BREAK_BLOCK", "pickaxe", "miner"},
            {"wheat", "BREAK_BLOCK", "hand", "farmer"},
            {"carrot", "BREAK_BLOCK", "hand", "farmer"},
            {"zombie", "KILL_MOB", "sword", "guardsman"},
            {"skeleton", "KILL_MOB", "sword", "guardsman"},
            {"iron_sword", "CRAFT_ITEM", "hand", "blacksmith"},
            {"diamond_pickaxe", "CRAFT_ITEM", "hand", "blacksmith"},
            {"oak_planks", "PLACE_BLOCK", "hand", "builder"},
            {"stone_bricks", "PLACE_BLOCK", "hand", "builder"},
            {"enchanting_table", "PLACE_BLOCK", "hand", "librarian"},
            {"book", "CRAFT_ITEM", "hand", "librarian"},
            {"potion", "CRAFT_ITEM", "hand", "healer"},
            {"golden_apple", "CRAFT_ITEM", "hand", "healer"}
        };
        
        int rlAttempts = 0;
        
        for (int i = 0; i < questionCount; i++) {
            String[] question = quizData[random.nextInt(quizData.length)];
            String context = question[0];
            String actionType = question[1];
            String tool = question[2];
            String expectedCategory = question[3];
            
            String state = buildState(ActionType.valueOf(actionType), context, tool);
            
            List<String> features = extractFeatures(context, actionType, tool);
            String predictedCategory = classifyFeatures(features);
            
            if (predictedCategory.equals(expectedCategory)) {
                correct++;
                provideReward(state, expectedCategory, 1.0);
            } else {
                provideReward(state, predictedCategory, -0.5);
                provideReward(state, expectedCategory, 1.0);
            }
            
            String rlPrediction = selectBestAction(state);
            if (rlPrediction != null) {
                rlAttempts++;
                if (rlPrediction.equals(expectedCategory)) {
                    rlCorrect++;
                }
            }
            
            learn(expectedCategory, features);
        }
        
        double nbAccuracy = questionCount > 0 ? (double) correct / questionCount * 100 : 0;
        double rlAccuracyKnown = rlAttempts > 0 ? (double) rlCorrect / rlAttempts * 100 : 0.0;
        double rlAccuracyTotal = questionCount > 0 ? (double) rlCorrect / questionCount * 100 : 0.0;
        
        return new TrainingResult(nbAccuracy, rlAccuracyTotal);
    }
    
    public int trainWithQuiz(int questionCount) {
        Random random = new Random();
        int correct = 0;
        int rlCorrect = 0;
        
        String[][] quizData = {
            {"diamond_ore", "BREAK_BLOCK", "pickaxe", "miner"},
            {"iron_ore", "BREAK_BLOCK", "pickaxe", "miner"},
            {"wheat", "BREAK_BLOCK", "hand", "farmer"},
            {"carrot", "BREAK_BLOCK", "hand", "farmer"},
            {"zombie", "KILL_MOB", "sword", "guardsman"},
            {"skeleton", "KILL_MOB", "sword", "guardsman"},
            {"iron_sword", "CRAFT_ITEM", "hand", "blacksmith"},
            {"diamond_pickaxe", "CRAFT_ITEM", "hand", "blacksmith"},
            {"oak_planks", "PLACE_BLOCK", "hand", "builder"},
            {"stone_bricks", "PLACE_BLOCK", "hand", "builder"},
            {"enchanting_table", "PLACE_BLOCK", "hand", "librarian"},
            {"book", "CRAFT_ITEM", "hand", "librarian"},
            {"potion", "CRAFT_ITEM", "hand", "healer"},
            {"golden_apple", "CRAFT_ITEM", "hand", "healer"}
        };
        
        for (int i = 0; i < questionCount; i++) {
            String[] question = quizData[random.nextInt(quizData.length)];
            String context = question[0];
            String actionType = question[1];
            String tool = question[2];
            String expectedCategory = question[3];
            
            String state = buildState(ActionType.valueOf(actionType), context, tool);
            
            List<String> features = extractFeatures(context, actionType, tool);
            String predictedCategory = classifyFeatures(features);
            
            if (predictedCategory.equals(expectedCategory)) {
                correct++;
                provideReward(state, expectedCategory, 1.0);
            } else {
                provideReward(state, predictedCategory, -0.5);
                provideReward(state, expectedCategory, 1.0);
            }
            
            String rlPrediction = selectBestAction(state);
            if (rlPrediction != null && rlPrediction.equals(expectedCategory)) {
                rlCorrect++;
            }
            
            learn(expectedCategory, features);
        }
        
        double rlAccuracy = questionCount > 0 ? (double) rlCorrect / questionCount * 100 : 0;
        plugin.getLogger().info(String.format("[RL] Q-Learning accuracy: %.1f%% (%d/%d)", 
            rlAccuracy, rlCorrect, questionCount));
        
        saveMLBrain();
        return correct;
    }
    
    public int trainWithSyntheticData(int exampleCount) {
        Random random = new Random();
        int trained = 0;
        int maxAttempts = exampleCount * 2;
        int attempts = 0;
        
        while (trained < exampleCount && attempts < maxAttempts) {
            attempts++;
            Material material = getRandomMaterial(random);
            String context = material.name().toLowerCase();
            
            ActionType actionType = getRandomActionType(random);
            String tool = getRandomTool(random);
            
            String expectedCategory = applyDeterministicRules(actionType, context);
            if (expectedCategory == null) {
                continue;
            }
            
            String state = buildState(actionType, context, tool);
            provideReward(state, expectedCategory, 1.0);
            
            List<String> features = extractFeatures(context, actionType.name(), tool);
            learn(expectedCategory, features);
            trained++;
        }
        
        return trained;
    }
    
    private Material getRandomMaterial(Random random) {
        Material[] materials = Material.values();
        return materials[random.nextInt(materials.length)];
    }
    
    private ActionType getRandomActionType(Random random) {
        ActionType[] types = ActionType.values();
        return types[random.nextInt(types.length)];
    }
    
    private String getRandomTool(Random random) {
        String[] tools = {"pickaxe", "axe", "shovel", "hoe", "sword", "hand"};
        return tools[random.nextInt(tools.length)];
    }
    
    private List<TrainingExample> generateTrainingExamplesFromMaterials() {
        List<TrainingExample> examples = new ArrayList<>();
        
        for (Material material : Material.values()) {
            if (!material.isItem() && !material.isBlock()) continue;
            
            String name = material.name().toLowerCase();
            
            for (ActionType actionType : ActionType.values()) {
                String category = applyDeterministicRules(actionType, name);
                
                if (category != null) {
                    ActionType inferredAction = inferActionTypeForMaterial(material);
                    String tool = inferToolForMaterial(material);
                    
                    examples.add(new TrainingExample(name, inferredAction.name(), tool, category));
                    examples.addAll(augmentExample(name, inferredAction, tool, category));
                    
                    if (material.isBlock()) {
                        examples.add(new TrainingExample(name, "BREAK_BLOCK", tool, category));
                        examples.add(new TrainingExample(name, "PLACE_BLOCK", "hand", category));
                    }
                    
                    break;
                }
            }
        }
        
        for (EntityType entityType : EntityType.values()) {
            if (!entityType.isAlive()) continue;
            
            String name = entityType.name().toLowerCase();
            String category = applyDeterministicRules(ActionType.KILL_MOB, name);
            
            if (category != null) {
                String[] weapons = {"sword", "axe", "bow", "trident", "crossbow", "hand"};
                for (String weapon : weapons) {
                    examples.add(new TrainingExample(name, "KILL_MOB", weapon, category));
                }
                
                examples.add(new TrainingExample(name, "ENTITY_KILL", "sword", category));
                examples.add(new TrainingExample(name, "INTERACT", "hand", category));
            }
        }
        
        plugin.getLogger().info("Generated " + examples.size() + " training examples from " + 
            Material.values().length + " materials and " + EntityType.values().length + " entities");
        return examples;
    }
    
    private List<TrainingExample> augmentExample(String context, ActionType actionType, String tool, String category) {
        List<TrainingExample> augmented = new ArrayList<>();
        
        if (category.equals("miner") && actionType == ActionType.MINE_BLOCK) {
            augmented.add(new TrainingExample(context, "BREAK_BLOCK", tool, category));
        }
        
        if (category.equals("farmer") && (context.contains("wheat") || context.contains("carrot") || 
            context.contains("potato") || context.contains("beetroot"))) {
            augmented.add(new TrainingExample(context, "HARVEST_CROP", tool, category));
            augmented.add(new TrainingExample(context, "PLANT_CROP", tool, category));
        }
        
        if (category.equals("builder")) {
            if (actionType == ActionType.BREAK_BLOCK) {
                augmented.add(new TrainingExample(context, "PLACE_BLOCK", tool, category));
            } else if (actionType == ActionType.PLACE_BLOCK) {
                augmented.add(new TrainingExample(context, "BREAK_BLOCK", tool, category));
            }
        }
        
        if (category.equals("blacksmith")) {
            if (context.contains("ingot")) {
                augmented.add(new TrainingExample(context, "SMELT_ITEM", tool, category));
                augmented.add(new TrainingExample(context, "CRAFT_ITEM", tool, category));
            }
        }
        
        return augmented;
    }
    
    private ActionType inferActionTypeForMaterial(Material material) {
        String name = material.name().toLowerCase();
        
        if (name.contains("ore") || name.contains("stone")) return ActionType.MINE_BLOCK;
        if (name.contains("wheat") || name.contains("carrot") || name.contains("potato")) return ActionType.HARVEST_CROP;
        if (material.isBlock()) return ActionType.BREAK_BLOCK;
        if (name.contains("sword") || name.contains("pickaxe") || name.contains("helmet")) return ActionType.CRAFT_ITEM;
        if (name.contains("ingot")) return ActionType.SMELT_ITEM;
        if (name.contains("potion")) return ActionType.BREW_POTION;
        if (name.contains("enchanted")) return ActionType.ENCHANT_ITEM;
        
        return ActionType.INTERACT;
    }
    
    private String inferToolForMaterial(Material material) {
        String name = material.name().toLowerCase();
        
        if (name.contains("ore") || name.contains("stone")) return "pickaxe";
        if (name.contains("log") || name.contains("wood")) return "axe";
        if (name.contains("dirt") || name.contains("sand") || name.contains("gravel")) return "shovel";
        if (name.contains("wheat") || name.contains("carrot") || name.contains("potato")) return "hoe";
        
        return "hand";
    }
    
    public int trainFromPlayerBehavior() {
        int trained = 0;
        
        for (Map.Entry<String, String> entry : learnedCategories.entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length != 2) continue;
            
            String actionType = parts[0];
            String context = parts[1];
            String category = entry.getValue();
            
            List<String> features = extractFeatures(context, actionType, "hand");
            learn(category, features);
            trained++;
        }
        
        plugin.getLogger().info("Learned from " + trained + " player behavior patterns");
        saveMLBrain();
        return trained;
    }
    
    public List<String> exportTrainingData() {
        List<String> exported = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : learnedCategories.entrySet()) {
            exported.add(entry.getKey() + " -> " + entry.getValue());
        }
        
        return exported;
    }
    
    public void pruneOldData(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        lastActionTime.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }
    
    public ActionType inferActionTypeFromContext(String context) {
        String lower = context.toLowerCase();
        
        if (lower.contains("furnace") || lower.contains("smoker") || lower.contains("blast")) {
            return ActionType.SMELT_ITEM;
        }
        if (lower.contains("anvil")) return ActionType.USE_ANVIL;
        if (lower.contains("grindstone")) return ActionType.USE_GRINDSTONE;
        if (lower.contains("stonecutter")) return ActionType.USE_STONECUTTER;
        if (lower.contains("loom")) return ActionType.USE_LOOM;
        if (lower.contains("smithing")) return ActionType.USE_SMITHING_TABLE;
        if (lower.contains("enchant")) return ActionType.ENCHANT_ITEM;
        if (lower.contains("brew")) return ActionType.BREW_POTION;
        if (lower.contains("craft")) return ActionType.CRAFT_ITEM;
        
        return ActionType.INTERACT;
    }
    
    public record ActionResult(String skillId, int xpAmount, double importance) {}
    
    private record TrainingExample(String context, String actionType, String tool, String category) {}
    
    public record ActionStats(int count, int totalXp, int minXp, int maxXp) {
        public double averageXp() {
            return count > 0 ? (double) totalXp / count : 0.0;
        }
    }
    
    private static class CategoryData {
        int exampleCount = 0;
        int totalFeatures = 0;
        Map<String, Integer> featureCounts = new ConcurrentHashMap<>();
    }
    
    public enum ActionType {
        MINE_BLOCK(1),
        BREAK_BLOCK(1),
        PLACE_BLOCK(1),
        HARVEST_CROP(1),
        PLANT_CROP(1),
        BREED_ANIMAL(2),
        KILL_MOB(2),
        CRAFT_ITEM(1),
        SMELT_ITEM(1),
        BREW_POTION(2),
        ENCHANT_ITEM(2),
        USE_ANVIL(2),
        USE_GRINDSTONE(1),
        USE_STONECUTTER(1),
        USE_LOOM(1),
        USE_SMITHING_TABLE(2),
        FISH(1),
        TRADE(2),
        USE_TOOL(1),
        INTERACT(1);
        
        public final int baseXp;
        
        ActionType(int baseXp) {
            this.baseXp = baseXp;
        }
    }
    
    public static String materialToContext(Material material) {
        return material.name().toLowerCase().replace("_ore", "").replace("deepslate_", "");
    }
    
    public static String entityToContext(EntityType entity) {
        return entity.name().toLowerCase();
    }
}
