package org.shotrush.atom.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.shotrush.atom.Atom;
import org.shotrush.atom.model.PlayerData;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final Atom plugin;
    private final Map<UUID, PlayerData> playerDataCache;
    private final File dataFolder;

    public PlayerDataManager(Atom plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData getPlayerData(UUID playerId) {
        return playerDataCache.computeIfAbsent(playerId, this::loadPlayerData);
    }

    private PlayerData loadPlayerData(UUID playerId) {
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        if (!playerFile.exists()) {
            return new PlayerData(playerId);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        Map<String, Double> actionExperience = new HashMap<>();
        Set<String> completedMilestones = new HashSet<>();

        if (config.contains("actions")) {
            for (String actionId : config.getConfigurationSection("actions").getKeys(false)) {
                actionExperience.put(actionId, config.getDouble("actions." + actionId));
            }
        }

        if (config.contains("milestones")) {
            completedMilestones.addAll(config.getStringList("milestones"));
        }

        PlayerData data = new PlayerData(playerId, actionExperience, completedMilestones);
        
        data.setModified(false);
        return data;
    }

    public void savePlayerData(UUID playerId) {
        PlayerData data = playerDataCache.get(playerId);
        if (data == null || !data.isModified()) return;

        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Double> entry : data.getActionExperience().entrySet()) {
            config.set("actions." + entry.getKey(), entry.getValue());
        }

        config.set("milestones", new ArrayList<>(data.getCompletedMilestones()));

        try {
            config.save(playerFile);
            data.setModified(false);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + playerId + ": " + e.getMessage());
        }
    }

    public void saveAllPlayerData() {
        for (UUID playerId : playerDataCache.keySet()) {
            savePlayerData(playerId);
        }
    }

    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        playerDataCache.remove(playerId);
    }

    public void clearCache() {
        saveAllPlayerData();
        playerDataCache.clear();
    }
}
