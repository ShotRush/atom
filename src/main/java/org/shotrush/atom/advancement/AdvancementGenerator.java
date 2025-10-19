package org.shotrush.atom.advancement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.engine.XpEngine;
import org.shotrush.atom.model.EffectiveXp;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;
import org.shotrush.atom.tree.SkillTree;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class AdvancementGenerator {
    
    private final Plugin plugin;
    private final XpEngine xpEngine;
    private final Map<String, NamespacedKey> skillAdvancementKeys;
    private final Gson gson;
    private final File advancementFolder;
    
    public AdvancementGenerator(Plugin plugin, XpEngine xpEngine) {
        this.plugin = plugin;
        this.xpEngine = xpEngine;
        this.skillAdvancementKeys = new HashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        File worldFolder = Bukkit.getWorlds().get(0).getWorldFolder();
        this.advancementFolder = new File(worldFolder, "datapacks/atom/data/atom/advancements");
        advancementFolder.mkdirs();
    }
    
    public void generateAdvancementsForTree(SkillTree tree) {
        generateNodeAdvancement(tree.root(), null);
    }
    
    public void generateMilestoneAdvancements(java.util.List<org.shotrush.atom.milestone.Milestone> milestones) {
        for (org.shotrush.atom.milestone.Milestone milestone : milestones) {
            generateMilestoneAdvancement(milestone);
        }
    }
    
    private void generateMilestoneAdvancement(org.shotrush.atom.milestone.Milestone milestone) {
        JsonObject advancement = new JsonObject();
        
        JsonObject display = new JsonObject();
        
        JsonObject icon = new JsonObject();
        icon.addProperty("item", getMaterialForMilestone(milestone.skillId()));
        display.add("icon", icon);
        
        JsonObject title = new JsonObject();
        title.addProperty("text", "§6" + milestone.displayName());
        display.add("title", title);
        
        JsonObject description = new JsonObject();
        description.addProperty("text", milestone.description() + " (" + (int)milestone.requiredLevel() + "% progress)");
        display.add("description", description);
        
        display.addProperty("frame", getFrameForMilestone(milestone));
        display.addProperty("show_toast", true);
        display.addProperty("announce_to_chat", true);
        
        advancement.add("display", display);
        
        String parentSkillId = milestone.skillId();
        advancement.addProperty("parent", "atom:" + parentSkillId.replace(".", "/"));
        
        JsonObject criteria = new JsonObject();
        JsonObject impossibleCriteria = new JsonObject();
        impossibleCriteria.addProperty("trigger", "minecraft:impossible");
        criteria.add("impossible", impossibleCriteria);
        advancement.add("criteria", criteria);
        
        saveAdvancement("milestones/" + milestone.id(), advancement);
    }
    
    private String getFrameForMilestone(org.shotrush.atom.milestone.Milestone milestone) {
        if (milestone.requiredLevel() >= 75.0) {
            return "challenge";
        } else if (milestone.requiredLevel() >= 50.0) {
            return "goal";
        }
        return "task";
    }
    
    private String getMaterialForMilestone(String skillId) {
        String id = skillId.toLowerCase();
        
        if (id.contains("miner")) {
            return "minecraft:diamond_pickaxe";
        } else if (id.contains("guardsman")) {
            return "minecraft:diamond_sword";
        } else if (id.contains("farmer")) {
            return "minecraft:golden_hoe";
        } else if (id.contains("blacksmith")) {
            return "minecraft:anvil";
        } else if (id.contains("builder")) {
            return "minecraft:bricks";
        }
        
        return "minecraft:nether_star";
    }
    
    private void generateNodeAdvancement(SkillNode node, String parentId) {
        JsonObject advancement = new JsonObject();
        
        JsonObject display = createDisplay(node, parentId == null);
        advancement.add("display", display);
        
        if (parentId != null) {
            advancement.addProperty("parent", "atom:" + parentId.replace(".", "/"));
        }
        
        JsonObject criteria = new JsonObject();
        JsonObject impossibleCriteria = new JsonObject();
        impossibleCriteria.addProperty("trigger", "minecraft:impossible");
        criteria.add("impossible", impossibleCriteria);
        advancement.add("criteria", criteria);
        
        saveAdvancement(node.id(), advancement);
        
        for (SkillNode child : node.children().values()) {
            generateNodeAdvancement(child, node.id());
        }
    }
    
    private JsonObject createDisplay(SkillNode node, boolean isRoot) {
        JsonObject display = new JsonObject();
        
        JsonObject icon = new JsonObject();
        icon.addProperty("item", getMaterialForNode(node).getKey().toString());
        display.add("icon", icon);
        
        JsonObject title = new JsonObject();
        title.addProperty("text", formatTitle(node.id()));
        display.add("title", title);
        
        JsonObject description = new JsonObject();
        description.addProperty("text", generateDescription(node));
        display.add("description", description);
        
        display.addProperty("frame", getFrameType(node));
        display.addProperty("show_toast", true);
        display.addProperty("announce_to_chat", true);
        
        if (isRoot) {
            display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/stone.png");
        }
        
        return display;
    }
    
    private String getFrameType(SkillNode node) {
        int depth = node.depth();
        
        if (depth == 0) {
            return "task";
        } else if (depth == 1) {
            return "task";
        } else if (depth == 2) {
            return "goal";
        } else {
            return "challenge";
        }
    }
    
    private Material getMaterialForNode(SkillNode node) {
        String id = node.id().toLowerCase();
        
        if (id.contains("miner") || id.contains("mining")) {
            if (id.contains("diamond")) return Material.DIAMOND_ORE;
            if (id.contains("gold")) return Material.GOLD_ORE;
            if (id.contains("iron")) return Material.IRON_ORE;
            if (id.contains("coal")) return Material.COAL_ORE;
            if (id.contains("ore")) return Material.IRON_PICKAXE;
            return Material.STONE_PICKAXE;
        }
        
        if (id.contains("guardsman") || id.contains("combat")) {
            if (id.contains("defense")) return Material.SHIELD;
            if (id.contains("zombie")) return Material.ROTTEN_FLESH;
            if (id.contains("skeleton")) return Material.BONE;
            if (id.contains("spider")) return Material.STRING;
            return Material.IRON_SWORD;
        }
        
        if (id.contains("farmer") || id.contains("farming")) {
            if (id.contains("wheat")) return Material.WHEAT;
            if (id.contains("carrot")) return Material.CARROT;
            if (id.contains("potato")) return Material.POTATO;
            if (id.contains("animal")) return Material.WHEAT_SEEDS;
            return Material.IRON_HOE;
        }
        
        if (id.contains("blacksmith")) {
            if (id.contains("armor")) return Material.IRON_CHESTPLATE;
            if (id.contains("tool")) return Material.IRON_PICKAXE;
            return Material.ANVIL;
        }
        
        if (id.contains("builder")) {
            return Material.BRICKS;
        }
        
        if (id.contains("healer")) {
            return Material.POTION;
        }
        
        if (id.contains("librarian")) {
            return Material.ENCHANTED_BOOK;
        }
        
        return Material.BOOK;
    }
    
    private String generateDescription(SkillNode node) {
        int depth = node.depth();
        String id = node.id().toLowerCase();
        
        if (depth == 0) {
            return "Begin your journey as a " + node.displayName();
        } else if (depth == 1) {
            return "Choose the path of " + node.displayName();
        } else if (depth == 2) {
            return "Specialize in " + node.displayName();
        } else {
            return "Master the art of " + node.displayName();
        }
    }
    
    private String formatTitle(String skillId) {
        String[] parts = skillId.split("\\.");
        String lastPart = parts[parts.length - 1];
        
        StringBuilder title = new StringBuilder();
        String[] words = lastPart.split("_");
        for (String word : words) {
            if (!title.isEmpty()) title.append(" ");
            title.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        
        int depth = parts.length - 1;
        if (depth == 0) {
            return "§6" + title + " (Root)";
        } else if (depth == 1) {
            return "§e" + title + " (Class)";
        } else if (depth == 2) {
            return "§a" + title + " (Specialization)";
        } else {
            return "§d" + title + " (Master)";
        }
    }
    
    private void saveAdvancement(String skillId, JsonObject advancement) {
        String path = skillId.replace(".", "/");
        File file = new File(advancementFolder, path + ".json");
        file.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(advancement, writer);
            NamespacedKey key = NamespacedKey.fromString("atom:" + path);
            if (key != null) {
                skillAdvancementKeys.put(skillId, key);
            }
            plugin.getLogger().info("Generated advancement: atom:" + path + " at " + file.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save advancement for " + skillId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void updatePlayerAdvancements(Player player, PlayerSkillData data, SkillTree tree) {
        updateNodeAdvancements(player, data, tree.root());
    }
    
    private void updateNodeAdvancements(Player player, PlayerSkillData data, SkillNode node) {
        NamespacedKey key = getOrCreateKey(node.id());
        Advancement advancement = Bukkit.getAdvancement(key);
        
        if (advancement != null) {
            EffectiveXp effectiveXp = xpEngine.getEffectiveXp(data, node.id());
            double progress = effectiveXp.progressPercent();
            
            var advancementProgress = player.getAdvancementProgress(advancement);
            
            if (progress >= 1.0) {
                for (String criteria : advancement.getCriteria()) {
                    advancementProgress.awardCriteria(criteria);
                }
            }
        }
        
        for (SkillNode child : node.children().values()) {
            updateNodeAdvancements(player, data, child);
        }
    }
    
    private NamespacedKey getOrCreateKey(String skillId) {
        return skillAdvancementKeys.computeIfAbsent(skillId, 
            id -> NamespacedKey.fromString("atom:" + id.replace(".", "/")));
    }
    
    public void clearPlayerAdvancements(Player player) {
        for (NamespacedKey key : skillAdvancementKeys.values()) {
            Advancement advancement = Bukkit.getAdvancement(key);
            if (advancement != null) {
                var progress = player.getAdvancementProgress(advancement);
                for (String criteria : advancement.getCriteria()) {
                    progress.revokeCriteria(criteria);
                }
            }
        }
    }
}
