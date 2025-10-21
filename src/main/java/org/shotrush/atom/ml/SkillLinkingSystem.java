package org.shotrush.atom.ml;

import org.bukkit.plugin.Plugin;
import org.shotrush.atom.advancement.AdvancementGenerator;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;
import org.shotrush.atom.tree.SkillTreeRegistry;

import java.util.*;
import java.util.stream.Collectors;


public final class SkillLinkingSystem {
    
    private static final int MAX_LINKS_PER_PLAYER = 4;
    private static final int MAX_SKILLS_PER_LINK = 5;
    private static final double MASTERY_THRESHOLD = 0.95; 
    
    private final Plugin plugin;
    private final SkillTreeRegistry treeRegistry;
    private final AdvancementGenerator advancementGenerator;
    private final Map<UUID, List<SkillLink>> playerLinks;
    private final Map<String, SkillNode> generatedLinkNodes;
    
    public SkillLinkingSystem(Plugin plugin, SkillTreeRegistry treeRegistry, AdvancementGenerator advancementGenerator) {
        this.plugin = plugin;
        this.treeRegistry = treeRegistry;
        this.advancementGenerator = advancementGenerator;
        this.playerLinks = new HashMap<>();
        this.generatedLinkNodes = new HashMap<>();
    }
    
    
    public List<PotentialLink> discoverPotentialLinks(
        PlayerSkillData playerData,
        Map<String, Double> effectiveXpMap
    ) {
        List<String> masteredLeafSkills = findMasteredLeafSkills(effectiveXpMap);
        
        if (masteredLeafSkills.size() < 2) {
            return List.of();
        }
        
        List<PotentialLink> potentialLinks = new ArrayList<>();
        
        
        for (int linkSize = 2; linkSize <= Math.min(MAX_SKILLS_PER_LINK, masteredLeafSkills.size()); linkSize++) {
            List<List<String>> combinations = generateCombinations(masteredLeafSkills, linkSize);
            
            for (List<String> skillCombo : combinations) {
                double compatibility = calculateSkillCompatibility(skillCombo);
                
                if (compatibility > 0.6) { 
                    String linkName = generateLinkName(skillCombo);
                    String linkDescription = generateLinkDescription(skillCombo);
                    
                    potentialLinks.add(new PotentialLink(
                        UUID.randomUUID().toString(),
                        linkName,
                        linkDescription,
                        skillCombo,
                        compatibility,
                        calculateLinkBonuses(skillCombo)
                    ));
                }
            }
        }
        
        
        potentialLinks.sort((a, b) -> Double.compare(b.compatibility(), a.compatibility()));
        
        return potentialLinks.stream().limit(10).collect(Collectors.toList());
    }
    
    
    public boolean createSkillLink(UUID playerId, PotentialLink potentialLink) {
        List<SkillLink> currentLinks = playerLinks.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        if (currentLinks.size() >= MAX_LINKS_PER_PLAYER) {
            return false;
        }
        
        SkillLink newLink = new SkillLink(
            potentialLink.id(),
            potentialLink.name(),
            potentialLink.description(),
            potentialLink.linkedSkills(),
            potentialLink.bonuses(),
            System.currentTimeMillis()
        );
        
        currentLinks.add(newLink);
        
        
        createLinkNode(newLink);
        
        return true;
    }
    
    
    private void createLinkNode(SkillLink link) {
        String linkNodeId = "link_" + link.id();
        
        
        if (generatedLinkNodes.containsKey(linkNodeId)) {
            return;
        }
        
        
        List<SkillNode> linkedNodes = new ArrayList<>();
        for (String skillId : link.linkedSkills()) {
            Optional<SkillNode> nodeOpt = treeRegistry.findNode(skillId);
            nodeOpt.ifPresent(linkedNodes::add);
        }
        
        if (linkedNodes.isEmpty()) {
            return;
        }
        
        
        SkillNode commonParent = findCommonAncestor(linkedNodes);
        if (commonParent == null) {
            
            commonParent = linkedNodes.get(0).parent().orElse(linkedNodes.get(0));
        }
        
        
        SkillNode linkNode = SkillNode.builder()
            .id(linkNodeId)
            .displayName(link.name())
            .maxXp((int) calculateLinkNodeXp(linkedNodes))
            .type(SkillNode.NodeType.BRANCH)
            .parent(commonParent)
            .build();
        
        generatedLinkNodes.put(linkNodeId, linkNode);
        
        
        generateLinkAdvancement(linkNode, link);
        
        plugin.getLogger().info("Created link node: " + link.name() + " (branches from " + 
            link.linkedSkills().size() + " skills)");
    }
    
    
    private SkillNode findCommonAncestor(List<SkillNode> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }
        
        if (nodes.size() == 1) {
            return nodes.get(0).parent().orElse(null);
        }
        
        
        Set<String> ancestors = new HashSet<>();
        SkillNode current = nodes.get(0);
        while (current != null) {
            ancestors.add(current.id());
            current = current.parent().orElse(null);
        }
        
        
        for (int i = 1; i < nodes.size(); i++) {
            current = nodes.get(i);
            while (current != null) {
                if (ancestors.contains(current.id())) {
                    return current;
                }
                current = current.parent().orElse(null);
            }
        }
        
