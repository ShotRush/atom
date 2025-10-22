package org.shotrush.atom.ml;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public final class RecipeProgressionML {
    
    private final Plugin plugin;
    private final Map<Material, RecipeNode> recipeGraph;
    private final Map<Material, Set<Material>> prerequisites;
    private final Map<Material, Double> complexityScores;
    private final Map<String, Set<Material>> categoryRecipes;
    
    public RecipeProgressionML(Plugin plugin) {
        this.plugin = plugin;
        this.recipeGraph = new ConcurrentHashMap<>();
        this.prerequisites = new ConcurrentHashMap<>();
        this.complexityScores = new ConcurrentHashMap<>();
        this.categoryRecipes = new ConcurrentHashMap<>();
        
        initializeRecipeGraph();
        calculateComplexityScores();
        clusterRecipesByCategory();
    }
    
    
    private void initializeRecipeGraph() {
        for (Material material : Material.values()) {
            if (!material.isItem()) continue;
            
            RecipeNode node = new RecipeNode(material);
            Set<Material> deps = inferPrerequisites(material);
            node.prerequisites.addAll(deps);
            prerequisites.put(material, deps);
            
            recipeGraph.put(material, node);
        }
        
        plugin.getLogger().info("[RecipeProgressionML] Initialized recipe graph with " + 
            recipeGraph.size() + " nodes");
    }
    
    
    private Set<Material> inferPrerequisites(Material material) {
        Set<Material> deps = new HashSet<>();
        String name = material.name().toLowerCase();
        if (name.contains("stone") && !name.contains("blackstone")) {
            deps.add(Material.WOODEN_PICKAXE);
        } else if (name.contains("iron")) {
            deps.add(Material.STONE_PICKAXE);
            if (name.endsWith("_pickaxe") || name.endsWith("_axe") || 
                name.endsWith("_shovel") || name.endsWith("_hoe") || name.endsWith("_sword")) {
                deps.add(Material.FURNACE);
            }
        } else if (name.contains("gold")) {
            deps.add(Material.IRON_PICKAXE);
        } else if (name.contains("diamond")) {
            deps.add(Material.IRON_PICKAXE);
        } else if (name.contains("netherite")) {
            deps.add(Material.DIAMOND_PICKAXE);
            deps.add(Material.SMITHING_TABLE);
        }
        if (name.endsWith("_pickaxe")) {
            if (name.startsWith("stone")) {
                deps.add(Material.WOODEN_PICKAXE);
            } else if (name.startsWith("iron")) {
                deps.add(Material.STONE_PICKAXE);
            } else if (name.startsWith("diamond")) {
                deps.add(Material.IRON_PICKAXE);
            }
        }
        if (name.contains("enchant")) {
            deps.add(Material.BOOKSHELF);
            deps.add(Material.DIAMOND_PICKAXE);
        }
        
        if (name.contains("anvil")) {
            deps.add(Material.IRON_BLOCK);
        }
        
        if (name.contains("brewing")) {
            deps.add(Material.BLAZE_ROD);
        }
        
        return deps;
    }
    
    
    private void calculateComplexityScores() {
        for (Map.Entry<Material, RecipeNode> entry : recipeGraph.entrySet()) {
            Material material = entry.getKey();
            RecipeNode node = entry.getValue();
            
            double score = 0.0;
            score += node.prerequisites.size() * 0.3;
            String name = material.name().toLowerCase();
            if (name.contains("wooden")) score += 0.1;
            else if (name.contains("stone")) score += 0.2;
            else if (name.contains("iron")) score += 0.4;
            else if (name.contains("gold")) score += 0.5;
            else if (name.contains("diamond")) score += 0.7;
            else if (name.contains("netherite")) score += 1.0;
            if (name.contains("enchant")) score += 0.5;
            if (name.contains("brewing")) score += 0.4;
            if (name.contains("anvil")) score += 0.3;
            if (name.contains("smithing")) score += 0.3;
            score = Math.min(1.0, score / 2.0);
            
            complexityScores.put(material, score);
        }
        
        plugin.getLogger().info("[RecipeProgressionML] Calculated complexity scores for " + 
            complexityScores.size() + " recipes");
    }
    
    
    private void clusterRecipesByCategory() {
        String[] categories = {"wood_crafting", "stone_crafting", "iron_crafting", 
                               "diamond_crafting", "netherite_crafting", "farming", 
                               "brewing", "enchanting", "building"};
        
        for (String category : categories) {
            categoryRecipes.put(category, ConcurrentHashMap.newKeySet());
        }
        
        int totalCategorized = 0;
        for (Material material : recipeGraph.keySet()) {
            String name = material.name().toLowerCase();
            boolean categorized = false;
            
            if (name.startsWith("wooden_") || name.contains("oak_") || name.contains("planks")) {
                categoryRecipes.get("wood_crafting").add(material);
                categorized = true;
            }
            if (name.startsWith("stone_") || name.contains("cobblestone")) {
                categoryRecipes.get("stone_crafting").add(material);
                categorized = true;
            }
            if (name.startsWith("iron_")) {
                categoryRecipes.get("iron_crafting").add(material);
                categorized = true;
            }
            if (name.startsWith("diamond_")) {
                categoryRecipes.get("diamond_crafting").add(material);
                categorized = true;
            }
            if (name.startsWith("netherite_")) {
                categoryRecipes.get("netherite_crafting").add(material);
                categorized = true;
            }
            if (name.contains("hoe") || name.contains("seeds") || name.contains("composter")) {
                categoryRecipes.get("farming").add(material);
                categorized = true;
            }
            if (name.contains("brewing") || name.contains("potion") || name.contains("cauldron")) {
                categoryRecipes.get("brewing").add(material);
                categorized = true;
            }
            if (name.contains("enchant") || name.contains("book")) {
                categoryRecipes.get("enchanting").add(material);
                categorized = true;
            }
            if (name.contains("brick") || name.contains("stairs") || name.contains("slab")) {
                categoryRecipes.get("building").add(material);
                categorized = true;
            }
            
            if (categorized) totalCategorized++;
        }
        
        plugin.getLogger().info("[RecipeProgressionML] Clustered " + totalCategorized + " recipes into " + 
            categories.length + " categories");
        for (String category : categories) {
            int size = categoryRecipes.get(category).size();
            if (size > 0) {
                plugin.getLogger().info("[RecipeProgressionML]   - " + category + ": " + size + " recipes");
            }
        }
    }
    
    
    public List<RecipeUnlock> getNextRecipes(Material crafted) {
        List<RecipeUnlock> unlocks = new ArrayList<>();
        
        RecipeNode node = recipeGraph.get(crafted);
        if (node == null) return unlocks;
        for (Map.Entry<Material, RecipeNode> entry : recipeGraph.entrySet()) {
            Material candidate = entry.getKey();
            RecipeNode candidateNode = entry.getValue();
            
            if (candidateNode.prerequisites.contains(crafted)) {
                double complexity = complexityScores.getOrDefault(candidate, 0.5);
                unlocks.add(new RecipeUnlock(candidate, complexity, 
                    "Unlocked by crafting " + formatMaterialName(crafted)));
            }
        }
        unlocks.sort(Comparator.comparingDouble(RecipeUnlock::complexity));
        
        return unlocks;
    }
    
    
    public List<Material> getRecipesByCategory(String category) {
        Set<Material> recipes = categoryRecipes.getOrDefault(category, Set.of());
        
        return recipes.stream()
            .sorted(Comparator.comparingDouble(m -> complexityScores.getOrDefault(m, 0.5)))
            .collect(Collectors.toList());
    }
    
    
    public String getSkillCategoryForRecipe(Material material) {
        String name = material.name().toLowerCase();
        if (name.endsWith("_pickaxe") || name.endsWith("_axe") || 
            name.endsWith("_shovel") || name.endsWith("_hoe") || name.endsWith("_sword")) {
            return "blacksmith.tool_crafting";
        }
        if (name.endsWith("_helmet") || name.endsWith("_chestplate") || 
            name.endsWith("_leggings") || name.endsWith("_boots")) {
            return "blacksmith.armor_crafting";
        }
        if (name.contains("pickaxe")) {
            return "miner.tools";
        }
        if (name.contains("hoe") || name.contains("composter")) {
            return "farmer.tools";
        }
        if (name.contains("brick") || name.contains("stairs") || name.contains("slab")) {
            return "builder.materials";
        }
        if (name.contains("enchant") || name.contains("bookshelf")) {
            return "librarian.enchanting";
        }
        if (name.contains("brewing") || name.contains("potion")) {
            return "healer.brewing";
        }
        
        return "general";
    }
    
    
    public double getComplexity(Material material) {
        return complexityScores.getOrDefault(material, 0.5);
    }
    
    
    public Set<Material> getPrerequisites(Material material) {
        return new HashSet<>(prerequisites.getOrDefault(material, Set.of()));
    }
    
    
    public boolean shouldUnlockRecipe(Material recipe, Set<Material> craftedItems, 
                                     Map<String, Double> skillProgress) {
        Set<Material> prereqs = getPrerequisites(recipe);
        if (!craftedItems.containsAll(prereqs)) {
            return false;
        }
        String skillCategory = getSkillCategoryForRecipe(recipe);
        double requiredProgress = getComplexity(recipe);
        double currentProgress = skillProgress.getOrDefault(skillCategory, 0.0);
        
        return currentProgress >= requiredProgress * 0.5;
    }
    
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        return Arrays.stream(name.split(" "))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
    }
    
    
    private static class RecipeNode {
        final Material material;
        final Set<Material> prerequisites;
        
        RecipeNode(Material material) {
            this.material = material;
            this.prerequisites = ConcurrentHashMap.newKeySet();
        }
    }
    
    
    public record RecipeUnlock(
        Material recipe,
        double complexity,
        String unlockReason
    ) {}
}
