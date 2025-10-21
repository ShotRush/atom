package org.shotrush.atom.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.config.TreeDefinition;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class TreeStorage {
    
    private final Plugin plugin;
    private final Path treesDirectory;
    private final Gson gson;
    
    public TreeStorage(Plugin plugin) {
        this.plugin = plugin;
        this.treesDirectory = plugin.getDataFolder().toPath().resolve("trees");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        initializeDirectory();
    }
    
    private void initializeDirectory() {
        try {
            if (!Files.exists(treesDirectory)) {
                Files.createDirectories(treesDirectory);
                plugin.getLogger().info("Created trees directory: " + treesDirectory);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create trees directory: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public CompletableFuture<Void> saveTree(String treeName, TreeDefinition tree) {
        return CompletableFuture.runAsync(() -> {
            File treeFile = treesDirectory.resolve(treeName + ".json").toFile();
            
            try (FileWriter writer = new FileWriter(treeFile)) {
                gson.toJson(tree, writer);
                plugin.getLogger().info("Saved tree: " + treeName + " to " + treeFile.getName());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save tree " + treeName + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public CompletableFuture<Optional<TreeDefinition>> loadTree(String treeName) {
        return CompletableFuture.supplyAsync(() -> {
            File treeFile = treesDirectory.resolve(treeName + ".json").toFile();
            
            if (!treeFile.exists()) {
                plugin.getLogger().info("Tree file not found: " + treeName + ".json");
                return Optional.empty();
            }
            
            try (FileReader reader = new FileReader(treeFile)) {
                TreeDefinition tree = gson.fromJson(reader, TreeDefinition.class);
                plugin.getLogger().info("Loaded tree: " + treeName + " from " + treeFile.getName());
                return Optional.of(tree);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load tree " + treeName + ": " + e.getMessage());
                e.printStackTrace();
                return Optional.empty();
            }
        });
    }
    
    public CompletableFuture<Map<String, TreeDefinition>> loadAllTrees() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, TreeDefinition> trees = new HashMap<>();
            
            File[] files = treesDirectory.toFile().listFiles((dir, name) -> name.endsWith(".json"));
            if (files == null || files.length == 0) {
                plugin.getLogger().info("No saved trees found");
                return trees;
            }
            
            for (File file : files) {
                String treeName = file.getName().replace(".json", "");
                
                try (FileReader reader = new FileReader(file)) {
                    TreeDefinition tree = gson.fromJson(reader, TreeDefinition.class);
                    trees.put(treeName, tree);
                    plugin.getLogger().info("Loaded tree: " + treeName);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to load tree from " + file.getName() + ": " + e.getMessage());
                }
            }
            
            plugin.getLogger().info("Loaded " + trees.size() + " trees from disk");
            return trees;
        });
    }
    
    public CompletableFuture<Void> saveDynamicBranches(String rootClusterId, List<TreeDefinition.NodeDefinition> branches) {
        return CompletableFuture.runAsync(() -> {
            File branchFile = treesDirectory.resolve("branches_" + rootClusterId + ".json").toFile();
            
            try (FileWriter writer = new FileWriter(branchFile)) {
                gson.toJson(branches, writer);
                plugin.getLogger().info("Saved dynamic branches for: " + rootClusterId);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save branches for " + rootClusterId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public CompletableFuture<Optional<List<TreeDefinition.NodeDefinition>>> loadDynamicBranches(String rootClusterId) {
        return CompletableFuture.supplyAsync(() -> {
            File branchFile = treesDirectory.resolve("branches_" + rootClusterId + ".json").toFile();
            
            if (!branchFile.exists()) {
                return Optional.empty();
            }
            
            try (FileReader reader = new FileReader(branchFile)) {
                TreeDefinition.NodeDefinition[] branches = gson.fromJson(reader, TreeDefinition.NodeDefinition[].class);
                plugin.getLogger().info("Loaded dynamic branches for: " + rootClusterId);
                return Optional.of(Arrays.asList(branches));
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load branches for " + rootClusterId + ": " + e.getMessage());
                return Optional.empty();
            }
        });
    }
    
    public CompletableFuture<Void> saveTreeMetadata(String treeName, Map<String, Object> metadata) {
        return CompletableFuture.runAsync(() -> {
            File metaFile = treesDirectory.resolve(treeName + "_meta.json").toFile();
            
            try (FileWriter writer = new FileWriter(metaFile)) {
                gson.toJson(metadata, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save metadata for " + treeName + ": " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Optional<Map<String, Object>>> loadTreeMetadata(String treeName) {
        return CompletableFuture.supplyAsync(() -> {
            File metaFile = treesDirectory.resolve(treeName + "_meta.json").toFile();
            
            if (!metaFile.exists()) {
                return Optional.empty();
            }
            
            try (FileReader reader = new FileReader(metaFile)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = gson.fromJson(reader, Map.class);
                return Optional.of(metadata);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load metadata for " + treeName + ": " + e.getMessage());
                return Optional.empty();
            }
        });
    }
    
    public boolean treeExists(String treeName) {
        return treesDirectory.resolve(treeName + ".json").toFile().exists();
    }
    
    public CompletableFuture<Void> deleteTree(String treeName) {
        return CompletableFuture.runAsync(() -> {
            File treeFile = treesDirectory.resolve(treeName + ".json").toFile();
            File metaFile = treesDirectory.resolve(treeName + "_meta.json").toFile();
            
            if (treeFile.exists()) {
                treeFile.delete();
                plugin.getLogger().info("Deleted tree: " + treeName);
            }
            
            if (metaFile.exists()) {
                metaFile.delete();
            }
        });
    }
    
    public List<String> listSavedTrees() {
        File[] files = treesDirectory.toFile().listFiles((dir, name) -> 
            name.endsWith(".json") && !name.endsWith("_meta.json") && !name.startsWith("branches_")
        );
        
        if (files == null) {
            return List.of();
        }
        
        return Arrays.stream(files)
            .map(f -> f.getName().replace(".json", ""))
            .toList();
    }
}
