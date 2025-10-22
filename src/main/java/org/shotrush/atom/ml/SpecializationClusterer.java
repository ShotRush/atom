package org.shotrush.atom.ml;

import org.bukkit.plugin.Plugin;
import org.shotrush.atom.manager.PlayerDataManager;
import org.shotrush.atom.model.PlayerSkillData;
import org.shotrush.atom.model.SkillNode;
import org.shotrush.atom.tree.Trees.SkillTree;
import org.shotrush.atom.tree.Trees.Registry;

import java.util.*;
import java.util.stream.Collectors;


public final class SpecializationClusterer {
    
    private final Plugin plugin;
    private final Registry treeRegistry;
    private final PlayerDataManager dataManager;
    private final int k;
    private final int maxIterations;
    private final double convergenceThreshold;
    
    private List<String> leafNodeIds;
    private List<Cluster> clusters;
    private Map<UUID, Integer> playerClusterAssignments;
    
    public SpecializationClusterer(
        Plugin plugin,
        Registry treeRegistry,
        PlayerDataManager dataManager,
        int k
    ) {
        this.plugin = plugin;
        this.treeRegistry = treeRegistry;
        this.dataManager = dataManager;
        this.k = k;
        this.maxIterations = 100;
        this.convergenceThreshold = 0.001;
        this.clusters = new ArrayList<>();
        this.playerClusterAssignments = new HashMap<>();
        
        initializeLeafNodes();
    }
    
    private void initializeLeafNodes() {
        Set<String> leafIds = new HashSet<>();
        
        for (SkillTree tree : treeRegistry.getAllTrees()) {
            collectLeafNodes(tree.root(), leafIds);
        }
        
        this.leafNodeIds = new ArrayList<>(leafIds);
        Collections.sort(this.leafNodeIds);
        
        plugin.getLogger().info("Initialized clustering with " + leafNodeIds.size() + " leaf nodes");
    }
    
    private void collectLeafNodes(SkillNode node, Set<String> accumulator) {
        if (node.isLeaf()) {
            accumulator.add(node.id());
        }
        
        for (SkillNode child : node.children().values()) {
            collectLeafNodes(child, accumulator);
        }
    }
    