        return null;
    }
    
    
    private long calculateLinkNodeXp(List<SkillNode> linkedNodes) {
        long totalXp = 0;
        for (SkillNode node : linkedNodes) {
            totalXp += node.maxXp();
        }
        
        return totalXp;
    }
    
    
    private void generateLinkAdvancement(SkillNode linkNode, SkillLink link) {
        
        
        plugin.getLogger().info("Generated advancement for link: " + link.name());
    }
    
    
    public boolean removeSkillLink(UUID playerId, String linkId) {
        List<SkillLink> links = playerLinks.get(playerId);
        if (links == null) {
            return false;
        }
        
        return links.removeIf(link -> link.id().equals(linkId));
    }
    
    
    public List<SkillLink> getPlayerLinks(UUID playerId) {
        return playerLinks.getOrDefault(playerId, List.of());
    }
    
    
    public void validatePlayerLinks(UUID playerId, Map<String, Double> effectiveXpMap) {
        List<SkillLink> links = playerLinks.get(playerId);
        if (links == null) {
            return;
        }
        
        links.removeIf(link -> {
            for (String skillId : link.linkedSkills()) {
                Double progress = effectiveXpMap.get(skillId);
                if (progress == null || progress < MASTERY_THRESHOLD) {
                    return true; 
                }
            }
            return false;
        });
    }
    
    
    
    private List<String> findMasteredLeafSkills(Map<String, Double> effectiveXpMap) {
        List<String> masteredLeafs = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : effectiveXpMap.entrySet()) {
            String skillId = entry.getKey();
            Double progress = entry.getValue();
            
            if (progress >= MASTERY_THRESHOLD) {
                Optional<SkillNode> nodeOpt = treeRegistry.findNode(skillId);
                if (nodeOpt.isPresent() && nodeOpt.get().isLeaf()) {
                    masteredLeafs.add(skillId);
                }
            }
        }
        
        return masteredLeafs;
    }
    
    private double calculateSkillCompatibility(List<String> skills) {
        if (skills.size() < 2) {
            return 0.0;
        }
        
        
        Set<String> rootClusters = new HashSet<>();
        for (String skillId : skills) {
            String[] parts = skillId.split("\\.");
            if (parts.length > 0) {
                rootClusters.add(parts[0]);
            }
        }
        
        
        double clusterDiversity = (double) rootClusters.size() / skills.size();
        
        
        double semanticSimilarity = calculateSemanticSimilarity(skills);
        
        
        return (clusterDiversity * 0.4) + (semanticSimilarity * 0.6);
    }
    
    private double calculateSemanticSimilarity(List<String> skills) {
        
        Set<String> materials = new HashSet<>();
        Set<String> actions = new HashSet<>();
        
        for (String skillId : skills) {
            String[] parts = skillId.split("\\.");
            if (parts.length >= 3) {
                materials.add(parts[2]);
            }
            if (parts.length >= 4) {
                actions.add(parts[parts.length - 1]);
            }
        }
        
        
        double materialOverlap = materials.size() < skills.size() ? 0.5 : 0.0;
        double actionOverlap = actions.size() < skills.size() ? 0.5 : 0.0;
        
        return materialOverlap + actionOverlap;
    }
    
    private String generateLinkName(List<String> skills) {
        if (skills.isEmpty()) {
            return "Unknown Specialization";
        }
        
        
        Set<String> clusters = skills.stream()
            .map(s -> s.split("\\.")[0])
            .collect(Collectors.toSet());
        
        if (clusters.size() == 1) {
            
            String cluster = clusters.iterator().next();
            return capitalize(cluster) + " Master";
        } else {
            
            List<String> clusterNames = clusters.stream()
                .map(this::capitalize)
                .sorted()
                .collect(Collectors.toList());
            
            return String.join("-", clusterNames) + " Specialist";
        }
    }
    
    private String generateLinkDescription(List<String> skills) {
        Set<String> clusters = skills.stream()
            .map(s -> s.split("\\.")[0])
            .map(this::capitalize)
            .collect(Collectors.toSet());
        
        return "Mastery of " + String.join(", ", clusters) + " skills combined";
    }
    
    private Map<String, Double> calculateLinkBonuses(List<String> skills) {
        Map<String, Double> bonuses = new HashMap<>();
        
        
        double baseBonus = 0.05 * skills.size(); 
        
        bonuses.put("xp_multiplier", 1.0 + baseBonus);
        bonuses.put("efficiency_bonus", baseBonus);
        bonuses.put("quality_bonus", baseBonus * 0.5);
        
        return bonuses;
    }
    
    private <T> List<List<T>> generateCombinations(List<T> items, int size) {
        List<List<T>> combinations = new ArrayList<>();
        generateCombinationsHelper(items, size, 0, new ArrayList<>(), combinations);
        return combinations;
    }
    
    private <T> void generateCombinationsHelper(
        List<T> items,
        int size,
        int start,
        List<T> current,
        List<List<T>> result
    ) {
        if (current.size() == size) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < items.size(); i++) {
            current.add(items.get(i));
            generateCombinationsHelper(items, size, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    
    
    
    public record PotentialLink(
        String id,
        String name,
        String description,
        List<String> linkedSkills,
        double compatibility,
        Map<String, Double> bonuses
    ) {}
    
    
    public record SkillLink(
        String id,
        String name,
        String description,
        List<String> linkedSkills,
        Map<String, Double> bonuses,
        long createdAt
    ) {}
}
