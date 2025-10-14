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
        
        if (config.contains("political.isBigMan")) {
            data.setBigMan(config.getBoolean("political.isBigMan"));
            data.setBigManScore(config.getDouble("political.bigManScore", 0.0));
            data.setBigManFollowers(config.getInt("political.bigManFollowers", 0));
            data.setBigManRedistributions(config.getInt("political.bigManRedistributions", 0));
            data.setAuthorityType(config.getString("political.authorityType", "NONE"));
            data.setLegitimacy(config.getDouble("political.legitimacy", 0.0));
        }
        
        if (config.contains("social.tradeNetwork")) {
            Map<UUID, Integer> tradeNetwork = new HashMap<>();
            for (String key : config.getConfigurationSection("social.tradeNetwork").getKeys(false)) {
                UUID partnerId = UUID.fromString(key);
                int count = config.getInt("social.tradeNetwork." + key);
                tradeNetwork.put(partnerId, count);
            }
            data.setTradeNetwork(tradeNetwork);
        }
        
        if (config.contains("social.socialCapital")) {
            data.setSocialCapital(config.getDouble("social.socialCapital"));
        }
        
        if (config.contains("collective.projectContributions")) {
            Map<String, Integer> contributions = new HashMap<>();
            for (String key : config.getConfigurationSection("collective.projectContributions").getKeys(false)) {
                contributions.put(key, config.getInt("collective.projectContributions." + key));
            }
            data.setPublicProjectContributions(contributions);
        }
        
        if (config.contains("environmental.foodSurplus")) {
            data.setFoodSurplus(config.getDouble("environmental.foodSurplus"));
        }
        
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
        
        config.set("political.isBigMan", data.isBigMan());
        config.set("political.bigManScore", data.getBigManScore());
        config.set("political.bigManFollowers", data.getBigManFollowers());
        config.set("political.bigManRedistributions", data.getBigManRedistributions());
        config.set("political.authorityType", data.getAuthorityType());
        config.set("political.legitimacy", data.getLegitimacy());
        
        for (Map.Entry<UUID, Integer> entry : data.getTradeNetwork().entrySet()) {
            config.set("social.tradeNetwork." + entry.getKey().toString(), entry.getValue());
        }
        config.set("social.socialCapital", data.getSocialCapital());
        
        for (Map.Entry<String, Integer> entry : data.getPublicProjectContributions().entrySet()) {
            config.set("collective.projectContributions." + entry.getKey(), entry.getValue());
        }
        
        config.set("environmental.foodSurplus", data.getFoodSurplus());

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
