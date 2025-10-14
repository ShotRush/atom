package org.shotrush.atom.manager;

import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.config.SocialSystemsConfig;

import java.util.*;

public class CollectiveActionManager {
    private final Atom plugin;
    private final SocialSystemsConfig config;
    private final Map<String, PublicProject> activeProjects;

    public CollectiveActionManager(Atom plugin, SocialSystemsConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.activeProjects = new HashMap<>();
    }
    
    public boolean isEnabled() {
        return config.isCollectiveActionEnabled();
    }

    public void createProject(String name, int requiredContributions, String reward) {
        activeProjects.put(name, new PublicProject(name, requiredContributions, reward));
    }

    public void contribute(Player player, String projectName, int amount) {
        PublicProject project = activeProjects.get(projectName);
        if (project == null) return;

        project.addContribution(player.getUniqueId(), amount);

        if (project.isComplete()) {
            distributeRewards(project);
        }
    }

    private void distributeRewards(PublicProject project) {
        Map<UUID, Integer> contributions = project.getContributions();
        int totalContributions = contributions.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<UUID, Integer> entry : contributions.entrySet()) {
            UUID playerUUID = entry.getKey();
            int contribution = entry.getValue();
            
            double share = (double) contribution / totalContributions;
            
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player != null) {
                player.sendMessage("§aProject completed! Your contribution: " + 
                    String.format("%.1f%%", share * 100));
            }
        }
    }

    public List<Player> identifyFreeRiders(String projectName) {
        if (!isEnabled()) return new ArrayList<>();
        
        PublicProject project = activeProjects.get(projectName);
        if (project == null) return new ArrayList<>();

        List<Player> freeRiders = new ArrayList<>();
        Map<UUID, Integer> contributions = project.getContributions();
        
        int avgContribution = contributions.values().stream()
            .mapToInt(Integer::intValue)
            .sum() / Math.max(1, contributions.size());

        for (Map.Entry<UUID, Integer> entry : contributions.entrySet()) {
            if (entry.getValue() < avgContribution * config.getFreeRiderThreshold()) {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null) {
                    freeRiders.add(player);
                }
            }
        }

        return freeRiders;
    }

    public double getContributionBonus(Player player, String projectName) {
        if (!isEnabled()) return 0.0;
        
        PublicProject project = activeProjects.get(projectName);
        if (project == null) return 0.0;

        int playerContribution = project.getContributions().getOrDefault(player.getUniqueId(), 0);
        int totalContributions = project.getContributions().values().stream()
            .mapToInt(Integer::intValue).sum();

        if (totalContributions == 0) return 0.0;

        double share = (double) playerContribution / totalContributions;
        return Math.min(share * config.getContributionBonusMultiplier(), config.getContributionBonusMax());
    }

    public static class PublicProject {
        private final String name;
        private final int requiredContributions;
        private final String reward;
        private final Map<UUID, Integer> contributions;

        public PublicProject(String name, int requiredContributions, String reward) {
            this.name = name;
            this.requiredContributions = requiredContributions;
            this.reward = reward;
            this.contributions = new HashMap<>();
        }

        public void addContribution(UUID player, int amount) {
            contributions.merge(player, amount, Integer::sum);
        }

        public boolean isComplete() {
            int total = contributions.values().stream().mapToInt(Integer::intValue).sum();
            return total >= requiredContributions;
        }

        public Map<UUID, Integer> getContributions() {
            return new HashMap<>(contributions);
        }

        public String getName() {
            return name;
        }
    }
}
