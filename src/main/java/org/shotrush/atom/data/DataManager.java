package org.shotrush.atom.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.shotrush.atom.Atom;
import org.shotrush.atom.skill.PlayerSkillData;
import org.shotrush.atom.skill.SkillData;
import org.shotrush.atom.skill.SkillType;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final Atom plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    
    public DataManager(Atom plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadData();
    }
    
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void saveAllData() {
        for (Map.Entry<UUID, PlayerSkillData> entry : plugin.getSkillManager().getAllPlayerData().entrySet()) {
            savePlayerData(entry.getKey(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void loadAllData() {
        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                loadPlayerData(playerId);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in data file: " + key);
            }
        }
    }
    
    private void savePlayerData(UUID playerId, PlayerSkillData playerData) {
        String basePath = playerId.toString();
        
        for (SkillType type : SkillType.values()) {
            SkillData skillData = playerData.getSkillData(type);
            String skillPath = basePath + "." + type.name();
            
            for (Map.Entry<String, Integer> entry : skillData.getAllExperience().entrySet()) {
                String itemKey = entry.getKey();
                dataConfig.set(skillPath + ".experience." + itemKey, entry.getValue());
                dataConfig.set(skillPath + ".repetitions." + itemKey, skillData.getRepetitionCount(itemKey));
                long lastTime = skillData.getLastActionTime(itemKey);
                if (lastTime > 0) {
                    dataConfig.set(skillPath + ".lastAction." + itemKey, lastTime);
                }
            }
        }
    }
    
    private void loadPlayerData(UUID playerId) {
        String basePath = playerId.toString();
        PlayerSkillData playerData = plugin.getSkillManager().getPlayerData(playerId);
        
        ConfigurationSection playerSection = dataConfig.getConfigurationSection(basePath);
        if (playerSection == null) {
            return;
        }
        
        for (SkillType type : SkillType.values()) {
            ConfigurationSection skillSection = playerSection.getConfigurationSection(type.name());
            if (skillSection == null) {
                continue;
            }
            
            SkillData skillData = playerData.getSkillData(type);
            
            ConfigurationSection expSection = skillSection.getConfigurationSection("experience");
            if (expSection != null) {
                for (String itemKey : expSection.getKeys(false)) {
                    int exp = expSection.getInt(itemKey);
                    skillData.setExperience(itemKey, exp);
                }
            }
            
            ConfigurationSection repsSection = skillSection.getConfigurationSection("repetitions");
            if (repsSection != null) {
                for (String itemKey : repsSection.getKeys(false)) {
                    int reps = repsSection.getInt(itemKey);
                    skillData.setRepetitionCount(itemKey, reps);
                }
            }
            
            ConfigurationSection lastActionSection = skillSection.getConfigurationSection("lastAction");
            if (lastActionSection != null) {
                for (String itemKey : lastActionSection.getKeys(false)) {
                    long lastTime = lastActionSection.getLong(itemKey);
                    skillData.setLastActionTime(itemKey, lastTime);
                }
            }
        }
    }
    
    public void savePlayerData(UUID playerId) {
        PlayerSkillData playerData = plugin.getSkillManager().getPlayerData(playerId);
        savePlayerData(playerId, playerData);
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
