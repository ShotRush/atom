package org.shotrush.atom.advancement;

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

import java.util.HashMap;
import java.util.Map;

public final class AdvancementGenerator {
    
    private final Plugin plugin;
    private final XpEngine xpEngine;
    private final Map<String, NamespacedKey> skillAdvancementKeys;
    private final org.shotrush.atom.config.AtomConfig config;
    
    public AdvancementGenerator(Plugin plugin, XpEngine xpEngine, org.shotrush.atom.config.AtomConfig config) {
        this.plugin = plugin;
        this.xpEngine = xpEngine;
        this.skillAdvancementKeys = new HashMap<>();
        this.config = config;
    }
    
    public void generateAdvancementsForTree(SkillTree tree) {
        generateNodeAdvancement(tree.root(), null);
        plugin.getLogger().info("Generated " + skillAdvancementKeys.size() + " advancements for tree: " + tree.name());
    }
    
    public void generateMilestoneAdvancements(java.util.List<org.shotrush.atom.milestone.Milestone> milestones) {
        int count = 0;
        for (org.shotrush.atom.milestone.Milestone milestone : milestones) {
            generateMilestoneAdvancement(milestone);
            count++;
        }
        plugin.getLogger().info("Generated " + count + " milestone advancements");
    }
    
    private void generateMilestoneAdvancement(org.shotrush.atom.milestone.Milestone milestone) {
        String path = "milestones/" + milestone.id();
        NamespacedKey key = new NamespacedKey(plugin, path);
        
        String parentPath = milestone.skillId().replace(".", "/");
        String icon = getMaterialForMilestone(milestone.skillId());
        String frame = getFrameForMilestone(milestone);
        
        String json = buildAdvancementJson(
            icon,
            "§6" + milestone.displayName(),
            milestone.description() + " (" + (int)milestone.requiredLevel() + "% progress)",
            frame,
            "atom:" + parentPath,
            true,
            true
        );
        
        loadAdvancement(key, json);
        skillAdvancementKeys.put("milestones/" + milestone.id(), key);
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
        String path = node.id().replace(".", "/");
        NamespacedKey key = new NamespacedKey(plugin, path);
        
        String icon = getMaterialForNode(node).getKey().toString();
        String title = formatTitle(node.id());
        String description = generateDescription(node);
        String frame = getFrameType(node);
        String parent = parentId != null ? "atom:" + parentId.replace(".", "/") : null;
        boolean isRoot = parentId == null;
        
        String json = buildAdvancementJson(icon, title, description, frame, parent, true, true, isRoot);
        
        loadAdvancement(key, json);
        skillAdvancementKeys.put(node.id(), key);
        
        for (SkillNode child : node.children().values()) {
            generateNodeAdvancement(child, node.id());
        }
    }
    
    @SuppressWarnings("deprecation")
    private void loadAdvancement(NamespacedKey key, String json) {
        try {
            Bukkit.getUnsafe().loadAdvancement(key, json);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load advancement " + key + ": " + e.getMessage());
        }
    }
    
    private String buildAdvancementJson(String icon, String title, String description, String frame, 
                                       String parent, boolean showToast, boolean announceToChat) {
        return buildAdvancementJson(icon, title, description, frame, parent, showToast, announceToChat, false);
    }
    
    private String buildAdvancementJson(String icon, String title, String description, String frame, 
                                       String parent, boolean showToast, boolean announceToChat, boolean isRoot) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"criteria\": {\n");
        json.append("    \"trigger\": {\n");
        json.append("      \"trigger\": \"minecraft:impossible\"\n");
        json.append("    }\n");
        json.append("  },\n");
        json.append("  \"display\": {\n");
        json.append("    \"icon\": {\n");
        json.append("      \"id\": \"").append(icon).append("\"\n");
        json.append("    },\n");
        json.append("    \"title\": {\n");
        json.append("      \"text\": \"").append(escapeJson(title)).append("\"\n");
        json.append("    },\n");
        json.append("    \"description\": {\n");
        json.append("      \"text\": \"").append(escapeJson(description)).append("\"\n");
        json.append("    },\n");
        
        if (isRoot) {
            json.append("    \"background\": \"minecraft:textures/block/deepslate.png\",\n");
        }
        
        json.append("    \"frame\": \"").append(frame).append("\",\n");
        json.append("    \"announce_to_chat\": ").append(announceToChat).append(",\n");
        json.append("    \"show_toast\": ").append(showToast).append(",\n");
        json.append("    \"hidden\": false\n");
        json.append("  }");
        
        if (parent != null) {
            json.append(",\n  \"parent\": \"").append(parent).append("\"");
        }
        
        json.append(",\n  \"requirements\": [\n");
        json.append("    [\"trigger\"]\n");
        json.append("  ]\n");
        json.append("}");
        
        return json.toString();
    }
    
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
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
    
    public void updatePlayerAdvancements(Player player, PlayerSkillData data, SkillTree tree) {
        updateNodeAdvancements(player, data, tree.root());
    }
    
    private void updateNodeAdvancements(Player player, PlayerSkillData data, SkillNode node) {
        NamespacedKey key = skillAdvancementKeys.get(node.id());
        if (key == null) {
            System.out.println("[Advancement Debug] No key found for skill: " + node.id());
            return;
        }
        
        Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) {
            System.out.println("[Advancement Debug] Advancement not registered for key: " + key + " (skill: " + node.id() + ")");
            return;
        }
        
        EffectiveXp effectiveXp = xpEngine.getEffectiveXp(data, node.id());
        double progress = effectiveXp.progressPercent();
        
        var advancementProgress = player.getAdvancementProgress(advancement);
        boolean wasCompleted = advancementProgress.isDone();
        
        double threshold = config.advancementGrantThreshold();
        boolean shouldBeGranted = progress >= threshold;
        
        if (shouldBeGranted && !wasCompleted) {
            System.out.println("[Advancement Grant] " + player.getName() + " unlocked '" + node.id() + 
                "' (" + effectiveXp.intrinsicXp() + "+" + effectiveXp.honoraryXp() + " XP, " + 
                String.format("%.1f%%", progress * 100) + ")");
            advancementProgress.awardCriteria("trigger");
        } else if (!shouldBeGranted && wasCompleted) {
            System.out.println("[Advancement Revoke] " + player.getName() + " lost '" + node.id() + 
                "' (now " + String.format("%.1f%%", progress * 100) + ")");
            advancementProgress.revokeCriteria("trigger");
        }
        
        for (SkillNode child : node.children().values()) {
            updateNodeAdvancements(player, data, child);
        }
    }
    
    public void clearPlayerAdvancements(Player player) {
        for (NamespacedKey key : skillAdvancementKeys.values()) {
            Advancement advancement = Bukkit.getAdvancement(key);
            if (advancement != null) {
                var progress = player.getAdvancementProgress(advancement);
                progress.revokeCriteria("trigger");
            }
        }
    }
}