    public void performClustering() {
        Collection<PlayerSkillData> allPlayers = dataManager.getAllCachedData();
        
        if (allPlayers.size() < k) {
            plugin.getLogger().warning("Not enough players (" + allPlayers.size() + ") for k=" + k + " clustering");
            return;
        }
        
        List<PlayerVector> playerVectors = allPlayers.stream()
            .map(this::createNormalizedVector)
            .filter(pv -> pv.magnitude() > 0)
            .collect(Collectors.toList());
        
        if (playerVectors.size() < k) {
            plugin.getLogger().warning("Not enough active players with XP for clustering");
            return;
        }
        
        initializeCentroids(playerVectors);
        
        boolean converged = false;
        int iteration = 0;
        
        while (!converged && iteration < maxIterations) {
            assignPlayersToClusters(playerVectors);
            
            List<double[]> oldCentroids = clusters.stream()
                .map(c -> Arrays.copyOf(c.centroid, c.centroid.length))
                .collect(Collectors.toList());
            
            updateCentroids(playerVectors);
            
            converged = hasConverged(oldCentroids);
            iteration++;
        }
        
        labelClusters();
        
        plugin.getLogger().info("K-means clustering completed in " + iteration + " iterations");
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            plugin.getLogger().info("  Cluster " + i + " (" + cluster.label + "): " + 
                cluster.members.size() + " players");
        }
    }
    
    private PlayerVector createNormalizedVector(PlayerSkillData playerData) {
        double[] vector = new double[leafNodeIds.size()];
        
        for (int i = 0; i < leafNodeIds.size(); i++) {
            String leafId = leafNodeIds.get(i);
            long xp = playerData.getIntrinsicXp(leafId);
            vector[i] = xp;
        }
        
        double magnitude = calculateMagnitude(vector);
        
        if (magnitude > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= magnitude;
            }
        }
        
        return new PlayerVector(playerData.playerId(), vector);
    }
    
    private double calculateMagnitude(double[] vector) {
        double sum = 0;
        for (double v : vector) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }
    
    private void initializeCentroids(List<PlayerVector> playerVectors) {
        clusters.clear();
        Random random = new Random();
        
        PlayerVector firstCentroid = playerVectors.get(random.nextInt(playerVectors.size()));
        clusters.add(new Cluster(Arrays.copyOf(firstCentroid.vector, firstCentroid.vector.length)));
        
        for (int i = 1; i < k; i++) {
            double[] distances = new double[playerVectors.size()];
            double totalDistance = 0;
            
            for (int j = 0; j < playerVectors.size(); j++) {
                double minDist = Double.MAX_VALUE;
                for (Cluster cluster : clusters) {
                    double dist = euclideanDistance(playerVectors.get(j).vector, cluster.centroid);
                    minDist = Math.min(minDist, dist);
                }
                distances[j] = minDist * minDist;
                totalDistance += distances[j];
            }
            
            double r = random.nextDouble() * totalDistance;
            double cumulative = 0;
            int selectedIndex = 0;
            
            for (int j = 0; j < distances.length; j++) {
                cumulative += distances[j];
                if (cumulative >= r) {
                    selectedIndex = j;
                    break;
                }
            }
            
            PlayerVector newCentroid = playerVectors.get(selectedIndex);
            clusters.add(new Cluster(Arrays.copyOf(newCentroid.vector, newCentroid.vector.length)));
        }
    }
    
    private void assignPlayersToClusters(List<PlayerVector> playerVectors) {
        for (Cluster cluster : clusters) {
            cluster.members.clear();
        }
        
        playerClusterAssignments.clear();
        
        for (PlayerVector pv : playerVectors) {
            int closestCluster = 0;
            double minDistance = Double.MAX_VALUE;
            
            for (int i = 0; i < clusters.size(); i++) {
                double distance = euclideanDistance(pv.vector, clusters.get(i).centroid);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestCluster = i;
                }
            }
            
            clusters.get(closestCluster).members.add(pv.playerId);
            playerClusterAssignments.put(pv.playerId, closestCluster);
        }
    }
    
    private void updateCentroids(List<PlayerVector> playerVectors) {
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            
            if (cluster.members.isEmpty()) {
                continue;
            }
            
            double[] newCentroid = new double[leafNodeIds.size()];
            
            for (UUID playerId : cluster.members) {
                PlayerVector pv = playerVectors.stream()
                    .filter(p -> p.playerId.equals(playerId))
                    .findFirst()
                    .orElse(null);
                
                if (pv != null) {
                    for (int j = 0; j < newCentroid.length; j++) {
                        newCentroid[j] += pv.vector[j];
                    }
                }
            }
            
            for (int j = 0; j < newCentroid.length; j++) {
                newCentroid[j] /= cluster.members.size();
            }
            
            cluster.centroid = newCentroid;
        }
    }
    
    private boolean hasConverged(List<double[]> oldCentroids) {
        for (int i = 0; i < clusters.size(); i++) {
            double distance = euclideanDistance(oldCentroids.get(i), clusters.get(i).centroid);
            if (distance > convergenceThreshold) {
                return false;
            }
        }
        return true;
    }
    
    private double euclideanDistance(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            double diff = v1[i] - v2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
    
    private void labelClusters() {
        for (int clusterIndex = 0; clusterIndex < clusters.size(); clusterIndex++) {
            Cluster cluster = clusters.get(clusterIndex);
            List<SkillWeight> topSkills = new ArrayList<>();
            
            for (int i = 0; i < cluster.centroid.length; i++) {
                if (cluster.centroid[i] > 0.01) {
                    topSkills.add(new SkillWeight(leafNodeIds.get(i), cluster.centroid[i]));
                }
            }
            
            topSkills.sort((a, b) -> Double.compare(b.weight, a.weight));
            
            if (topSkills.isEmpty()) {
                cluster.label = "Cluster " + clusterIndex;
                cluster.topSkills = new ArrayList<>();
                continue;
            }
            
            Set<String> dominantCategories = topSkills.stream()
                .limit(5)
                .map(sw -> extractCategory(sw.skillId))
                .filter(cat -> !cat.equals("unknown"))
                .collect(Collectors.toSet());
            
            if (dominantCategories.isEmpty()) {
                String topSkill = topSkills.get(0).skillId;
                cluster.label = formatSkillName(topSkill);
            } else if (dominantCategories.size() == 1) {
                cluster.label = capitalize(dominantCategories.iterator().next()) + " Specialist";
            } else if (dominantCategories.size() == 2) {
                List<String> cats = new ArrayList<>(dominantCategories);
                cluster.label = capitalize(cats.get(0)) + "-" + capitalize(cats.get(1)) + " Hybrid";
            } else {
                cluster.label = "Multi-Specialist";
            }
            
            cluster.topSkills = topSkills.stream()
                .limit(5)
                .map(sw -> sw.skillId)
                .collect(Collectors.toList());
        }
    }
    
    private String extractCategory(String skillId) {
        String[] parts = skillId.split("\\.");
        return parts.length > 0 ? parts[0] : "unknown";
    }
    
    private String formatSkillName(String skillId) {
        String[] parts = skillId.split("\\.");
        String lastPart = parts[parts.length - 1];
        return Arrays.stream(lastPart.split("_"))
            .map(this::capitalize)
            .collect(Collectors.joining(" "));
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    public Optional<Specialization> getPlayerSpecialization(UUID playerId) {
        Integer clusterIndex = playerClusterAssignments.get(playerId);
        if (clusterIndex == null || clusterIndex >= clusters.size()) {
            return Optional.empty();
        }
        
        Cluster cluster = clusters.get(clusterIndex);
        return Optional.of(new Specialization(
            cluster.label,
            clusterIndex,
            cluster.topSkills,
            cluster.members.size()
        ));
    }
    
    public List<Cluster> getClusters() {
        return new ArrayList<>(clusters);
    }
    
    private record PlayerVector(UUID playerId, double[] vector) {
        double magnitude() {
            double sum = 0;
            for (double v : vector) {
                sum += v * v;
            }
            return Math.sqrt(sum);
        }
    }
    
    private record SkillWeight(String skillId, double weight) {}
    
    public static final class Cluster {
        double[] centroid;
        List<UUID> members;
        String label;
        List<String> topSkills;
        
        Cluster(double[] centroid) {
            this.centroid = centroid;
            this.members = new ArrayList<>();
            this.label = "Unknown";
            this.topSkills = new ArrayList<>();
        }
        
        public double[] getCentroid() {
            return Arrays.copyOf(centroid, centroid.length);
        }
        
        public List<UUID> getMembers() {
            return new ArrayList<>(members);
        }
        
        public String getLabel() {
            return label;
        }
        
        public List<String> getTopSkills() {
            return new ArrayList<>(topSkills);
        }
    }
    
    public record Specialization(
        String label,
        int clusterId,
        List<String> topSkills,
        int clusterSize
    ) {}
}
